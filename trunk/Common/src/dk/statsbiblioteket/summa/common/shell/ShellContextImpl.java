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

import dk.statsbiblioteket.util.qa.QAInfo;
import jline.ConsoleReader;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Stack;

/**
 * Silent shell context, used for running one or more command(s) in a silent
 * mode.
 *
 * @author Henrik Kirk <mailto:hbk@statsbiblioteket.dk>
 * @since Aug 11, 2010
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hbk")
public class ShellContextImpl implements ShellContext {
    /** Print stream for errors. */
    private PrintStream error;

    /** Print stream for warn. */
    private PrintStream warn;

    /** Print stream for info. */
    private PrintStream info;

    /** Print stream for debug. */
    private PrintStream debug;

    /** The used console reader. */
    private ConsoleReader lineIn;

    /** Line buffer, that means a stack for multiple commands. */
    private Stack<String> lineBuffer = new Stack<String>();

    /** Last error message. */
    String lastError = null;

    /**
     * Creates a {@link ShellContext} where errors is printed on the error
     * stream, info, warn and if {@code debug} is true also debug is printed on
     * info stream.
     * @param consoleReader The console reader.
     * @param error The error stream.
     * @param info The info stream.
     * @param debug If {@link true} debug message are printed on the info print
     * stream.
     */
    public ShellContextImpl(final ConsoleReader consoleReader,
                            final PrintStream error, final PrintStream info,
                                                                boolean debug) {
        this(consoleReader);
        if(debug) {
            initStreams(error, info, info, info);
        } else {
            initStreams(error, info, info, null);
        }
    }

    /**
     * Default constructor, this initialize the {@link ConsoleReader} and sets
     * all streams to null. If any streams are needed, call
     * {@link #initStreams(java.io.PrintStream, java.io.PrintStream, java.io.PrintStream, java.io.PrintStream)}
     * after construction via this method. 
     * Note: This constructor can also be used with the intention of creating a
     * silent shell.
     * @param consoleReader The console reader to user.
     */
    public ShellContextImpl(final ConsoleReader consoleReader) {
        this.lineIn = consoleReader;
        initStreams(null, null, null, null);
    }

    /**
     * Private helper method, initialize streams.
     * @param error The error stream.
     * @param info The info stream.
     * @param warn The warn stream.
     * @param debug The debug stream.
     */
    private void initStreams(final PrintStream error, final PrintStream info,
                             final PrintStream warn, final PrintStream debug) {
        this.error = error;
        this.info = info;
        this.debug = debug;
        this.warn = warn;
    }

    /**
     * {@inheritDoc}
     * @param msg The message to print.
     */
    @Override
    public void error(String msg) {
        lastError = msg;
        if(error != null) {
            error.println("[ERROR] " + msg);
        }
    }

    /**
     * {@inheritDoc}
     * @param msg The message to print.
     */
    @Override
    public void info(String msg) {
        if(info != null) {
            info.println(msg);
        }
    }

    /**
     * {@inheritDoc}
     * @param msg The message to print.
     */
    @Override
    public void warn(String msg) {
        if(warn != null) {
            warn.println("[WARN] " + msg);
        }
    }

    /**
     * {@inheritDoc}
     * @param msg The message to print.
     */
    @Override
    public void debug(String msg) {
        if(debug != null) {
            debug.println("[DEBUG] " + msg);
        }
    }

    /**
     * {@inheritDoc}
     * @return
     */
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
    public void pushLine(String line) {
        lineBuffer.push(line.trim());

    }

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public String getLastError() {
        return lastError;
    }

    /**
     * {@inheritDoc}
     * @param msg The message to prompt.
     */
    @Override
    public void prompt(String msg) {
        if(info != null) {
            info.print(msg);
            info.flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        try {
            lineIn.clearScreen();
        } catch(IOException e) {
            error("Error clearing screen");
            error(e.toString());
        }
    }
}