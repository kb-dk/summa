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
package dk.statsbiblioteket.summa.facetbrowser.util.pool;

import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.LineReader;

/**
 * Handles value-specific conversions. To be used together with the pool
 * framework.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface ValueConverter<E extends Comparable> {
    /**
     * Converts the given value to an array of bytes, suitable for storage.
     * The conversion should mirror {@link #bytesToValue}.
     * @param value the value to converto to bytes.
     * @return bytes based on values.
     */
    abstract byte[] valueToBytes(E value);

    /**
     * Converts the given bytes to a value. The conversion should mirror
     * {@link #valueToBytes}.
     * @param buffer the bytes to convert.
     * @param length the number of bytes to convert.
     * @return a value constructed from the given bytes.
     */
    abstract E bytesToValue(byte[] buffer, int length);
}
