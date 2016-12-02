jenkins-pipeline-goodness [![Build Status](https://travis-ci.org/doublescoring/jenkins-pipeline-goodness.png?branch=master)](https://travis-ci.org/doublescoring/jenkins-pipeline-goodness)
=========================

Daily usage Jenkins Pipeline utilities covered by JUnit tests.

Basic usage
-----------

In your `Jenkninsfile`

```groovy
#!groovy

node {
    def docker
    String version

    stage("checkout code") {
        // checkout your code here or there ...

        // and then:
        checkout([$class                           : 'GitSCM',
                  extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                       relativeTargetDir: 'jenkins-pipeline-goodness'],
                                                      [$class           : 'CleanCheckout']],
                  userRemoteConfigs                : [[url: 'git@github.com:doublescoring/jenkins-pipeline-goodness.git']], // Jenkins Pipeline Goodness url
                  branches                         : [[name: 'refs/tags/1.1.1']]]) // Jenkins Pipeline Goodness version
    }
    docker = load "jenkins-pipeline-goodness/src/main/groovy/docker.groovy" // attach docker module
    try {
        stage("build image") {
            dir('path/to/your/dockerfile/') {
                def dockerFileContent = readFile('Dockerfile')
                version = docker.readVersion(dockerFileContent)
                echo "Found version in Dockerfile ${version}"
                docker.imageBuildPush("organization/name", version, "latest", "our-private-registry:12345")
            }
        }
    } finally {
        if (something_exists_that_we_must_clean) {
            docker.stopById(dockerContainerId);
            docker.removeImage("organization/name", version);
        }
    }
}
```

Utilities
---------

### [Docker](https://github.com/doublescoring/jenkins-pipeline-goodness/blob/master/src/main/groovy/docker.groovy)

* `exec` - Execute command inside docker image
* `imageBuildPush` - Build a docker image, tag it, and push to repo
* `run` - Run a docker image, wait the TCP port to be ready, return container Id
* `removeImage` - Remove image from repo
* `stopById` - Stop docker container.
* `readVersion` - Read LABEL version="n.n.n" from Dockerfile
* `temporaryFile` - Get temporary file


Versioning Guidelines
---------------------

This project follow [Apache rules of semantic versioning](https://commons.apache.org/releases/versioning.html). Use git tags of the current project.

CONTRIBUTORS
------------

* Alexey Aksenov

LICENSE
-------

The Jenkins Pipeline Goodness Project is licensed to you under the terms of
the Apache License, version 2.0, a copy of which has been included in the LICENSE file.
Please check the individual source files for details.

Copyright
---------

Copyright © 2016 DoubleData Ltd. All rights reserved.

<a href="https://www.apache.org/licenses/LICENSE-2.0"><img src="https://airflow.incubator.apache.org/_images/apache.jpg" width="140" alt="Apache License" title="Apache License"/></a>

