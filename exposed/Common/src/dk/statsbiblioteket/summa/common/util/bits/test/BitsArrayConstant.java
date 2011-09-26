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
package dk.statsbiblioteket.summa.common.util.bits.test;

import dk.statsbiblioteket.summa.common.util.bits.BitsArray;

import java.util.AbstractList;

/**
 * Constants for BitsArray performance test.
 */
public class BitsArrayConstant extends AbstractList<Integer>
                                                          implements BitsArray {
    @Override
    public Integer get(final int index) {
        return 87;
    }

    @Override
    public int size() {
        return 88;  // TODO Implement this
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
    public void clear() { }
}
