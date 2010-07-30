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

/**
 * Simple interface to provide user feedback and read input
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public interface ShellContext {

    /**
     * Clear the line buffer (populated by calling {@link #pushLine(String)})
     * and print an error message to the user.
     * @param msg The message to print.
     */
    public void error(String msg);

    /**
     * Print an info message to the user.
     * @param msg The message to print.
     */
    public void info(String msg);

    /**
     * Print a warning to the user.
     * @param msg The message to print.
     */
    public void warn(String msg);

    /**
     * Print a debug message to the user.
     * Note that debug messages might be ignored.
     * @param msg The message to print.
     */
    public void debug(String msg);

    /**
     * Read a line of input. If the input stream has been closed this method
     * returns {@code null}.
     * @return The next line of input or {@code null} if the input stream has
     *         been closed.
     */
    public String readLine();

    /**
     * Push a line of input into the shell context. This line will
     * be read when any other pending lines have been read with
     * {@link #readLine}.
     *
     * @param line The line of input to add to the buffer.
     */
    public void pushLine(String line);

    /**
     * Get the last error message sent to the shell context.
     * @return The last error message sent to the shell or {@code null} if
     *         no errors has been reported.
     */
    public String getLastError();

    /**
     * Print a prompt string to the user, without a trailing newline.
     * Useful for example when asking for user feedback.
     * @param msg The message to prompt.
     */
    public void prompt(String msg);

    /**
     * Clears the screen for input. 
     */
    public void clear();
}




