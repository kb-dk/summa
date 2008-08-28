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
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

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
public class CoreMapBitStuffed extends CoreMapImpl implements CoreMap {
    private static Logger log = Logger.getLogger(CoreMapBitStuffed.class);

//    private static final int VERSION = 10000;

    private static final int CONTENT_INITIAL_SIZE = 10000;
    private static final int MIN_GROWTH_SIZE = 1000;
    private static final double CONTENT_GROWTHFACTOR = 1.2;

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
     * is index[docID+1]. If the document has been removed and no shifting has
     * been performed, index[docID] == Integer.MAX_VALUE. The last valud element
     * in the list will always point to the next free position in values.
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
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException(String.format(
                    "This core map only allows %d facets. The ID for the facet "
                    + "in add was %d", FACET_LIMIT, facetID));
        }
        if (docID > getDocCount()) {
            // We could auto expand, but gaps indicate problems with the feeder
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "Adding new documents require the doc-id to be <= (the "
                    + "largest existing doc-id + 1). The doc-id was %d"
                    + " and the highest legalvalue is %d",
                    docID, getDocCount()));
        }
        // Check index capacity
        fitStructure(docID, tagIDs);

        int insertPos = index[docID + 1];
        if (docID == getDocCount()) { // New at the end
            highestDocID = docID;
            if (highestDocID == 0) { // First addition
                index[0] = 0;
                index[1] = tagIDs.length;
                insertPos = 0;
            } else {
                index[getDocCount()] = index[highestDocID] + tagIDs.length;
                insertPos = index[highestDocID];
            }
        } else if (docID <= highestDocID) { // Expand existing
            throw new UnsupportedOperationException(String.format(
                    "Insertion into index not permitted in this core map. "
                    + "Wanted position was %d with an index-length of %d",
                    docID, getDocCount()));
/*            if (docID < highestDocID) { // Move values
                System.arraycopy(values, index[docID + 1], values,
                                 index[docID + 1] + tagIDs.length, valuePos);
            }
            // Update positions
            for (int iPos = docID + 1 ; iPos <= highestDocID + 1 ; iPos++) {
                index[iPos] += tagIDs.length;
            }*/
        }
        // Insert values
        for (int tagPos = 0 ; tagPos < tagIDs.length ; tagPos++) {
            int tagID = tagIDs[tagPos];
            values[insertPos + tagPos] = getValue(facetID, tagID);
        }
        valuePos += tagIDs.length;
    }

    private int getValue(int facetID, int tagID) {
        return facetID << FACETSHIFT | tagID & TAG_MASK;
    }

    private void fitStructure(int docID, int[] tagIDs) {
        if (docID > index.length - 1) {
            int newSize = Math.max(MIN_GROWTH_SIZE,
                                   (int)(index.length * CONTENT_GROWTHFACTOR));
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

    private int EMPTY_VALUE = getValue(getEmptyFacet(), 0);
    public void remove(int docID) {
        if (docID > highestDocID) {
            throw new IllegalArgumentException(String.format(
                    "Cannot remove non-existing docID %d from map with size %d",
                    docID, getDocCount()));
        }
        if (!shift) {
            try {
                for (int i = index[docID] ; i < index[docID + 1] ; i++) {
                    values[i] = EMPTY_VALUE;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                log.warn(String.format(
                        "Out of bounds in remove(%d) with shift %s",
                        docID, shift), e);
            }
            return;
        }
        int removeAmount = index[docID + 1] - index[docID];
        System.arraycopy(values, index[docID + 1],
                         values, index[docID], valuePos - index[docID + 1]);
        valuePos -= removeAmount;
        for (int indexPos = docID ; indexPos <= highestDocID ; indexPos++) {
            index[indexPos] = index[indexPos + 1] - removeAmount;
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
        sw.append("Index: ").append(dump(index, highestDocID+2));
        sw.append(", Values: ").append(dump(values, valuePos));
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
            highestDocID = index[index.length-2];
            valuePos = index[index.length-1];
            int valueSize = (int)Math.max(MIN_GROWTH_SIZE,
                                        valuePos * CONTENT_GROWTHFACTOR);
            values = new int[valueSize];
            openValues(index[index.length-1]);
        } catch (IOException e) {
            log.warn(String.format(
                    "Could not load persistent data for core map at '%s'."
                    + " Creating new core map",
                    location), e);
            clear();
            return false;
        }
        log.debug(String.format("Retrieved %d indexes and %d values in %s ms",
                                index.length, values.length,
                                System.currentTimeMillis() - startTime));
        return true;
    }

    public int getDocCount() {
        return highestDocID + 1;
    }

    public void markCounterLists(TagCounter tagCounter, int[] docIDs,
                                 int startPos, int endPos) {
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("Marking " + docIDs.length +" docs " +
                      startPos + "=>" + endPos);
        }
        int hitID;
        int to;
//        int value;
        for (int hitPos = startPos ; hitPos <= endPos ; hitPos++) {
  //          System.out.println("- Get hitID " + hitPos + "/" + docIDs.length);
            hitID = docIDs[hitPos];
            try {
  //              System.out.println("- Getting to at " + (hitID + 1) + "/" + index.length);
                to = index[hitID+1];
/*                System.out.println("\nHitID: " + hitID +
                                   "(" + index[hitID] +
                                   ") to " + to +
                                   " range: " + (to - index[hitID]));*/
                for (int i = index[hitID] ; i < to ; i++) {
//                    System.out.println("- Fetching value " + i + "/" + values.length);
//                    value = values[i]; // Seems slower than 2 * direct access
/*                    System.out.println("Marking " + (value >>> FACETSHIFT) +
                                       " to " + (value & TAG_MASK));*/
                    tagCounter.increment(values[i] >>> FACETSHIFT,
                                         values[i] & TAG_MASK);
//                    System.out.println("Marked");
//                    counterLists[value >>> FACETSHIFT]
//                                [value &  TAG_MASK]++;
                }
            } catch (Exception ex) {
                if (log.isTraceEnabled()) {
                    //noinspection DuplicateStringLiteralInspection
                    log.trace("Index out of bounds for doc " + hitID, ex);
                }
/*                System.out.println("Exception for hitID " + hitID);
                ex.printStackTrace();
                System.out.println("");*/
                // Don't do anything else, as discussed with Hans and Mads.
                // We throw an error upon open, if the index is larger than
                // the map, so we'll live
            }
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
        values[position] = getValue(persistentValueToFacetID(value),
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

}
