/* $Id$
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
      * In order to make an efficient value-getter, conditionals should be
      * avoided. For this BitsArray, values are always aligned so that they fit
      * inside of a single block. Extraction of a value thus requires a single
      * SHR followed by a mask.
      * </p><p>
      * In code, this is {@code
       elementPos = index * BLOCK_SIZE / elementBits
       bitPos  =    index * BLOCK_SIZE % elementBits
       value =
       (blocks[elementPos] >> SHIFTS[elementBits][bitPos]) &
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
        int bits = BitsArrayFactory.bits(maxValue);
        if (bits > 16) {
            bits = 32;
        } else if (bits > 8) {
            bits = 16;
        } else if (bits > 4) {
            bits = 8;
        } else if (bits > 2) {
            bits = 4;
        }
        log.trace("Creating BitsArrayAligned of length " + length
                  + " with element bit size " + bits + " for max value "
                  + maxValue);
        setAttributes(bits, 0, new int[length * bits / BLOCK_SIZE + 2]);
    }

    private void setAttributes(int elementBits, int size, int[] blocks) {
        this.elementBits = elementBits;
        this.size = size;
        this.blocks = blocks;
        updateCached();
    }

    private void updateCached() {
        shifts = SHIFTS[elementBits];
        readMask = READ_MASKS[elementBits];
        writeMasks = WRITE_MASKS[elementBits];
        maxValue = (int)Math.pow(2, elementBits)-1;
        maxPos = (blocks.length * BLOCK_SIZE / elementBits) - 2;
    }

    /**
     * @param index the position of the value.
     * @return the value at the given position.
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

        final int elementPos = (int)(majorBitPos >>> (BLOCK_BITS-1)); // / BLOCK_SIZE
        final int bitPos = (int)(majorBitPos - (elementPos << (BLOCK_BITS-1))); // % BLOCK_SIZE);

        return (blocks[elementPos] >>> shifts[bitPos]) & readMask;
    }

    @Override
    protected void unsafeSet(int index, int value) {
        final long majorBitPos = index * elementBits;

        final int elementPos = (int)(majorBitPos >>> (BLOCK_BITS-1)); // / BLOCK_SIZE
        final int bitPos = (int)(majorBitPos - (elementPos << (BLOCK_BITS-1))); // % BLOCK_SIZE);

        blocks[elementPos] = (blocks[elementPos] & writeMasks[bitPos])
                             | (value << shifts[bitPos]);
        size = Math.max(size, index+1);
    }


    /* Make sure we have room for value at position */
    @Override
    protected void ensureSpace(int position, int value) {
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
    public void assign(BitsArray other) {
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