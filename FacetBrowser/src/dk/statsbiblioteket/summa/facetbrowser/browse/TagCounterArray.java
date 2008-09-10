/* $Id: TagCounterArray.java,v 1.16 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.16 $
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
 * CVS:  $Id: TagCounterArray.java,v 1.16 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.summa.common.util.PriorityQueue;
import dk.statsbiblioteket.summa.common.util.PriorityQueueLong;
import dk.statsbiblioteket.summa.common.util.FlexiblePair;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResult;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.io.StringWriter;

/**
 * A tag-counter optimized for medium to large search-results. The performance
 * is dependent on search-result size as well as index-size.
 * </p><p>
 * Resetting the tag-counter for next usage takes place in a designated Thread.
 * If the tag-counter is immediately re-used,
 * </p><p>
 * If this tag-counter is reused, it should have zero impact on garbage
 * collection, as the internal structure can be cleared instead of reallocated.
 * </p><p>
 * In the case of size-changing pools of Tags, the underlying arrays will be
 * adjusted, which will impact the garbage collector due to re-allocation.
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TagCounterArray implements TagCounter, Runnable {
    private static Logger log = Logger.getLogger(TagCounterArray.class);

    /**
     * This is multiplied with the wanted tag array lengt when creating a new
     * tag array, in order to allow for expansion.
     */
    private static final double expansionFactor = 1.2;
    private static final int MIN_EXPANSION = 10;

    /**
     * Ensures that the cleaning of the tagCounter has finished before the
     * next request.
     */
    private ReentrantLock lock = new ReentrantLock();

    /**
     * The tags-array has the signature int[facetID][tagID] and contains
     * counts for the occurence of all facet/tag pairs. Note that the size of
     * the secondary array might exceed the number of possible Tags for the
     * corresponding Facet.
     * </p><p>
     * Likewise the size of the primary array is not equal to the number of
     * Facets. See {@link #facetCount} for that.
     * </p><p>
     * Note: The number of facets only change when a new FacetControl is
     *       created.
     */
    private int[][] tags;
    private int facetCount = -1;
    private int emptyFacet;
    private TagHandler tagHandler;

    /**
     * Create the array from the informations in the given tagHandler. The
     * tagHandler must be correct with regard to the number of facets.
     * </p><p>
     * The emptyFacet will always contain a counter of length 1. It is used as
     * a dummy placeholder for deleted tags, in order to avoid a conditional in
     * the tight inner loop.
     * @param tagHandler an properly initialized tagHandler.
     * @param emptyFacet the id for the empty facet. This must be greater than
     *                   or equal to the number of proper facets in TagHandler.
     *                   The first dimension of {@link #tags} is determined by
     *                   this, so high values are to be avoided.
     */
    // TODO: Verify String.format(...%s$d...)
    public TagCounterArray(TagHandler tagHandler, int emptyFacet) {
        this.tagHandler = tagHandler;
        this.emptyFacet = emptyFacet;
        if (emptyFacet < tagHandler.getFacetNames().size()) {
            log.error(String.format(
                    "The emptyFacet was set to %d with the number of Facets "
                    + "being %d. emptyFacet is adjusted to %2$d, but this will "
                    + "probably not work with the core map",
                    emptyFacet, tagHandler.getFacetNames().size()));
            this.emptyFacet = tagHandler.getFacetNames().size();
        }
    }
    public synchronized void verify() {
        log.trace("verify() called");
        if (lock.isLocked()) {
            log.trace("verify: Waiting for lock (probably due to running "
                      + "reset)");
        }
        lock.lock();
        log.trace("verify: Got lock");
        try {
            log.trace("Verifying the number of facets");
            int newFacetCount = tagHandler.getFacetNames().size();
            if (facetCount != -1 && facetCount != newFacetCount) {
                log.error(String.format(
                        "The old facet count was %d but was changed to %d. "
                        + "Facet count should be constant. Correctness is not "
                        + "guaranteed after this",
                        facetCount, newFacetCount));
                if (emptyFacet < newFacetCount) {
                    log.error(String.format(
                            "The old emptyFacet of %d is smaller than the new "
                            + "number of Facets %d. Adjusting emptyFacet to "
                            + "%2$d, which will probably lead to problems",
                            emptyFacet, newFacetCount));
                    emptyFacet = newFacetCount;
                }
            }
            facetCount = newFacetCount;
            if (tags == null || tags.length != emptyFacet + 1) {
                log.debug("Creating new FacetTag-array with " + (emptyFacet + 1)
                          + " facets");
                tags = new int[emptyFacet + 1][];
            }
            log.trace("Verifying the length of tag-arrays");
            int pos = 0;
            for (String facetName: tagHandler.getFacetNames()) {
                int tagCount = tagHandler.getTagCount(facetName);
                if (tags[pos] == null || tags[pos].length < tagCount) {
                    int newCount = (int)Math.max(
                            MIN_EXPANSION, tagHandler.getTagCount(facetName)
                                           * expansionFactor);
                    log.debug("Increasing tag-array for Facet " + facetName
                              + " to max " + newCount + " with " + tagCount
                              + " active tags");
                    tags[pos] = new int[newCount];
                }
                pos++;
            }
            log.trace("Verifying the existence of emptyFacet array");
            if (tags[emptyFacet] == null || tags[emptyFacet].length != 1) {
                log.debug("Creating emptyFacet array of length 1 at position "
                          + emptyFacet);
                tags[emptyFacet] = new int[1];
            }
            log.trace("verify() finished");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Increment the count for a Tag in a Facet. The count is incremented by 1.
     * This is expected to be called millions of times per second, so
     * performance is king. Error-checking is done solely by catching
     * exceptions.
     * Note: due to performance reasons, this is not thread-safe.
     * @param facetID the ID for the Facet.
     * @param tagID   the ID for the Tag.
     */
    @QAInfo(level = QAInfo.Level.FINE,
            state = QAInfo.State.IN_DEVELOPMENT,
            author = "te",
            comment = "There is a potential for throwing tons of exceptions." +
                      "Consider throwing only the first in each run")
    public void increment(int facetID, int tagID) {
        try {
            tags[facetID][tagID]++;
        } catch (ArrayIndexOutOfBoundsException e) {
            if (facetID < 0
                || (facetID > facetCount && facetID != emptyFacet)) {
                log.error(String.format(
                        "facetId was %d. It should be emptyFacet %d or (> 0 "
                        + "and < facetCount %d). Skipping increment",
                        facetID, emptyFacet, facetCount));
                return;
            }

            if (facetID == emptyFacet) {
                log.warn(String.format("tagID for emptyFacet %d was %d, where "
                                       + "the only valid value is 0",
                                       emptyFacet, tagID));
                return;
            }
            if (tagID < 0) {
                log.warn(String.format(
                        "Illegal tagID %d for facet %s (id %d)",
                        tagID, tagHandler.getFacetNames().get(facetID),
                        facetID));
                return;
            }
            if (tagID >= tags[facetID].length) {
                log.debug(String.format(
                        "increment(%d, %d) exceeded length %d of tag array for "
                        + "facet %s. Increasing tag array accordingly",
                        facetID, tagID, tags[facetID].length,
                        tagHandler.getFacetNames().get(facetID)));
                try {
                    int[] newTagArray = new int[(int)(tagID * expansionFactor)];
                    System.arraycopy(tags[facetID], 0,
                                     newTagArray, 0, tags[facetID].length);
                    tags[facetID] = newTagArray;
                    tags[facetID][tagID]++;
                } catch (Exception ex) {
                    log.error("Exception calling increment with facetID "
                              + facetID + ", tagID " + tagID
                              + " after expansion to size "
                              + (int)(tagID * expansionFactor), ex);
                }
                return;
            }
            log.warn(String.format(
                    "increment(%d, %d) encountered an unexpected "
                    + "ArrayIndexOutOfBoundsException", facetID, tagID), e);
        } catch (Exception e) {
            log.warn(String.format(
                    "Unexpected exception in increment(%d, %d)",
                    facetID, tagID), e);
        }
    }

    public synchronized FacetResult getFirst(Structure requestStructure) {
        lock.lock();
        if (requestStructure == null) {
            log.trace("getFirst: Specified requestStructure was null, using "
                      + "default structure");
        }
//        System.out.println("GetFirst called: " + this);
        try {
            FacetResultLocal result =
                    new FacetResultLocal(requestStructure, tagHandler);
            for (Map.Entry<String, FacetStructure> facetEntry:
                    requestStructure.getFacets().entrySet()) {
                FacetStructure facet = facetEntry.getValue();
                int facetID = facet.getFacetID();
                int maxTags = facet.getMaxTags();
                String facetName = facet.getName();
//                System.out.println("Before getFirst" + this);
                int[] counterList = tags[facetID];
                if (FacetStructure.SORT_ALPHA.equals(facet.getSortType())) {
                    addFirstTagsAlpha(maxTags, counterList,
                                      tagHandler.getFacetSize(facetID),
                                      result, facetName);
                } else {
                    if (!FacetStructure.SORT_POPULARITY.equals(facet.getSortType())){
                        log.warn(String.format(
                                "Unknown sort-order '%s' in getFirst, using %s",
                                facet.getSortType(),
                                FacetStructure.SORT_POPULARITY));
                    }
                    addFirstTagsPopularity(maxTags, counterList,
                                           tagHandler.getFacetSize(facetID),
                                           result, facetID);
                }
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    private void addFirstTagsPopularity(int maxTags, int[] counterList,
                                        int counterLength,
                                        FacetResultLocal result,
                                        int facetID) {
        if (log.isTraceEnabled()) {
            log.trace(String.format(
                    "addFirstTagsPopularity(maxTags %d, counterList.length %d, "
                    + "counterLength %d, result, facetID %d) called",
                    maxTags, counterList.length, counterLength, facetID));
        }
        int minPop = 0;
        //noinspection unchecked
        FlexiblePair<Integer, Integer>[] popResult =
                (FlexiblePair<Integer, Integer>[])new FlexiblePair[maxTags+1];
        int tagCount = 0;
        // TODO: Check for 1-off error in counterlength
        for (int i = 0 ; i < counterLength ; i++) {
            int currentPop = counterList[i];
            if (currentPop != 0 && (currentPop > minPop || tagCount < maxTags)){
                if (tagCount <= maxTags) {
                    popResult[tagCount] =
                            new FlexiblePair<Integer, Integer>(
                                    i, currentPop,
                                    FlexiblePair.SortType.SECONDARY_DESCENDING);
                    if (tagCount == maxTags) {
                        Arrays.sort(popResult);
                    }
                } else {
                    /* We're know that popResult[maxTags] will be
                 * pushed out of the array, so we reuse it. */
                    FlexiblePair<Integer, Integer> newPair =
                            popResult[maxTags];
                    newPair.setKey(i);
                    newPair.setValue(currentPop);
                    /* Binary search probably has too much overhead.
                 */
                    for (int p = 0 ; p <= maxTags ; p++) {
                       // for (int p = 0 ; p < maxTags+1 ; p++) {
                       // if (popResult[p].compareTo(newPair) > 0) {
                        if (popResult[p].getValue() <
                            currentPop) {
                            /* Insertion point found, make room */
                            System.arraycopy(popResult, p,
                                             popResult,
                                             p+1, maxTags-p);
                            popResult[p] = newPair;
                            break;
                        }
                    }
                    // Arrays.sort(popResult);
                    minPop = popResult[maxTags].getValue();
                }
                tagCount++;
            }
        }
        int sTags = Math.min(maxTags, tagCount);
        ArrayList<FlexiblePair<Integer, Integer>> niceList =
                new ArrayList<FlexiblePair<Integer,
                                           Integer>>(sTags);
        for (int i = 0 ; i < sTags ; i++) {
            niceList.add(popResult[i]);
        }
        result.assignTags(tagHandler.getFacetNames().get(facetID),
                          niceList);
    }

    // Too slow
    @SuppressWarnings({"UnusedDeclaration"})
    private void addFirstTagsPopularityQueue(int maxTags, int[] counterList,
                                             int counterLength,
                                            FacetResultLocal result,
                                            int facetID) {
        PriorityQueueLong queue = new PriorityQueueLong(maxTags);
        for (int i = counterLength - 1 ; i != 0 ; i--) {
            if (counterList[i] > 0) {
                queue.insert(Integer.MAX_VALUE - counterList[i]);
//                queue.insert((long)(Integer.MAX_VALUE - counterList[i]) << 32 |
//                             i); // Why not 32?
            }
        }
        ArrayList<FlexiblePair<Integer, Integer>> niceList =
                new ArrayList<FlexiblePair<Integer,
                                           Integer>>(queue.getSize());
        while (queue.getSize() > 0) {
            long v = queue.removeMin();
            niceList.add(new FlexiblePair<Integer, Integer>(
                    (int)(v & 0xFFFF), Integer.MAX_VALUE - (int)(v >>> 31),
                    FlexiblePair.SortType.SECONDARY_DESCENDING));
        }
        result.assignTags(tagHandler.getFacetNames().get(facetID),
                          niceList);
    }

    // Slower than the other method
    @SuppressWarnings({"UnusedDeclaration"})
    private void addFirstTagsPopularityGenericQueue(int maxTags,
                                                    int[] counterList,
                                                    int counterLength,
                                                    FacetResultLocal result,
                                                    int facetID) {
        PriorityQueue<FlexiblePair<Integer, Integer>> queue =
                new PriorityQueue<FlexiblePair<Integer, Integer>>(maxTags);
        FlexiblePair<Integer, Integer> entry = null;
        for (int i = 0 ; i < counterLength ; i++) {
            if (counterList[i] > 0) {
                if (entry == null) {
                    entry = new FlexiblePair<Integer, Integer>(
                            i, counterList[i],
                            FlexiblePair.SortType.SECONDARY_DESCENDING);
                } else {
                    entry.setKey(i);
                    entry.setValue(counterList[i]);
                }
                entry = queue.insert(entry);
//                queue.insert((long)(Integer.MAX_VALUE - counterList[i]) << 32 |
//                             i); // Why not 32?
            }
        }
        ArrayList<FlexiblePair<Integer, Integer>> niceList =
                new ArrayList<FlexiblePair<Integer,
                                           Integer>>(queue.getSize());
        while (queue.getSize() > 0) {
            niceList.add(queue.removeMin());
        }
        result.assignTags(tagHandler.getFacetNames().get(facetID),
                          niceList);
    }

    private void addFirstTagsAlpha(int maxTags, int[] counterList,
                                   int counterLength,
                                   FacetResultLocal result,
                                   String facetName) {
        ArrayList<FlexiblePair<Integer, Integer>> alphaResult =
                new ArrayList<FlexiblePair<Integer, Integer>>(
                        Math.min(maxTags, 50000)); // Sanity-check
        int counter = 0;
        for (int i = 0 ; i < counterLength ; i++) {
            if (counterList[i] != 0) {
                alphaResult.add(new FlexiblePair<Integer, Integer>(
                        i, counterList[i],
                        FlexiblePair.SortType.PRIMARY_ASCENDING));
                if (++counter == maxTags) {
                    break;
                }
            }
        }
        //                log.info("Finished sorting " + facetName);
        result.assignTags(facetName, alphaResult);
    }

    public synchronized void reset() {
        lock.lock();
        new Thread(this).run();
    }

    public void run() {
        try {
            log.trace("Clearing counter lists...");
            for (int[] tag : tags) {
                if (tag != null) {
                    Arrays.fill(tag, 0);
                }
            }
            log.trace("Clearing finished");
        } finally {
            lock.unlock();
        }
    }

    public String toString() {
        int MAX_TAGS = 10;
        if (tags == null) {
            return "TagCounterArray: No facet/tag pairs";
        }
        StringWriter sw = new StringWriter(1000);
        sw.append("TagCounterArray ");
        for (int i = 0 ; i < tags.length && i < facetCount; i++) {
            sw.append("Facet #").append(Integer.toString(i)).append(": ");
            if (tags[i] == null) {
                sw.append("null");
            } else {
                sw.append(Integer.toString(tags[i].length)).append("(");
                for (int t = 0 ; t < MAX_TAGS && t < tags[i].length
                                 && t < tagHandler.getTagCount(
                        tagHandler.getFacetNames().get(i)); t++) {
                    sw.append(Integer.toString(tags[i][t])).append(" ");
                }
                sw.append(")");
            }
            sw.append(" ");
        }
        return sw.toString();
    }
}
