/* $Id: Command.java,v 1.6 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.6 $
 * $Date: 2007/10/04 13:28:20 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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

    public Command (String name, String description) {
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
    public String getName () {
        return name;
    }

    public String getDescription () {
        return description;
    }

    public String getUsage () {
        return usage;
    }

    public void setUsage (String usage) {
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
     * @return {@code true} if the command ran succesfully, {@code false}
     *         otherwise.
     */
    public abstract void invoke (ShellContext ctx) throws Exception;

    /**
     * Called by the shell {@link Core} to retrieve command line options.
     * @return
     */
    Options getOptions () {
        return options;
    }

    /**
     * Set up the {@code Command} object with a set of command line options.
     * This method should be called before the {@link #invoke} method.
     *
     * @param cli the parsed command line to use on next invocation.
     * @param raw the raw command line entered by the user
     */
    void prepare (CommandLine cli, String raw) {
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
    protected boolean hasOption (String option) {
        return cli.hasOption(option);
    }

    /**
     * Get the value for a given switch.
     * @param option the long or short name of the switch
     * @return the value supplied on the command line or {@code null} if none
     *         is set.
     */
    protected String getOption (String option) {
        return cli.getOptionValue(option);
    }

    /**
     * Get any non-recognized or left over arguments
     * @return an array with the extra arguments
     */
    protected String[] getArguments () {
        return cli.getArgs();
    }

    protected String getRawCommandLine() {
        return raw;
    }

}
