/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.facetbrowser.core.map;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.XProperties;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Generic implementation of some methods for CoreMap. The generic methods
 * expects the implementation to mimic the persistent structures internally.
 * If this is not the case, implementations are better off by overriding the
 * default store-related methods.
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
        log.trace("Storing meta data for CoreMap to '"
                  + getPersistenceFile(META_FILE) + "'");
        XProperties xp = new XProperties();
        xp.put(META_VERSION, PERSISTENCE_VERSION);
        xp.put(META_DOCUMENTS, getDocCount());
        xp.put(META_FACETS, Strings.join(structure.getFacetNames(), ", "));
        enrichMetaBeforeStore(xp);
        File metaFile = getPersistenceFile(META_FILE);
        if (!metaFile.getParentFile().exists()) {
            //noinspection DuplicateStringLiteralInspection
            log.debug("store: Folder '" + metaFile.getParentFile()
                      + " does not exist. Attempting creation");
            if (!metaFile.getParentFile().mkdirs()) {
                throw new IOException(String.format(
                        "Unable to create folder '%s' for CoreMap",
                        metaFile.getParentFile()));
            }
        }
        remove(metaFile, "meta-file");
        xp.store(new FileOutputStream(metaFile), null);
    }

    /**
     * Enrich the meta data for the persistent facet/tag structure.
     * Implementations should provide statistics and other useful information
     * here. 
     * @param meta meta data for the facet/tag structure.
     */
    protected void enrichMetaBeforeStore(XProperties meta) {

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
        } catch (FileNotFoundException e) {
            throw new IOException(String.format(
                    "File not found, attempting to retrieve values from '%s'",
                    valuesFile), e);
        } catch (IOException e) {
            throw new IOException(String.format(
                    "IOException, attempting to retrieve values from '%s'",
                    valuesFile), e);
        } catch (Exception e) {
            throw new IOException(String.format(
                    "Unable to retrieve all values from '%s'", valuesFile), e);
        }
    }

    /**
     * Callback for {@link #openValues}. Puts the facet/tag-pair defined by the
     * value at position in the array of values. Note that this implies an
     * underlying structure that can reflect the persistent format.
     * </p><p>
     * It is highly recommended forimplementing classes to use the methods
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
        //noinspection DuplicateStringLiteralInspection
        log.trace("Storing " + length + " index data to '"
                  + getPersistenceFile(INDEX_FILE) + "'");
        File tmpIndex = new File(getPersistenceFile(
                INDEX_FILE).toString() + ".tmp");
        remove(tmpIndex, "previously stored core map index");
        FileOutputStream indexOut = new FileOutputStream(tmpIndex);
        BufferedOutputStream indexBuf = new BufferedOutputStream(indexOut);
        ObjectOutputStream indexWrapper = new ObjectOutputStream(indexBuf);
        for (int i = 0 ; i < length ; i++) {
            indexWrapper.writeInt(index[i]);
//            System.out.println("*=> " + index[i]);
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

    /**
     * Clears the given map and assigns copies all mappings to the new map.
     * This has optimization for CoreMapBitStuffed, but works for all CoreMaps.
     * @param other the map to assign to.
     */
    public void copyTo(CoreMap other) {
        log.trace("Performing copyTo from type " + this.getClass().getName()
                  + " to " + other.getClass().getName());
        long starttime = System.currentTimeMillis();
        List<Integer> facetIDs = new ArrayList<Integer>(
                structure.getFacetNames().size());
        for (Map.Entry<String, Integer> facet:
                structure.getFacetIDs().entrySet()) {
            facetIDs.add(facet.getValue());
        }
        other.clear();
        for (int docID = 0 ; docID < getDocCount() ; docID++) {
            for (Integer facetID: facetIDs) {
                int[] tagIDs = get(docID, facetID);
                if (tagIDs.length != 0) {
                    other.add(docID, facetID, tagIDs);
                }
            }
        }
        log.debug("Finished copyTo from type " + this.getClass().getName()
                  + " to " + other.getClass().getName() + " in "
                  + (System.currentTimeMillis() - starttime) + " ms");
    }
}

