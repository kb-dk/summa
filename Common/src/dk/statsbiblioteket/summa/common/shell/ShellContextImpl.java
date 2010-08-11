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

    private Stack<String> lineBuffer = new Stack<String>();
    String lastError = "";

    public ShellContextImpl(ConsoleReader consoleReader, PrintStream error,
                                                             PrintStream info) {
        this.lineIn = consoleReader;
        this.error = error;
        this.info = info;
        this.debug = info;
        this.warn = info;
    }

    /**
     * {@inheritDoc}
     * @param msg The message to print.
     */
    @Override
    public void error(String msg) {
        lastError = msg;
        error.println("[ERROR] " + msg);
    }

    /**
     * {@inheritDoc}
     * @param msg The message to print.
     */
    @Override
    public void info(String msg) {
        info.println("[INFO] " + msg);
    }

    /**
     * {@inheritDoc}
     * @param msg The message to print.
     */
    @Override
    public void warn(String msg) {
        warn.println("[WARN] " + msg);
    }

    /**
     * {@inheritDoc}
     * @param msg The message to print.
     */
    @Override
    public void debug(String msg) {
        debug.println("[DEBUG] " + msg);
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
        info.println(msg);
        info.flush();
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