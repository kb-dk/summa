/* $Id: Core.java,v 1.7 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.7 $
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

import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;

import dk.statsbiblioteket.summa.common.shell.notifications.AbortNotification;
import dk.statsbiblioteket.summa.common.shell.notifications.BadCommandLineNotification;
import dk.statsbiblioteket.summa.common.shell.notifications.Notification;
import dk.statsbiblioteket.summa.common.shell.notifications.HelpNotification;
import dk.statsbiblioteket.summa.common.shell.notifications.TraceNotification;
import dk.statsbiblioteket.summa.common.shell.commands.Help;
import dk.statsbiblioteket.summa.common.shell.commands.Quit;
import dk.statsbiblioteket.summa.common.shell.commands.Trace;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A generic shell driver.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class Core {

    private BufferedReader in;
    private HashMap<String, Command> commands;
    private String lastTrace;
    private String header, prompt;
    private ShellContext shellCtx;
    private CommandLineParser cliParser;

    /**
     * Create a new shell {@code Core}.
     * @param withDefaultCommands if {@code true} install the default commands
     *                            found in the
     *                            {@link dk.statsbiblioteket.summa.common.shell.commands}
     *                            package.
     */
    public Core(boolean withDefaultCommands) {
        in = new BufferedReader (new InputStreamReader(System.in));
        cliParser = new PosixParser();
        commands = new HashMap<String,Command>();
        lastTrace = null;
        header = "Summa Generic Shell $Id: Core.java,v 1.7 2007/10/04 13:28:20 te Exp $";
        prompt = "summa-shell> ";

        shellCtx = new ShellContext () {

            public void error(String msg) {
                System.out.println ("[ERROR] " + msg);
            }

            public void info(String msg) {
                System.out.println (msg);
            }

            public void warn(String msg) {
                System.out.println ("[WARNING] " + msg);
            }

            public void debug(String msg) {
                System.out.println ("[DEBUG] " + msg);
            }

            public String readLine() {
                return waitForInput();
            }

            public void prompt (String prompt) {
                System.out.print(prompt);
            }
        };

        if (withDefaultCommands) {
            installCommand(new Help());
            installCommand(new Quit());
            installCommand(new Trace());
        }
    }

    /**
     * Create a new shell {@code Core} with the default command set found in
     * {@link dk.statsbiblioteket.summa.common.shell.commands}.
     */
    public Core () {
        this(true);
    }

    public void setHeader (String header) {
        this.header = header;
    }

    public String getHeader () {
        return header;
    }

    public void installCommand (Command cmd) {
        commands.put (cmd.getName(), cmd); 
    }

    public ShellContext getShellContext() {
        return shellCtx;
    }

    private String waitForInput () {
        try {
            return in.readLine().trim();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read input", e);
        }
    }

    public String getPrompt () {
        return prompt;
    }

    public void setPrompt (String prompt) {
        this.prompt = prompt;
    }

    /**
     * A main iteration of the core
     * @throws Exception if an error happens inside the running {@link Command}
     * @throws ParseException if there is an error parsing the command line
     *                        as entered by the user.
     */
    private void do_main_iteration () throws Exception {
        String cmdString;
        String[] tokens;
        Command cmd;

        shellCtx.prompt(getPrompt());

        cmdString = waitForInput ();
        if ("".equals(cmdString)) {
            return;
        }

        tokens = tokenize(cmdString);
        cmd = commands.get (tokens[0]);

        if (cmd == null) {
            shellCtx.error("No such command '" + cmdString +"'");
            return;
        }

        String[] args = new String[tokens.length-1];
        for (int i = 1; i < tokens.length; i++) {
            args[i-1] = tokens[i];
        }

        CommandLine cli = cliParser.parse (cmd.getOptions(), args);
        cmd.prepare(cli);
        cmd.invoke (shellCtx);
    }

    /**
     * Return the string tokized by white space
     *
     * FIXME: Handle quoted string with spaces as one token
     *
     * @param in the string to tokenize
     * @return the string tokens as separated by white space
     */
    private String[] tokenize (String in) {
        StringTokenizer tknzr = new StringTokenizer(in, " ");
        String[] tokens = new String[tknzr.countTokens()];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tknzr.nextToken();
        }

        return tokens;
    }

    private void handleHelpNotification (HelpNotification help) {
        String target = help.getTargetCommand();
        if (target == null) {
            printCommands();
        } else {
            if (commands.containsKey(target)) {
                Command cmd = commands.get(target);
                printHelp (cmd, cmd.getDescription());
            } else {
                shellCtx.error ("No such command: '" + target
                              + "'.\nThe available commands are:\n");
                printCommands();
            }
        }
    }

    private void handleTraceNotification (TraceNotification help) {
        if (lastTrace == null) {
            shellCtx.info ("No stack trace recorded");
            return;
        }

        shellCtx.info ("Last recorded stack trace:\n\n\t" + lastTrace + "\n");
    }

    /**
     * Print a help message for a command
     * @param cmd the command to print help for
     * @param msg a message, possible empty or null, to print after the
     *            usage instructions.
     **/
    private void printHelp (Command cmd, String msg) {
        HelpFormatter formatter = new HelpFormatter();

        if ("".equals(msg) || msg == null) {
            msg = cmd.getDescription();
        }

        // FIXME: We should really render this into a
        //        StringBuffer with formatter.renderOptions
        //        and print it via the ShellContext.
        formatter.printHelp(cmd.getUsage(), msg, cmd.getOptions(), "");
    }

    private void printCoreHelp (String msg) {
        shellCtx.error ("CORE HELP!");
    }

    private void printCommands () {
        for (Command cmd : commands.values()) {
            shellCtx.info("\t" + cmd.getName() + "\t" + cmd.getDescription());
        }
    }

    public void run (String[] args) {

        getShellContext().info (getHeader());

        while (true) {
            try {
                do_main_iteration();
            }
            catch (Notification e) {
                if (e instanceof BadCommandLineNotification) {
                    printHelp (e.getCommand(), e.getMessage());
                } else if (e instanceof AbortNotification) {
                    // This is a clean exit
                    if (!"".equals(e.getMessage())) {
                        shellCtx.info(e.getMessage());
                    }
                    // Exit the shell
                    break;
                } else if (e instanceof HelpNotification) {
                    handleHelpNotification((HelpNotification)e);
                } else if (e instanceof TraceNotification) {
                    handleTraceNotification((TraceNotification)e);
                } else {
                    // This is a bug in the shell core
                    shellCtx.error ("Shell Core encountered an unknown "
                                    + "notification: " + e.getClass().getName());
                }
            } catch (ParseException e) {
                getShellContext().error ("Error parsing command line: "
                        + e.getMessage());
            } catch (Exception e) {
                lastTrace = Strings.getStackTrace(e);
            }
        }


        getShellContext().info ("Bye.");
    }


}
