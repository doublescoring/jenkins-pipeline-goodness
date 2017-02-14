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

String getArrayBlock(List value, int level) {
    def output = ""
    for (i = 0; i < value.size(); i++) {
        if (Map.isAssignableFrom(value[i].getClass())) {
            output = output + "  " * (level + 1) + "-\n" + getMapBlock(value[i], level + 2)
        } else if ([Collection, Object[]].any { it.isAssignableFrom(value[i].getClass()) }) {
            output = output + "  " * level + "-\n" + getArrayBlock(value, level + 1)
        } else {
            output = output + "  " * (level + 1) + "- " + value[i] + "\n"
        }
    }
    return output
}

String getMapBlock(Map map, int level) {
    def output = ""
    def keys = map.keySet()
    def keysSize = keys.size()
    for (i = 0; i < keysSize; i++) {
        def saveI = i
        def value = map[keys[i]]
        if (Map.isAssignableFrom(value.getClass())) {
            output = output + "  " * level + keys[i] + ":\n" + getMapBlock(value, level + 1)
        } else if ([Collection, Object[]].any { it.isAssignableFrom(value.getClass()) }) {
            output = output + "  " * level + keys[i] + ":\n" + getArrayBlock(value, level + 1)
        } else {
            output = output + "  " * level + keys[i] + ":" + value
        }
        i = saveI
        output = output + "\n"
    }
    return output
}

return this;
