/* $Id: IndexServiceException.java,v 1.3 2007/10/04 13:28:19 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:19 $
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
package dk.statsbiblioteket.summa.common.lucene.index;

import java.io.Serializable;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * This is a generic wrapper exception for use over RMI. It is used so that the
 * client should not care about which exception are thrown (and therefore is
 * needed in client class path ).<br />
 * Exceptions part of java core libraries are wrapped in this exception,
 * otherwise an an appropriate message is given.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class IndexServiceException extends Exception {

    /**
     * Constructs an IndexServiceException with an error message.
     *
     * @param message the message of the exception.
     */
    public IndexServiceException(final String message) {
        super(message);
    }

    /**
     * Constructs an IndexServiceException with both an error message, and with
     * the cause of the exception.
     *
     * @param message the message of the exception.
     * @param cause   the causing exception to the exception.
     */
    public IndexServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an IndexServiceException, wrapping the cause exception.
     *
     * @param cause the causing exception to the exception.
     */
    public IndexServiceException(final Throwable cause) {
        super(cause);
    }

}



