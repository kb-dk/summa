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

import java.io.Serializable;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Pair with flexible sorting.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FlexiblePair<T extends Comparable<T>, S extends Comparable<S>>
        implements Comparable<FlexiblePair<T, S>>, Serializable {
    public enum SortType {PRIMARY_ASCENDING, PRIMARY_DESCENDING,
                          SECONDARY_ASCENDING, SECONDARY_DESCENDING}
    private static final long serialVersionUID = 94819491L;
    protected T key;
    protected S value;

    private SortType sortType;

    public FlexiblePair(T key, S value, SortType sortType) {
        this.key = key;
        this.value = value;
        this.sortType = sortType;
    }
    public int compareTo(FlexiblePair<T, S> o) {
        if (!o.getClass().equals(getClass())) {
            return 0;
        }
        if (!o.getKey().getClass().equals(key.getClass())) {
            return 0;
        }
        // TODO: Move this out of the class, as it is prone to error
        // TODO: Sont on secondary value?
        switch (sortType) {
            case PRIMARY_ASCENDING:
                return key.compareTo(o.getKey());
            case PRIMARY_DESCENDING:
                return -1 * key.compareTo(o.getKey());
            case SECONDARY_ASCENDING:
                return value.compareTo(o.getValue());
            case SECONDARY_DESCENDING:
                return -1 * value.compareTo(o.getValue());
            default:
                return 0;
        }
    }
    public void setKey(T key) {
        this.key = key;
    }
    public T getKey() {
        return key;
    }
    public void setValue(S value) {
        this.value = value;
    }
    public S getValue() {
        return value;
    }
    public SortType getSortType() {
        return sortType;
    }
    public void setSortType(SortType sortType) {
        this.sortType = sortType;
    }
    public String toString() {
        //noinspection ObjectToString
        return value + "(" + key + ")";
    }
}




