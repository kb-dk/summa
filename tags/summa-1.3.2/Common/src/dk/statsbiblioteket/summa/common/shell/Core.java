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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.rmi.UnmarshalException;

import dk.statsbiblioteket.summa.common.shell.notifications.AbortNotification;
import dk.statsbiblioteket.summa.common.shell.notifications.BadCommandLineNotification;
import dk.statsbiblioteket.summa.common.shell.notifications.Notification;
import dk.statsbiblioteket.summa.common.shell.notifications.HelpNotification;
import dk.statsbiblioteket.summa.common.shell.notifications.TraceNotification;
import dk.statsbiblioteket.summa.common.shell.notifications.SyntaxErrorNotification;
import dk.statsbiblioteket.summa.common.shell.commands.Help;
import dk.statsbiblioteket.summa.common.shell.commands.Quit;
import dk.statsbiblioteket.summa.common.shell.commands.Trace;
import dk.statsbiblioteket.summa.common.shell.commands.Exec;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A generic shell driver.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class Core {

    private HashMap<String, Command> commands;
    private String lastTrace;
    private String header, prompt;
    private ShellContext shellCtx;
    private CommandLineParser cliParser;

    /**
     * Create a new shell {@code Core} outputting to a custom
     * {@link ShellContext}.
     *
     * @param shellCtx the {@link ShellContext} to write output to. If this
     *                 parameter is {@code null} a default shell context will
     *                 be used. 
     * @param withDefaultCommands if {@code true} install the default commands
     *                            found in the
     *                            {@link dk.statsbiblioteket.summa.common.shell.commands}
     *                            package.
     */
    public Core (ShellContext shellCtx, boolean withDefaultCommands) {
        cliParser = new PosixParser();
        commands = new HashMap<String,Command>();
        lastTrace = null;
        header = "Summa Generic Shell $Id: Core.java,v 1.7 2007/10/04 13:28:20 te Exp $";
        prompt = "summa-shell> ";

        if (shellCtx != null) {
            this.shellCtx = shellCtx;
        } else {
            this.shellCtx = new ShellContext () {

                private Stack<String> lineBuffer = new Stack<String>();
                private BufferedReader lineIn =  new BufferedReader(
                                           new InputStreamReader(System.in), 1);
                private String lastError = null;

                public void error(String msg) {
                    lineBuffer.clear();
                    lastError = msg;
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
                    if (!lineBuffer.empty()) {
                        return lineBuffer.pop();
                    }

                    try {
                        return lineIn.readLine().trim();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read input", e);
                    }
                }

                public void pushLine (String line) {
                    lineBuffer.push(line.trim());                    
                }

                public String getLastError () {
                    return lastError;
                }

                public void prompt (String prompt) {
                    System.out.print(prompt);
                }
            };

        }

        if (withDefaultCommands) {
            installCommand(new Help());
            installCommand(new Quit());
            installCommand(new Trace());
            installCommand(new Exec());
        }
    }

    /**
     * Create a new shell {@code Core} outputting to a default
     * {@link ShellContext}.
     *
     * @param withDefaultCommands if {@code true} install the default commands
     *                            found in the
     *                            {@link dk.statsbiblioteket.summa.common.shell.commands}
     *                            package.
     */
    public Core(boolean withDefaultCommands) {
        this (null, withDefaultCommands);
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

    public String getPrompt () {
        return prompt;
    }

    public void setPrompt (String prompt) {
        this.prompt = prompt;
    }

    /**
     * A main iteration of the core. The caller is responsible for handling
     * any exceptions from the underlying subsystems.
     * 
     * @throws Exception if an error happens inside the running {@link Command}
     * @throws ParseException if there is an error parsing the command line
     *                        as entered by the user.
     */
    private void doMainIteration() throws Exception {
        String cmdString;

        shellCtx.prompt(getPrompt());

        /* This call will block until we have input */
        cmdString = shellCtx.readLine();
        if ("".equals(cmdString)) {
            return;
        }

        invoke (cmdString);
    }

    /**
     * Parse an run a command.
     *
     * @param cmdString the command line to run
     * @throws Exception any exception from the invoked {@link Command} will
     *                   cascade upwards from this method
     * @return true if and only if {@code cmdString} was executed successfully
     */
    boolean invoke (String cmdString) throws Exception {
        String[] tokens;
        Command cmd;

        tokens = tokenize(cmdString);
        cmd = commands.get (tokens[0]);

        if (cmd == null) {
            shellCtx.error("No such command '" + cmdString +"'");
            return false;
        }

        String[] args = new String[tokens.length-1];
        for (int i = 1; i < tokens.length; i++) {
            args[i-1] = tokens[i];
        }

        CommandLine cli = cliParser.parse (cmd.getOptions(), args);

        int commandEnd = tokens.length > 1 ? cmd.getName().length() + 1 :
                                             cmd.getName().length(); 
        cmd.prepare(cli, cmdString.substring(commandEnd));

        cmd.invoke (shellCtx);

        return true;
    }

    /**
     * Return the string tokized by white space respecting phrases enclosed
     * by double-quotes.
     *
     * @param in the string to tokenize
     * @throws SyntaxErrorNotification if there are improperly formatted
     *                                 double-quoted substrings
     * @return the string tokens as separated by white space with double-quoted
     *         substrings treated as one token
     */
    public String[] tokenize (String in) {
        StringTokenizer tok = new StringTokenizer(in, "\"", true);
        List<String> result = new ArrayList<String> (tok.countTokens());

        while (tok.hasMoreElements()) {
            String s = tok.nextToken();

            /* If this is the start of a phrase read the whole phrase in
             * one go */
            if (s.equals("\"")) {
                if (tok.hasMoreElements()) {
                    s = tok.nextToken().trim();
                    result.add(s);
                    if (tok.hasMoreElements()) {
                        s = tok.nextToken();
                        if (!s.equals("\"")) {
                            throw new SyntaxErrorNotification
                                    ("Unexpected token '" + s + "' when "
                                     +"tokenizing phrase. Expected '\"'");
                        }
                        continue;
                    } else {
                        throw new SyntaxErrorNotification ("Unclosed phrase "
                                                           +"near token "
                                                           + "'" + s + "'");
                    }
                } else {
                    throw new SyntaxErrorNotification ("Stray '\"' at line "
                                                       + "end");
                }

            }

            /* A normal token, split by spaces */
            for (String ss : s.split(" ")) {
                if (!ss.equals("")) {
                    result.add(ss.trim());
                }
            }
        }

        return result.toArray(new String[result.size()]);
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

    private void handleSyntaxErrorNotification(
            SyntaxErrorNotification syntaxErrorNotification) {
        shellCtx.error("Syntax error: " + syntaxErrorNotification.getMessage());

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

    /**
     * Run the shell's main loop. If {@code script} is non-null the script will
     * be executed and the shell core will exit afterwards. If {@code script}
     * is {@code null} it will enter interactive mode reading commands from
     * stdin.
     *
     * @param script a script to execute or {@code null} to enter interactive
     *               mode
     * @return 0 on a clean exit which also indicates the any script passed to
     *         to the core returned without any errors. If this method returns
     *         non-zero some error occured and the caller must understand the
     *         error code in its current context.
     */
    public int run (Script script) {
        int returnVal = 0;
        Iterator<String> scriptIter = null;
        String scriptStatement = null;

        if (script != null) {
            scriptIter = script.iterator();
        }

        /* Print a greeting if running interactively */
        if (scriptIter == null) {
            getShellContext().info(getHeader());
        }

        while (true) {
            try {
                /* If the shell is scripted feed the next script line to the
                 * parser*/
                if (scriptIter != null) {
                    if (getShellContext().getLastError() != null) {
                        throw new AbortNotification("Error executing '"
                                                    + scriptStatement + "'",
                                                    returnVal == 0 ?
                                                                -1 : returnVal);
                    }

                    if (scriptIter.hasNext()) {
                        scriptStatement = scriptIter.next();
                        getShellContext().pushLine(scriptStatement);
                    } else {
                        // Script is done. Exit the shell
                        break;
                    }
                }

                doMainIteration();
            }
            catch (Notification e) {
                if (e instanceof BadCommandLineNotification) {
                    if (scriptIter != null) {
                        getShellContext().error ("Bad command line for '"
                                                 + e.getCommand() + "': "
                                                 + e.getMessage());
                        returnVal = -2;
                    } else {
                        printHelp (e.getCommand(), e.getMessage());
                    }
                } else if (e instanceof AbortNotification) {
                    // This is a clean exit
                    if (!"".equals(e.getMessage())) {
                        shellCtx.info(e.getMessage());
                    }
                    returnVal = ((AbortNotification)e).getReturnValue();
                    // Exit the shell
                    break;
                } else if (e instanceof HelpNotification) {
                    handleHelpNotification((HelpNotification)e);
                } else if (e instanceof TraceNotification) {
                    handleTraceNotification((TraceNotification)e);
                } else if (e instanceof SyntaxErrorNotification) {
                    if (scriptIter != null) {
                        getShellContext().error("Syntax error when parsing "
                                                + "'" + scriptStatement
                                                + "': "
                                                + e.getMessage());
                        returnVal = -3;
                    } else {
                        handleSyntaxErrorNotification((SyntaxErrorNotification)e);
                    }
                } else {
                    // This is a bug in the shell core
                    shellCtx.error ("Shell Core encountered an unknown "
                                    + "notification: " + e.getClass().getName());
                }
            } catch (ParseException e) {
                returnVal = -4;
                getShellContext().error ("Error parsing command line: "
                        + e.getMessage());
            } catch (UnmarshalException e) {
                /* This is a specific hack to handle the case where an RMI
                 * service returns an unknown class */
                if (e.getCause() instanceof ClassNotFoundException) {
                    shellCtx.error ("Caught exception of unknown class type: "
                                    + e.getCause().getMessage() + "\n\n"
                                    + "This usually happens if a remote "
                                    + "service throws a custom exception");
                } else {
                    shellCtx.error ("RMI protocol error:" + e.getMessage());
                }
                lastTrace = Strings.getStackTrace(e);
            } catch (Exception e) {
                shellCtx.error ("Caught error: '" + e.getMessage() + "'");
                lastTrace = Strings.getStackTrace(e);
            }
        }


        getShellContext().info ("Bye.");
        return returnVal;
    }


}



