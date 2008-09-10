/* $Id: FlexiblePair.java,v 1.3 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/05 10:20:24 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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

/**
 * Pair with flexible sorting.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FlexiblePair<T extends Comparable<T> , S extends Comparable<S>>
        implements Comparable<FlexiblePair<T, S>> {
    public enum SortType {PRIMARY_ASCENDING, PRIMARY_DESCENDING,
                          SECONDARY_ASCENDING, SECONDARY_DESCENDING}
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
