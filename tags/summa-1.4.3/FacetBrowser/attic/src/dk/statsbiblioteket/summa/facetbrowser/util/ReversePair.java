/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.facetbrowser.util;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A simple pair with descending sorting order.
 * @deprecated use {@link FlexiblePair} instead.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public class ReversePair <T extends Comparable<T> , S>
        implements Comparable<ReversePair<T, S>> {
    private T key;
    private S value;

    public ReversePair(T key, S value) {
        this.key = key;
        this.value = value;
    }
    public int compareTo(ReversePair<T, S> o) {
        if (!o.getClass().equals(getClass())) {
            return 0;
        }
        if (!o.getKey().getClass().equals(key.getClass())) {
            return 0;
        }
        return -key.compareTo(o.getKey());
    }
    public T getKey() {
        return key;
    }
    public S getValue() {
        return value;
    }
    public String toString() {
        //noinspection ObjectToString
        return value + "(" + key + ")";
    }
}


