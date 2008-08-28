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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import dk.statsbiblioteket.summa.facetbrowser.util.pool.SortedPool;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * PROBLEMS: Duplicates wreak havoc and there is no checking for duplicates.
 * @deprecated in favor of {@link SortedPool}.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public class SortedHash<T extends Comparable> {
    private static Logger log = Logger.getLogger(SortedHash.class);
//    private LinkedList<T> list;
    private ArrayList<T> vector; // TODO: Make do without Vector
    private HashMap<T, Integer> hashmap = null;
    private boolean autoAdd = false;
    private T lastNotFound = null;

    private boolean hashed = false;
    private boolean sorted = true;

    public SortedHash() {
        log.debug("Creating Sorted Hash");
        vector = new ArrayList<T>(5000);
    }

    public SortedHash(int initialSize) {
        log.debug("Creating Sorted Hash with initial size " + initialSize);
        vector = new ArrayList<T>(initialSize);
//        list = new LinkedList<T>();
    }

    public boolean add(T value) {
        vector.add(value);
//        list.add(value);
        hashed = false;
        sorted = false;
        return true;
    }

    public T get(int index) {
        if (!sorted) {
            sortVector();
        }
        return vector.get(index);
    }

    public int get(T value) {
        if (value == "") {
            log.trace("Asked for the index for the empty String");
            return -1;
        }
        if (!hashed) {
            cleanup();
        }

        Integer result =  hashmap.get(value);
        if (result == null) {
            if (autoAdd) {
                log.debug("The tag \"" + value +"\" was not in the index. " +
                          "The tag is now added, which invalidates previous " +
                          "indexes.");
                add(value);
                cleanup();
                result =  hashmap.get(value);
                if (result == null) {
                    log.warn("Unable to add \"" + value + "\", returning -1");
                    return -1;
                }
                return result;
            } else {
                if (lastNotFound != null && !lastNotFound.equals(value)) {
                    log.warn("Could not locate index for \"" + value +
                             "\" out of "+
                             vector.size() + " elements");
                }
                return -1;
            }
        }
        return result;
    }

    public ArrayList<T> getSorted() {
        if (!sorted) {
            sortVector();
        }
        return vector;
    }

    public List<T> getList() {
        return new LinkedList<T>(vector);
    }

    private void cleanup() {
        log.debug("Cleaning up " + vector.size() + " elements");
        sortVector();

        hashmap = new HashMap<T, Integer>(vector.size());
        int counter = 0;
        for (T value: vector) {
            hashmap.put(value, counter++);
        }
        hashed = true;
        log.debug("Clean-up finished");
    }

    private void sortVector() {
        if (hashmap != null) {
            hashmap.clear();
        }
        Collections.sort(vector);
        sorted = true;
        hashed = false;
    }

    /**
     * Releases the hashmap, and thus frees memory.
     */
    public void createVectorHack() {
//        vector = new ArrayList<T>(list.size());
//        vector.addAll(list);
//        list.clear();
        if (hashmap != null) {
            hashmap.clear();
        }
        hashed = false;
        getSorted();
    }

    /**
     * Sort, the remove last element.
     */
    public void removeLast() {
        if (vector.size() > 0) {
            vector.remove(vector.size()-1);
        }
        hashed = true;
    }

    public int size() {
        return vector.size();
    }

    /**
     * Note that setting autoadd will make previous values returned from
     * get out of sync with this SortedHash.
     * @param autoAdd whether or not to auto-add requested values.
     */
    public void setAutoAdd(boolean autoAdd) {
        this.autoAdd = autoAdd;
    }

    public boolean getAutoAdd() {
        return autoAdd;
    }

}
