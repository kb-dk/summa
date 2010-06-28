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

import java.io.IOException;
import java.rmi.UnmarshalException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeSet;

import dk.statsbiblioteket.summa.common.shell.commands.Clear;
import dk.statsbiblioteket.summa.common.shell.notifications.AbortNotification;
import dk.statsbiblioteket.summa.common.shell.notifications.BadCommandLineNotification;
import dk.statsbiblioteket.summa.common.shell.notifications.ClearNotification;
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
import jline.ConsoleReader;
import jline.SimpleCompletor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * A generic shell driver.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class Core {

    private HashMap<String, Command> commands;
    private HashMap<String, String> aliases;
    private String lastTrace;
    private String header, prompt;
    private ShellContext shellCtx;
    private CommandLineParser cliParser;
    private static SimpleCompletor cmdComparator;


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
     * @param debug if {@code true} debug messages will be printed to the
     *              created shell context if {@code shellCtx} is {@code null}.
     */
    public Core (ShellContext shellCtx,
                       final boolean withDefaultCommands, final boolean debug) {
        cliParser = new PosixParser();
        commands = new HashMap<String,Command>();
        aliases = new HashMap<String, String>();
        lastTrace = null;
        header = "Summa Generic Shell v@summa.api.version@";
        prompt = "summa-shell> ";

        if (shellCtx != null) {
            this.shellCtx = shellCtx;
        } else {
            this.shellCtx = new ShellContext () {
                private Stack<String> lineBuffer = new Stack<String>();
                private String lastError = null;
                private ConsoleReader lineIn = createConsoleReader();
                private boolean enableDebug = debug;

                @Override
                public void error(String msg) {
                    lineBuffer.clear();
                    lastError = msg;
                    System.out.println ("[ERROR] " + msg);
                }

                @Override
                public void info(String msg) {
                    System.out.println (msg);
                }

                @Override
                public void warn(String msg) {
                    System.out.println ("[WARNING] " + msg);
                }

                @Override
                public void debug(String msg) {
                    if (enableDebug) {
                        System.out.println ("[DEBUG] " + msg);
                    }
                }

                @Override
                public String readLine() {
                    if (!lineBuffer.empty()) {
                        return lineBuffer.pop();
                    }

                    try {
                        String line = lineIn.readLine();
                        if (line != null) {
                            return line.trim();
                        }
                        return null;
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read input", e);
                    }
                }

                @Override
                public void pushLine (String line) {
                    lineBuffer.push(line.trim());                    
                }

                @Override
                public String getLastError () {
                    return lastError;
                }

                @Override
                public void prompt (String prompt) {
                    System.out.print(prompt);
                    System.out.flush();
                }

                @Override
                public void clear() {
                  try {
                    lineIn.clearScreen();
                  } catch(IOException e) {
                    error("Clearing screen");
                  }
                }
            };

        }

        if (withDefaultCommands) {
            installCommand(new Help());
            installCommand(new Quit());
            installCommand(new Trace());
            installCommand(new Exec());
            installCommand(new Clear());
        }
    }

    private static ConsoleReader createConsoleReader() {
        cmdComparator = new SimpleCompletor(new String[] {});
        try {
            ConsoleReader reader = new ConsoleReader();
            reader.addCompletor(cmdComparator);
            return reader;          
        } catch (IOException e) {
            throw new RuntimeException("Unable to create ConsoleReader: "
                                       + e.getMessage(), e);
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
        this(null, withDefaultCommands, false);
    }

    /**
     * Create a new shell {@code Core} with the default command set found in
     * {@link dk.statsbiblioteket.summa.common.shell.commands}.
     */
    public Core() {
        this(true);
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getHeader() {
        return header;
    }

    public void installCommand (Command cmd) {
        commands.put(cmd.getName(), cmd);
        aliases.put(cmd.getName(), cmd.getName());
        for(String alias: cmd.getAliases()) {
            aliases.put(alias, cmd.getName());
        }
        cmdComparator.addCandidateString(cmd.getName());
    }

    public ShellContext getShellContext() {
        return shellCtx;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * A main iteration of the core. The caller is responsible for handling
     * any exceptions from the underlying subsystems.
     * 
     * @throws Exception if an error happens inside the running {@link Command}.
     * @throws ParseException if there is an error parsing the command line
     *                        as entered by the user.
     */
    private void doMainIteration() throws Exception {
        String cmdString;

        shellCtx.prompt(getPrompt());

        /* This call will block until we have input */
        cmdString = shellCtx.readLine();

        /* Check if the input stream has been closed an exit the shell if so */
        if (cmdString == null) {
            throw new AbortNotification("Input steam closed", 0);
        }

        /* Ignore empty commands, and re-print the prompt */
        if ("".equals(cmdString)) {
            return;
        }

        invoke (cmdString);
    }

    /**
     * Parse and run a command.
     *
     * @param cmdString the command line to run.
     * @throws Exception any exception from the invoked {@link Command} will
     *                   cascade upwards from this method.
     * @return true if and only if {@code cmdString} was executed successfully.
     */
    boolean invoke(String cmdString) throws Exception {
        String[] tokens;
        Command cmd;

        tokens = tokenize(cmdString);
        cmd = commands.get(aliases.get(tokens[0]));

        if (cmd == null) {
            shellCtx.error("No such command '" + cmdString +"'");
            return false;
        }

        String[] args = new String[tokens.length-1];
        // Copy arguments from command array to args array
        System.arraycopy(tokens, 1, args, 0, tokens.length - 1);

        CommandLine cli = cliParser.parse(cmd.getOptions(), args);

        int commandEnd = tokens.length > 1 ? cmd.getName().length() + 1 :
                                             cmd.getName().length(); 
        cmd.prepare(cli, cmdString.substring(commandEnd));

        cmd.invoke(shellCtx);

        return true;
    }

    /**
     * Return the string tokized by white space respecting phrases enclosed
     * by double-quotes.
     *
     * @param in the string to tokenize.
     * @throws SyntaxErrorNotification if there are improperly formatted
     *                                 double-quoted substrings.
     * @return the string tokens as separated by white space with double-quoted
     *         substrings treated as one token.
     */
    public String[] tokenize(String in) {
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

    private void handleHelpNotification(HelpNotification help) {
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

    private void handleTraceNotification(TraceNotification help) {
        if (lastTrace == null) {
            shellCtx.info("No stack trace recorded");
            return;
        }

        shellCtx.info ("Last recorded stack trace:\n\n\t" + lastTrace + "\n");
    }

    private void handleClearNotification() {
      shellCtx.clear();
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
    private void printHelp(Command cmd, String msg) {
        HelpFormatter formatter = new HelpFormatter();

        if ("".equals(msg) || msg == null) {
            msg = cmd.getDescription();
        }

        // FIXME: We should really render this into a
        //        StringBuffer with formatter.renderOptions
        //        and print it via the ShellContext.
        formatter.printHelp(cmd.getUsage(), msg, cmd.getOptions(), "");
    }

    private void printCoreHelp(String msg) {
        shellCtx.error ("CORE HELP!");
    }

    private void printCommands() {
        SortedSet<String> sortedCommands =
                                       new TreeSet<String>(commands.keySet());
        for (String cmdName : sortedCommands) {
            Command cmd = commands.get(cmdName);
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
    public int run(Script script) {
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
                } else if (e instanceof ClearNotification) {
                    handleClearNotification();
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
                    shellCtx.error("Shell Core encountered an unknown "
                                    + "notification: " + e.getClass().getName());
                }
            } catch (ParseException e) {
                returnVal = -4;
                getShellContext().error("Error parsing command line: "
                        + e.getMessage());
            } catch (UnmarshalException e) {
                /* This is a specific hack to handle the case where an RMI
                 * service returns an unknown class */
                if (e.getCause() instanceof ClassNotFoundException) {
                    shellCtx.error("Caught exception of unknown class type: "
                                    + e.getCause().getMessage() + "\n\n"
                                    + "This usually happens if a remote "
                                    + "service throws a custom exception");
                } else {
                    shellCtx.error("RMI protocol error:" + e.getMessage());
                }
                lastTrace = Strings.getStackTrace(e);
            } catch (Exception e) {
                shellCtx.error("Caught error: '" + e.getMessage() + "'");
                lastTrace = Strings.getStackTrace(e);
            }
        }


        getShellContext().info("Bye.");
        return returnVal;
    }


}




