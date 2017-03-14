/*
 *  Jenkins Pipeline Goodness
 *
 *  Copyright (c) 2017 DoubleData Ltd. All Rights Reserved.
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
 * Make a copy of a git repository with GitSCM
 *
 * @param branch remoteBranch repository branch name
 * @param branch localBranch repository branch name
 * @param path local path
 * @param url repository uri
 */
void shallowClone(String remoteBranch, String localBranch, String path, String url, int depth) {
    shallowClone(remoteBranch, localBranch, path, [name: 'origin', url: url], depth)
}
/**
 * Make a copy of a git repository with GitSCM
 *
 * @param branch remoteBranch repository branch name
 * @param branch localBranch repository branch name
 * @param path local path
 * @param userRemoteConfigs remote settings
 */
void shallowClone(String remoteBranch, String localBranch, String path, Map userRemoteConfigs, int depth) {
    shallowClone(remoteBranch, localBranch, path, [userRemoteConfigs], depth)
}
/**
 * Make a copy of a git repository with GitSCM
 *
 * @param branch remoteBranch repository branch name
 * @param branch localBranch repository branch name
 * @param path local path
 * @param userRemoteConfigs remote settings
 */
void shallowClone(String remoteBranch, String localBranch, String path, List userRemoteConfigs, int depth) {
    checkout([$class                     : 'GitSCM', branches: [[name: remoteBranch]],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [[$class           : 'RelativeTargetDirectory', relativeTargetDir: path],
                                                  [$class           : 'CloneOption', depth: depth, noTags: false, reference: '', shallow: true],
                                                  [$class           : 'LocalBranch', localBranch: localBranch],
                                                  [$class           : 'CleanCheckout']],
              submoduleCfg                     : [],
              userRemoteConfigs                : userRemoteConfigs])
}

return this;
