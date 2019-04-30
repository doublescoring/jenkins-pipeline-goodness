import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
 * Wrap groovy block with saltstack environment
 *
 * @param modelPath Path to the model of the saltstack
 * @param controllerPath Path to the saltstack customization controller
 * @param closure Code block with saltstack
 */
void withSalt(String modelPath, String controllerPath, closure) {
    def modelFile = new File(env.WORKSPACE + "/" + modelPath)
    if (!modelFile.isDirectory())
        error("Salt model not found at " + modelFile.getAbsolutePath())
    def controllerFile = new File(env.WORKSPACE + "/" + controllerPath)
    if (!controllerFile.isDirectory())
        error("Salt controller not found at " + controllerFile.getAbsolutePath())
    if (env.PYTHONPATH?.trim())
        withEnv(["PYTHONPATH=" + controllerFile.getAbsolutePath() + ':' + env.PYTHONPATH,
                 "SALT_MODEL_DIR=" + modelFile.getAbsolutePath(),
                 "SALT_IN_ROOT_DIRS=api_logfile:api_pidfile:autoreject_file:autosign_file:cachedir:extension_modules:formula_path:key_logfile:log_file:pidfile:pillar_path:pki_dir:reactor_path:spm_build_dir:spm_cache_dir:spm_logfile:sqlite_queue_dir:syndic_dir:token_dir:token_dirpki_dir"], closure)
    else
        withEnv(["PYTHONPATH=" + controllerFile.getAbsolutePath(),
                 "SALT_MODEL_DIR=" + modelFile.getAbsolutePath(),
                 "SALT_IN_ROOT_DIRS=api_logfile:api_pidfile:autoreject_file:autosign_file:cachedir:extension_modules:formula_path:key_logfile:log_file:pidfile:pillar_path:pki_dir:reactor_path:spm_build_dir:spm_cache_dir:spm_logfile:sqlite_queue_dir:syndic_dir:token_dir:token_dirpki_dir"], closure)
}

/**
 * Wrap groovy block with saltstack environment
 *
 * @param environment Conda environment
 * @param closure Code block with conda
 */
void withConda(String environment, closure) {
    if (env.CONDA_DEFAULT_ENV != environment) {
        def command = "set +xv; source activate '${environment}'; set -xv"
        if (env.JENKINS_PIPELINE_GOODNESS_DEBUG)
            command = "source activate '${environment}'"
        importFromShell(command, "CONDA_DEFAULT_ENV", "CONDA_PATH_BACKUP",
                "CONDA_PREFIX", "MSYS2_ENV_CONV_EXCL", "PATH")
    }
    withEnv(["CONDA_DEFAULT_ENV=" + env_CONDA_DEFAULT_ENV, "CONDA_PATH_BACKUP=" + env_CONDA_PATH_BACKUP,
            "CONDA_PREFIX=" + env_CONDA_PREFIX, "MSYS2_ENV_CONV_EXCL=" + env_MSYS2_ENV_CONV_EXCL, "PATH=" + env_PATH]) {
        sh "echo test conda environment; test \"\$CONDA_DEFAULT_ENV\" = \"${environment}\""
        closure()
    }
}

/**
 * Create a fresh Conda environment from a given specification.
 * @param namePrefix name prefix of environment being created.
 * @param condaRequirementsFilename Path to <a href="https://docs.conda.io/projects/conda/en/latest/user-guide/tasks/manage-environments.html#creating-an-environment-from-an-environment-yml-file">YAML specification</a> of the environment.
 * @return the name of created environment
 */
void createCondaEnvFromSpec(String namePrefix,
                    String condaRequirementsFilename) {
    name = namePrefix + "_" + DateTimeFormatter.ofPattern("yyyyMMdd_hhmmss").format(ZonedDateTime.now())
    echo "Create Conda env $name"
    createCode = sh script: "conda env create -n $name -f $condaRequirementsFilename",
            returnStatus: true
    if (createCode > 0) {
        echo "Failed to create env $name, exit code $createCode"
    }
    sh script: "conda list -n $name"
}

/**
 * Import environment variables to groovy from shell command via temporary file
 *
 * @param command Shell command line
 * @param args Environment variables to import
 */
void importFromShell(String command, String... args) {
    def shellScript = File.createTempFile("importShell", ".tmp")
    def groovyScript = File.createTempFile("importGroovy", ".tmp")
    shellScript.delete()
    groovyScript.delete()
    importFromShellWithFiles(shellScript, groovyScript, command, args)
    shellScript.delete()
    groovyScript.delete()
}

/**
 * Import environment variables to groovy from shell command via the specific file
 *
 * @param shellScript Temporary file with groovyScript generator
 * @param groovyScript Temporary file with list of imported variables
 * @param command Shell command line
 * @param args Environment variables to import
 */
void importFromShellWithFiles(File shellScript, File groovyScript, String command, String... args) {
    def exportCommands = "set -xv\n${command}\n"
    for (i = 0; i < args.size(); i++) { 
        def environmentVariable = args[i]
        if (i == 0)
            exportCommands = exportCommands + "echo \"env_${environmentVariable}='\$${environmentVariable}'\" > ${groovyScript.absolutePath}\n"
        else
            exportCommands = exportCommands + "echo \"env_${environmentVariable}='\$${environmentVariable}'\" >> ${groovyScript.absolutePath}\n"
    }
    dir(shellScript.getParentFile().getAbsolutePath()) {
        writeFile file: shellScript.getName(), text: exportCommands
    }
    sh("chmod +x ${shellScript.absolutePath}; ${shellScript.absolutePath}")
    if (env.JENKINS_PIPELINE_GOODNESS_DEBUG) echo("Groovy content:\n" + groovyScript.text)
    load(groovyScript.getAbsolutePath())
}

return this;
