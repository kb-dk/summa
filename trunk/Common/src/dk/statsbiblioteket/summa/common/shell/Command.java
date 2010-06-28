/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.common.shell;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public abstract class Command {

    private CommandLine cli;
    private Options options;
    private String raw;
    private String name;
    private String description;
    private String usage;
    private String[] aliases = new String[] {};

    /**
     * Constructs a command with a single command name.
     * @param name The command name.
     * @param description Command description.
     */
    public Command (String name, String description) {
        constructorHelper(name, description);
    }

    /**
     * Create a command with aliases.
     * @param name The command name
     * @param aliases The aliases if any.
     * @param description Command description.
     */
    public Command(String name, String description, String[] aliases) {
        constructorHelper(name, description);
        System.arraycopy(aliases, 0, this.aliases, 0, aliases.length - 1);
    }

    /**
     *  Helper method for the constructors.
     * @param name The command name.
     * @param description Command description.
     */
    private void constructorHelper(String name, String description) {
        options = new Options();
        this.name = name;
        this.description = description;
        cli = null;
        usage = name;
    }

    /**
     * Get the string used to invoke this command
     * @return the string used to invoke this command
     */
    public String getName() {
        return name;
    }

    public String[] getAliases() {
        return aliases;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    /**
     * <p>Invoke the command. All command line arguments are parsed
     * and set up. They can be retrieved or checked via the {@link #getOption}
     * and {@link #hasOption} methods.</p>
     *
     * <p>In case of errors the command should throw an exception</p>
     *
     * @param ctx Context used to print messages and retrieve user feedback
     * @throws Exception when running a command.
     */
    public abstract void invoke(ShellContext ctx) throws Exception;

    /**
     * Called by the shell {@link Core} to retrieve command line options.
     * @return
     */
    Options getOptions() {
        return options;
    }

    /**
     * Set up the {@code Command} object with a set of command line options.
     * This method should be called before the {@link #invoke} method.
     *
     * @param cli the parsed command line to use on next invocation.
     * @param raw the raw command line entered by the user
     */
    void prepare(CommandLine cli, String raw) {
        this.cli = cli;
        this.raw = raw;
    }

    /**
     * Called in the constructor of a {@code Command} implementation
     * to add a command line switch.
     * @param shortOpt
     * @param longOpt
     * @param needsValue
     * @param helpMsg
     */
    protected void installOption (String shortOpt, String longOpt,
                                           boolean needsValue, String helpMsg) {
        options.addOption(shortOpt, longOpt, needsValue, helpMsg);
    }

    /**
     * Check if a switch has been set on the command line
     * @param option the long or short name of the switch
     * @return true if the switch has been set
     */
    protected boolean hasOption(String option) {
        return cli.hasOption(option);
    }

    /**
     * Get the value for a given switch.
     * @param option the long or short name of the switch
     * @return the value supplied on the command line or {@code null} if none
     *         is set.
     */
    protected String getOption(String option) {
        return cli.getOptionValue(option);
    }

    /**
     * Get any non-recognized or left over arguments
     * @return an array with the extra arguments
     */
    protected String[] getArguments() {
        return cli.getArgs();
    }

    protected String getRawCommandLine() {
        return raw;
    }

}




