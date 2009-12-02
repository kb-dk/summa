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

import dk.statsbiblioteket.summa.common.pool.CollatorSortedPool;
import dk.statsbiblioteket.summa.common.pool.DiskStringPool;
import dk.statsbiblioteket.summa.common.pool.MemoryStringPool;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.util.CachedCollator;
import dk.statsbiblioteket.util.InvalidPropertiesException;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.XProperties;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.*;

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
 * </p><p>
 * The Facet extends the persistent files from pools with a meta-file containing
 * name, fields and locale. If the fields or the locale differs upon open, the
 * persistent data are classified as invalid.
 */
// TODO: Supply real-world measurements for conventional harddisks and SSDs
// TODO: Resort upon changed locale instead of discarding (must signal core map)
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class Facet implements CollatorSortedPool {
    private Log log = LogFactory.getLog(Facet.class);

    private static final String META_FILE_POSTFIX = ".facet";
    private final static String META_NAME = "name";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private final static String META_FIELDS = "fields";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private final static String META_LOCALE = "locale";
    private static final Object META_FACET_COUNT = "facetCount";

    private FacetStructure structure;
    private CollatorSortedPool pool;
    private boolean readOnly;

    /**
     * Constructs a Facet from the given properties. If createNew is set to
     * false, it is the responsibility of the caller to either ensure that the
     * existing Facet was created with the same collator as specified or that
     * {@link #cleanup()} is called after creation. 
     * @param structure  the static definition of a Facet. This holds values
     * @param useMemory  hold the entire structure in memory. See the java-doc
     *                   for the class for details.
     * @param readOnly   if true, updates are not possible.
     * @throws IOException if an existing structure could not be loaded.
     */
    public Facet(FacetStructure structure, boolean useMemory, boolean readOnly)
                                                            throws IOException {
        this.structure = structure;
        this.readOnly = readOnly;
        //noinspection DuplicateStringLiteralInspection
        log.debug(String.format(
                "Creating %s-based pool for Facet '%s'",
                useMemory ? "memory" : "storage", structure.getName()));
        if (useMemory) {
            pool = new MemoryStringPool(getCollatorFromPool(structure));
        } else {
            pool = new DiskStringPool(getCollatorFromPool(structure));
        }
        log.debug(String.format("Facet '%s' is constructed",
                                structure.getName()));
    }

    private static Map<String, Collator> collators =
            new HashMap<String, Collator>(5);
    private static synchronized Collator getCollatorFromPool(FacetStructure
            structure) {
        if (structure.getLocale() == null) {
            return null;
        }
        if (collators.get(structure.getLocale()) == null) {
            collators.put(
                    structure.getLocale(),
                    // TODO: Consider if it is safe to use summa_extracted here
                    // Maybe check for locale == "da"?
                    new CachedCollator(
                            new Locale(structure.getLocale()),
                            CachedCollator.COMMON_SUMMA_EXTRACTED, true));
        }
        return collators.get(structure.getLocale());
    }

    /* Mutators */


    /**
     * Loads persistent Facet data. If no data exists, the underlying structure
     * creates needed files and are made ready for filling.
     * @param location the base folder for the persistent data.
     * @return true if a open could be performed, false if there was no existing
     *              structure or if the structure differed from expectations.
     * @throws IOException if there was an I/O error.
     */
    public boolean open(File location) throws IOException {
        boolean forceNew = true;
        File metaFile = getPoolPersistenceFile(
                location, structure.getName(), META_FILE_POSTFIX);
        try {
            //noinspection MismatchedQueryAndUpdateOfCollection
            XProperties xp = new XProperties(metaFile.toString());
            String n = structure.getName();
            String f = Strings.join(Arrays.asList(structure.getFields()), ", ");
            String l = structure.getLocale() == null ? "":structure.getLocale();
            if (xp.containsKey(META_NAME)) {
                if (n.equals(xp.getString(META_NAME))
                    && f.equals(Strings.join(
                        Arrays.asList(xp.getString(META_FIELDS)), ", "))
                    && l.equals(xp.getString(META_LOCALE))) {
                    forceNew = false;
                } else {
                    log.info(String.format(
                            "The meta-data for the persistent facet did not "
                            + "match the structure for Facet '%s' at '%s', "
                            + "forcing new",
                            structure.getName(), location));
                }
            } else {
                log.info("No name located in properties for '" + metaFile
                         + ". Forcing new facets");
            }
        } catch (IOException e) {
            log.debug(String.format(
                    "Could not open facet meta data for pool '%s' at '%s', "
                    + "no persistent data will be loaded",
                    pool.getName(), metaFile), e);
        } catch (InvalidPropertiesException e) {
            log.warn(String.format(
                    "Invalid properties detected while attempting to open facet"
                    + " meta data for pool '%s' at '%s', no persistent data"
                    + " loaded",
                    pool.getName(), metaFile), e);
        } catch (Exception e) {
            log.warn(String.format(
                    "Exception while attempting to open facet meta data for "
                    + "pool '%s' at '%s', no persistent data loaded",
                    pool.getName(), metaFile), e);
        }
        try {
            log.debug(String.format(
                    "Opening facet '%s' at '%s', read only: %s, force new: %s",
                    structure.getName(), location, readOnly, forceNew));
            return pool.open(location, structure.getName(), readOnly, forceNew);
        } catch (IOException e) {
            if (!forceNew) {
                if (readOnly) {
                    throw new IOException(String.format(
                            "Unable to open pool '%s' at '%s' in "
                            + "read only mode",
                            structure.getName(), location), e);
                }
                log.warn(String.format(
                        "IOException while opening pool '%s' at '%s', "
                        + "attempting to force a new structure",
                        structure.getName(), location), e);
                try {
                    return pool.open(location, structure.getName(), readOnly,
                                     true);
                } catch (IOException e2) {
                    throw new IOException(String.format(
                            "Unable to force new facet '%s' at '%s' at second "
                            + "attempt", structure.getName(), location), e2);
                }
            }
            throw new IOException(String.format(
                    "Unable to force new facet '%s' at '%s'",
                    structure.getName(), location), e);
        }
    }
    public boolean open(File location, String poolName, boolean readOnly,
                        boolean forceNew) throws IOException {
       throw new UnsupportedOperationException("Use (open(File) instead");
    }


    public void store() throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.debug(String.format("store() called for Facet '%s'",
                                structure.getName()));
        File metaFile = pool.getPoolPersistenceFile(META_FILE_POSTFIX);
        XProperties xp = new XProperties(metaFile.toString());
        xp.put(META_NAME, structure.getName());
        String f = Strings.join(Arrays.asList(structure.getFields()), ", ");
        xp.put(META_FIELDS, f);
        String l = structure.getLocale() == null ? "":structure.getLocale();
        xp.put(META_LOCALE, l);
        xp.put(META_FACET_COUNT, size());
        log.trace(String.format("Storing meta file '%s' for Facet '%s'",
                                metaFile, structure.getName()));
        //System.out.println(metaFile.getParentFile());
        if (!metaFile.getParentFile().exists()) {
            //noinspection DuplicateStringLiteralInspection
            log.debug("store: Folder '" + metaFile.getParentFile()
                      + " does not exist. Attempting creation");
            if (!metaFile.getParentFile().mkdirs()) {
                throw new IOException(String.format(
                        "Unable to create folder '%s' for Facet '%s'",
                        metaFile.getParentFile(), getName()));
            }
        }
        xp.store(new FileOutputStream(metaFile), null);

        log.trace(String.format("Storing %d Tags for Facet '%s'",
                                size(), structure.getName()));
        pool.store();
    }

    public File getPoolPersistenceFile(String postfix) {
        return pool.getPoolPersistenceFile(postfix);
    }

    public File getPoolPersistenceFile(File location, String poolName,
                                       String postfix) {
        return pool.getPoolPersistenceFile(location, poolName, postfix);
    }

    public void close() {
        log.debug(String.format("close() called for Facet '%s'",
                                structure.getName()));
        if (pool == null) {
            return;
        }
        pool.close();
    }

    /* CollatorSortedPool delegations */

    public Collator getCollator() {
        return pool.getCollator();
    }

    public void setCollator(Collator collator) {
        log.warn("setLocale should be used instead of setCollator");
        pool.setCollator(collator);
        cleanup();
    }

    /* SortedPool delegations */

    public String getName() {
        return structure.getName();
    }

    public int insert(String value) {
        return pool.insert(value);
    }

    public void cleanup() {
        pool.cleanup();
    }

    public boolean add(String value) {
        return pool.add(value);
    }

    public boolean dirtyAdd(String value) {
        return pool.dirtyAdd(value);
    }

    public void add(int index, String element) {
        pool.add(index, element);
    }

    public String remove(int position) {
        return pool.remove(position);
    }

    public String get(int position) {
        return pool.get(position);
    }

    public int indexOf(String value) {
        return pool.indexOf(value);
    }

    public String set(int index, String element) {
        return pool.set(index, element);
    }

    public String[] getFields() {
        return structure.getFields();
    }

    public String getLocale() {
        return structure.getLocale();
    }

    /* List delegations */

    public int size() {
        return pool.size();
    }

    public boolean isEmpty() {
        return pool.isEmpty();
    }

    public boolean contains(Object o) {
        return pool.contains(o);
    }

    public Iterator<String> iterator() {
        return pool.iterator();
    }

    public Object[] toArray() {
        return pool.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return pool.toArray(a);
    }

    public boolean remove(Object o) {
        return pool.remove(o);
    }

    public boolean containsAll(Collection<?> c) {
        return pool.containsAll(c);
    }

    public boolean addAll(Collection<? extends String> c) {
        return pool.addAll(c);
    }

    public boolean addAll(int index, Collection<? extends String> c) {
        return pool.addAll(index, c);
    }

    public boolean removeAll(Collection<?> c) {
        return pool.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return pool.retainAll(c);
    }

    public void clear() {
        pool.clear();
    }

    public int indexOf(Object o) {
        return pool.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        return pool.lastIndexOf(o);
    }

    public ListIterator<String> listIterator() {
        return pool.listIterator();
    }

    public ListIterator<String> listIterator(int index) {
        return pool.listIterator(index);
    }

    public List<String> subList(int fromIndex, int toIndex) {
        return pool.subList(fromIndex, toIndex);
    }
}




