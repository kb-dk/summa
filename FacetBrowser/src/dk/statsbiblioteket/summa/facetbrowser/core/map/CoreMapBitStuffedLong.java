/* $Id: CoreMapBitStuffedLong.java,v 1.5 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/05 10:20:22 $
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
 * CVS:  $Id: CoreMapBitStuffedLong.java,v 1.5 2007/10/05 10:20:22 te Exp $
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

import org.apache.log4j.Logger;
import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounter;
import dk.statsbiblioteket.summa.facetbrowser.util.ClusterCommon;
import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class CoreMapBitStuffedLong implements CoreMap {
    private static Logger log = Logger.getLogger(CoreMapBitStuffedLong.class);
    private static final String MAP_FILENAME =        "map.dat";
    protected static final int VERSION = 10000;
    
    private static final int CONTENT_INITIAL_SIZE = 10000;
    private static final double CONTENT_GROWTHFACTOR = 1.2;

    private int docCapacity;
    private int facetCount;

    private int lastDocID = -1;
    private int[] index;

    private int valuePos = 0;
    private int finalIndex;
    private long[] values = new long[CONTENT_INITIAL_SIZE];

    protected static final int FACETBITS =  16;
    protected static final int FACETSHIFT = 64 - FACETBITS;
    protected static final long VALUE_MASK =
            0xFFFFFFFFFFFFFFFFL << FACETBITS >>> FACETBITS;


    protected int[] getIndex() {
        return index;
    }
    protected long[] getValues() {
        return values;
    }

    public CoreMapBitStuffedLong(int docCount, int facetCount) {
/*        int limit1 = (int) StrictMath.pow(2, FACETSHIFT);
        if (docCount > limit1) {
            throw new ArrayIndexOutOfBoundsException("Dimension 1 must be" +
                                                     " at most " + limit1);
        }*/
        int facetLimit = (int) StrictMath.pow(2, FACETBITS);
        if (facetCount > facetLimit) {
            throw new ArrayIndexOutOfBoundsException("The facet count was " +
                                                     facetCount +
                                                     ". It must be" +
                                                     " at most " + facetLimit);
        }
        docCapacity = docCount;
        this.facetCount = facetCount;
        finalIndex = docCount -1;
        index = new int[docCapacity + 1];
    }

    public void add(int docID, int facetID, int[] tagIDs) {
        if (docID != lastDocID && docID != lastDocID + 1) {
            throw new ArrayIndexOutOfBoundsException(
                    "Assignment must be sequential. Expected docID "
                    + (lastDocID + 1) + " but got " + docID);
        }
        if (docID > docCapacity-1) {
            int newSize = (int)(index.length * CONTENT_GROWTHFACTOR);
            log.debug("Expanding index array from " + index.length
                      + " to " + newSize);
            int[] exp = new int[newSize];
            System.arraycopy(index, 0, exp, 0, docCapacity-1);
            index = exp;
            docCapacity = newSize;
        }

/*        int pos = docID * facetCount + facetID;
        if (pos != expectedNext++) {
            throw new ArrayIndexOutOfBoundsException("Assignment must be " +
                                                     "sequential");
        }*/
        if (lastDocID != docID) {
            lastDocID = docID;
            index[lastDocID] = valuePos;
        }
        if (valuePos + tagIDs.length >= values.length) {
            int newSize = (int)Math.max(values.length * CONTENT_GROWTHFACTOR,
                                        values.length + tagIDs.length);
            log.debug("Expanding value array from " + values.length
                      + " to " + newSize);
            long[] exp = new long[newSize];
            System.arraycopy(values, 0, exp, 0, values.length);
            values = exp;
        }
        for (int aContent : tagIDs) {
            values[valuePos++] = (long)facetID << FACETSHIFT
                                 | aContent & VALUE_MASK;
        }
        index[lastDocID + 1] = valuePos;
    }

    public int[] get(int docID, int facetID) {
        int to = index[docID +1];
//        System.out.println("X " + docID + ", Y " + facetID +
//                           ", From " + index[docID] + ", To " + to);
        int[] result = new int[to-index[docID]];
        int resultPos = 0;
        for (int i = index[docID] ; i < to ; i++) {
            long value = values[i];
            int currentFacetID = (int)(value >>> FACETSHIFT);
            if (currentFacetID == facetID) {
                result[resultPos++] = (int)(value & VALUE_MASK);
            }
        }
        int[] reducedResult = new int[resultPos];
        System.arraycopy(result, 0, reducedResult, 0, resultPos);
        return reducedResult;
    }

    public void remove(int docID) {
        // TODO: Implement this
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void store(File location) throws IOException {
        log.info("Storing map");
        ObjectOutputStream mapOut =
                            ClusterCommon.objectPrinter(location, MAP_FILENAME);

        mapOut.writeInt(CoreMapBitStuffedLong.VERSION);
        mapOut.writeInt(docCapacity);
        mapOut.writeInt(facetCount);
        mapOut.writeInt(87); // Dummy to maintain backwards compatability
        mapOut.writeInt(lastDocID);
        mapOut.writeInt(valuePos);
        mapOut.writeInt(finalIndex);

        log.trace("Storing index");
        mapOut.writeInt(index.length);
        for (int anIndex : index) {
            mapOut.writeInt(anIndex);
        }

        log.trace("Storing values");
        mapOut.writeInt(values.length);
        for (long value : values) {
            mapOut.writeLong(value);
        }
        mapOut.close();
        log.debug("Finished storing map");
    }

    public void load(File location) throws IOException {
        log.info("Loading map " + MAP_FILENAME);
        ObjectInputStream mapIn =
                             ClusterCommon.objectLoader(location, MAP_FILENAME);
        int version = mapIn.readInt();
        if (CoreMapBitStuffedLong.VERSION != version) {
            throw new IOException("Wrong version. Expected " +
                                  CoreMapBitStuffedLong.VERSION +
                                  " got " + version);
        }
        docCapacity = mapIn.readInt();
        facetCount = mapIn.readInt();
        mapIn.readInt(); // Read the dummy valut to maintain compatability
        lastDocID = mapIn.readInt();
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
        values = new long[valueSize];
        for (int i = 0 ; i < values.length ; i++) {
            values[i] = mapIn.readLong();
        }
        log.debug("Map loading finished");
        mapIn.close();
    }

    public int getDocCount() {
        return lastDocID + 1;
    }

    public void markCounterLists(TagCounter tagCounter, int[] docIDs,
                                 int startPos, int endPos) {
        log.debug("Marking " + docIDs.length +" docs " +
                  startPos + "=>" + endPos);
        int hitID;
        int to;
        long value;
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
                    value = values[i];
/*                    System.out.println("Marking " + (value >>> FACETSHIFT) +
                                       " to " + (value & VALUE_MASK));*/
                    tagCounter.increment((int)(value >>> FACETSHIFT),
                                         (int)(value & VALUE_MASK));
//                    System.out.println("Marked");
//                    counterLists[value >>> FACETSHIFT]
//                                [value &  VALUE_MASK]++;
                }
            } catch (Exception ex) {
                if (log.isTraceEnabled()) {
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
        log.debug("Marking finished");

    }

    public void adjustPositions(int facetID, int position, int delta) {
        throw new IllegalAccessError("adjustPositions not implemented yet!");
    }


    public static boolean canHandle(int dimension2, int maxContent) {
        int limitDim2 = (int) StrictMath.pow(2, FACETBITS);
        int limitMax = (int) StrictMath.pow(2, FACETSHIFT);
        return dimension2 <= limitDim2 && maxContent <= limitMax;
    }

    public void clear() {
        lastDocID = -1;
        index = new int[docCapacity + 1];

        finalIndex = docCapacity - 1;
        valuePos = 0;
        values = new long[CONTENT_INITIAL_SIZE];
    }

}
