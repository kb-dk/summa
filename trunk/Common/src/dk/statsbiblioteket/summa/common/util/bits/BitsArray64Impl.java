/**
 * Created: te 27-12-2009 14:38:34
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.common.util.bits;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.AbstractList;
import java.util.Arrays;

/**
 * Building block for packed BitsArrays with long[] as internal placeholder.
 * Handles bookkeeping.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class BitsArray64Impl extends AbstractList<Integer>
                                                          implements BitsArray {

    /**
     * The factor to multiply to the position when a {@link #set} triggers a
     * growth of the underlying structure.
     */
    public static final double LENGTH_GROWTH_FACTOR = 1.2;

    static final int BLOCK_SIZE = 64; // 32 = int, 64 = long
    static final int BLOCK_BITS = 7; // The #bits representing BLOCK_SIZE

//    static final int BLOCK_SIZE = 64; // 32 = int, 64 = long
//    static final int BLOCK_BITS = 7; // The #bits representing BLOCK_SIZE

    /* The number of bits representing an element */
    int elementBits;
    /* The number of blocks. */
    int size;
    /* The bits */
    long[] blocks;


    /**
     * Set the element at the given index.
     * @param index the position for the value.
     * @param value the value to assign.
     */
    public void set(int index, int value) {
        ensureSpace(index, value);
        try {
            unsafeSet(index, value);
        } catch (ArrayIndexOutOfBoundsException e) {
            String message = String.format(
                    "Internal array inconsistency for %s, setting %d at "
                    + "position %d", this, value, index);
            throw new IllegalStateException(message, e);
        }
    }

    /**
     * Set the element at the given index, without checks for boundaries.
     * @param index the position for the value.
     * @param value the value to assign.
     */
    protected abstract void unsafeSet(int index, int value);

    /**
     * Ensure that the internal structure can accept the given value at the
     * given index.
     * @param index where to set the value.
     * @param value the value to set.
     */
    protected abstract void ensureSpace(int index, int value);

    /**
     * @param maxValue a value.
     * @return the number of bits needed to represent the given value.
     */
    protected int calculateBits(int maxValue) {
        return (int)Math.ceil(Math.log(((long)maxValue)+1)/Math.log(2));
    }

    /**
     * Clears the array of values but maintains the internal buffers.
     * This does not free any resources.
     */
    @Override
    public void clear() {
        size = 0;
        Arrays.fill(blocks, 0);
    }

    /**
     * Setter from the List interface. This uses auto boxing. For fast access,
     * {@link #set(int, int)} is recommended.
     */
    @Override
    public Integer set(int index, Integer value) {
        Integer oldVal = index <= size ? get(index) : null;
        set(index, value.intValue());
        return oldVal;
    }

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

    @Override
    public int size() {
        return size;
    }

    /**
     * @return the number of bytes used for the internal array.
     */
    public int getMemSize() {
        return blocks.length * BLOCK_SIZE / 8;
    }

}