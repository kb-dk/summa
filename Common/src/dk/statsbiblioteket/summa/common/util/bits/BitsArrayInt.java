/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2009  The State and University Library
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
package dk.statsbiblioteket.summa.common.util.bits;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.AbstractList;

/**
 * An integer-based BitsArray: Trivial implementation.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BitsArrayInt extends AbstractList<Integer> implements BitsArray{
    private static Log log = LogFactory.getLog(BitsArrayInt.class);

    /**
     * The factor to multiply to the position when a {@link #set} triggers a
     * growth of the underlying structure.
     */
    public static final double LENGTH_GROWTH_FACTOR = 1.2;

    private int[] values;
    private int size = 0;

    public BitsArrayInt() {
        this(10);
    }

    public BitsArrayInt(int length) {
        values = new int[length];
    }

    @Override
    public Integer get(int index) {
        return getAtomic(index);
    }

    @Override
    public int size() {
        return size;
    }

    public int getAtomic(final int index) {
        if (index >= size()) {
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "The index %d was requested in an array of size %s",
                    index, size()));
        }
        return values[index];
    }

    public int fastGetAtomic(final int index) {
        return values[index];
    }

    public void set(int position, int value) {
        if (position >= values.length) {
            int[] newValues = new int[(int)(
                    Math.max(values.length, position) * LENGTH_GROWTH_FACTOR)];
            System.arraycopy(values, 0, newValues, 0, values.length);
            values = newValues;
        }
        values[position] = value;
        size = Math.max(size, position + 1);
    }

    public void fastSet(int position, int value) {
        values[position] = value;
    }

    public void assign(BitsArray other) {
        if (!(other instanceof BitsArrayInt)) {
            throw new UnsupportedOperationException(String.format(
                    "Unable to assign from other types of BitsArray. Got %s",
                    other.getClass()));
        }
        BitsArrayInt bai = (BitsArrayInt)other;
        values = new int[bai.size];
        System.arraycopy(bai.values, 0, values, 0, bai.size);
        size = bai.size;
    }

    public int getMemSize() {
        return values.length * 4;
    }

    public int getMaxValue() {
        return Integer.MAX_VALUE;
    }
}
