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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.AbstractList;
import java.util.Arrays;

/**
 * A map from positive int positions to positive int values. The internal
 * representation is {@code (max_position + 1) * ceil(log2(max_value))} bits
 * plus overhead.
 * </p><p>
 * Example: The array contains the values {@code [120, 7, 8, 8, 0, 122]}.
 *          ceil(log2(122)) == 7, so 6 * 7 == 42 bits are used for storage.
 * </p><p>
 * The map auto-expands to accomodate given values at given positions. 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BitsArray extends AbstractList<Integer> {
    private static Log log = LogFactory.getLog(BitsArray.class);

    /**
     * The factor to multiply to the position when a {@link #set} triggers a
     * growth of the underlying structure.
     */
    public static final double LENGTH_GROWTH_FACTOR = 1.2;

    /* The number of bits representing an element */
    private int elementBits;
    /* The number of elements. */
    private int size;
    /* The bits */
    private long[] elements;

    // Cached calculations
    private int maxValue;    // Math.pow(alementBits, 2)
    private int maxPos;      // elements.length * 64 / elementBits
    private int elementMask; // The mask for the bits for an element

    /**
     * Creates a BitsArray of minimal size. If there is knowledge of expected
     * values, it is recommended to use {@link #BitsArray(int, int)} instead.
     */
    public BitsArray() {
        this(64, 1);
    }

    /**
     * Creates a BitsArray with the internal structures optimized for the given
     * limits. This is the recommended constructor.
     * @param length   the number of expected elements.
     * @param maxValue the expected maximum value for an element.
     */
    public BitsArray(int length, int maxValue) {
        log.trace("Creating BitsArray of length " + length
                  + " with element size " + elementBits);
        setAttributes((int)Math.ceil(Math.log(maxValue)/Math.log(2)),
                      0,
                      new long[length * elementBits / 64 + 1]);
    }

    private void setAttributes(int elementBits, int size, long[] elements) {
        this.elementBits = elementBits;
        this.size = size;
        this.elements = elements;
        updateCached();
    }

    private void updateCached() {
        elementMask = ~0 >>> (64 - elementBits);
        maxValue = (int)Math.pow(elementBits, 2);
        maxPos = elements.length *64 / elementBits;
    }

    /**
     * @param index the position of the value.
     * @return the value at the given position.
     */
    public int getAtomic(int index) {
        if (index >= size()) {
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "The index %d was requested in an array of size %s",
                    index, size()));
        }
        long bitPos = index * elementBits;

        int bytePos = (int)(bitPos / 64);    // Position in bytes
        int subPosLeft = (int)(bitPos % 64); // Position in the bits at bytePos
        // The number of remaining bits at bytePos+1
        int subRemainingBits = elementBits - (64 - subPosLeft);
        if (subRemainingBits > 0) {
            return (int)(elements[bytePos] << subRemainingBits
                       | elements[bytePos+1] >>> elementBits - subRemainingBits)
                   & elementMask;
        }
        return (int)((elements[bytePos] >>> (64 - elementBits - subPosLeft))
                     & elementMask);
    }

    /**
     * Set the element at the given position. The least significant elementSize
     * bits from value will be used.
     * @param position the position for the value.
     * @param value    the value to assign.
     */
    public void set(int position, int value) {
        ensureSpace(position, value);
        unsafeSet(position, value);
    }

    /* No checks for capacity or maxValue */
    private void unsafeSet(int index, int value) {
        // TODO: Implement this
        long bitPos = index * elementBits;

        int bytePos = (int)(bitPos / 64);    // Position in bytes
        int subPosLeft = (int)(bitPos % 64); // Position in the bits at bytePos
        // The number of remaining bits at bytePos+1
        int subRemainingBits = elementBits - (64 - subPosLeft);
        throw new UnsupportedOperationException("Not implemented yet");
/*        if (subRemainingBits > 0) {
            elements[bytePos] = // Clear bits
                    (elements[bytePos] & ~0L << (elementBits - subPosLeft)) | (value >>> subPosLeft);
                                >> << subRemainingBits
                       | elements[bytePos+1] >>> elementBits - subRemainingBits)
                   & elementMask;
        }
        return (int)((elements[bytePos] >>> (64 - elementBits - subPosLeft))
                     & elementMask);*/
//        size = Math.max(index + 1, size);
    }

    /* Make sure we have room for value at position */
    private void ensureSpace(int position, int value) {
        if (position > maxPos || value > maxValue) {
            BitsArray array = new BitsArray(
                    (int)(position * LENGTH_GROWTH_FACTOR), value);
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
        clear();
        ensureSpace(other.maxPos, maxValue); // Safe recursive check
        for (int pos = 0 ; pos < other.size ; pos++) {
            set(pos, other.getAtomic(pos));
        }
    }

    @Override
    public String toString() {
        return "BitsArray(elementBits=" + elementBits + ", size="
               + size + ", maxPos=" + maxPos + ")";
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

}
