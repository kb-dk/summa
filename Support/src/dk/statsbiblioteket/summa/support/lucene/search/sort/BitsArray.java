/* $Id:$
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
package dk.statsbiblioteket.summa.support.lucene.search.sort;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A compressed array of bits. The maximum number of bits/entry is defined at
 * construction time and an internal array holds the concatenated entries.
 * </p><p>
 * Example: The array is initialized with 4 and 20 elements are added.
 *          The internal array uses 4 * 20 = 80 bits or 92/32 = 2 longs.
 * </p><p>
 * Important: This class is unfinished and uses non-optimal memory allocation!
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BitsArray {
    private static Log log = LogFactory.getLog(BitsArray.class);

    private int elementSize;
    private int length;
    private int[] elements;

    private int elementMask;

    public BitsArray(int length, int elementSize) {
        if (elementSize > 32) {
            throw new IllegalArgumentException(String.format(
                    "Only elements of 32 bits or less are supported."
                    + " %d was requested", elementSize));
        }
        log.debug("Creating BitsArray of length " + length
                  + " with element size " + elementSize);
        this.length = length;
        this.elementSize = elementSize;
        elements = new int[length];
//        elements = new long[length * elementSize / 32 + 1];
        elementMask = ~0 >>> (32 - elementSize);
    }

    /**
     * Set the element at the given position. The least significant elementSize
     * bits from value will be used.
     * @param position the position for the value.
     * @param value    the value to assign.
     */
    public void set(int position, int value) {
        elements[position] = value & elementMask;
    }

    /**
     * @param position the position of the value.
     * @return the value at the given position.
     */
    public int get(int position) {
        return elements[position];
    }

}
