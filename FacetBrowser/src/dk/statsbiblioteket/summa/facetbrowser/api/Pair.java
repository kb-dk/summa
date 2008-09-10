/* $Id: Pair.java,v 1.3 2007/10/05 10:20:24 te Exp $
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
package dk.statsbiblioteket.summa.facetbrowser.api;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A standard pair that compares on key.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class Pair <T extends Comparable<T> , S>
        implements Comparable<Pair<T, S>> {
    protected T key;
    protected S value;

    public Pair(T key, S value) {
        this.key = key;
        this.value = value;
    }
    public int compareTo(Pair<T, S> o) {
        if (!o.getClass().equals(getClass())) {
            return 0;
        }
        if (!o.getKey().getClass().equals(key.getClass())) {
            return 0;
        }
        return key.compareTo(o.getKey());
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
    public String toString() {
        //noinspection ObjectToString
        return value + "(" + key + ")";
    }
}
