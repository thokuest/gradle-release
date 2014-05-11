package release.helper

import org.gradle.api.logging.Logging
import org.slf4j.Logger

class CommandLineLauncher {
    private Logger log = Logging.getLogger(getClass())

    List<String> commands = [];

    String errorPattern;
    String errorMessage;

    Map<String, String> environment = [:];

    String standardOutput;
    String errorOutput;

    private boolean executed;

    CommandLineLauncher execute() {
        def out = new StringBuilder()
        def err = new StringBuilder()

        def logMessage = joinCommands()

        log.info("Running $logMessage")

        Process process = commands.execute(createProcessingEnvironment(), null)
        process.waitForProcessOutput(out, err)

        this.standardOutput = out.toString().trim()
        this.errorOutput = err.toString().trim()

        debugCommandOutput();

        Validate.assertTrue(err.size() == 0, {
            errorMessage ?: "'$logMessage' produced an error: [${errorOutput}]"
        })

        Validate.assertTrue(process.exitValue() == 0, {
            errorMessage ?: "Failed to run '$logMessage' (exit value was: ${process.exitValue})"
        })

        Validate.assertTrue(!checkErrorPattern(), {
            errorMessage ?: "Failed to run '$logMessage'"
        })

        return this
    }

    private String[] createProcessingEnvironment() {
        def processEnv = [:] << System.getenv();
        processEnv << environment;

        return processEnv.collect { "$it.key=$it.value" } as String[]
    }

    private boolean checkErrorPattern() {
        if (!errorPattern) {
            return false;
        }

        return standardOutput.contains(errorPattern) || errorOutput.contains(errorPattern);
    }

    void debugCommandOutput() {
        if (log.infoEnabled) {
            log.info("command standard output: $standardOutput")
            log.info("command error output: $errorOutput")
        }
    }

    String joinCommands() {
        commands.join(' ')
    }

    String out() {
        executeOnce().standardOutput
    }

    private CommandLineLauncher executeOnce() {
        if (!executed) {
            execute()
            executed = true
        }

        return this
    }

    String err() {
        executeOnce().errorOutput
    }
}
