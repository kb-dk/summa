/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.common.index;

import java.rmi.RemoteException;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Thrown in case of index-related problems, such as un-available or corrupted
 * indexes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class IndexException extends RemoteException {
    private String indexLocation = null;

    public IndexException(String s) {
        super(s);
    }
    public IndexException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs an exception where the location of the index is specified.
     * The location will be automatically appended to the error message and
     * is explicitely retrievable with {@link #getIndexLocation()}.
     * This is the recommended constructor.
     * @param s             error message.
     * @param indexLocation the location for the index which caused the
     *                      exception.
     * @param cause         the cause, if any, of the exception.
     */
    public IndexException(String s, String indexLocation, Throwable cause) {
        super(s + " (index at '" + indexLocation + "')", cause);
        this.indexLocation = indexLocation;
    }

    /**
     * @return the index-location for the index which caused the exception.
     */
    public String getIndexLocation() {
        return indexLocation;
    }
}
