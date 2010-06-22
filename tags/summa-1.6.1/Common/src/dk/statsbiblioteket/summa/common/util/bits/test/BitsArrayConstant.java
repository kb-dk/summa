/**
 * Created: te 29-12-2009 14:56:59
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.common.util.bits.test;

import dk.statsbiblioteket.summa.common.util.bits.BitsArray;

import java.util.AbstractList;

public class BitsArrayConstant extends AbstractList<Integer> implements BitsArray {
    @Override
    public Integer get(final int index) {
        return 87;
    }

    @Override
    public int size() {
        return 88;  // TODO: Implement this
    }

    public int getAtomic(final int index) {
        return 89;
    }
    public int fastGetAtomic(final int index) {
        return 89;
    }

    public void set(final int position, final int value) {}

    public void fastSet(int index, int value) {}

    public void assign(final BitsArray other) {}

    public int getMemSize() {
        return 90;
    }

    public int getMaxValue() {
        return 91;
    }

    @Override
    public void clear() {}

}
