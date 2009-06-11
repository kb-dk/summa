/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.common.lucene.distribution;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.pool.SortedPool;
import dk.statsbiblioteket.summa.common.pool.MemoryPool;
import dk.statsbiblioteket.summa.common.pool.DiskPool;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.FactoryPool;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * Persistent structure for TermStats from an index. This is basically a list
 * that allows for lookups in O(log n).
 * </p><p>
 * Note: {@link #open} or {@link #create} must be called before the TermStat
 * can be updated or queries. 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStat {
    private static Log log = LogFactory.getLog(TermStat.class);

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String TERMSTAT_PERSISTENT_NAME = "termstats";

    /**
     * If true, the terms are memory based. If false, the content is stored on
     * storage during operation.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_MEMORYBASED =
            "common.distribution.termstat.memorybased";
    public static final boolean DEFAULT_MEMORYBASED = false;

    private SortedPool<TermEntry> termCounts;
    private boolean memoryBased = DEFAULT_MEMORYBASED;
    private FactoryPool<TermEntry> entryPool;

    public TermStat(Configuration conf) {
        memoryBased = conf.getBoolean(CONF_MEMORYBASED, memoryBased);
        //noinspection DuplicateStringLiteralInspection
        log.debug(String.format("Constructed %s-based TermStat",
                                memoryBased ? "memory" : "storage"));
        if (memoryBased) {
            termCounts = new MemoryPool<TermEntry>(
                    new TermValueConverter(), null);
        } else {
            termCounts = new DiskPool<TermEntry>(
                    new TermValueConverter(), null);
        }
        entryPool = new FactoryPool<TermEntry>() {
            @Override
            protected TermEntry createNewElement() {
                return new TermEntry("DummyTermEntry", 0); 
            }
        };
    }

    /**
     * Open the TermStats at the given location.
     * @param location where the TermStats are stored.
     * @param readOnly recommended if the Termstats are not to be modified as it
     *                 allows for internal speed-optimizations.
     * @return true if the open was successful.
     * @throws IOException if the open failed due to invalid persistent data.
     */
    public boolean open(File location, boolean readOnly) throws IOException {
        if (!location.exists()) {
            throw new FileNotFoundException("Unable to locate " + location);
        }
        return termCounts.open(
                location, TERMSTAT_PERSISTENT_NAME, readOnly, false);
    }

    /**
     * Create a persistent structure at the given location, making it ready for
     * updates.
     * @param location where the TermStats should be stored.
     * @return true if the creation succeded.
     * @throws IOException if the structure could not be created.
     */
    public boolean create(File location) throws IOException {
        return termCounts.open(
                location, TERMSTAT_PERSISTENT_NAME, false, true);
    }

    /**
     * Update the persistent data for the term stat representation. This uses
     * the location given in {@link #open} or {@link #create}.
     * @throws IOException if the data could not be stored.
     */
    public void store() throws IOException {
        termCounts.store();
    }

    /**
     * Close down all connections to persistent files and free memory. Only
     * {@link #create} or {@link #open} must be called after close.
     */
    public void close() {
        termCounts.close();
    }

    /**
     * Add the term entry to the stats. {@link#cleanup} must be called when adds
     * has been finished.
     * @param value the term and term count to add.
     * @return true if the value was added.
     */
    public boolean dirtyAdd(TermEntry value) {
        return termCounts.dirtyAdd(value);
    }

    /**
     * Ensures that the structure is consistent. This must be called after
     * calls to {@link #dirtyAdd}.
     */
    public void cleanup() {
        termCounts.cleanup();
    }

    /**
     * Locate the given value in the Structure. Note that this only uses the
     * term embedded in the TermEntry and ignores the term count.
     * @param value the value to locate.
     * @return the position of the given value in the structure or -1 if it does
     *         not exist.
     */
    public int indexOf(TermEntry value) {
        return termCounts.indexOf(value);
    }

    /**
     * Returns the term count or 0 if the term is not present.
     * @param term the (@code field:value} to resolve the count for.
     * @return the count for the given term og 0 of the term does not exist.
     */
    public int getTermCount(String term) {
        TermEntry te = entryPool.get().setTerm(term);
        int pos = indexOf(te);
        return pos == -1 ? 0 : get(pos).getCount();
    }
    
    /**
     * @return the number of TermEntries in the structure.
     */
    public int size() {
        return termCounts.size();
    }

    /**
     * @param index the index for the wanted TermEntry.
     * @return the TermEntry at index.
     */
    public TermEntry get(int index) {
        return termCounts.get(index);
    }

    /**
     * Convenience pointer for inter-package use (primarily merging).
     */
    int position = 0;
}
