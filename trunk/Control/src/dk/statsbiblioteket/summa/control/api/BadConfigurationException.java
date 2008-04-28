/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.control.api;

import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Exception thrown when a {@link Client} or {@link Service} receives a
 * configuration that is insufficient or malformed in a such way that they
 * cannot continue operation.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class BadConfigurationException extends RuntimeException {

    /**
     * @param message message about the exact configuration paramter causing
     *        the problem
     */
    public BadConfigurationException(String message) {
        super(message);
    }

    /**
     * Create a {@code BadConfigurationException} with a message and a cause.
     * @param msg the message to include
     * @param cause the {@code Throwable} that triggered the exception
     */
    public BadConfigurationException (String msg, Throwable cause) {
        super (msg, cause);
    }
}
