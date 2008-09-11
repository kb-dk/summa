/* $Id: CoreMapBitStuffed.java,v 1.6 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.6 $
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
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.util.Logs;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.BitSet;

/**
 * The BitStuffed CoreMap packs pointers from document-id's to facet/tags in an
 * array of integers. This is a very compact representation, with the drawback
 * that it is limited to 31 facets of 134 million tags each.
 * </p><p>
 * It is expected that this structure might some day be memory-mapped.
 * To prepare for this, the following constraint has been introduced:<br />
 * Additions of mappings are always in the end. No in-array editing.<br />
 * @see {@link #add}.
 */
// TODO: Handle emptyFacet translation int<->long for open and store
public class CoreMapBitStuffed extends CoreMapImpl {
    private static Logger log = Logger.getLogger(CoreMapBitStuffed.class);

//    private static final int VERSION = 10000;

    private static final int CONTENT_INITIAL_SIZE = 10000;
    private static final int MIN_GROWTH_SIZE = 1000;
    private static final int MAX_GROWTH_SIZE = 1000 * 1000;
    private static final double CONTENT_GROWTHFACTOR = 1.5;
    /* If true, the order of the values for a given docID are sorted */
    private static final boolean SORT_VALUES = true;

    private static final int FACETBITS =  5;
    private static final int FACETSHIFT = 32 - FACETBITS;
    private static final int TAG_MASK = 0xFFFFFFFF << FACETBITS >>> FACETBITS;
    /**
     * The -1 in the calculation is to make room for the emptyFacet.
     */
    private static final int FACET_LIMIT = (int) StrictMath.pow(2, FACETBITS)-1;

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

    /**
     * Adds the given tags in the given facet to the given docID.
     * @param docID   the ID of a document. This needs to be
     * 0 or <= (the largest existing document-id + 1).
     * @param facetID the ID for a facet.
     * @param tagIDs an array of IDs for tags belonging to the given facet.
     */
    public void add(int docID, int facetID, int[] tagIDs) {
        if (facetID >= structure.getFacets().size()) {
            throw new IllegalArgumentException(String.format(
                    "This core map only allows %d facets. The ID for the facet "
                    + "in add was %d", FACET_LIMIT, facetID));
        }

        if (log.isTraceEnabled()) {
            log.trace(String.format(
                    "add(%d, %d, %d tagIDs) called",
                    docID, facetID, tagIDs.length));
        }

        /* Ensure that both index and values has room for the data */
        fitStructure(docID, tagIDs);

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

        /* We now know that there is room in index and values and that the
           entries in each are consistent. Time to find out what values to
           update for the document.
         */

        if (tagIDs.length == 0) { // No new tags, so we just exit
            log.trace("No tags specified for doc #" + docID
                      + " and facet #" + facetID);
            return;
        }


        /* Get the existing values for the docID and add the new values */
        Set<Integer> newValues = new HashSet<Integer>(getValues(docID));
        for (int tagID: tagIDs) {
            newValues.add(calculateValue(facetID, tagID));
        }

        /* Make room in values for the new data and change the index to reflect
           the new gap-size */
        assignValues(docID, newValues);
    }

    /**
     * Assign the given values to the docID. This will replace all existing
     * values for the docID. This method does not check if there is room
     * enough, so {@link #fitStructure} should be called beforehand.
     * @param docID     the document to update.
     * @param newValues the values (FacetID/TagID-pairs encoded to an int) to
     *                  assign to the document.
     * @return the number of positions that the values were shiftet.
     */
    private int assignValues(int docID, Set<Integer> newValues) {

        int valueDelta = newValues.size() - (index[docID+1] - index[docID]);
        if (valueDelta != 0) {
            if (log.isTraceEnabled()) {
                log.trace(String.format(
                        "assignValues(%d, %d Integer): Adjusting %d values from"
                        + " position %d with delta %d",
                        docID, newValues.size(), values.length, index[docID],
                        valueDelta));
            }
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

        /* Insert values */
        int position = 0;
        for (Integer value: newValues) {
            values[index[docID] + position++] = value;
        }
        if (SORT_VALUES) {
            Arrays.sort(values, index[docID], index[docID+1]);
        }
        return valueDelta;
    }

    private int calculateValue(int facetID, int tagID) {
        if (facetID < 0) {
            throw new IllegalArgumentException(String.format(
                    "The facetID must be >= 0. It was %d", facetID));
        }
        if (tagID < 0) {
            throw new IllegalArgumentException(String.format(
                    "The tagID must be >= 0. It was %d", tagID));
        }
        return facetID << FACETSHIFT | tagID & TAG_MASK;
    }

    private void fitStructure(int docID, int[] tagIDs) {
        if (docID > index.length - 2) {
            int newSize = Math.min(
                    MAX_GROWTH_SIZE, Math.max(
                    MIN_GROWTH_SIZE,
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
            int newSize = (int)Math.max(MIN_GROWTH_SIZE, Math.max(
                                        values.length * CONTENT_GROWTHFACTOR,
                                        values.length + tagIDs.length));
            //noinspection DuplicateStringLiteralInspection
            log.debug("Expanding value array from " + values.length
                      + " to " + newSize);
            int[] exp = new int[newSize];
            System.arraycopy(values, 0, exp, 0, values.length);
            values = exp;
        }
    }

//    private int EMPTY_VALUE = calculateValue(getEmptyFacet(), 0);
    private Set<Integer> EMPTY_SET = new HashSet<Integer>(0);
    public void remove(int docID) {
        if (docID > highestDocID) {
            throw new IllegalArgumentException(String.format(
                    "Cannot remove non-existing docID %d from map with size %d",
                    docID, getDocCount()));
        }
        assignValues(docID, EMPTY_SET);
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
        //noinspection DuplicateStringLiteralInspection
        // TODO: Make a better toString (make a List with array as backing)
        sw.append("Index: ").append(Logs.expand(Arrays.asList(index),
                                                Math.min(20, highestDocID+2)));
        sw.append(", Values: ");
        sw.append(Logs.expand(Arrays.asList(values), 
                              Math.min(20, valuePos)));
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

    private List<Integer> getValues(int docID) {
        if (docID > highestDocID) {
            //noinspection DuplicateStringLiteralInspection
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "Requested %d out of %d documents", docID, getDocCount()));
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("getValues(" + docID + ") called");
        int to = index[docID + 1];
        List<Integer> result = new ArrayList<Integer>((to-index[docID]) * 2);
        for (int i = index[docID] ; i < to ; i++) {
            result.add(values[i]);
        }
        return result;
    }

    public void store() throws IOException {
        log.info("Storing integer-based map");
        storeMeta();
        storeIndex(index, getDocCount() + 1);
        storeValues(valuePos);
        log.debug("Finished storing integer-based map");
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
            highestDocID = index.length-2;
            valuePos = index[index.length-1];
            int valueSize = (int)Math.max(MIN_GROWTH_SIZE,
                                        valuePos * CONTENT_GROWTHFACTOR);
            values = new int[valueSize];
            openValues(valuePos);
        } catch (IOException e) {
            log.warn(String.format(
                    "Could not load persistent data for core map at '%s'."
                    + " Creating new core map",
                    location), e);
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

    public void markCounterLists(TagCounter tagCounter, DocIDCollector docIDs,
                                 int startPos, int endPos) {
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("Marking " + docIDs.getDocCount() +" docs " + startPos
                      + " => " + endPos + " from " + docIDs);
        }
//        System.out.println("*** " + docIDs);
        BitSet ids = docIDs.getBits();
        int hitID = startPos;
        int to;
        boolean outOfBoundsHandled = false;
        while ((hitID = ids.nextSetBit(hitID)) != -1 && hitID <= endPos) {
//            System.out.println("- Get hitID " + hitID + "/" + docIDs.getDocCount());
            try {
  //              System.out.println("- Getting to at " + (hitID + 1) + "/" + index.length);
                to = index[hitID+1];
/*                System.out.println("\nHitID: " + hitID +
                                   "resolved to values (" + index[hitID] +
                                   " to " + to +
                                   ") = " + (to - index[hitID]));*/
                for (int i = index[hitID] ; i < to ; i++) {
//                    System.out.println("- Fetching value " + i + "/" + values.length);
//                    value = values[i]; // Seems slower than 2 * direct access
//                    System.out.println("Marking " + (values[i] >>> FACETSHIFT) +
//                                       " to " + (values[i] & TAG_MASK));
                    tagCounter.increment(values[i] >>> FACETSHIFT,
                                         values[i] & TAG_MASK);
//                    System.out.println("Marked");
//                    counterLists[value >>> FACETSHIFT]
//                                [value &  TAG_MASK]++;
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
/*                System.out.println("Exception for hitID " + hitID);
                ex.printStackTrace();
                System.out.println("");*/
                // Don't do anything else, as discussed with Hans and Mads.
                // We throw an error upon open, if the index is larger than
                // the map, so we'll live
            }
            hitID++;
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("Marking finished");
    }

    public void adjustPositions(int facetID, int position, int delta) {
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

    protected long getPersistentValue(int index) {
        return getPersistentValue(values[index] >>> FACETSHIFT,
                                  values[index] & TAG_MASK);
    }

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

    public String toString() {
        return exposeInternalState();
    }

    public void setShift(boolean shift) {
        this.shift = shift;
    }
}



