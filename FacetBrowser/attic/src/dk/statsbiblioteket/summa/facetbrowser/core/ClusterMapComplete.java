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
package dk.statsbiblioteket.summa.facetbrowser.core;

import dk.statsbiblioteket.summa.facetbrowser.util.FlexiblePair;
import dk.statsbiblioteket.summa.facetbrowser.util.Pair;
import dk.statsbiblioteket.summa.facetbrowser.util.ClusterCommon;
import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounter;
import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounterArray;
import dk.statsbiblioteket.summa.facetbrowser.connection.IndexConnectionFactory;
import dk.statsbiblioteket.summa.facetbrowser.connection.IndexConnection;
import dk.statsbiblioteket.summa.facetbrowser.build.ClusterEngineTVHandler;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Note that the getFirstXThreaded method is synchronized and that the internal
 * states of ClusterMapComplete depends on that. This is primarily due to
 * memory optimization.
 * @deprecated a suitable replacement is under development.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public abstract class ClusterMapComplete {
    private static Logger log = Logger.getLogger(ClusterMapComplete.class);

    protected static final String VERSION = "20061108"; // Used for logging
    StructureDescription clusterDescription;

    protected int limit = Integer.MAX_VALUE;
    protected int threadCutoff = 1000;

    protected ClusterMapCompleteClear cleaner;
    protected ArrayList<ClusterMapCompleteThread> runnables =
                                     new ArrayList<ClusterMapCompleteThread>(8);


    // Precreation of Profiler shaves 8-9 ms of clustering time
    Profiler pf = new Profiler();
    private static final int MAX_WAIT_MS_FOR_THREADS = 20000;

    public String getStats() {
        return "Stats not implemented yet for " + getClass();
    }

    public enum SortOrder {ALPHA, POPULARITY}

    protected IndexReader ir;
    protected IndexConnection ic;
    protected ClusterEngineTVHandler tvHandler;

    public ClusterMapComplete() throws IOException {
        init();
    }
    public ClusterMapComplete(int limit) throws IOException {
        this.limit = limit;
        init();
    }
    public ClusterMapComplete(int limit,
                              int maxFacets, int maxTags,
                              int maxObjects) throws IOException {
        this.limit = limit;
        setLimits(maxFacets, maxTags, maxObjects);
        init();
    }

    protected void init() throws IOException {
        log.info("Initializing Cluster Map v " + VERSION);
        ic = IndexConnectionFactory.getIndexConnection();
        ir = ic.getIndexReader();
/*        try {
            ir = ClusterCommon.getIndexReaderParallelIndex();
        } catch (IOException e) {
            log.fatal("Could not connect to the index specified in " +
                      ClusterCommon.INDEXLOCATION);
            return;
        }*/
        tvHandler = new ClusterEngineTVHandler();
        Set<ClusterCommon.SpecialFacet> specials = tvHandler.getSpecialFacets();
        specials.remove(ClusterCommon.SpecialFacet.FRBR);
        specials.remove(ClusterCommon.SpecialFacet.MANYINONE);
        tvHandler.setSpecialFacets(specials);

        clusterDescription = new StructureDescription();

        log.info("Finished initializing cluster map");
        cleaner = new ClusterMapCompleteClear(runnables);
    }

    public List<String> getFacets() {
        return clusterDescription.getFacetNames();
    }
    public int getLimit() {
        return limit;
    }
    public int getDocCount() {
        try {
            return Math.min(limit, ir.maxDoc());
        } catch(NullPointerException ex) {
            throw new NullPointerException("Index not connected");
        }
    }

    public void shutdown() throws IOException {
        ir.close();
    }

    /**
     * Take the result from a call to one of the result-producing methods,
     * such as getFirstX, and convert it into human readable text.
     * @param result a HashMap with the result of a clustercall
     * @return a human readable representation of the result
     */
    public String resultToString(HashMap<String,
                                 List<FlexiblePair<String, Integer>>>
            result) {
        StringWriter sw = new StringWriter();
        for (String facet: clusterDescription.getFacetNames()) {
            sw.append(facet).append(": ");
            for (FlexiblePair<String, Integer> tag: result.get(facet)) {
                sw.append(tag.getKey()).append("(");
                sw.append(Integer.toString(tag.getValue()));
                sw.append(") ");
            }
            sw.append("\n");
        }
        return sw.toString();
    }

    protected List<Pair<String, List<FlexiblePair<String, Integer>>>>
            sortResult(HashMap<String, List<FlexiblePair<String, Integer>>>
                clusterMap) {
        List<Pair<String, List<FlexiblePair<String, Integer>>>> result =
                new LinkedList<Pair<String,
                        List<FlexiblePair<String, Integer>>>>();
        for (Map.Entry<String, List<FlexiblePair<String, Integer>>> facet:
                clusterMap.entrySet()) {
            result.add(new Pair<String, List<FlexiblePair<String, Integer>>>
                    (facet.getKey(), facet.getValue()));
        }

        Collections.sort(result,
                         new Comparator<Pair<String,
                                 List<FlexiblePair<String, Integer>>>>() {

                   public int compare(
                       Pair<String, List<FlexiblePair<String, Integer>>> o1,
                       Pair<String, List<FlexiblePair<String, Integer>>> o2) {

                                 Integer score1 =
                                         clusterDescription.
                                                 getFacetSortOrder(o1.getKey());
                                 Integer score2 =
                                         clusterDescription.
                                                 getFacetSortOrder(o2.getKey());
                                 if (score1 != null && score2 != null) {
                                     return score1.compareTo(score2);
                                 } else if (score1 != null) {
                                     return -1; // TODO: Check this!
                                 } else if (score2 != null) {
                                     return 1;
                                 } else {
                                     return o1.getKey().compareTo(o2.getKey());
                                 }
                             }
                   });
        return result;
    }

    /**
     * Take the result from a call to one of the result-producing methods,
     * such as getFirstX, and convert it into XML, suitable for further
     * processing.
     * @param clusterMap a HashMap with the result of a clustercall
     * @return an XML representation of the result
     */
    public String resultToXML(HashMap<String, List<FlexiblePair<String,
                                                                Integer>>>
            clusterMap) {
        StringWriter sw = new StringWriter(10000);

        sw.write("<facetmodel>\n");
        List<Pair<String, List<FlexiblePair<String, Integer>>>> sorted =
                sortResult(clusterMap);
        for (Pair<String, List<FlexiblePair<String, Integer>>> facet:
                sorted) {
            if (facet.getValue().size() > 0) {
                sw.write("  <facet name=\"");
                sw.write(ClusterCommon.simpleEntityEscape(facet.getKey()));
                // TODO: Preserve scoring
                sw.write("\">\n");

    //            sw.write(facet.getCustomString());
                int tagCount = 0;

                Integer maxTags = clusterDescription.getMaxTags(facet.getKey());
                for (FlexiblePair<String, Integer> tag: facet.getValue()) {
                    if (tagCount++ < maxTags) {
                        sw.write("    <tag name=\"");
                        sw.write(ClusterCommon.simpleEntityEscape(tag.
                                                                     getKey()));
        /*                if (!Element.NOSCORE.equals(tag.getScore())) {
                            sw.write("\" score=\"");
                            sw.write(Float.toString(tag.getScore()));
                        }*/
                        sw.write("\" addedobjects=\"");
                        sw.write(Integer.toString(tag.getValue()));
                        sw.write("\">\n");
                        sw.write("<query>" + facet.getKey() + ":\"" +
                                 ClusterCommon.simpleEntityEscape(tag.
                                                                     getKey()) +
                                 "\"</query>\n");
        /*                for (T object: tag.getObjects()) {
                            sw.write("      <object>");
                            sw.write(object.toString());
                            sw.write("</object>\n");
                        }*/
                        sw.write("    </tag>\n");
                    }
                }
                sw.write("  </facet>\n");
            }
        }
        sw.write("</facetmodel>\n");
        return sw.toString();
    }

    /**
     * The XML structure for no hits.
     */
    public String noHitsXML() {
        return "<facetmodel>\n</facetmodel>\n";
    }

    /**
     * Fill the ClusterMap with values from the Lucene index. Depending on
     * maschine speed and other factors, this might take a while. On a fairly
     * fast Pentium 4, the speed is around 10-15 minutes/million documents
     * in the index.
     */
    public abstract void fillMap() throws IOException, ClusterException;

    /**
     * Store the map at the default location.
     */
    public void storeMap() throws IOException {
        storeMap(getPersistenceLocation());
    }

    /**
     * Stores the ClusterMap to disc.
     * @param mapLocation the folder for a persistent ClusterMap
     */
    public abstract void storeMap(File mapLocation) throws IOException;

    /**
     * Load the map from the default location.
     * @throws IOException
     * return true if the map could be loaded
     */
    public boolean loadMap() throws IOException, ClusterException {
        return loadMap(getPersistenceLocation());
    }

    public abstract File getPersistenceLocation();

    /**
     * Reads the ClusterMap from disc.
     * @param mapLocation the folder for a persistent ClusterMap
     */
    public abstract boolean loadMap(File mapLocation) throws IOException, ClusterException;

    /**
     * Convert a structure with tag-indexes to the same structure, just with
     * tag-names.
     * @param index an index-based cluster structure
     * @return a name-based cluster structure
     */
    protected abstract HashMap<String, List<FlexiblePair<String, Integer>>>
                   indexesToString(HashMap<String, 
                                   List<FlexiblePair<Integer, Integer>>> index);

    /**
     * The search-result based cluster map generator methos.
     * @param docIDs    the documentID's to extract the tags from
     * @param startIndex the index for the first document in docIDs to use
     * @param endIndex  the index for the last document in docIDs to use
     * @param sortOrder ALPHA or POPULARITY (the number of times the tag
     *                                       occurs in the documents)
     * @param tagCounter the structure responsible for collecting and counting
     *                  tags belonging to the facets.
     * @return          A map of tags, derived from the docIDs. The structure
     *                  is [FacetName]([TagName, TagCount]{0, limit})
     */
    protected abstract HashMap<String, List<FlexiblePair<Integer, Integer>>>
                   getFirstX(int[] docIDs, int startIndex, int endIndex,
                             SortOrder sortOrder,
                             StructureDescription structureDescription,
                             TagCounter tagCounter);

/*    protected HashMap<String, List<FlexiblePair<String, Integer>>>
                   getFirstX(int[] docIDs,
                             SortOrder sortOrder,
                             int maxFacets, int maxTags, int maxObjects) {
        return getFirstX(docIDs, 0, docIDs.length-1, sortOrder,
                         maxFacets, maxTags, maxObjects);
    }*/

    /**
     * Extracts the wanted number of runnables from the properties and calls
     * getFirstXThreaded(docIDs, limit, sortOrder, threadCount).
     * @param docIDs    the documentID's to extract the tags from. Note that
     *                  the order of the docIDs might be modified by
     *                  this method
     * @param sortOrder ALPHA or POPULARITY (the number of times the tag
     *                                       occurs in the documents)
     * @return          A map of tags, derived from the docIDs. The structure
     *                  is [FacetName]([TagName, TagCount]{0, limit})
     */
    public HashMap<String, List<FlexiblePair<String, Integer>>>
                           getFirstXThreaded(int[] docIDs,
                                             SortOrder sortOrder) {
        Integer threadCount = ClusterCommon.getPropertyInt("ClusterMapThreads");
        if (threadCount == null) {
            log.error("Could not get ClusterMapThreads from properties file. " +
                      " Defaulting to 2 threads");
            threadCount = 2;
        }
        return getFirstXThreaded(docIDs, sortOrder, threadCount);
    }

    /**
     * Creates a structure with the wanted cluster information.
     * @param docIDs    the documentID's to extract the tags from. Note that
     *                  the order of the docIDs might be modified by
     *                  this method
     * @param sortOrder ALPHA or POPULARITY (the number of times the tag
     *                                       occurs in the documents)
     * @param threadCount the number of runnables to use for this query
     * @return          A map of tags, derived from the docIDs. The structure
     *                  is [FacetName]([TagName, TagCount]{0, limit})
     */
    public synchronized HashMap<String, List<FlexiblePair<String, Integer>>>
                           getFirstXThreaded(int[] docIDs,
                                             SortOrder sortOrder,
                                             int threadCount) {
        pf.reset();
        if (docIDs.length < threadCutoff) {
            log.debug("Throttled runnables to 1 because there were only " +
                      docIDs.length + " hits");
            threadCount = 1;
        }
        log.info("Clustering with " + docIDs.length + " hits and " +
                 threadCount + " runnables");

        int partSize = docIDs.length / threadCount;

        // Create and start runnables
        while (runnables.size() < threadCount) {
            runnables.add(new ClusterMapCompleteThread(this,
                                                       getTagCounter()));
        }

        // Ensure that we are not in the process of cleaning
        cleaner.waitForCleanup();
        log.debug("Cleanup after " + pf.getSpendTime());

        List<Thread> threads = new ArrayList<Thread>(threadCount);
        for (int threadID = 0 ; threadID < threadCount ; threadID++) {
            ClusterMapCompleteThread runnable = runnables.get(threadID);
            if (threadCount == 1) {
                runnable.prepareRun(docIDs, 0, docIDs.length, sortOrder,
                                    clusterDescription);
            } else {
                int concreteSize = threadID != threadCount-1 ? partSize :
                                   docIDs.length-threadID*partSize;
                log.debug("Thread-call with " + threadID*partSize + "=>" +
                          (threadID*partSize+concreteSize) +
                          " out of " + docIDs.length);
                runnable.prepareRun(docIDs,
                                    threadID*partSize,
                                    threadID*partSize+concreteSize-1,
                                    sortOrder,
                                    clusterDescription);
            }

            Thread thread = new Thread(runnable);
            threads.add(thread);
            thread.start();
        }
        log.debug("Cluster runnables started after " + pf.getSpendTime());
        // Wait for runnables to finish
        // TODO: Use timeout here
//        Profiler pft = new Profiler();
        for (Thread thread: threads) {
            try {
                thread.join(MAX_WAIT_MS_FOR_THREADS);
            } catch (InterruptedException e) {
                log.fatal("Exception waiting for threads to finish", e);
            }
        }
        log.debug("Finishing thread-wait");
        log.debug("All threads finished after " + pf.getSpendTime());

        List<HashMap<String, List<FlexiblePair<Integer, Integer>>>> results =
        new LinkedList<HashMap<String, List<FlexiblePair<Integer, Integer>>>>();
        for (int threadID = 0 ; threadID < threadCount ; threadID++) {
            ClusterMapCompleteThread runnable = runnables.get(threadID);
            results.add(runnable.getResult());
        }
        log.debug("Merging and reducing results");
        HashMap<String, List<FlexiblePair<Integer, Integer>>> resultIndex =
                reduceMap(mergeMaps(results), sortOrder);
        log.debug("Indexes calculated, fetching names");
        HashMap<String, List<FlexiblePair<String, Integer>>> resultName =
                indexesToString(resultIndex);
        log.info("Total clustering of " + docIDs.length + " documents " +
                 "completed in " + pf.getSpendTime());
        // Start a threaded optimise before returning
        cleaner.cleanup();
        return resultName;
    }

    private HashMap<String, List<FlexiblePair<Integer, Integer>>> mergeMaps(
       List<HashMap<String, List<FlexiblePair<Integer, Integer>>>> clusterMaps) {
        log.debug("Merging facets");
        HashMap<String, List<FlexiblePair<Integer, Integer>>> merged = null;
        for (HashMap<String, List<FlexiblePair<Integer, Integer>>> clusterMap:
                clusterMaps) {
            if (merged == null) {
                merged = clusterMap;
            } else {
                addMap(clusterMap, merged);
            }
        }
        return merged;
    }

    private void addMap(HashMap<String, List<FlexiblePair<Integer, Integer>>>
                                source,
                        HashMap<String, List<FlexiblePair<Integer, Integer>>>
                                destination) {
        for (Map.Entry<String, List<FlexiblePair<Integer, Integer>>> sEntry:
                source.entrySet()) { // Iterate through source facets
            List<FlexiblePair<Integer, Integer>> dList =
                    destination.get(sEntry.getKey());
            if (dList == null) { // Just add the taglist
                destination.put(sEntry.getKey(), sEntry.getValue());
            } else { // Merge the tags
                List<FlexiblePair<Integer, Integer>> sList = sEntry.getValue();
                for (FlexiblePair<Integer, Integer> sPair: sList) {
                    boolean found = false;
                    for (FlexiblePair<Integer, Integer> dPair: dList) {
                        if (sPair.getKey().equals(dPair.getKey())) { // Merge
                            dPair.setValue(sPair.getValue()+dPair.getValue());
                            found = true;
                            break;
                        }
                    }
                    if (!found) { // Add non-existing
                        dList.add(sPair);
                    }
                }
            }
        }
    }

    private HashMap<String, List<FlexiblePair<Integer, Integer>>> reduceMap(
            HashMap<String, List<FlexiblePair<Integer, Integer>>> clusterMap,
            SortOrder sortOrder) {
        log.debug("Reducing facets");
        for (Map.Entry<String, List<FlexiblePair<Integer, Integer>>> facet:
                clusterMap.entrySet()) { // Iterate through facets
//            log.debug("Reducing facet " + facet.getKey() + " " +
//                      facet.getValue().size() + " => " +
//                      Math.min(facet.getValue().size(), maxTags));*/
            if (sortOrder == SortOrder.ALPHA) {
                Collections.sort(facet.getValue()); // Pairs sort on primary
            } else if (sortOrder == SortOrder.POPULARITY) {
                Collections.sort(facet.getValue()); // Pairs sort on secondary
            }
            Integer maxTags = clusterDescription.getMaxTags(facet.getKey());

            if (facet.getValue() != null && facet.getValue().size() > maxTags) {
                List<FlexiblePair<Integer, Integer>> reduced =
                                           facet.getValue().subList(0, maxTags);
                facet.setValue(reduced);
            }
        }
        return clusterMap;
    }

    public void setLimits(int maxFacets, int maxTags, int maxObjects) {
        // TODO: Should these override?
//        this.maxFacets = maxFacets;
//        this.maxTagsGeneric = maxTags;
//        this.maxObjects = maxObjects;
    }
    /**
     * A counterlist is a list of integers of the same length as a given
     * expanded facet list. The entries in the counter list are counters for
     * the corresponding tag in the expanded facet list.
     */
    public int[][] getCounterLists() {
        log.info("Creating counter lists");
        int[][] counterLists =
                new int[clusterDescription.getFacetNames().size()][];
        for (int facetID = 0 ;
             facetID < clusterDescription.getFacetNames().size() ;
             facetID++) {
            counterLists[facetID] = getCounterList(facetID);
        }
        return counterLists;
    }

    public TagCounter getTagCounter() {
        log.info("Creating tag counter");
        // TODO: Use the Strategy Pattern to switch between TagCounters dynamically
//        return new TagCounterLight(facetNames);
        int[][] counterLists =
                new int[clusterDescription.getFacetNames().size()][];
        for (int facetID = 0 ;
             facetID < clusterDescription.getFacetNames().size() ;
             facetID++) {
            counterLists[facetID] = getCounterList(facetID);
        }
        throw new UnsupportedOperationException("Deprecated class");
//        return new TagCounterArray(clusterDescription.getFacetNames(),
//                                   counterLists);
    }

    protected abstract int[] getCounterList(int facetID);
}
