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
 * Execute salt-ssh and verify output
 */
void saltSSH(argument) {
    def output = sh(script: 'salt-ssh ' + argument, returnStdout: true).trim()
    echo(output)
    if (!saltSSHVerify(output))
        error("SaltStack failed")
}

/**
 * Verify salt-ssh output
 */
boolean saltSSHVerify(output) {
    def trimmed = output.trim()

    // Error state (python dependencies are absent)
    // dbl.dev.salt-test:
    // ----------
    // retcode:
    //     0
    // stderr:
    //    Connection to 34.202.157.113 closed.
    // stdout:
    def errorRegEx01 = /(?ms).*----------\n\s+retcode:\n\s+0\n\s+stderr:\n\s+Connection to .* closed.\n\s+stdout:$/

    // Errar state DEVOPS-230
    // dbl.dev.emails-box.staging:
    // ----------
    // retcode:
    //     0
    // stderr:
    // stdout:
    def errorRegEx02 = /(?ms).*----------\n\s+retcode:\n\s+0\n\s+stderr:\n\s+stdout:$/
    return !(trimmed ==~ errorRegEx01 || trimmed ==~ errorRegEx02)
}

/**
 * Generate salt configuration
 */
String generateConfig(Map map) {
    return getMapBlock(map, 0)
}

/**
 * Write salt configuration
 */
void writeConfig(String file, Map map) {
    writeFile encoding: 'UTF-8', file: file, text: generateConfig(map)
}

String getArrayBlock(List list, int level) {
    def output = ""
    def iValue = 0
    while (iValue < list.size()) {
        def value = list[iValue]
        if (Map.isAssignableFrom(value.getClass())) {
            output = output + "  " * (level + 1) + "-\n" + getMapBlock(value[i], level + 2)
        } else if (Collection.isAssignableFrom(value.getClass()) || Object[].isAssignableFrom(value.getClass())) {
            output = output + "  " * level + "-\n" + getArrayBlock(value, level + 1)
        } else {
            output = output + "  " * (level + 1) + "- '$value'\n"
        }
        iValue++
    }
    return output
}

String getMapBlock(Map map, int level) {
    def output = ""
    def keys = map.keySet()
    def iKey = 0
    while (iKey < keys.size()) {
        def key = keys[iKey]
        def value = map[key]
        if (Map.isAssignableFrom(value.getClass())) {
            output = output + "  " * level + "$key:\n" + getMapBlock(value, level + 1)
        } else if (Collection.isAssignableFrom(value.getClass()) || Object[].isAssignableFrom(value.getClass())) {
            output = output + "  " * level + "$key:\n" + getArrayBlock(value, level + 1)
        } else {
            output = output + "  " * level + "$key: " + value
        }
        output = output + "\n"
        iKey++
    }
    return output
}

return this;
