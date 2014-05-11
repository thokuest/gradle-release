package release.helper


class CommandLine {
    private static final String LINE_SEP = System.getProperty('line.separator')
    private static final String PROMPT = "${LINE_SEP}??>"

    /**
     * Reads user input from the console.
     *
     * @param message Message to display
     * @param defaultValue (optional) default value to display
     * @return User input entered or default value if user enters no data
     */
    String readLine(String message, String defaultValue = null) {
        String fullMessage = "$PROMPT $message" + (defaultValue ? " [$defaultValue] " : "")

        if (System.console()) {
            return System.console().readLine(fullMessage) ?: defaultValue
        }

        println "$fullMessage (WAITING FOR INPUT BELOW)"

        return System.in.newReader().readLine() ?: defaultValue
    }

    boolean promptYesOrNo(String message, boolean defaultValue = false) {
        def defaultStr = defaultValue ? 'Y' : 'n'

        String consoleVal = readLine("${message} (Y|n)", defaultStr)

        if (consoleVal) {
            return consoleVal.toLowerCase().startsWith('y')
        }

        defaultValue
    }

    CommandLineLauncher launcher(Closure configuration) {
        CommandLineLauncher launcher = new CommandLineLauncher();
        launcher.with configuration

        return launcher
    }
}
