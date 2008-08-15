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

import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.File;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounter;
import dk.statsbiblioteket.summa.facetbrowser.util.ClusterCommon;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

/**
 * The BitStuffed CoreMap packs pointers from document-id's to facet/tags in an
 * array of integers. This is a very compact representation, with the drawback
 * that it is limited to 31 facets of 134 million tags each.
 * </p><p>
 * Updates needs to be done, so that any new document-id is either
 * 0 or <= (the largest existing document-id + 1).
 */
// TODO: Handle emptyFacet translation int<->long for load and store
public class CoreMapBitStuffed implements CoreMap {
    private static Logger log = Logger.getLogger(CoreMapBitStuffed.class);
    private static final String MAP_FILENAME = "map.dat";
//    private static final int VERSION = 10000;

    private static final int CONTENT_INITIAL_SIZE = 10000;
    private static final int MIN_GROWTH_SIZE = 1000;
    private static final double CONTENT_GROWTHFACTOR = 1.2;

    private static final int FACETBITS =  5;
    private static final int FACETSHIFT = 32 - FACETBITS;
    private static final int VALUE_MASK = 0xFFFFFFFF << FACETBITS >>> FACETBITS;
    /**
     * The -1 in the calculation is to make room for the emptyFacet.
     */
    private static final int FACET_LIMIT = (int) StrictMath.pow(2, FACETBITS)-1;

    private int docCapacity;
    private int facetCount;
    private boolean shift = DEFAULT_SHIFT_ON_REMOVE;

    private int highestDocID = -1;
    private int[] index;

    private int valuePos = 0;
    private int finalIndex;
    private int[] values = new int[CONTENT_INITIAL_SIZE];

    /**
     * Creates a core map with initial size based on the given number of
     * documents.
     * @param docCount the expected number of documents.
     * @param facetCount the number of facets for this core map.
     */
    public CoreMapBitStuffed(int docCount, int facetCount) {
        init(docCount, facetCount);
    }

    /**
     * Creates a core map with initial size based on the given number of
     * documents.
     * @param conf       configuration for the CoreMap.
     * @param docCount   the expected number of documents.
     * @param facetCount the number of facets for this core map.
     */
    public CoreMapBitStuffed(Configuration conf, int docCount, int facetCount) {
        init(docCount, facetCount);
        shift = conf.getBoolean(CONF_SHIFT_ON_REMOVE, shift);
    }

    private void init(int docCount, int facetCount) {
        if (facetCount >= FACET_LIMIT) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException("This core map only allows "
                                               + FACET_LIMIT + " facets. "
                                               + facetCount + " facets was"
                                               + "specified");
        }
        docCapacity = docCount;
        this.facetCount = facetCount;
        finalIndex = docCount -1;
        index = new int[docCapacity + 1];
    }

    /**
     * Adds the given tags in the given facet to the given docID.
     * @param docID   the ID of a document. This needs to be
     * 0 or <= (the largest existing document-id + 1).
     * @param facetID the ID for a facet.
     * @param tagIDs an array of IDs for tags belonging to the given facet.
     */
    public void add(int docID, int facetID, int[] tagIDs) {
        if (facetID >= facetCount) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException("This core map only allows "
                                               + FACET_LIMIT + " facets. The "
                                               + "ID for the facet in add was "
                                               + facetID);
        }
        if (docID > highestDocID + 1) {
            throw new ArrayIndexOutOfBoundsException(
                    "Adding new documents require the doc-id to be <= (the "
                    + "largest existing doc-id + 1). The doc-id was "
                    + docID + " and the highest legalvalue is "
                    + (highestDocID + 1));
        }
        // Check index capacity
        if (docID > docCapacity-1) {
            int newSize = Math.max(MIN_GROWTH_SIZE,
                                   (int)(index.length * CONTENT_GROWTHFACTOR));
            //noinspection DuplicateStringLiteralInspection
            log.debug("Expanding index array from " + index.length
                      + " to " + newSize);
            int[] exp = new int[newSize];
            System.arraycopy(index, 0, exp, 0, index.length);
            index = exp;
            docCapacity = newSize;
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

        int insertPos = index[docID + 1];
        if (docID == highestDocID + 1) { // New at the end
            highestDocID = docID;
            if (highestDocID == 0) {
                index[0] = 0;
                index[1] = tagIDs.length;
                insertPos = 0;
            } else {
                index[highestDocID + 1] = index[highestDocID] + tagIDs.length;
                insertPos = index[highestDocID];
            }
        } else if (docID <= highestDocID) { // Expand existing
            if (docID < highestDocID) { // Move values
                System.arraycopy(values, index[docID + 1], values,
                                 index[docID + 1] + tagIDs.length, valuePos);
            }
            // Update positions
            for (int iPos = docID + 1 ; iPos <= highestDocID + 1 ; iPos++) {
                index[iPos] += tagIDs.length;
            }
        }
        // Insert values
        for (int tagPos = 0 ; tagPos < tagIDs.length ; tagPos++) {
            int aContent = tagIDs[tagPos];
            values[insertPos + tagPos] =
                    facetID << FACETSHIFT | aContent & VALUE_MASK;
        }
        valuePos += tagIDs.length;
    }

    public void remove(int docID) {
        if (docID > highestDocID) {
            throw new IllegalArgumentException("Cannot remove non-existing "
                                               + "docID " + docID + " from "
                                               + " the map with size "
                                               + (highestDocID + 1));
        }
        if (!shift) {
            try {
                for (int i = index[docID] ; i < index[docID + 1] ; i++) {
                    values[i] = FACET_LIMIT << FACETSHIFT;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                log.warn(String.format("Out of bounds in remove(%d, %b)",
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
            throw new ArrayIndexOutOfBoundsException("Requested " + docID
                                                     + " out of "
                                                     + (highestDocID + 1)
                                                     + " documents");
        }
        int to = index[docID + 1];
//        System.out.println("X " + docID + ", Y " + facetID +
//                           ", From " + index[docID] + ", To " + to);
        int[] result = new int[to-index[docID]];
        int resultPos = 0;
        for (int i = index[docID] ; i < to ; i++) {
            int value = values[i];
            int currentFacetID = value >>> FACETSHIFT;
            if (currentFacetID == facetID) {
                result[resultPos++] = value & VALUE_MASK;
            }
        }
        int[] reducedResult = new int[resultPos];
        System.arraycopy(result, 0, reducedResult, 0, resultPos);
        return reducedResult;
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void store(File location) throws IOException {
        log.info("Storing integer-based map");
        ObjectOutputStream mapOut =
                            ClusterCommon.objectPrinter(location, MAP_FILENAME);

        mapOut.writeInt(CoreMapBitStuffedLong.VERSION);
        mapOut.writeInt(docCapacity);
        mapOut.writeInt(facetCount);
        mapOut.writeInt(87); // Dummy to maintain backwards compatability
        mapOut.writeInt(highestDocID);
        mapOut.writeInt(valuePos);
        mapOut.writeInt(finalIndex); // TODO: Consider this

        log.trace("Storing index");
        mapOut.writeInt(index.length);
        for (int anIndex : index) {
            mapOut.writeInt(anIndex);
        }

        log.trace("Storing values");
        mapOut.writeInt(values.length);
        for (int value : values) {
            mapOut.writeLong(intValueToLong(value));
        }
        mapOut.close();
        log.debug("Finished storing integer-based map");
    }

    protected long intValueToLong(int value) {
        int dim2 = value >>> FACETSHIFT;
        int dim1 = value & VALUE_MASK;
        return (long)dim2 << CoreMapBitStuffedLong.FACETSHIFT | dim1;
    }

    protected int longValueToInt(long value) {
        int dim2 = (int)(value >>> CoreMapBitStuffedLong.FACETSHIFT);
        int dim1 = (int)(value & CoreMapBitStuffedLong.VALUE_MASK);
        return dim2 << FACETSHIFT | dim1;
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void load(File location) throws IOException {
        log.info("Loading map " + MAP_FILENAME);
        ObjectInputStream mapIn =
                             ClusterCommon.objectLoader(location, MAP_FILENAME);
        int version = mapIn.readInt();
        if (CoreMapBitStuffedLong.VERSION != version) {
            //noinspection DuplicateStringLiteralInspection
            throw new IOException("Wrong version. Expected " +
                                  CoreMapBitStuffedLong.VERSION +
                                  " got " + version);
        }
        docCapacity = mapIn.readInt();
        facetCount = mapIn.readInt();
        mapIn.readInt(); // Read the dummy valut to maintain compatability
        highestDocID = mapIn.readInt();
        valuePos = mapIn.readInt();
        finalIndex = mapIn.readInt();

        int indexSize = mapIn.readInt();
        log.debug("Loading index with size " + indexSize);
        index = new int[indexSize];
        for (int i = 0 ; i < index.length ; i++) {
            index[i] = mapIn.readInt();
        }

        int valueSize = mapIn.readInt();
        log.debug("Loading values with size " + valueSize);
        values = new int[valueSize];
        for (int i = 0 ; i < values.length ; i++) {
            values[i] = longValueToInt(mapIn.readLong());
        }
        log.debug("Finished loading map");
        mapIn.close();
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
                                       " to " + (value & VALUE_MASK));*/
                    tagCounter.increment(values[i] >>> FACETSHIFT,
                                         values[i] & VALUE_MASK);
//                    System.out.println("Marked");
//                    counterLists[value >>> FACETSHIFT]
//                                [value &  VALUE_MASK]++;
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
            int tag = value & VALUE_MASK;
            if (facet == facetID && tag >= position) {
                values[vPos] = facet << FACETSHIFT |
                               tag + delta & VALUE_MASK;
            }
        }
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
        index = new int[docCapacity + 1];

        finalIndex = docCapacity - 1;
        valuePos = 0;
        values = new int[CONTENT_INITIAL_SIZE];
    }

    protected int[] getIndex() {
        return index;
    }
    protected int[] getValues() {
        return values;
    }

}
