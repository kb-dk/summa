/* $Id: ShellContext.java,v 1.2 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.2 $
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

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Simple interface to provide user feedback and read input
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public interface ShellContext {

    /**
     * Print an error message to the user.
     * @param msg the message to print
     */
    public void error (String msg);

    /**
     * Print an info message to the user.
     * @param msg the message to print
     */
    public void info (String msg);

    /**
     * Print a warning to the user.
     * @param msg the message to print
     */
    public void warn (String msg);

    /**
     * Print a debug message to the user.
     * Note that debug messages might be ignored.
     * @param msg the message to print
     */
    public void debug (String msg);

    /**
     * Read a line of input.
     * @return the next line of input
     */
    public String readLine ();

    /**
     * Push a line of input into the shell context. This line will
     * be read when any other pending lines have been read with
     * {@link #readLine}
     *
     * @param line the line of input to add to the buffer
     */
    public void pushLine (String line);

    /**
     * Print a prompt string to the user, without a trailing newline.
     * Useful for example when asking for user feedback.
     * @param msg the message to prompt
     */
    public void prompt (String msg);
}
