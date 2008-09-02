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
/*
 * The State and University Library of Denmark
 * CVS:  $Id$
 */
package dk.statsbiblioteket.summa.facetbrowser.browse;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;

import dk.statsbiblioteket.summa.facetbrowser.util.FlexiblePair;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

/**
 * A tag-counter optimized for small search-results. Using this should give
 * clustering performance independent of index-size, but very dependent on
 * search-result size.
 *
 * Using this taxes the garbage collecter somewhat, as HashMaps are constructed
 * and destroyed for each clustering.
 *
 * Currently this counter is not used, but as the index grows, it might be wise
 * to make a per-search selection between this and TagCounterHeavy, based on
 * input-size.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TagCounterLight implements TagCounter, Runnable {
    private static Logger log = Logger.getLogger(TagCounterLight.class);

    private ArrayList<Map<Integer, Integer>> facets;
    private static final int INITIAL_CAPACITY = 5000;

    /**
     * Ensures that the cleaning of the tagCounter has finished before the
     * next request.
     */
    private ReentrantLock lock = new ReentrantLock();

    StructureDescription structure;
    TagHandler tagHandler;

    public TagCounterLight(StructureDescription structure,
                           TagHandler tagHandler) {
        this.structure = structure;
        this.tagHandler = tagHandler;
        facets = new ArrayList<Map<Integer, Integer>>(
                structure.getFacetNames().size());
        //noinspection UnusedDeclaration
        for (String facetName: structure.getFacetNames()) {
            facets.add(new HashMap<Integer, Integer>(INITIAL_CAPACITY));
        }
    }

    public void increment(int facetID, int tagID) {
        Map<Integer, Integer> facet = facets.get(facetID);
        Integer count = facet.get(tagID);
        facet.put(tagID, count == null ? 1 : count+1);
    }

    public synchronized FacetResult getFirst(
            FacetResult.TagSortOrder sortOrder) {
        lock.lock();
        try {
            FacetResultLocal result = new FacetResultLocal(structure,
                                                                 tagHandler);

            for (int facetID = 0 ; facetID < facets.size() ; facetID++) {
                int maxTags = structure.getMaxTags(facetID);
                if (sortOrder == FacetResult.TagSortOrder.tag) {
                    Map<Integer, Integer> facet = facets.get(facetID);
                    if (facet.size() > 0) {
                        ArrayList<FlexiblePair<Integer, Integer>> alphaResult =
                                new ArrayList<FlexiblePair<Integer, Integer>>(facet.size());
                        for (Map.Entry<Integer, Integer> tagCount: facet.entrySet()) {
                            alphaResult.add(new FlexiblePair<Integer, Integer>(
                                    tagCount.getKey(), tagCount.getValue(),
                                    FlexiblePair.SortType.PRIMARY_ASCENDING));
                        }
                        Collections.sort(alphaResult);
                        result.assignTags(structure.getFacetName(facetID),
                                          alphaResult.subList(0, Math.min(alphaResult.size(),
                                                                          maxTags)));
                    }
                } else if (sortOrder == FacetResult.TagSortOrder.popularity) {
                    Map<Integer, Integer> facet = facets.get(facetID);
                    if (facet.size() > 0) {
                        ArrayList<FlexiblePair<Integer, Integer>> popResult =
                                new ArrayList<FlexiblePair<Integer, Integer>>(facet.size());
                        for (Map.Entry<Integer, Integer> tagCount: facet.entrySet()) {
                            popResult.add(new FlexiblePair<Integer, Integer>(
                                    tagCount.getKey(), tagCount.getValue(),
                                    FlexiblePair.SortType.SECONDARY_DESCENDING));
                        }
                        Collections.sort(popResult);
                        result.assignTags(structure.getFacetName(facetID),
                                          popResult.subList(0, Math.min(popResult.size(),
                                                                        maxTags)));
                    }
                } else {
                    log.fatal("Unknown TagSortOrder: " + sortOrder);
                }
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    public synchronized void reset() {
        new Thread(this).run();
    }

    public void run() {
        lock.lock();
        try {
            for (Map<Integer, Integer> facet: facets) {
                facet.clear();
            }
        } finally {
            lock.unlock();
        }
    }
}
