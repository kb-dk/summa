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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A range number represents a range as well as a pivot or "best guess" value.
 * It can be seen as a special case of a fuzzy number.
 * $see http://whatis.techtarget.com/definition/0,,sid9_gci283979,00.html
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RangeNumber {
    public static final int NEG_INF = Integer.MIN_VALUE;
    public static final int POS_INF = Integer.MAX_VALUE;

    private final int pivot;
    private final int min;
    private final int max;

    /**
     * The values int.MIN_VALUE designates negative infinity while the
     * values int.MAX_VALUE designates positive infinity.
     * @param pivot the best guess value.
     *              Cannot be negative or positive infinity.
     * @param min   the start of the range, inclusive.
     * @param max   the end of the range, inclusive.
     */
    public RangeNumber(int pivot, int min, int max) {
        if (pivot < min || pivot > max || max < min) {
            throw new IllegalStateException(String.format(
                "The values should be logically consistent with "
                + "min(%d) <= pivot(%d> <= max(%d)", min, pivot, max));
        }
        if (pivot == NEG_INF || pivot == POS_INF) {
            throw new IllegalStateException(String.format(
                "The pivot(%d) must not be negative or positive infinity",
                pivot));
        }
        this.pivot = pivot;
        this.min = min;
        this.max = max;
    }

    /**
     * @param other will be added to this number.
     * @return the result of the addition.
     */
    public RangeNumber add(RangeNumber other) {
        return new RangeNumber(edge((long)getPivot() + (long)other.getPivot()),
                               edge((long)getMin() + (long)other.getMin()),
                               edge((long)getMax() + (long)other.getMax()));
    }
    public int edge(long value) {
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int)value;
    }

    /**
     * @param other will be subtracted from this number.
     * @return the result of the subtraction.
     */
    public RangeNumber subtract(RangeNumber other) {
        return new RangeNumber(edge((long)getPivot() - (long)other.getPivot()),
                               edge((long)getMin() - (long)other.getMin()),
                               edge((long)getMax() - (long)other.getMax()));
    }
    
    /**
     * The minimum of this and other will be assigned to this.
     * @param other .
     * @return the result of the min operation.
     */
    public RangeNumber min(RangeNumber other) {
        return new RangeNumber(
            edge(Math.min((long)getPivot(), (long)other.getPivot())),
            edge(Math.min((long)getMin(), (long)other.getMin())),
            getMax() == POS_INF || other.getMax() == POS_INF ?
            POS_INF : edge(Math.min((long)getMax(), (long)other.getMax())));
    }

    /**
     * The maximum of this and other will be assigned to this.
     * @param other .
     * @return the result of the max operation.
     */
    public RangeNumber max(RangeNumber other) {
        return new RangeNumber(
            edge(Math.max((long) getPivot(), (long) other.getPivot())),
            getMin() == NEG_INF || other.getMin() == NEG_INF ?
            NEG_INF : edge(Math.max((long) getMax(), (long) other.getMax())),
            edge(Math.max((long) getMin(), (long) other.getMin())));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RangeNumber)) {
            return false;
        }
        RangeNumber other = (RangeNumber)obj;
        return getPivot() == other.getPivot() && getMin() == other.getMin()
               && getMax() == other.getMax();
    }

    @SuppressWarnings({"CloneDoesntCallSuperClone"})
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new RangeNumber(getPivot(), getMin(), getMax());
    }

    @Override
    public int hashCode() {
        return getPivot() ^ getMin() ^ getMax();
    }

    /* Plain getters */
    
    public int getPivot() {
        return pivot;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }
}
