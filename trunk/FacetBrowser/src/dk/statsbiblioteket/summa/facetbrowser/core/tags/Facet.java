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
package dk.statsbiblioteket.summa.facetbrowser.core.tags;

import java.io.File;
import java.io.IOException;
import java.text.Collator;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.facetbrowser.util.pool.CollatorSortedPool;
import dk.statsbiblioteket.summa.facetbrowser.util.pool.MemoryStringPool;
import dk.statsbiblioteket.summa.facetbrowser.util.pool.DiskStringPool;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Facet is a named collection of Tags. This will normally map to a field with
 * corresponding terms in an index. Facets are index-format agnostic and are
 * only concerned with maintaining the structure of Tags.
 * </p><p>
 * Unless a Locale is provided, Tags are sorted in Unicode order. Using a
 * Locale is relevant if the Tags are to be presented is a specific order,
 * such as alphabetically. Note that using a Locale is more processor-heavy
 * than not using one, so only provide a Locale if it is expected that special
 * ordering are to be used.
 * </p><p>
 * Facets can be filesystem- or memory-based. Persistence of data is compatible
 * across the two methods and they can be freely interchanged. Characteristica
 * of the different approaches are as follows, where the time-scale assumes
 * that the Tag-count is in the lower end of millions.
 * </p><p>
 * - Filesystem-based<br />
 * Startup from existing data - seconds (only an index is retrieved).<br />
 * Persistent storing of data - seconds (same as startup).<br />
 * Update of Tags - 100s of milliseconds.<br />
 * Index => Tag lookup - microseconds to milliseconds.<br />
 * Tag => Index lookup - milliseconds to 100s of milliseconds.<br />
 * Cleanup - seconds (re-sorting in case of changed Collator or similar).
 * </p><p>
 * - Memory-based<br />
 * Startup from existing data - minutes (Everything myst be loaded).<br />
 * Persistent storing of data - minutes (same as startup).<br />
 * Update of Tags - milliseconds.<br />
 * Index => Tag lookup - nanoseconds.<br />
 * Tag => Index lookup - milliseconds.<br />
 * Cleanup - 100s of milliseconds to a few seconds.
 * </p><p>
 * The filesystem-based approach is - of course - penalized by requests to the
 * underlying file-system. A seek on a conventional harddisk takes 5-15ms and
 * thus a Tag => Index lookup might take as much as log(n)*15ms. For a million
 * tags, this translates to 150ms worst case scenario. Standard caching by the
 * operating system mitigates this, but experiments verify that the slowdown
 * is noticeable, compared to the memory-based approach. For Solid State Drives,
 * seek penalties are about 1/10ms or less (july, 2008), depending on drive.
 * This translates to 1-2ms worst-case for the scenario above. If a Solid
 * State Drive is available and the Facet has a non-trivial size, it is highly
 * recommended to use the filesystem-based approach on the SSD as it allows
 * for frequent persistence check-points with little penalty.
 */
// TODO: Supply real-world measurements for conventional harddisks and SSDs
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class Facet implements CollatorSortedPool {
    private Log log = LogFactory.getLog(Facet.class);

    private CollatorSortedPool pool;
    private File location;

    private FacetStructure structure;

    /**
     * Constructs a Facet from the given properties. If createNew is set to
     * false, it is the responsibility of the caller to either ensure that the
     * existing Facet was created with the same collator as specified or that
     * {@link #cleanup()} is called after creation. 
     * @param location   the folder with the Facet data.
     * @param structure  the static definition of a Facet. This holds values
     *                   such as name, locale and fields.
     * @param createNew  ignore any existing Tags and start with no Tags.
     * @param useMemory  hold the entire structure in memory. See the java-doc
     *                   for the class for details.
     * @throws IOException if an existing structure could not be loaded.
     */
    public Facet(File location, FacetStructure structure,
                 boolean createNew, boolean useMemory) throws IOException {
        if (location == null || "".equals(location.toString())) {
            throw new IllegalArgumentException("location must be specified");
        }
        this.location = location;
        this.structure = structure;
        if (useMemory) {
            log.debug("Creating memory-based pool for Facet '"
                      + structure.getName() + "'");
            pool = new MemoryStringPool();
            if (!createNew) {
                pool.load(location, structure.getName());
            }
        } else {
            log.debug("Creating disk-based pool for Facet '"
                      + structure.getName() + "'");
            pool = new DiskStringPool(location, structure.getName(), createNew);
        }
        log.debug(String.format("Facet '%s' at '%s' is ready for use",
                                structure.getName(), location));
    }

    /**
     * Load the content for the Facet from the previously stated location.
     * @throws IOException if the data could not be loaded.
     */
    public void load() throws IOException {
        log.debug(String.format(
                "Reloading content for Facet '%s' at '%s", structure.getName(),
                location));
        pool.load(location, structure.getName());
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
        pool.store(location, structure.getName());
    }

    /* Mutators */

    public String getName() {
        return structure.getName();
    }

    public String[] getFields() {
        return structure.getFields();
    }

    public String getLocale() {
        return structure.getLocale();
    }

    /* Delegated methods */

    public Collator getCollator() {
        return pool.getCollator();
    }

    public void setCollator(Collator collator) {
        log.warn("setLocale should be used instead of setCollator");
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

    private static final String NAME_CLASH =
            "%s: The specified poolName '%s' was different from the name of "
            + "the underlying FacetStructure '%s'. Ignoring poolName";
    public void load(File location, String poolName) throws IOException {
        if (!structure.getName().equals(poolName)) {
            log.warn(String.format(NAME_CLASH, "load", poolName,
                                   structure.getName()));
        }
        this.location = location;
        pool.load(location, structure.getName());
    }
    public void store(File location, String poolName) throws IOException {
        if (!structure.getName().equals(poolName)) {
            //noinspection DuplicateStringLiteralInspection
            log.warn(String.format(NAME_CLASH, "store", poolName,
                                   structure.getName()));
        }
        this.location = location;
        pool.store(location, structure.getName());
    }

    public int compare(String o1, String o2) {
        return pool.compare(o1, o2);
    }
}
