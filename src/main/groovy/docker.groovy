/*
 *  Jenkins Pipeline Goodness
 *
 *  Copyright (c) 2016 DoubleData Ltd. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * Execute command inside docker image
 *
 * @param Id Docker container Id
 * @param args Execution arguments
 */
int exec(Id, String... args) {
    def exitCode = sh(returnStatus: true, script: "docker exec -t ${Id} '" + args.join("' '") + "'")
    if (exitCode != 0)
        error("Unable to execute docker.exec(${Id}, '${args}'), exit code ${exitCode}")
    exitCode
}
/**
 * Build a docker image, tag it, and push to repo
 *
 * @param name Image name
 * @param base Base build tag (i.e. latest, release)
 * @param tag Special tag to push, i.e. 0.1.2 or env.BUILD_NUMBER
 * @param repo Docker repository
 */
void imageBuildPush(name, base, tag, repo) {
  stage "Building docker image ${name}:${base}"
  sh "docker build -t ${name}:${base} ."

  stage "Push to ${repo} with tag ${base}"
  sh "docker tag ${name}:${base} ${repo}/${name}:${base}"
  sh "docker push ${repo}/${name}:${base}"

  stage "Push to ${repo} with tag ${tag}"
  sh "docker tag ${name}:${base} ${repo}/${name}:${tag}"
  sh "docker push ${repo}/${name}:${tag}"
}
/**
 * Run a docker image, wait the TCP port to be ready, return container Id
 *
 * @param name Image name
 * @param tag Image tag, i.e. 0.1.2 or latest
 * @param port TCP port waiting for
 * @param options Docker options
 * @param repo Docker repository
 * @param timeoutSeconds Timeout waiting for TCP port readiness
 */
String run(name, tag, port, options = "", repo = "", int timeoutSeconds = 5*60) {
  def dockerIdFile = temporaryFile("dockerId", "", false).toString()
  if (repo?.trim()) {
    sh "docker run -d ${options} ${repo}/${name}:${tag} > ${dockerIdFile}"
  } else {
    sh "docker run -d ${options} ${name}:${tag} > ${dockerIdFile}"
  }
  def dockerId = readFile(dockerIdFile).trim()
  echo "Start container with docker ID: ${dockerId}"
  // Wait for container deployment
  timeout(time: timeoutSeconds, unit: 'SECONDS') {
    waitUntil {
      sleep 5L //poll only every 5 seconds
      def flagFile = temporaryFile("dockerIdListen", "-${port}", false).toString()
      sh "docker exec -t ${dockerId} /bin/sh -c 'netstat -apn | grep -e \"${port}.*LISTEN\" >/dev/null' && touch ${flagFile}; true"
      if (fileExists(flagFile)) {
        echo "${name}:${tag} is listening on port ${port}"
        return true
      } else {
        echo "${name}:${tag} is not listening on port ${port} yet"
        return false
      }
    }
  }
  return dockerId
}
/**
 * Remove image from repo
 *
 * http://stackoverflow.com/questions/33527782/delete-docker-image-from-remote-repo
 * curl -X DELETE registry-url/v1/repositories/repository-name/
 * https://github.com/docker/docker/issues/17304
 *
 * @param name Image name
 * @param tag Image tag, i.e. 0.1.2 or latest
 * @param repo Docker repository
 */
@NonCPS
void removeImage(name, tag, repo="", boolean alwaysTrue = true) {
    def dockerImagesIdFile = temporaryFile("dockerImagesId", "", false).toString()
    if (repo?.trim()) {
        sh "docker images ${repo}/${name}:${tag} | tail -n +2 | awk '{ print \$1 \":\" \$2}' > ${dockerImagesIdFile}"
    } else {
        sh "docker images ${name}:${tag} | tail -n +2 | awk '{ print \$1 \":\" \$2}' > ${dockerImagesIdFile}"
    }
    String dockerImagesId = readFile(dockerImagesIdFile).trim()
    dockerImagesId.eachLine { dockerImageId, count ->
        echo("Remove image " + dockerImageId)
        // TODO: add PKI auth
        //if (repo?.trim()) {
        //  sh "curl -X DELETE registry-url/v1/repositories/repository-name/"
        //}
        if (alwaysTrue) {
            sh "docker rmi ${dockerImageId}; true"
        } else {
            sh "docker rmi ${dockerImageId}"
        }
    }
}
/**
 * Stop docker container.
 * 
 * @param Id Docker container Id
 */
void stopById(Id, boolean alwaysTrue = true) {
    if (alwaysTrue) {
        sh "docker stop ${Id}; true"
        sh "docker rm ${Id}; true"
    } else {
        sh "docker stop ${Id}"
        sh "docker rm ${Id}"
    }
}
/**
 * Read LABEL version="n.n.n" from Dockerfile
 */
String readVersion(content) {
  def matcher = (content =~ /\s*LABEL\s+version\s*=\s*"(.+)"/)
  matcher ? matcher[0][1] : error("Unable to find LABEL version=... in Dockerfile")
}

File temporaryFile(String prefix, String suffix, Boolean create) {
  def file = File.createTempFile(prefix, suffix)
  file.deleteOnExit()
  if (!create) {
    file.delete()
  }
  return file
}

return this;
