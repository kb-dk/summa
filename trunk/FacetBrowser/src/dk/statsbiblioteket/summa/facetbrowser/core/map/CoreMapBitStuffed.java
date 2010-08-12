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
import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounter;
import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounterArray;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.util.XProperties;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;
import org.apache.lucene.util.OpenBitSet;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

/**
 * The BitStuffed CoreMap packs pointers from document-id's to facet/tags in an
 * array of integers. This is a very compact representation, with the drawback
 * that it is limited to 31 facets of 134 million tags each.
 * </p><p>
 * The implementation is fast for lookups and counting, but very slow for
 * updating. It should only be used by searchers og indexers for small corpuses.
 * A small corpus means somewhere below 1 million documents with less than
 * 10 million references to tags.
 * @see #add(int, int, int[])
 * @see #add(int, int, int)
 * @see #add(int[], int, int, int) 
 */
// TODO: Handle emptyFacet translation int<->long for open and store
// TODO: Experiment with OpenBitSet from the SOLR project for speed
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "te")
public class CoreMapBitStuffed extends CoreMap32 {
    private static Logger log = Logger.getLogger(CoreMapBitStuffed.class);

    private static final Object META_REFERENCE_COUNT = "references";
    private static final int CONTENT_INITIAL_SIZE = 10000;
    private static final int MIN_GROWTH_SIZE = 1000;
    private static final double CONTENT_GROWTHFACTOR = 1.5;

    private boolean shift = DEFAULT_SHIFT_ON_REMOVE;
    private static final String NULL = "null";

    /**
     * Mapping from docID => start position in {@link #values}. The end position
     * is index[docID+1]. The last value-element in the list will always point
     * to the next free position in values.
     * </p><p>
     * The amount of documents mapped in index is {@link #highestDocID}+1.
     */
    private int[] index;

    /**
     * The highest inserted docID.
     */
    private int highestDocID = -1;

    /**
     * A value is a composite of
     * [facetID({@link #FACETBITS} bits)][tagID({@link #FACETSHIFT} bits].
     */
    private int[] values;
    /**
     * The next free position in the values.
     */
    private int valuePos = 0;

    /**
     * Creates a core map awaiting {@link #open}.
     * @param conf      Configuration for the CoreMap.
     *                  See {@link CoreMap} for details.
     * @param structure The structure for the Facets.
     */
    public CoreMapBitStuffed(Configuration conf, Structure structure) {
        super(conf, structure);
        shift = conf.getBoolean(CONF_SHIFT_ON_REMOVE, shift);
        if (structure.getFacets().size() > FACET_LIMIT) {
            throw new IllegalArgumentException(String.format(
                    "The number of facets in the structure was %d. This "
                    + "CoreMap supports a maximum of %d Facets",
                    structure.getFacets().size(), FACET_LIMIT));
        }
        log.trace("Constructed CoreMapBitStuffed with shifting: " + shift);
    }

    /**
     * {@inheritDoc}
     * @param docID A document ID.
     * @param newValues New values for the local values {@link #values}.
     * @return A delta value.
     */
    @Override
    public int setValues(int docID, int[] newValues) {
        fitStructure(docID, newValues);
        int valueDelta = newValues.length - (index[docID+1] - index[docID]);
        if (valueDelta != 0) {
            if (log.isTraceEnabled()) {
                log.trace(String.format(
                        "assignValues(%d, %d Integer): Adjusting %d values from"
                        + " position %d with delta %d",
                        docID, newValues.length, values.length, index[docID],
                        valueDelta));
            }
            prepareIndexAndValues(docID, valueDelta);
        }

        /* Insert values */
        System.arraycopy(newValues, 0, values, index[docID], newValues.length);
        return valueDelta;
    }

    /**
     * {@inheritDoc}
     * @param docID A document ID.
     * @return True if the document behind the id has any tags.
     */
    @Override
    public boolean hasTags(int docID) {
        return docID < getDocCount() && index[docID] != index[docID + 1];
    }

    /**
     * Helper for assignValues that makes room in the values-array and adjusts
     * the positions in the index.
     * @param docID      The position from where to make room.
     * @param valueDelta The delta for values to adjust.
     */
    private void prepareIndexAndValues(int docID, int valueDelta) {
        /* Make room (or shrink) the values to fit newValues */
        System.arraycopy(values, index[docID+1], values,
                         index[docID+1] + valueDelta,
                         valuePos - index[docID+1]);
        valuePos += valueDelta;
        /* Update the indexes to reflect the change */
        for (int i = docID+1 ; i <= highestDocID+1 ; i++) {
            index[i] += valueDelta;
        }
    }

    private void fitStructure(int docID, int[] tagIDs) {
        //noinspection DuplicateStringLiteralInspection
        index = fitArray(index, docID + 2, "index");
        values = fitArray(values, valuePos + tagIDs.length + 2, "values");

        if (docID > highestDocID) {
            log.trace("Expanding active index to docID " + docID);
            /* The docID is larger than any previously encountered, so we
               extend the active index by setting the new index-entries
                to the end of the value-list. */
            for (int i = highestDocID + 2 ; i <= docID + 1 ; i++) {
                index[i] = valuePos;
            }
            highestDocID = docID;
        }
    }

    /**
     * Ensures that an element can be inserted at pos by potentially expanding
     * the array.
     * @param array the array that should have room for an element at pos.
     * @param pos   the position of an element.
     * @param arrayDesignation the name of the array - used for debugging.
     * @return the array if not expanded and the new array if expanded.
     */
    private int[] fitArray(int[] array, int pos, String arrayDesignation) {
        if (pos > array.length - 1) {
            int newSize = Math.min(
                    Integer.MAX_VALUE,
                    Math.max(pos + 1 + MIN_GROWTH_SIZE,
                             (int)(array.length * CONTENT_GROWTHFACTOR)));
            if (pos > newSize - 1) {
                throw new IllegalArgumentException(String.format(
                        "Unable to expand the array %s of length %d to contain"
                        + " element at position %d as the position is > "
                        + "Integer.MAX_VALUE", arrayDesignation, array.length,
                                               pos));
            }
            //noinspection DuplicateStringLiteralInspection
            log.debug("Expanding " + arrayDesignation + " array from "
                      + array.length + " to " + newSize);
            int[] exp = new int[newSize];
            System.arraycopy(array, 0, exp, 0, array.length);
            return exp;
        }
        return array;
    }

    /**
     * {@inheritDoc}
     * @param docID The docID top remove.
     */
    @Override
    public void remove(int docID) {
        if (docID > highestDocID) {
            throw new IllegalArgumentException(String.format(
                    "Cannot remove non-existing docID %d from map with size %d",
                    docID, getDocCount()));
        }
        setValues(docID, EMPTY);
        if (!shift) {
            return;
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("Shifting index down 1 position from " + docID);

        //for (int indexPos = docID ; indexPos <= highestDocID ; indexPos++) {
        //    index[indexPos] = index[indexPos + 1];
        //}
        System.arraycopy(index, docID+1, index, docID, highestDocID+1);
        highestDocID--;
    }

    private String dump(int[] ints, int max) {
        StringWriter sw = new StringWriter(ints.length * 4);
        sw.append("[");
        for (int i = 0 ; i < ints.length && i < max ; i++) {
            sw.append(Integer.toString(ints[i]));
            if (i < ints.length - 1 && i < max - 1) {
                sw.append(", ");
            }
        }
        sw.append("]");
        return sw.toString();
    }

    /**
     * Expcse the internal state of the index.
     * @return String representation of the internal state of the index.
     */
    protected String exposeInternalState() {
        StringWriter sw = new StringWriter(500);
        // TODO: Make a better toString (make a List with array as backing)
        //noinspection DuplicateStringLiteralInspection
        sw.append("Index: ").append(dump(index, Math.min(20, highestDocID+2)));
        sw.append(", Values: ");
        sw.append(dump(values, Math.min(20, valuePos)));
        return sw.toString();
    }

    /**
     * Request an array of tagIDs for the given document-ID and the given facet.
     * @param docID   the document to get tags for.
     * @param facetID the facet to get tags for.
     * @return an array of facetIDs, belonging to the given facet for the given
     *         document-ID.
     */
    @Override
    public int[] get(int docID, int facetID) {
        if (docID > highestDocID) {
            //noinspection DuplicateStringLiteralInspection
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "Requested %d out of %d documents", docID, getDocCount()));
        }
        int to = index[docID + 1];
        int[] result = new int[to-index[docID]];
        int resultPos = 0;
        for (int i = index[docID] ; i < to ; i++) {
            int value = values[i];
            if (facetID == value >>> FACETSHIFT) {
                result[resultPos++] = value & TAG_MASK;
            }
        }
        int[] reducedResult = new int[resultPos];
        System.arraycopy(result, 0, reducedResult, 0, resultPos);
        return reducedResult;
    }

    private static final int[] EMPTY = new int[0];

    /**
     * {@inheritDoc}
     * @param docID The document ID to get the values from.
     * @return The values for the document behind the given ID.
     */
    @Override
    public int[] getValues(int docID) {
        if (docID > highestDocID) {
            //noinspection DuplicateStringLiteralInspection
            return EMPTY;
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("getValues(" + docID + ") called");
        int to = index[docID + 1];
        if (index[docID] == to) {
            return EMPTY;
        }
        int[] result = new int[to-index[docID]];
        System.arraycopy(values, index[docID], result, 0, to - index[docID]);
        return result;
    }

    /**
     * {@inheritDoc}
     * @throws IOException if error occur while storing meta-/index-/values-
     * data.
     */
    @Override
    public void store() throws IOException {
        log.info("Storing integer-based map with docCount " + getDocCount());
        storeMeta();
        storeIndex(index, getDocCount() + 1);
        storeValues(valuePos);
        log.debug("Finished storing integer-based map");
    }

    /**
     * {@inheritDoc}
     * @param meta Meta data for the facet/tag structure.
     */
    @Override
    protected void enrichMetaBeforeStore(XProperties meta) {
        meta.put(META_REFERENCE_COUNT, valuePos);
    }

    /**
     * {@inheritDoc}
     * @param location The location of the core map.
     * @param forceNew Ignore existing persistent data and create a new map.
     * @return True if the opening of the location was done without errors.
     * @throws IOException If error occur while opening location.
     */
    @Override
    public boolean open(File location, boolean forceNew) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.info(String.format("open(%s, %b) called", location, forceNew));
        setBaseData(location, readOnly);

        if (forceNew) {
            clear();
            return false;
        }

        long startTime = System.currentTimeMillis();
        try {
            openMeta();
            index = openIndex();
/*            for (int i: index) {
                System.out.println("<*= " + i);
            }*/
            highestDocID = index.length-2;
            valuePos = index[index.length-1];
            int valueSize = (int)Math.max(MIN_GROWTH_SIZE,
                                        valuePos * CONTENT_GROWTHFACTOR);
            values = new int[valueSize];
            openValues(valuePos);
        } catch (IOException e) {
            log.info(String.format(
                    "Could not load persistent data for core map at '%s'."
                    + " Creating new core map",
                    location));
            clear();
            return false;
        }
        log.debug(String.format(
                "open(%s, %b) got highestDocID=%d, valuePos=%d in %s ms",
                location,  forceNew, highestDocID, valuePos,
                System.currentTimeMillis() - startTime));
        return true;
    }

    /**
     * {@inheritDoc}     ??
     * @return The doc ID with the highest ID plus one.
     */
    @Override
    public int getDocCount() {
        return highestDocID + 1;
    }

    /**
     * {@inheritDoc}
     * @param tagCounter Counts the occurrences of the tags in the given
     *                   documents.
     * @param docIDs     Ids for the documents from which the tags should be
     *                   counted.
     * @param startPos   Only use the docIDs from this position (inclusive).
     * @param endPos     Only use the docIDs up to this position (inclusive).
     */
    @Override
    public void markCounterLists(
            final TagCounter tagCounter, final DocIDCollector docIDs,
            final int startPos, final int endPos) {
        if (tagCounter instanceof TagCounterArray) {
            markCounterListsOptimized(
                    (TagCounterArray)tagCounter, docIDs, startPos, endPos);
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace("Marking " + docIDs.getDocCount() +" docs " + startPos
                      + " => " + endPos + " from " + docIDs);
        }
        OpenBitSet ids = docIDs.getBits();
        int hitID = startPos;
        boolean outOfBoundsHandled = false;
        while ((hitID = ids.nextSetBit(hitID)) != -1 && hitID <= endPos) {
            try {
                final int to = index[hitID+1];
                for (int i = index[hitID] ; i < to ; i++) {
                    tagCounter.increment(values[i] >>> FACETSHIFT,
                                         values[i] & TAG_MASK);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                if (log.isTraceEnabled() && !outOfBoundsHandled) {
                    //noinspection DuplicateStringLiteralInspection
                    log.debug(String.format(
                            "Index out of bounds for doc %d with "
                            + "index.length=%s, values.length=%s. Ignoring "
                            + "subsequent out-of-bounds for this search",
                            hitID, index == null ? NULL : index.length,
                            values == null ? NULL : values.length), ex);
                    outOfBoundsHandled = true;
                }
            }
            hitID++;
        }
        log.trace("Marking finished");
    }


    public void markCounterListsOptimized(
            final TagCounterArray tagCounter, final DocIDCollector docIDs,
            final int startPos, final int endPos) {
        if (log.isTraceEnabled()) {
            log.trace("Marking " + docIDs.getDocCount() +" docs " + startPos
                      + " => " + endPos + " from " + docIDs);
        }
        final OpenBitSet ids = docIDs.getBits();
        int hitID = startPos;
        boolean outOfBoundsHandled = false;

        int[][] tags = tagCounter.getTags();
        while ((hitID = ids.nextSetBit(hitID)) != -1 && hitID <= endPos) {
            try {
                final int to = index[hitID+1];
                for (int i = index[hitID] ; i < to ; i++) {
                    try {
                        final int v = values[i]; // Performance: Keep inside try
                        tags[v >>> FACETSHIFT][v & TAG_MASK]++;
                    } catch (Exception e) {
                        final int v = values[i]; // Performance: Keep inside try
                        tagCounter.increment(v >>> FACETSHIFT, v & TAG_MASK);
                        tags = tagCounter.getTags();
                    }
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                if (log.isTraceEnabled() && !outOfBoundsHandled) {
                    //noinspection DuplicateStringLiteralInspection
                    log.debug(String.format(
                            "Index out of bounds for doc %d with "
                            + "index.length=%s, values.length=%s. Ignoring "
                            + "subsequent out-of-bounds for this search",
                            hitID, index == null ? NULL : index.length,
                            values == null ? NULL : values.length), ex);
                    outOfBoundsHandled = true;
                }
            }
            hitID++;
        }
        log.trace("Marking finished");
    }

    /**
     * {@inheritDoc}
     * @param facetID  The ID for the facet with affected tags.
     * @param position The lowest position for affected tags.
     * @param delta    The amount that the position should be adjusted with.
     */
    @Override
    public void adjustPositions(int facetID, int position, int delta) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adjusting position for facetID " + facetID + " tags >= "
                  + position + " with delta " + delta);
        for (int vPos = 0 ; vPos < valuePos ; vPos++) {
            int value = values[vPos];
            int facet = value >>> FACETSHIFT;
            int tag = value & TAG_MASK;
            if (facet == facetID && tag >= position) {
                values[vPos] = facet << FACETSHIFT |
                               tag + delta & TAG_MASK;
            }
        }
    }

    /**
     * {@inheritDoc}
     * @param index The index for the value to get.
     * @return {@inheritDoc}
     */
    @Override
    protected long getPersistentValue(int index) {
        return getPersistentValue(values[index] >>> FACETSHIFT,
                                  values[index] & TAG_MASK);
    }

    /**
     * {@inheritDoc}
     * @param position The position of the value in the values array/list.
     * @param value    The value as defined in {@link #VALUES_FILE}.
     */
    @Override
    protected void putValue(int position, long value) {
        if (log.isTraceEnabled()) {
            log.trace("putValue(" + position + ", " + value + " has facetID " 
                      + persistentValueToFacetID(value) + " and tagID "
                      + persistentValueToTagID(value));
        }
        values[position] = calculateValue(persistentValueToFacetID(value),
                                    persistentValueToTagID(value));
    }

    /**
     * {@inheritDoc}
     * @return The {@link #FACET_LIMIT}.
     */
    public int getEmptyFacet() {
        return FACET_LIMIT;
    }

    /**
     * Answers the question, whether the given paramters (dimension, maxContent)
     * can be handled.
     * @param dimension2 Number of dimension.
     * @param maxContent Size of the maximal content.
     * @return True if these numbers can be handled.
     */
    public static boolean canHandle(int dimension2, int maxContent) {
        int limitDim2 = (int) StrictMath.pow(2, FACETBITS);
        int limitMax = (int) StrictMath.pow(2, FACETSHIFT);
        return dimension2 <= limitDim2 && maxContent <= limitMax;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        highestDocID = -1;
        index = new int[CONTENT_INITIAL_SIZE];

        valuePos = 0;
        values = new int[CONTENT_INITIAL_SIZE];
    }

    /**
     * {@inheritDoc}
     * @return This is the same as a call to {@link #exposeInternalState()}.
     */
    @Override
    public String toString() {
        return exposeInternalState();
    }

    /**
     * Set the {@link #shift} of this class.
     * @param shift The new value of the {@link #shift}.
     */
    public void setShift(boolean shift) {
        this.shift = shift;
    }
}