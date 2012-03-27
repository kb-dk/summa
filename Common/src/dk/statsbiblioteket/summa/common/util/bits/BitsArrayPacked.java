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
 * A map from positive int index to positive int values. The internal
 * representation is {@code (max_position + 1) * ceil(log2(max_value))} bits
 * plus overhead.
 * </p><p>
 * Example: The array contains the values {@code [120, 7, 8, 8, 0, 122]}.
 *          ceil(log2(122)) == 7, so 6 * 7 == 42 bits are used for storage.
 * </p><p>
 * The map auto-expands to accomodate given values at given index.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BitsArrayPacked extends BitsArrayImpl {
    private static Log log = LogFactory.getLog(BitsArrayPacked.class);

    private static final int ENTRY_SIZE = BLOCK_SIZE + 1;
    private static final int FAC_BITPOS = 3;

    /*
      * In order to make an efficient value-getter, conditionals should be
      * avoided. A value can be positioned inside of a block, requiring shifting
      * left or right or it can span two blocks, requiring a left-shift on the
      * first block and a right-shift on the right block.
      * </p><p>
      * By always shifting the first block both left and right, we get exactly
      * the right bits. By always shifting the second block right and applying
      * a mask, we get the right bits there. After that, we | the two bitsets.
     */
    private static final int[][] SHIFTS =
            new int[ENTRY_SIZE][ENTRY_SIZE * FAC_BITPOS];
            //new int[BLOCK_SIZE+1][BLOCK_SIZE][BLOCK_SIZE+1];
    private static final int[][] MASKS = new int[ENTRY_SIZE][ENTRY_SIZE];

    static { // Generate shifts
        for (int elementBits = 1 ; elementBits <= BLOCK_SIZE ; elementBits++) {
            for (int bitPos = 0 ; bitPos < BLOCK_SIZE ; bitPos++) {
                int[] currentShifts = SHIFTS[elementBits];
                int base = bitPos * FAC_BITPOS;
                currentShifts[base    ] = bitPos;
                currentShifts[base + 1] = BLOCK_SIZE - elementBits;
                if (bitPos <= BLOCK_SIZE - elementBits) { // Single block
                    currentShifts[base + 2] = 0;
                    MASKS[elementBits][bitPos] = 0;
                } else { // Two blocks
                    int rBits = elementBits - (BLOCK_SIZE - bitPos);
                    currentShifts[base + 2] = BLOCK_SIZE - rBits;
                    MASKS[elementBits][bitPos] = ~(~0 << rBits);
                }
            }
        }
    }

    /*
      * The setter requires more masking than the getter.
     */
    private static final int[][] WRITE_MASKS =
            new int[ENTRY_SIZE][ENTRY_SIZE * FAC_BITPOS];
    static {
        for (int elementBits = 1 ; elementBits <= BLOCK_SIZE ; elementBits++) {
            int elementPosMask = ~(~0 << elementBits);
            int[] currentShifts = SHIFTS[elementBits];
            int[] currentMasks = WRITE_MASKS[elementBits];
            for (int bitPos = 0 ; bitPos < BLOCK_SIZE ; bitPos++) {
                int base = bitPos * FAC_BITPOS;
                currentMasks[base  ] =~((elementPosMask
                                   << currentShifts[base + 1])
                                  >>> currentShifts[base]);
                currentMasks[base+1] = ~(elementPosMask
                                   << currentShifts[base + 2]);
                currentMasks[base+2] = currentShifts[base + 2] == 0 ? 0 : ~0;
            }
        }
    }

    // Cached calculations
    private int maxValue;    // Math.pow(elementBits, 2)-1
    private int maxPos;      // blocks.length * BLOCK_SIZE / elementBits - 1
    private int[] shifts;    // The shifts for the current elementBits
    private int[] readMasks;
    private int[] writeMasks;

    /**
     * Creates a BitsArray of minimal size. If there is knowledge of expected
     * values, it is recommended to use {@link #BitsArrayPacked(int, int)} instead.
     */
    public BitsArrayPacked() {
        this(1, 1);
    }

    /**
     * Creates a BitsArray with the internal structures optimized for the given
     * limits. This is the recommended constructor.
     * @param length   the number of expected elements.
     * @param maxValue the expected maximum value for an element.
     */
    public BitsArrayPacked(int length, int maxValue) {
        int bits = calculateBits(maxValue);
        log.trace("Creating BitsArrayPacked of length " + length
                  + " with element bit size " + bits + " for max value "
                  + maxValue);
        // TODO: Calculate length with long instead of int, then cast to int
        setAttributes(
                bits, 0, new int[length * bits / BLOCK_SIZE + 2]);
    }

    private void setAttributes(int elementBits, int size, int[] blocks) {
        this.elementBits = elementBits;
        this.size = size;
        this.blocks = blocks;
        updateCached();
    }

    private void updateCached() {
        readMasks = MASKS[elementBits];
        maxValue = (int)Math.pow(2, elementBits)-1;
        maxPos = (blocks.length * BLOCK_SIZE / elementBits) - 2;
        shifts = SHIFTS[elementBits];
        writeMasks = WRITE_MASKS[elementBits];
    }

    /**
     * @param index the position of the value.
     * @return the value at the given index.
     */
    @QAInfo(level = QAInfo.Level.FINE,
            state = QAInfo.State.IN_DEVELOPMENT,
            author = "te")
    public int getAtomic(final int index) {
        if (index >= size) {
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "The index %d was requested in an array of size %s",
                    index, size()));
        }
        final long majorBitPos = index * elementBits;
        final int elementPos = (int)(majorBitPos >>> BLOCK_BITS); // / BLOCK_SIZE
        final int bitPos =     (int)(majorBitPos & MOD_MASK); // % BLOCK_SIZE);

        final int base = bitPos * FAC_BITPOS;

        return ((blocks[elementPos] << shifts[base]) >>> shifts[base+1]) |
               ((blocks[elementPos+1] >>> shifts[base+2])
                & readMasks[bitPos]);
    }

    public int fastGetAtomic(final int index) {
        final long majorBitPos = index * elementBits;
        final int elementPos = (int)(majorBitPos >>> BLOCK_BITS); // / BLOCK_SIZE
        final int bitPos =     (int)(majorBitPos & MOD_MASK); // % BLOCK_SIZE);

        final int base = bitPos * FAC_BITPOS;

        return ((blocks[elementPos] << shifts[base]) >>> shifts[base+1]) |
               ((blocks[elementPos+1] >>> shifts[base+2])
                & readMasks[bitPos]);
    }

    public void fastSet(final int index, final int value) {
        final long majorBitPos = index * elementBits;
        final int elementPos = (int)(majorBitPos >>> BLOCK_BITS); // / BLOCK_SIZE
        final int bitPos =     (int)(majorBitPos & MOD_MASK); // % BLOCK_SIZE);
        final int base = bitPos * FAC_BITPOS;

        blocks[elementPos  ] = (blocks[elementPos  ] & writeMasks[base])
                               | (value << shifts[base + 1] >>> shifts[base]);
        blocks[elementPos+1] = (blocks[elementPos+1] & writeMasks[base+1])
                               | ((value << shifts[base + 2])
                                  & writeMasks[base+2]);
    }

    @Override
    public void set(int index, int value) {
        super.set(index, value);
        size = Math.max(size, index+1);
    }

    /* Make sure we have room for value at index */
    @Override
    protected void ensureSpace(final int index, final int value) {
        if (index > maxPos || value > maxValue) {
            //noinspection MismatchedQueryAndUpdateOfCollection
            BitsArrayPacked array;
            if (index > maxPos) {
                array = new BitsArrayPacked(
                        (int)((index + 1) * LENGTH_GROWTH_FACTOR),
                        Math.max(maxValue, value));
            } else {
                array = new BitsArrayPacked(maxPos, Math.max(maxValue, value));
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
        if (!(other instanceof BitsArrayPacked)) {
            throw new UnsupportedOperationException(String.format(
                    "Unable to assign from other types of BitsArray. Got %s",
                    other.getClass()));
        }
        BitsArrayPacked bap = (BitsArrayPacked)other;
        clear();
        ensureSpace(bap.maxPos, bap.maxValue); // Safe recursive check
        for (int pos = 0 ; pos < bap.size ; pos++) {
            set(pos, bap.getAtomic(pos));
        }
    }

    @Override
    public String toString() {
        return "BitsArrayPacked(elementBits=" + elementBits + ", size="
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

