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
package dk.statsbiblioteket.summa.facetbrowser.core.map;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.XProperties;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

import java.io.*;

/**
 * Generic implementation of some methods for CoreMap. The generic methods
 * expects the implementation to mimic the persistens structures internally.
 * If this is not the case, implementations are better of by overriding the
 * default sotre-related methods.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: Implement inserting and skipping (used by builder)
public abstract class CoreMapImpl implements CoreMap {
    private static Logger log = Logger.getLogger(CoreMapImpl.class);

    protected static final int PERSISTENCE_VERSION = 4;

    protected Structure structure;
    protected File location;
    protected boolean readOnly = false;


    @SuppressWarnings({"UnusedDeclaration"}) // We'll probably use conf later
    public CoreMapImpl(Configuration conf, Structure structure) {
        this.structure = structure;
    }

    /**
     * Store the meta-data for the pool, as described in {@link #META_FILE}.
     * @throws IOException if the data could not be stored.
     */
    protected void storeMeta() throws IOException {
        log.trace("Storing meta data to '"
                  + getPersistenceFile(META_FILE) + "'");
        XProperties xp = new XProperties();
        xp.put(META_VERSION, PERSISTENCE_VERSION);
        xp.put(META_DOCUMENTS, getDocCount());
        xp.put(META_FACETS, Strings.join(structure.getFacetNames(), ", "));
        remove(getPersistenceFile(META_FILE), "meta-file");
        xp.store(getPersistenceFile(META_FILE).toString());
    }

    /**
     * Opens existing meta-data and verify their consistency.
     * The current implementation does not allow for reordering of facets
     * without signalling the need for at complete rebuild. If the list
     * of Facets differ in any way from the {@link Structure#getFacetNames},
     * an IOException is thrown.
     * @return xproperties with the meta data.
     * @throws java.io.IOException if the meta-data did not exist or if they
     *                             were incompatible with {@link #structure}.
     */
    // TODO: Allow reordering and decimation of wanted Facets
    protected XProperties openMeta() throws IOException {
        log.trace("openMeta called");
        File meta = getPersistenceFile(META_FILE);
        if (!meta.exists()) {
            throw new FileNotFoundException(String.format(
                    "Could not locate meta file '%s''", meta));
        }
        XProperties xp = new XProperties(meta.toString());
        try {
            if (PERSISTENCE_VERSION != xp.getInteger(META_VERSION)) {
                throw new IOException(String.format(
                        "The version in '%s' was %d. Expected %d", meta,
                        xp.getInteger(META_VERSION), PERSISTENCE_VERSION));
            }
            int documents = xp.getInteger(META_DOCUMENTS);
            if (documents < 0) {
                throw new IOException(String.format(
                        "The number of documents was %d, it must be positive",
                        documents));
            }
            String expected = Strings.join(structure.getFacetNames(), ", ");
            String actual = xp.getString(META_FACETS);
            if (!expected.equals(actual)) {
                throw new IOException(String.format(
                        "The stored Facets was '%s', expected '%s'",
                        actual, expected));
            }
            return xp;
        } catch (Exception e) {
            throw new IOException(String.format(
                    "Exception extracting properties from '%s'", meta), e);
        }
    }

    /**
     * Retrieve a persistent index from {@link #INDEX_FILE}.
     * @return the index consisting of doc-count pointers to values followed
     *         by a pointer to the next logical value. The doc-count is
     *         specified in {@link #META_FILE}.
     * @throws IOException if the index could not be retrieved.
     */
    protected int[] openIndex() throws IOException {
        XProperties xp = openMeta();
        int docCount = xp.getInteger(META_DOCUMENTS);
        int[] index = new int[docCount+1];
        File indexFile = getPersistenceFile(INDEX_FILE);
        log.trace("Loading " + (docCount + 1) + " index-data from '"
                  + indexFile + "'");
        try {
            FileInputStream indexIn = new FileInputStream(indexFile);
            BufferedInputStream indexBuf = new BufferedInputStream(indexIn);
            ObjectInputStream indexData = new ObjectInputStream(indexBuf);
            for (int i = 0 ; i < docCount + 1 ; i++) {
                index[i] = indexData.readInt();
            }
            log.trace("Loaded index from '" + indexFile + "'. Closing streams");
            indexData.close();
            indexBuf.close();
            indexIn.close();
            log.trace("Returning index from '" + indexFile + "'");
            return index;
        } catch (Exception e) {
            throw new IOException(String.format(
                    "Exception while loading index from '%s'",
                    getPersistenceFile(INDEX_FILE)), e);
        }
    }

    /**
     * Load valueCount values from {@link #VALUES_FILE} and populate the
     * structure with the callback {@link #putValue(int, long)}. It is the
     * responsibility of the caller to make sure that putValue can handle
     * positions up til valueCount-1.
     * @param valueCount the number of values to retrieve.
     * @throws IOException if the values could not be retrieved.
     */
    protected void openValues(int valueCount) throws IOException {
        log.trace(String.format("openValues(%d) called", valueCount));
        File valuesFile = getPersistenceFile(VALUES_FILE);
        try {
            long startTime = System.currentTimeMillis();
            FileInputStream indexIn = new FileInputStream(valuesFile);
            BufferedInputStream indexBuf = new BufferedInputStream(indexIn);
            ObjectInputStream indexData = new ObjectInputStream(indexBuf);
            for (int i = 0 ; i < valueCount ; i++) {
                putValue(i, indexData.readLong());
            }
            log.debug(String.format("Retrieved %d values from '%s' in %d ms",
                                    valueCount, valuesFile,
                                    System.currentTimeMillis() - startTime));
            log.trace("openValues: closing readers");
            indexData.close();
            indexBuf.close();
            indexIn.close();
        } catch (Exception e) {
            throw new IOException(String.format(
                    "Unable to retrieve all values from '%s'", valueCount), e);
        }
    }

    /**
     * Callback for {@link #openValues}. It is highly recommended for
     * implementing classes to use the methods
     * {@link #persistentValueToFacetID} and {@link #persistentValueToTagID}
     * to extract facetIDs and tagIDs from values.
     * @param position the position of the value in the values array/list.
     * @param value    the value as defined in {@link #VALUES_FILE}.
     */
    protected abstract void putValue(int position, long value);

    /**
     * Store the given index-data, as described in {@link #INDEX_FILE}.
     * @param index  the index for the values followed by the index for the next
     *               free value.
     * @param length the number of entries in index that should be stored.
     * @throws java.io.IOException if the index could not be stored.
     */
    protected void storeIndex(int[] index, int length) throws IOException {
        log.trace("Storing index data to '"
                  + getPersistenceFile(INDEX_FILE) + "'");
        File tmpIndex = new File(getPersistenceFile(
                INDEX_FILE).toString() + ".tmp");
        remove(tmpIndex, "previously stored core map index");
        FileOutputStream indexOut = new FileOutputStream(tmpIndex);
        BufferedOutputStream indexBuf = new BufferedOutputStream(indexOut);
        ObjectOutputStream indexWrapper = new ObjectOutputStream(indexBuf);
        for (int i = 0 ; i < length ; i++) {
            indexWrapper.writeInt(index[i]);
        }
        log.trace("Finished writing core map index data, closing streams");
        indexWrapper.close();
        indexBuf.close();
        indexOut.close();
        log.trace("Replacing temporary index file with the new one");
        remove(getPersistenceFile(INDEX_FILE), "old index data");
        tmpIndex.renameTo(getPersistenceFile(INDEX_FILE));
        log.trace("Finished index storing");
    }

    /**
     * Stores valueCount values in the file {@link #VALUES_FILE}. This involves
     * a callback to the method {@link #getPersistentValue(int)}.
     * @param valueCount the number of values to store.
     * @throws IOException if the values could not be stored.
     */
    protected void storeValues(int valueCount) throws IOException {
        log.trace("Storing value data to '"
                  + getPersistenceFile(VALUES_FILE) + "'");
        File tmpValues = new File(getPersistenceFile(
                VALUES_FILE).toString() + ".tmp");
        remove(tmpValues, "previously stored core map values");
        FileOutputStream valueOut = new FileOutputStream(tmpValues);
        BufferedOutputStream valueBuf = new BufferedOutputStream(valueOut);
        ObjectOutputStream valueWrapper = new ObjectOutputStream(valueBuf);
        for (int i = 0 ; i < valueCount ; i++) {
            valueWrapper.writeLong(getPersistentValue(i));
        }
        log.trace("Finished writing core map value data, closing streams");
        valueWrapper.close();
        valueBuf.close();
        valueOut.close();
        log.trace("Replacing temporary value file with the new one");
        remove(getPersistenceFile(VALUES_FILE), "old value data");
        tmpValues.renameTo(getPersistenceFile(VALUES_FILE));
        log.trace("Finished value storing");
    }

    /**
     * Callback for {@link #storeValues}. It is highly recommended for
     * implementing classes to extract facetID and tagID for the entry in
     * the internal values-structure and return
     * {@code getPersistentValue(facetID, tagID);}.
     * @param index the index for the value to get.
     * @return the value as defined in {@link #VALUES_FILE}.
     */
    protected abstract long getPersistentValue(int index);

    /**
     * Convert the given facetID and tagID to persistent value format.
     * Note that the special facetID {@link #getEmptyFacet()} will be stored
     * as {@link #PERSISTENT_EMPTY_FACET}.
     * @param facetID the id for the facet.
     * @param tagID   the id for the tag.
     * @return a persistent-compatible encoding of facetID and tagID.
     */
    protected long getPersistentValue(int facetID, int tagID) {
        if (facetID == getEmptyFacet()) {
            return PERSISTENT_EMPTY_FACET;
        }
        return (long)facetID << PERSISTENT_FACET_SHIFT
               | (long)tagID & PERSISTENT_TAG_MASK;
    }

    /**
     * Extract the implementation-specific facetID from the persistent value.
     * This is a direct copy except for emptyFacet which gets converted from
     * {@link #PERSISTENT_EMPTY_FACET} to {@link #getEmptyFacet()}.
     * @param persistentValue a stored value.
     * @return the facetID from the value.
     * @see {@link #VALUES_FILE}.
     */
    protected int persistentValueToFacetID(long persistentValue) {
        if (persistentValue == PERSISTENT_EMPTY_FACET) {
            return getEmptyFacet();
        }
        return (int)(persistentValue >>> PERSISTENT_FACET_SHIFT);
    }

    /**
     * Extract the tagID from the persistent value. This is a direct copy.
     * </p><p>
     * Note: A limitation in this implementation is that the highest tagID
     *       is 2 billion.
     * @param persistentValue a stored value.
     * @return the tagID from the value.
     * @see {@link #VALUES_FILE}.
     */
    protected int persistentValueToTagID(long persistentValue) {
        return (int)(persistentValue & PERSISTENT_TAG_MASK);
    }

    /**
     * Checks basic data for validity and stores them. This should be called
     * by {@link #open}.
     * @param location the location of the core map.
     * @param readOnly if true, the map is read-only.
     */
    protected void setBaseData(File location, boolean readOnly) {
        try {
            checkLocation(location);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format(
                    "setBaseData: Unable to create folder '%s' for core map",
                    location), e);
        }
        this.location = location;
        this.readOnly = readOnly;
        log.trace(String.format("Assigned core map data location '%s' and "
                                + "readOnly %s", location, readOnly));
    }

    private void checkLocation(File location) throws IOException {
        if (!location.exists()) {
            if (!location.mkdirs()) {
            throw new IOException(String.format(
                    "Unable to create folder '%s' for core map", location));
            }
            log.debug(String.format(
                    "Created folder '%s' for core map", location));
        }
    }

    /**
     * Resolve the location for a core map persistence file.
     * @param file the wanted file without path.
     * @return a full qualified path to a core map file.
     */
    protected File getPersistenceFile(String file) {
        return new File(location, file);
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    protected void remove(File file, String description) throws IOException {
        if (file.exists()) {
            log.debug(String.format(
                    "Removing %s file '%s'", description, file));
            if (!file.delete()) {
                throw new IOException(String.format(
                        "Unable to delete %s file '%s'", description, file));
            }
        }
    }
}