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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: CoreMapBitStuffed.java,v 1.6 2007/10/04 13:28:21 te Exp $
     *
     * This is a special-purpose array that allocates memory for each assignment.
     * Pro: Memory-usage is near optimal, compared to a dynamic-length
     * triple-indexed array.
     * Con: The state of non-assigned entries is undefined.
     *      The entries must be added sequentially.
     *      All entries must be assigned, before querying.
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
 * @see {@link #add}.
 */
// TODO: Handle emptyFacet translation int<->long for open and store
// TODO: Experiment with OpenBitSet from the SOLR project for speed
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class CoreMapBitStuffed extends CoreMap32 {
    private static Logger log = Logger.getLogger(CoreMapBitStuffed.class);

//    private static final int VERSION = 10000;

    private static final Object META_REFERENCE_COUNT = "references";
    private static final int CONTENT_INITIAL_SIZE = 10000;
    private static final int MIN_GROWTH_SIZE = 1000;
//    private static final int MAX_GROWTH_SIZE = 1000 * 1000;
    private static final double CONTENT_GROWTHFACTOR = 1.5;

    private boolean shift = DEFAULT_SHIFT_ON_REMOVE;

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
     * @param conf      configuration for the CoreMap.
     *                  See {@link CoreMap} for details.
     * @param structure the structure for the Facets.
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
    
//    private void init(int docCount, int facetCount) {
  //      docCapacity = docCount;
    //    finalIndex = docCount -1;
//        index = new int[docCapacity + 1];
//    }


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
/*        int position = 0;
        for (Integer value: newValues) {
            values[index[docID] + position++] = value;
        }*/
        return valueDelta;
    }

    @Override
    public boolean hasTags(int docID) {
        return docID < getDocCount() && index[docID] != index[docID + 1];
    }

    /**
     * Helper for assignValues that makes room in the values-array and adjusts
     * the positions in the index.
     * @param docID      the position from where to make room.
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
/*        if (docID > index.length - 2) {
            int newSize = Math.min(
                    index.length + MAX_GROWTH_SIZE, Math.max(
                    index.length + MIN_GROWTH_SIZE,
                    (int)(index.length * CONTENT_GROWTHFACTOR)));
            //noinspection DuplicateStringLiteralInspection
            log.debug("Expanding index array from " + index.length
                      + " to " + newSize);
            int[] exp = new int[newSize];
            System.arraycopy(index, 0, exp, 0, index.length);
            index = exp;
        }
        // Check value capacity
        if (valuePos + tagIDs.length >= values.length) {
            int newSize = (int)Math.max(
                    values.length * CONTENT_GROWTHFACTOR,
                    values.length + tagIDs.length + 1);
            //noinspection DuplicateStringLiteralInspection
            log.debug("Expanding value array from " + values.length
                      + " to " + newSize);
            int[] exp = new int[newSize];
            System.arraycopy(values, 0, exp, 0, values.length);
            values = exp;
        }*/
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

//    private int EMPTY_VALUE = calculateValue(getEmptyFacet(), 0);
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
        for (int indexPos = docID ; indexPos <= highestDocID ; indexPos++) {
            index[indexPos] = index[indexPos + 1];
        }
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
    @Override
    public int[] getValues(int docID) {
        if (docID > highestDocID) {
            //noinspection DuplicateStringLiteralInspection
/*            log.warn(String.format("Requested %d out of %d documents",
                                   docID, getDocCount()));*/
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

    public void store() throws IOException {
        log.info("Storing integer-based map with docCount " + getDocCount());
        storeMeta();
        storeIndex(index, getDocCount() + 1);
        storeValues(valuePos);
        log.debug("Finished storing integer-based map");
    }
    @Override
    protected void enrichMetaBeforeStore(XProperties meta) {
        meta.put(META_REFERENCE_COUNT, valuePos);
    }

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

    public int getDocCount() {
        return highestDocID + 1;
    }

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
                            hitID, index == null ? "null" : index.length,
                            values == null ? "null" : values.length), ex);
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
                            hitID, index == null ? "null" : index.length,
                            values == null ? "null" : values.length), ex);
                    outOfBoundsHandled = true;
                }
            }
            hitID++;
        }
        log.trace("Marking finished");
    }



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

    @Override
    protected long getPersistentValue(int index) {
        return getPersistentValue(values[index] >>> FACETSHIFT,
                                  values[index] & TAG_MASK);
    }

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

    public int getEmptyFacet() {
        return FACET_LIMIT;
    }

    public static boolean canHandle(int dimension2, int maxContent) {
        int limitDim2 = (int) StrictMath.pow(2, FACETBITS);
        int limitMax = (int) StrictMath.pow(2, FACETSHIFT);
        return dimension2 <= limitDim2 && maxContent <= limitMax;
    }

    public void clear() {
        highestDocID = -1;
        index = new int[CONTENT_INITIAL_SIZE];

        valuePos = 0;
        values = new int[CONTENT_INITIAL_SIZE];
    }

    @Override
    public String toString() {
        return exposeInternalState();
    }

    public void setShift(boolean shift) {
        this.shift = shift;
    }
}

