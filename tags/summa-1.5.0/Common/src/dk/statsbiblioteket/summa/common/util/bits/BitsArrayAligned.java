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

/**
 * A map from positive int positions to positive int values. The internal
 * representation is that of {@link BitsArrayAligned} with the notable
 * difference that the amount of bits allocated for values are always 1, 2, 4,
 * 8, 16 or 32 so that the values always fit inside a single integer.
 * </p><p>
 * The map auto-expands to accommodate given values at given positions.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BitsArrayAligned extends BitsArrayImpl {
    private static Log log = LogFactory.getLog(BitsArrayAligned.class);

    private static final int ENTRY_SIZE = BLOCK_SIZE + 1;

    /*
      * For this BitsArray, values are always aligned so that they fit
      * inside of a single block. Extraction of a value thus requires a single
      * SHR followed by a mask.
      * </p><p>
      * In code, this is {@code
       elementPos = index * BLOCK_SIZE / elementBits
       bitPos  =    index * BLOCK_SIZE % elementBits
       value =
       (blocks[elementPos] >>> SHIFTS[elementBits][bitPos]) &
       READ_MASKS[elementBits]}
     */
    private static final int[][] SHIFTS = new int[ENTRY_SIZE][ENTRY_SIZE];
    private static final int[] READ_MASKS = new int[ENTRY_SIZE];

    { // Generate shifts
        for (int elementBits = 1 ; elementBits <= BLOCK_SIZE ; elementBits++) {
            int[] currentShifts = SHIFTS[elementBits];
            for (int bitPos = 0 ; bitPos < BLOCK_SIZE ; bitPos++) {
                currentShifts[bitPos] = BLOCK_SIZE + bitPos - elementBits;
                READ_MASKS[elementBits]  = ~(~0 << elementBits);
            }
        }
    }

    /*
     * For setting a value, a clear-mask must be applied to the relevant part
      * of the block. After that, the elementBits can be shifted into place
      * by using the SHIFTS from above. In code this is {@code
        majorBitPos = index * elementBits;
        elementPos = index * BLOCK_SIZE / elementBits
        bitPos  =    index * BLOCK_SIZE % elementBits

        blocks[elementPos] =
          (blocks[elementPos] & writeMasks[elementBits][bitPos])
          | (value << shifts[elementBits][bitPos]);
       }
     */
    private static final int[][] WRITE_MASKS = new int[ENTRY_SIZE][ENTRY_SIZE];
    {
        for (int elementBits = 1 ; elementBits <= BLOCK_SIZE ; elementBits++) {
            int elementPosMask = ~(~0 << elementBits);
            int[] currentShifts = SHIFTS[elementBits];
            int[] currentMasks = WRITE_MASKS[elementBits];
            for (int bitPos = 0 ; bitPos < BLOCK_SIZE ; bitPos++) {
                currentMasks[bitPos] = ~(elementPosMask
                                         << currentShifts[bitPos]);
            }
        }
    }

    // Cached calculations
    private int maxValue;    // Math.pow(elementBits, 2)-1
    private int maxPos;      // blocks.length * BLOCK_SIZE / elementBits - 1

    private int[] shifts;
    private int readMask;
    private int majorPosShift;
    private int[] writeMasks;

    /**
     * Creates a BitsArray with the internal structures optimized for the given
     * limits. This is the recommended constructor.
     * </p><p>
     * Note that maxValue will be rounded up to
     * 2^1-1, 2^2-1, 2^4-1.2^8-1, 2^16-1 or 2^32-1.
     * @param length   the number of expected elements.
     * @param maxValue the expected maximum value for an element.
     */
    public BitsArrayAligned(int length, int maxValue) {
        int bits = calculateBits(maxValue);
        log.trace("Creating BitsArrayAligned of length " + length
                  + " with element bit size " + bits + " for max value "
                  + maxValue);
        setAttributes(bits, 0, new int[length * bits / BLOCK_SIZE + 2]);
    }

    @Override
    protected int calculateBits(int maxValue) {
        int bits = super.calculateBits(maxValue);
        if (bits > 16) {
            bits = 32;
        } else if (bits > 8) {
            bits = 16;
        } else if (bits > 4) {
            bits = 8;
        } else if (bits > 2) {
            bits = 4;
        }
        return bits;
    }

    private void setAttributes(int elementBits, int size, int[] blocks) {
        this.elementBits = elementBits;
        this.size = size;
        this.blocks = blocks;
        updateCached();
    }

    // Updated when the number of bits/value changes
    private void updateCached() {
        shifts = SHIFTS[elementBits];
        readMask = READ_MASKS[elementBits];
        writeMasks = WRITE_MASKS[elementBits];
        maxValue = (int)Math.pow(2, elementBits)-1;
        maxPos = (blocks.length * BLOCK_SIZE / elementBits) - 2;
        majorPosShift = super.calculateBits(elementBits-1);
    }

    @QAInfo(level = QAInfo.Level.FINE,
            state = QAInfo.State.IN_DEVELOPMENT,
            author = "te")
    public int getAtomic(final int index) {
        if (index >= size) {
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "The index %d was requested in an array of size %s",
                    index, size()));
        }
        final long majorBitPos = index << majorPosShift; // * elementBits;
        final int elementPos = (int)(majorBitPos >>> BLOCK_BITS); // / BLOCK_SIZE
        final int bitPos =     (int)(majorBitPos & MOD_MASK); // % BLOCK_SIZE);

        return (blocks[elementPos] >>> shifts[bitPos]) & readMask;
    }

    public int fastGetAtomic(int index) {
        final long majorBitPos = index << majorPosShift; // * elementBits;
        final int elementPos = (int)(majorBitPos >>> BLOCK_BITS); // / BLOCK_SIZE
        final int bitPos =     (int)(majorBitPos & MOD_MASK); // % BLOCK_SIZE);

        return (blocks[elementPos] >>> shifts[bitPos]) & readMask;
    }

    public void fastSet(final int index, final int value) {
        final long majorBitPos = index << majorPosShift; // * elementBits;
        final int elementPos = (int)(majorBitPos >>> BLOCK_BITS); // / BLOCK_SIZE
        final int bitPos =     (int)(majorBitPos & MOD_MASK); // % BLOCK_SIZE);

        blocks[elementPos] = (blocks[elementPos] & writeMasks[bitPos])
                             | (value << shifts[bitPos]);
    }

    @Override
    public void set(int index, int value) {
        super.set(index, value);
        size = Math.max(size, index+1);
    }

    /* Make sure we have room for value at position */
    @Override
    protected void ensureSpace(final int position, final int value) {
        if (position > maxPos || value > maxValue) {
            //noinspection MismatchedQueryAndUpdateOfCollection
            BitsArrayAligned array;
            if (position > maxPos) {
                array = new BitsArrayAligned(
                        (int)((position + 1) * LENGTH_GROWTH_FACTOR),
                        Math.max(maxValue, value));
            } else {
                array = new BitsArrayAligned(maxPos, Math.max(maxValue, value));
            }
            array.assign(this);
            setAttributes(array.elementBits, array.size, array.blocks);
        }
    }

    /**
     * Assigns the values from the given BitsArray to this. Existing values
     * will be cleared.
     * @param other the source of new values.
     */
    public void assign(final BitsArray other) {
        if (!(other instanceof BitsArrayAligned)) {
            throw new UnsupportedOperationException(String.format(
                    "Unable to assign from other types of BitsArray. Got %s",
                    other.getClass()));
        }
        BitsArrayAligned bap = (BitsArrayAligned)other;
        clear();
        ensureSpace(bap.maxPos, bap.maxValue); // Safe recursive check
        for (int pos = 0 ; pos < bap.size ; pos++) {
            set(pos, bap.getAtomic(pos));
        }
    }

    @Override
    public String toString() {
        return "BitsArrayAligned(elementBits=" + elementBits + ", size="
               + size + ", maxPos=" + maxPos
               + ", elments.length=" + blocks.length + ")";
    }

    /*
     * Updates the internal representation to use the minimum valid amount of
     * bits/value, as per the Class description.
     * @param normalise if true, existing values are normalised, with their
      *       natural ordering maintained. After normalisation, the array is
      *       compressed.
      *       Example: {@code [27, 2, 2, 12] => [2, 0, 0, 1]}
     */
/*    public void compress(boolean normalise) {
        throw new IllegalArgumentException("Not implemented yet");
    }*/


    public int getMaxValue() {
        return maxValue;
    }
}
