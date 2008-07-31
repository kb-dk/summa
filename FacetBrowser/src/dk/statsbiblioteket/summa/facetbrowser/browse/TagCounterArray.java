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

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.io.StringWriter;

import dk.statsbiblioteket.summa.facetbrowser.util.FlexiblePair;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.common.util.PriorityQueueLong;
import dk.statsbiblioteket.summa.common.util.PriorityQueue;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

/**
 * A tag-counter optimized for medium to large search-results. The performance
 * is dependent on search-result size as well as index-size.
 *
 * If this tag-counter is reused, it should have zero impact on garbage
 * collection, as the internal structure can be cleared instead of reallocated.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TagCounterArray implements TagCounter, Runnable {
    private static Logger log = Logger.getLogger(TagCounterArray.class);

    /**
     * Ensures that the cleaning of the tagCounter has finished before the
     * next request.
     */
    private ReentrantLock lock = new ReentrantLock();

    /**
     * The tags-array has the signature int[facetID][tagID] and contains
     * counts for the occurence of all facet/tag pairs.
     */
    private int[][] tags;
    private StructureDescription structure;
    private TagHandler tagHandler;

    /**
     * Create the array from the informations in the given tagHandler. The
     * tagHandler must be properly with regard to the number of facets. The
     * expected number of tags for the individual facets is only used for
     * initial size.
     * @param structure the structure for facet browser.
     * @param tagHandler an properly initialized tagHandler.
     */
    public TagCounterArray(StructureDescription structure,
                           TagHandler tagHandler) {
        this.structure = structure;
        this.tagHandler = tagHandler;
        int[][] tagCounts = new int[tagHandler.getFacetNames().size()][];
        for (int i = 0 ; i < tagHandler.getFacetNames().size() ; i++) {
            tagCounts[i] = new int[
                    tagHandler.getTagCount(tagHandler.getFacetNames().get(i))];
        }
        initialize(tagCounts, tagHandler.getFacetNames());
    }

    /**
     * Constructs a tagCounter for the given Facets, using the given matrix
     * for tags.
     * @param facetNames the names of the Facets to count tags for.
     * @param tags       a matrix with room for the counters for the tags.
     *                   The first dimension must be equal to the length of
     *                   facetNames, while the arrays in the second dimension
     *                   must have lengths equal to the number of unique tags
     *                   for the facet they correspond to.
     * @deprecated this constructor is no longer valid and will be removed ASAP
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public TagCounterArray(List<String> facetNames, int[][] tags) {
        throw new UnsupportedOperationException("This constructor is no longer supported!");
    }

    private void initialize(int[][] tags, List<String> facetNames) {
        StringWriter sw = new StringWriter();
        int counter = 0;
        for (int[] tagList: tags) {
            sw.append(facetNames.get(counter++)).append("(");
            sw.append(Integer.toString(tagList.length)).append(") ");
        }
        log.info("Creating tagCounter with " + facetNames.size() + " facets: " +
                 sw.toString());
        this.tags = tags;
        if (tags.length != facetNames.size()) {
            log.fatal("Dimension 1 for tags (" + tags.length +
                      ") did not match facet count (" + facetNames.size() +
                      ")");
        }
    }

    public void increment(int facetID, int tagID) {
        try {
            tags[facetID][tagID]++;
        } catch (Exception e) {
            if (tagID >= tags[facetID].length) {
                //noinspection DuplicateStringLiteralInspection
                log.error("Faulty incrementing tag " + tagID
                          + "/" + tags[facetID].length
                          + " for facet " +  structure.getFacetName(facetID)
                          + " (" + facetID + "/" + tags.length + ")");
            } else {
                log.debug("Expanding tag array for facet "
                          + structure.getFacetName(facetID) + " from "
                          + tags[facetID].length + " to " + (tagID + 1)
                          + " elements", e);
                try {
                    int[] newTagArray = new int[tagID + 1];
                    System.arraycopy(tags[facetID], 0,
                                     newTagArray, 0, tags[facetID].length);
                    tags[facetID] = newTagArray;
                    tags[facetID][tagID]++;
                } catch (Exception ex) {
                    log.error("Exception calling increment with facetID "
                              + facetID + ", tagID " + tagID
                              + " after expansion to size "
                              + tags[facetID].length, ex);
                }
            }
        }
    }

    public synchronized Result getFirst(
            Result.TagSortOrder sortOrder) {
        lock.lock();
        try {
            ResultLocal result =
                    new ResultLocal(structure, tagHandler);
            for (int facetID = 0 ;
                 facetID < structure.getFacetNames().size() ;
                 facetID++) {
                int maxTags = structure.getMaxTags(facetID);
                String facetName = structure.getFacetName(facetID);
//            System.out.println(facetName +" reduce to " + maxTags);
//            log.info("Sorting facet " + facetName);
                int[] counterList = tags[facetID];
                if (sortOrder == Result.TagSortOrder.tag) {
                    addFirstTagsAlpha(maxTags, counterList, result, facetName);
                } else if (sortOrder ==
                           Result.TagSortOrder.popularity) {
                    addFirstTagsPopularity(maxTags, counterList, result,
                                           facetID);
                } else {
                    log.fatal("Unknown sort order for tag: " + sortOrder);
                }
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    private void addFirstTagsPopularity(int maxTags, int[] counterList,
                                        ResultLocal result,
                                        int facetID) {
        int minPop = 0;
        System.out.println("1");
        //noinspection unchecked
        FlexiblePair<Integer, Integer>[] popResult =
                (FlexiblePair<Integer, Integer>[])new FlexiblePair[maxTags+1];
        int tagCount = 0;
        for (int i = 0 ; i < counterList.length ; i++) {
            int currentPop = counterList[i];
            if (currentPop > minPop || tagCount < maxTags) {
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
        result.assignTags(structure.getFacetName(facetID),
                                      niceList);
    }

    // Too slow
    @SuppressWarnings({"UnusedDeclaration"})
    private void addFirstTagsPopularityQueue(int maxTags, int[] counterList,
                                            ResultLocal result,
                                            int facetID) {
        PriorityQueueLong queue = new PriorityQueueLong(maxTags);
        for (int i = counterList.length - 1 ; i != 0 ; i--) {
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
        result.assignTags(structure.getFacetName(facetID),
                                      niceList);
    }

    // Slower than the other method
    @SuppressWarnings({"UnusedDeclaration"})
    private void addFirstTagsPopularityGenericQueue(int maxTags,
                                                    int[] counterList,
                                            ResultLocal result,
                                            int facetID) {
        PriorityQueue<FlexiblePair<Integer, Integer>> queue =
                new PriorityQueue<FlexiblePair<Integer, Integer>>(maxTags);
        FlexiblePair<Integer, Integer> entry = null;
        for (int i = 0 ; i < counterList.length ; i++) {
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
        result.assignTags(structure.getFacetName(facetID),
                                      niceList);
    }

    private void addFirstTagsAlpha(int maxTags, int[] counterList,
                                   ResultLocal result,
                                   String facetName) {
        ArrayList<FlexiblePair<Integer, Integer>> alphaResult =
                new ArrayList<FlexiblePair<Integer, Integer>>(
                        Math.min(maxTags, 50000)); // Sanity-check
        int counter = 0;
        for (int i = 0 ; i < counterList.length ; i++) {
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
        new Thread(this).run();
    }

    public void run() {
        lock.lock();
        try {
            log.debug("Clearing counter lists...");
            for (int[] tag : tags) {
                Arrays.fill(tag, 0);
            }
            log.debug("Clearing finished");
        } finally {
            lock.unlock();
        }
    }
}
