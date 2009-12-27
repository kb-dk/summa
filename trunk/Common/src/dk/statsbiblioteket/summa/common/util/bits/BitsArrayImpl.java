/**
 * Created: te 27-12-2009 14:38:34
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.common.util.bits;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.AbstractList;
import java.util.Arrays;

/**
 * Building block for packed BitsArrays. Handles bookkeeping.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class BitsArrayImpl extends AbstractList<Integer>
                                                          implements BitsArray {

    /**
     * The factor to multiply to the position when a {@link #set} triggers a
     * growth of the underlying structure.
     */
    public static final double LENGTH_GROWTH_FACTOR = 1.2;

    static final int BLOCK_SIZE = 32; // 32 = int, 64 = long
    static final int BLOCK_BITS = 6; // The #bits representing BLOCK_SIZE

    /* The number of bits representing an element */
    int elementBits;
    /* The number of blocks. */
    int size;
    /* The bits */
    int[] blocks;


    public abstract void set(int index, int value);

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
    public Integer set(int index, Integer element) {
        Integer oldVal = index <= size ? get(index) : null;
        set(index, element.intValue());
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
