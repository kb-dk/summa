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

import java.util.AbstractList;
import java.util.Arrays;

/**
 * A map from positive int positions to positive int values. The internal
 * representation is akin to {@link BitsArrayAligned} with the notable
 * difference that the amount of bits allocated for values are always 1, 2, 4,
 * 8, 16 or 32 so that the values always fit inside a single integer.
 * </p><p>
 * The map auto-expands to accommodate given values at given positions.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BitsArrayAligned extends AbstractList<Integer> implements BitsArray{
    private static Log log = LogFactory.getLog(BitsArrayAligned.class);

    /**
     * The factor to multiply to the position when a {@link #set} triggers a
     * growth of the underlying structure.
     */
    public static final double LENGTH_GROWTH_FACTOR = 1.2;

    static final int BLOCK_SIZE = 32; // 32 = int, 64 = long
    static final int BLOCK_BITS = 6; // The #bits representing BLOCK_SIZE

    static final int ENTRY_SIZE = BLOCK_SIZE + 1;
    static final int FAC_ELEBITS = ENTRY_SIZE * 4;
    static final int FAC_BITPOS = 4;

    /**
     * Algorithm for efficient bit-gets without conditionals: {@code
       elementPos = index * BLOCK_SIZE / elementBits
       bitPos  =    index * BLOCK_SIZE % elementBits
       value =
       (elements[elementPos] <<    SHIFTS[elementBits][bitPos][0] |
        elements[elementPos] >>>   SHIFTS[elementBits][bitPos][1] |
        elements[elementPos+1] >>> SHIFTS[elementBits][bitPos][2]) &
       MASKS[elementBits]}
     */
    private static final int[] SHIFTS = new int[ENTRY_SIZE * ENTRY_SIZE * 4];
            //new int[BLOCK_SIZE+1][BLOCK_SIZE][BLOCK_SIZE+1];
    private static final int[] MASKS = new int[BLOCK_SIZE+1];

    { // Generate shifts
        for (int elementBits = 1 ; elementBits <= BLOCK_SIZE ; elementBits++) {
            MASKS[elementBits] = ~(~0 << elementBits);
            for (int bitPos = 0 ; bitPos < BLOCK_SIZE ; bitPos++) {
                // ?????xxx : elementBits=3, bitPos=5
                if (bitPos <= BLOCK_SIZE - elementBits) { // Single block
                    SHIFTS[elementBits* FAC_ELEBITS + bitPos * FAC_BITPOS] =
                            BLOCK_SIZE; // Ignore
                    // ?????xxx : elementBits=3, bitPos=5
                    SHIFTS[elementBits* FAC_ELEBITS + bitPos * FAC_BITPOS + 1] =
                            BLOCK_SIZE - elementBits - bitPos;
                    SHIFTS[elementBits* FAC_ELEBITS + bitPos * FAC_BITPOS + 2] =
                            BLOCK_SIZE; // Ignore
                } else { // Two blocks
                    // ??????xx x??????? : elementBits=3, bitPos=6
                    SHIFTS[elementBits* FAC_ELEBITS + bitPos * FAC_BITPOS] =
                            bitPos + elementBits - BLOCK_SIZE;
                    SHIFTS[elementBits* FAC_ELEBITS + bitPos * FAC_BITPOS + 1] =
                            BLOCK_SIZE; // Ignore
                    SHIFTS[elementBits* FAC_ELEBITS + bitPos * FAC_BITPOS + 2] =
                            BLOCK_SIZE -
                         SHIFTS[elementBits* FAC_ELEBITS + bitPos * FAC_BITPOS];
                }
                // To handle shifting into oblivion, shifts of BLOCK_SIZE are
                // adjusted to shift further (see the getAtomic method).
                for (int i = 0 ; i < 3 ; i++) {
                    if (SHIFTS[elementBits* FAC_ELEBITS
                               + bitPos * FAC_BITPOS + i] == BLOCK_SIZE) {
                        SHIFTS[elementBits* FAC_ELEBITS
                               + bitPos * FAC_BITPOS + i] =
                                BLOCK_SIZE | (BLOCK_SIZE - 1);
                    }
                }
            }
        }
    }

    /* The number of bits representing an element */
    private int elementBits;
    /* The number of elements. */
    private int size;
    /* The bits */
    private int[] elements;

    // Cached calculations
    private int maxValue;    // Math.pow(alementBits, 2)
    private int maxPos;      // elements.length * BLOCK_SIZE / elementBits - 1
    private int elementMask; // The mask for the bits for an element

    /**
     * Creates a BitsArray with the internal structures optimized for the given
     * limits. This is the recommended constructor.
     * @param length   the number of expected elements.
     * @param maxValue the expected maximum value for an element.
     */
    public BitsArrayAligned(int length, int maxValue) {
        int bits = BitsArrayFactory.bits(maxValue);
        log.trace("Creating BitsArrayPacked of length " + length
                  + " with element bit size " + bits + " for max value "
                  + maxValue);
        setAttributes(
                bits, 0, new int[length * bits / BLOCK_SIZE + 2]);
    }

    private void setAttributes(int elementBits, int size, int[] elements) {
        this.elementBits = elementBits;
        this.size = size;
        this.elements = elements;
        updateCached();
    }

    private void updateCached() {
        elementMask = MASKS[elementBits]; //~0 >>> (BLOCK_SIZE - elementBits);
        maxValue = (int)Math.pow(2, elementBits)-1;
        maxPos = (elements.length * BLOCK_SIZE / elementBits) - 2;
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
        final int bitPos = (int)(majorBitPos - (elementPos << (BLOCK_BITS-1))); // * BLOCK_SIZE);
        //int bitPos =     (int)(majorBitPos % BLOCK_SIZE);

/*        long element0 = ((long)(0x7FFFFFFF & elements[elementPos])) |
                        (((long)(elements[elementPos] >>> 1)) << 1);
        long element1 = ((long)(0x7FFFFFFF & elements[elementPos+1])) |
                        (((long)(elements[elementPos+1] >>> 1)) << 1);
  */
//        long element0 = ((long)elements[elementPos])   & 0x00000000FFFFFFFFL;
//        long element1 = ((long)elements[elementPos+1]) & 0x00000000FFFFFFFFL;

//        return (int)(element0 + element1);

/*        final int[] specificShifts = currentShifts[bitPos];
        return (int)(((element0 <<  4) |
                 (element0 >>> 5) |
                 (element1 >>> 1) | specificShifts[0])
                & elementMask);

        /**/
        final int base = elementBits * FAC_ELEBITS + bitPos * FAC_BITPOS;
//        return bitPos;
        //return base;

        final int first = elements[elementPos];
        return (((first
                  << (SHIFTS[base] & (BLOCK_SIZE-1)))
                 << (SHIFTS[base] >>> 5))
                | ((first
                    >>> (SHIFTS[base+1] & (BLOCK_SIZE-1)))
                   >>> (SHIFTS[base+1] >>> 5))
                | ((elements[elementPos+1]
                    >>> (SHIFTS[base+2] & (BLOCK_SIZE-1)))
                   >>> (SHIFTS[base+2] >>> 5)))
               & elementMask;

    }

    /* No checks for capacity or maxValue */
    private void unsafeSet(int index, int value) {
        // TODO: Implement this
        long bitPos = index * elementBits;

        // Samples: 3 bits/value (A, B and C) stored in bytes
        // Case A: ???ABC??
        // Case B: ???????A BC??????

        int bytePos =    (int)(bitPos / BLOCK_SIZE); // Position in bytes
        int subPosLeft = (int)(bitPos % BLOCK_SIZE); // Position in the bits at bytePos
        // Case A: subPosLeft == 3, Case B: subPosLeft == 7

        // The number of remaining bits at bytePos+1
        int subRemainingBits = elementBits - (BLOCK_SIZE - subPosLeft);
        // Case A: -2, Case B: 2

        if (subRemainingBits > 0) {
            // Case B: ???????A BC??????
            elements[bytePos] =
                    ((elements[bytePos] & (~0 << (elementBits - subPosLeft))))
                    | (value >>> subRemainingBits);
            elements[bytePos+1] =
                    ((elements[bytePos+1] & (~0 >>> subRemainingBits)))
                    | (value << (BLOCK_SIZE - subRemainingBits));
        } else {
            // Case A: ???ABC??, subPosLeft == 3, subRemainingBits == -2
            elements[bytePos] =
                    (elements[bytePos]
                     & ((subPosLeft == 0 ? 0 : ~0 << (BLOCK_SIZE - subPosLeft))
                        | (~0 >>> (elementBits - -subRemainingBits))))
                    | (value << (BLOCK_SIZE - subPosLeft - elementBits));
/*            if (log.isTraceEnabled()) {
                log.trace(String.format(
                        "unsafeSet(index=%d, value=%d) -> elements[%d] bits %s",
                index, value, bytePos, Long.toBinaryString(elements[bytePos])));
            }*/
        }
        size = Math.max(size, index+1);
    }

    /**
     * Set the element at the given position. The least significant elementSize
     * bits from value will be used.
     * @param position the position for the value.
     * @param value    the value to assign.
     */
    public void set(int position, int value) {
        ensureSpace(position, value);
        try {
            unsafeSet(position, value);
        } catch (ArrayIndexOutOfBoundsException e) {
            String message = String.format(
                    "Internal array inconsistency for %s, setting %d at "
                    + "position %d", this, value, position);
            throw new IllegalStateException(message, e);
        }
    }

    /* Make sure we have room for value at position */
    private void ensureSpace(int position, int value) {
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
            setAttributes(array.elementBits, array.size, array.elements);
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
        return "BitsArrayPacked(elementBits=" + elementBits + ", size="
               + size + ", maxPos=" + maxPos
               + ", elments.length=" + elements.length + ")";
    }

    /**
     * Clears the array of values but maintains the internal buffers.
     * This does not free any resources.
     */
    @Override
    public void clear() {
        size = 0;
        Arrays.fill(elements, 0);
    }

    /* List interface */


    /**
     * Getter from the List interface. This uses auto boxing. For fast access,
     * {@link #getAtomic(int)} is recommended.
     * @param index the position of the wanted value.
     * @return the value at the position.
     */
    @Override
    public Integer get(int index) {
        return getAtomic(index);
    }

    /**
     * Setter from the List interface. This uses auto boxing. For fast access,
     * {@link #set(int, int)} is recommended.
     */
    @Override
    public Integer set(int index, Integer element) {
        Integer oldVal = index <= size ? get(index) : null;
        set(index, element.intValue());
        return oldVal;
    }

    @Override
    public int size() {
        return size;
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

    /**
     * @return the number of bytes used for the internal array.
     */
    public int getMemSize() {
        return elements.length * BLOCK_SIZE / 8;
    }

    public int getMaxValue() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}