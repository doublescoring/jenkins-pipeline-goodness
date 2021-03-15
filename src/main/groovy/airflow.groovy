import groovy.json.JsonBuilder
import groovy.json.JsonSlurperClassic

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
/**
 * Trigger Airflow DAG
 * @param airflowUrl Airflow instance base URL
 * @param dagName DAG name (i.e. dag_id) to trigger
 * @param dagParams Params to be passed to the DAG
 * @param runId Optional run_id for Airflow
 * @return Map with two keys: executionDate (execution_date from Airflow) and dagUrl
 */
String trigger(String airflowUrl, String dagName, Map dagParams = [], String runId = null) {
    def runDagRequest = [conf: dagParams]
    if (runId != null) {
        runDagRequest << [run_id: runId]
    }
    def body = new JsonBuilder(runDagRequest).toString()
    echo("Sending request to airflow ${body}")
    def airflowApiUrl = "${airflowUrl}/api/v1/dags/${dagName}/dagRuns"
    def response = httpRequest url: airflowApiUrl, requestBody: body, httpMode: 'POST'
    echo("Response from Airflow: ${response.content}")
    assert(response.status == 200)
    def responseMessage = new JsonSlurperClassic().parseText(response.content)['message']
    // Here is
    executionDate = parseExecutionDate(responseMessage, dagName)
    echo("Got execution date ${executionDate}")
    dagUrl = "${airflowUrl}/admin/airflow/graph?dag_id=${dagName}&root=&execution_date=${executionDate}"
    return [executionDate: executionDate, dagUrl: dagUrl]
}

/**
 * Internal method - parse execution date from Airflow API respnse
 * @param message
 * @return
 */
String parseExecutionDate(String message, String dagName) {
    return message.split("${dagName} @ ")[1].replaceFirst(/\+.*/, "").replace(" ", "T")
}

/**
 * Return current state for DAG run
 * @param airflowUrl Airflow instance base URL
 * @param dagName DAG name (i.e. dag_id)
 * @param executionDate execution_date from trigger method invocation
 * @return current state
 */
String getState(String airflowUrl, String dagName, String executionDate) {
    def airflowApiUrl = "${airflowUrl}/api/experimental/dags/${dagName}/dag_runs/${executionDate}"
    def response = httpRequest url: airflowApiUrl
    assert(response.status == 200)
    return new JsonSlurperClassic().parseText(response.content)['state']
}

/**
 * Wait for DAG completion
 * @param airflowUrl Airflow instance base URL
 * @param dagName DAG name (i.e. dag_id)
 * @param executionDate execution_date from trigger method invocation
 * @param timeout Polling timeout in ms
 * @return nothing, block until DAG is not finished
 * @throws Exception if DAG is failed
 */
void waitFor(String airflowUrl, String dagName, String executionDate, long timeout = 5000) {
    while (true) {
        def currentState = getState(airflowUrl, dagName, executionDate)
        if (currentState == 'running') {
            echo("DAG ${dagName}@${executionDate} is still runnig")
            Thread.sleep(timeout)
        } else if (currentState == 'success') {
            echo("DAG is finished with success status")
            break
        } else {
            error("DAG ${dagName} is finished with wrong state: ${currentState}")
        }
    }
}

return this;
