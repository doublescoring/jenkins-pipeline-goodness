/*
 *  Jenkins Pipeline Goodness
 *
 *  Copyright (c) 2019 DoubleData Ltd. All Rights Reserved.
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



import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

class airflowTest {
    GroovyShell shell
    Binding binding
    Script script
    String scriptText = this.class.getResourceAsStream('testHeader.groovy').text + new File("src/main/groovy/airflow.groovy").text

    @Before
    void setUp() {
        binding = new Binding()
        binding.setVariable("fileExists", this.&fileExists)
        binding.setVariable("readFile", this.&readFile)
        binding.setVariable("sh", this.&sh)
        binding.setVariable("timeout", this.&timeout)
        binding.setVariable("waitUntil", this.&waitUntil)
        shell = new GroovyShell(binding)
        script = shell.parse(scriptText)
        // fix Jenkins sleep
        script.metaClass.static.sleep = { long ms -> Thread.sleep(ms * 1000) }
        binding.setVariable("script", script)
    }

    @After
    void tearDown() {
    }

    @Test
    void testAirflowMessageParsing() {
        def message = 'Created <DagRun test_dag @ 2019-05-07 14:13:49+00:00: jenkins-#36_pshuvalov, externally triggered: True>'
        def testDag = 'test_dag'
        def parsed = shell.evaluate('script.parseExecutionDate("' + message + '", "' + testDag + '")')
        assertEquals("2019-05-07T14:13:49", parsed)
    }
}
