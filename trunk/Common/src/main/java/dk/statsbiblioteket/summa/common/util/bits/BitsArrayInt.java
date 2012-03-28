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

