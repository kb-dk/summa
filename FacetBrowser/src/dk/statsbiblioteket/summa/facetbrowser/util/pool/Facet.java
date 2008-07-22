/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.facetbrowser.util.pool;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.text.Collator;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Facet is a named collection of Tags. This will normally map to a field with
 * corresponding terms in an index. Facets can be group-based, in which case
 * they will map to multiple fields and their terms.
 * </p><p>
 * Facets can be filesystem- or memory-based. Filesystem-based facets are
 * penalized at lookup- and update-time, but offers fast start-up and shutdown.
 * Memory-based facets has the reverse performance characteristics.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class Facet implements CollatorSortedPool {
    private Log log = LogFactory.getLog(Facet.class);

    private CollatorSortedPool pool;
    private File location;
    private String name;

    /**
     * Constructs a Facet from the given properties. If createNew is set to
     * false, it is the responsibility of the caller to either ensure that the
     * existing Facet was created with the same collator as specified or that
     * {@link #cleanup()} is called after creation. 
     * @param location   the folder with the Facet data.
     * @param name       the name of the Facet. This is used to generate file
     *                   names and should normally be a field in an index.
     * @param collator   used to provide localized sorting of Tags. If null,
     *                   Unicode-sorting is used.
     * @param createNew  ignore any existing Tags and start with no Tags.
     * @param useMemory  hold the entire structure in memory. See the java-doc
     *                   for the class for details.
     * @throws IOException if an existing structure could not be loaded.
     */
    public Facet(File location, String name, Collator collator,
                 boolean createNew, boolean useMemory) throws IOException {
        this.location = location;
        this.name = name;
        if (useMemory) {
            log.debug("Creating memory-based pool for Facet '" + name + "'");
            pool = new MemoryStringPool();
            if (!createNew) {
                pool.load(location, name);
            }
        } else {
            log.debug("Creating disk-based pool for Facet '" + name + "'");
            pool = new DiskStringPool(location, name, createNew);
        }
        if (collator != null) {
            log.debug("Assigning Collator to Facet '" + name + "'");
            pool.setCollator(collator);
        }
        log.debug(String.format("Facet '%s' at '%s' is ready for use",
                                name, location));
    }

    /**
     * Load the content for the Facet from the previously stated location.
     * @throws IOException if the data could not be loaded.
     */
    public void load() throws IOException {
        log.debug(String.format(
                "Reloading content for Facet '%s' at '%s", name, location));
        pool.load(location, name);
    }

    /**
     * Store the content of the Facet at the previously stated location.
     * </p><p>
     * Note: This will not trigger a {@link #cleanup()}. The only way to ensure
     * a complete cleanup with erasure of all orphaned Tags is to store at a
     * new location or with a new name.
     * @throws IOException if the data could not be stored.
     */
    public void store() throws IOException {
        pool.store(location, name);
    }

    /* Delegated methods */

    public Collator getCollator() {
        return pool.getCollator();
    }

    public void setCollator(Collator collator) {
        log.debug("setCollator called. This triggers a clean-up");
        pool.setCollator(collator);
        cleanup();
    }

    public int add(String value) {
        return pool.add(value);
    }

    public void dirtyAdd(String value) {
        pool.dirtyAdd(value);
    }

    public void remove(int position) {
        pool.remove(position);
    }

    public int size() {
        return pool.size();
    }

    public void cleanup() {
        pool.cleanup();
    }

    public void clear() {
        pool.clear();
    }

    public int getPosition(String value) {
        return pool.getPosition(value);
    }

    public String getValue(int position) {
        return pool.getValue(position);
    }

    public void load(File location, String poolName) throws IOException {
        this.location = location;
        name = poolName;
        pool.load(location, poolName);
    }

    public void store(File location, String poolName) throws IOException {
        this.location = location;
        name = poolName;
        pool.store(location, poolName);
    }

    public int compare(String o1, String o2) {
        return pool.compare(o1, o2);
    }
}
