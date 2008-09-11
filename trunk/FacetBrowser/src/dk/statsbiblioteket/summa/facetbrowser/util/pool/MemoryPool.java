/* $Id: MemoryPool.java,v 1.4 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/04 13:28:21 $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: MemoryPool.java,v 1.4 2007/10/04 13:28:21 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.util.pool;

import dk.statsbiblioteket.util.LineReader;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The MemoryPool stores all values in RAM. No connection to any underlying
 * persistent data are maintained during use, except when opening and storing.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MemoryPool<E extends Comparable<E>> extends SortedPoolImpl<E> {
    private Log log = LogFactory.getLog(MemoryPool.class);

    private static final int DEFAULT_SIZE = 1000;
    private static final double EXPAND_FACTOR = 1.2;

    protected List<E> values;

    public MemoryPool(ValueConverter<E> valueConverter,
                          Comparator comparator) {
        super(valueConverter, comparator);
        log.trace("Created MemoryPool");
    }

    public boolean open(File location, String poolName, boolean readOnly,
                        boolean forceNew) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Open(" + location + ", " + poolName + ", " + readOnly 
                  + ", " + forceNew + ") called");
        setBaseData(location, poolName, readOnly);
        if (forceNew) {
            log.debug(String.format("Force creating new pool '%s' at '%s'",
                                    poolName, location));
            values = new ArrayList<E>(DEFAULT_SIZE);
            return false;
        }
        if (!(getIndexFile().exists() && getValueFile().exists())) {
            log.debug(String.format("No existing data for '%s' at '%s'. "
                                    + "Creating new pool", poolName, location));
            values = new ArrayList<E>(DEFAULT_SIZE);
            return false;
        }
        log.trace(String.format("Attempting load of index for '%s' from '%s'",
                                poolName, getIndexFile()));
        log.debug(String.format(
                "Loading index and values for pool '%s' at '%s'",
                poolName, getIndexFile()));
        long[] index = loadIndex();
        log.debug(String.format("Index loaded for pool '%s' at '%s'",
                                poolName, getIndexFile()));

        LineReader lr = new LineReader(getValueFile(), "r");
        values =  new ArrayList<E>(readOnly ? index.length :
                                   (int)(index.length * EXPAND_FACTOR));
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(index.length);
        for (long element : index) {
            add(readValue(lr, element));
            profiler.beat();
        }
        log.debug(String.format("Loaded values for '%s' from '%s' in %s",
                                poolName, getValueFile(),
                                profiler.getSpendTime()));
        lr.close();
        return true;
    }

    public void close() {
        if (values != null) {
            values.clear();
        }
    }

    public void store() throws IOException {
        store(location, poolName);
    }

    protected void sort() {
        Collections.sort(this, this); // Works fine for memory-based
    }

    /* List interface delegations */

    public E remove(int position) {
        E e = values.remove(position);
        //noinspection DuplicateStringLiteralInspection
        log.trace("Removed element '" + e + "' at position " + position);
        return e;
    }

    public E get(int position) {
        // No trace here, as it needs to be FAST
        return values.get(position);
    }

    public int size() {
        return values.size();
    }

    public void clear() {
        values.clear();
    }

    public void add(int index, E element) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding '" + element + "' at position " + index);
        values.add(index, element);
    }

    public E set(int index, E element) {
        return values.set(index, element);
    }

}


