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
package dk.statsbiblioteket.summa.facetbrowser.core;

import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.io.IOException;
import java.io.StringWriter;
import java.io.File;
import java.io.PrintStream;
import java.io.BufferedReader;

import org.apache.log4j.Logger;
import dk.statsbiblioteket.summa.facetbrowser.build.facet.FacetModelImplOO;
import dk.statsbiblioteket.summa.facetbrowser.build.facet.Facet;
import dk.statsbiblioteket.summa.facetbrowser.build.facet.Tag;
import dk.statsbiblioteket.summa.facetbrowser.util.ClusterCommon;
import dk.statsbiblioteket.summa.facetbrowser.util.FlexiblePair;
import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounter;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMapFactory;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMapBitStuffedLong;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandlerFactory;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * @deprecated a suitable replacement is under development.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public class ClusterMapCompleteArray extends ClusterMapComplete {
    int maxTagsPerFacetOnBuild = Integer.MAX_VALUE;
    private static Logger log = Logger.getLogger(ClusterMapCompleteArray.class);
    protected static final String PERSISTENCE_LOCATION =
            "CompleteClustermapLocation";
    public static File defaultPersistenceLocation =
            new File(new File(System.getProperty("java.io.tmpdir")), "completeClustermap").getAbsoluteFile();

    // [FacetID][TagName, TagPosition] || [FacetID][TagPosition, TagName]
    protected TagHandler expandedFacets;

    // [DocID, FacetID][TagPositions]
    public CoreMap map; // TODO: Encapsulate this

    private static final String MAPINFO_FILENAME = "mapinfo.dat";

    public ClusterMapCompleteArray() throws IOException {
        super();
    }
    public ClusterMapCompleteArray(int limit) throws IOException {
        super(limit);
    }
    public ClusterMapCompleteArray(int limit,
                              int maxFacets, int maxTags,
                              int maxObjects) throws IOException {
        super(limit, maxFacets, maxTags, maxObjects);
    }

    protected void init() throws IOException {
        super.init();
    }

    protected void logFacetNames() {
        StringWriter sw = new StringWriter();
        sw.append("Facets: ");
        int counter = 0;
        for (String facet: clusterDescription.getFacetNames()) {
            sw.append(facet).append("(").append(Integer.toString(counter++));
            sw.append(") ");
        }
        log.debug(sw.toString());
    }

    protected int getTagID(int facetID, String value) {
        return expandedFacets.getTagID(facetID, value);
    }


    /**
     * Run through all the Documents in the Lucene index and fill the map with
     * tag-indexes to the expandedFacets. After this, the Cluster Map is ready
     * for cluster showing.
     */
    public void fillMap() throws IOException, ClusterException {
        log.info("Filling complete cluster map");
        Profiler totalProgress = new Profiler();
        int maxDoc = getDocCount();
        long entries = 0;
        int facetCount = clusterDescription.getFacetNames().size();

        log.info("Expanding facet content");
        // TODO: Implement this
//        expandedFacets =
//                TagHandlerFactory.getTagHandler(ir, clusterDescription);

//        expandedFacets = new ArrayList<SortedHash<String>>(facetNames.size());
//        int fCounter = 0;
//        expandedFacets.fill(ir, facetNames);
/*        for (String facet: facetNames) {
            log.info("  Expanding " + facet +
                     " (" + ++fCounter + "/" + facetNames.size() + ")");
            expandedFacets.add(expandFacetContent(facet));
        }
  */
        log.info("Initializing to " + maxDoc + " docs with " +
                 facetCount + " facets with max " + maxTagsPerFacetOnBuild +
                 " tags. " + ClusterCommon.getMem());
        map = CoreMapFactory.getCoreMap(maxDoc, facetCount,
                                        expandedFacets.getMaxTagCount());
        log.info("Core map created");
        Profiler progress = new Profiler();
        progress.setExpectedTotal(maxDoc);
        int feedbackEvery = Math.min(10000, Math.max(1000, maxDoc / 1000));
        progress.setBpsSpan(feedbackEvery * 5);
        log.debug("Feedback for every " + feedbackEvery + " document");
        for (int docID = 0 ; docID < maxDoc ; docID++) {
            if (docID % feedbackEvery == 0) {
                log.debug("Mapped " + docID + "/" + maxDoc +
                          " ETA: " +  progress.getETAAsString(true) +
                          ". " + ClusterCommon.getMem());
            }
            FacetModelImplOO<String> model =
                    new FacetModelImplOO<String>(-1, 0);
            tvHandler.handle(ic, model, docID, "", 0.0f);
            for (int facetID = 0 ; facetID < facetCount ; facetID++) {
                Facet<String> facet =
                        model.getFacet(clusterDescription.getFacetName(facetID));
                if (facet == null) {
                    map.add(docID, facetID, new int[0]);
                } else {
                    Collection<Tag<String>> tags = facet.getTags();
                    int[] tagArray = new int[Math.min(tags.size(),
                                                      maxTagsPerFacetOnBuild)];
                    int counter = 0;
                    for (Tag tag: tags) {
                        String tagName = tag.getName().replaceAll("\n", "");
                        Integer tagID =
                                expandedFacets.getTagID(facetID, tagName);
                        if (tagID == null || tagID == -1) {
                            // This should happen rarely, so we accept the
                            // overhead from the creation of a new array
                            log.debug("Could not locate tag " + tagName +
                                      " from " +
                                      clusterDescription.getFacetName(facetID) +
                                      " in the expanded facets");
                            int[] newArray = new int[tagArray.length-1];
                            System.arraycopy(tagArray, 0, newArray, 0, counter);
                            tagArray = newArray;
                        } else {
                            tagArray[counter] = tagID;
                            counter++;
                        }
                        if (counter == maxTagsPerFacetOnBuild) {
                            log.trace("Skipping the remaining tags for doc " +
                                      docID + " - " + facet.getName());
                            break;
                        }
                    }
                    entries += counter;
                    map.add(docID, facetID, tagArray);
                }
            }
            progress.beat();
        }
        log.info("Complete cluster map with " + entries + " entries " +
                 "created in " + totalProgress.getSpendTime());
    }

    /**
     * Note: This method should be kept thread-safe!
     */
    public HashMap<String, List<FlexiblePair<Integer, Integer>>>
            getFirstX(int[] docIDs, int startIndex, int endIndex,
                      SortOrder sortOrder,
                      StructureDescription structureDescription,
                      TagCounter tagCounter) {
        log.debug("getFirstX called with " + docIDs.length +
                  " docs, responsibility for " + startIndex +
                  " => " + endIndex);
        Profiler mapPF = new Profiler();
        if (tagCounter == null) {
            log.info("Creating tagCounter for getFirstX (performance hit)");
            tagCounter = getTagCounter();
        }

/*        Arrays.sort(docIDs, startIndex, endIndex-1); // TODO: Check the -1 part
        log.info("Sorting " + (endIndex - startIndex) +
                 " docIDs took " + mapPF.getSpendTime());
        // While sorting speeds the mapping, it seems to take too long

  */
        endIndex = Math.min(docIDs.length-1, endIndex); // Sanity check

        mapPF.reset();
        map.markCounterLists(tagCounter, docIDs, startIndex, endIndex);
        log.info("Mapped in " + mapPF.getSpendTime());

        mapPF.reset();
        // Sort
        throw new UnsupportedOperationException("Due to API change, this does not work anymore");
/*        return null;
        HashMap<String, List<FlexiblePair<Integer, Integer>>> result =
                tagCounter.getFirst(sortOrder);
        log.info("Sorted in " + mapPF.getSpendTime());
        log.debug("Extraction and sorting finished");
        return result;*/
    }

    /**
     * Human readable statistics for the map.
     * @return a String describing the map
     */
    public String getStats() {
        StringWriter sw = new StringWriter(500);
        sw.append("CompleteClusterMap v" + VERSION);
        sw.append("Documents in ClusterMap/Index: ").append(String.valueOf(getDocCount())).
                append("/").append(String.valueOf(ir.maxDoc())).append("\n");
        sw.append("Unique tags in facets:\n");
        int facetID = 0;
        int tagCount = 0;
        for (String facet: getFacets()) {
            sw.append("  ").append(facet).append("\t(");
            sw.append(Integer.toString(expandedFacets.getFacetSize(facetID++)));
            sw.append(")");
            tagCount += expandedFacets.getFacetSize(facetID-1);
            sw.append("\n");
        }
        sw.append("Total: ").append("Facets ");
        sw.append(Integer.toString(getFacets().size())).append(", ");
        sw.append("Tags ").append(Integer.toString(tagCount)).append("\n");

        sw.append("Pointers from ").append(String.valueOf(getDocCount())).
                append(" documents to Tags in Facets, with max/total:\n");
        facetID = 0;
        int totalTagCount = 0;
        for (String facet: getFacets()) {
            sw.append("  ").append(facet).append("\t(");
            tagCount = 0;
            int max = 0;
            for (int docID = 0 ; docID < getDocCount() ; docID++) {
                int count = map.get(docID, facetID).length;
                max = Math.max(max, count);
                tagCount += count;
                totalTagCount += count;
            }
            sw.append(Integer.toString(max)).append("/");
            sw.append(Integer.toString(tagCount)).append(")");
            facetID++;
            sw.append("\n");
        }
        sw.append("Total pointers: ");
        sw.append(Integer.toString(totalTagCount)).append("\n");
        sw.append("Memory usage: ");
        sw.append(ClusterCommon.getMem());
        return sw.toString();
    }

    public void storeMap(File mapLocation) throws IOException {
        if (!mapLocation.exists()) {
            mapLocation.mkdir();
        }
        log.info("Storing statistics");
        File stats = new File(mapLocation, "stats.txt");
        Files.saveString(getStats(), stats);

        expandedFacets.store(mapLocation);

        log.info("Storing map information");
        PrintStream mapInfoPrint =
                    ClusterCommon.stringPrinter(mapLocation, MAPINFO_FILENAME);
        mapInfoPrint.println("Key\tValue");
        mapInfoPrint.print("Documents\t");
        mapInfoPrint.println(Integer.toString(getDocCount()));
        mapInfoPrint.print("Facets\t");
        mapInfoPrint.println(Integer.toString(
                                    clusterDescription.getFacetNames().size()));
        mapInfoPrint.close();

        map.store(mapLocation);

        log.info("Finished storing.");

    }

    public File getPersistenceLocation() {
        return new File(
                ClusterCommon.getProperties().getProperty(PERSISTENCE_LOCATION,
                                        defaultPersistenceLocation.toString()));
    }

    public boolean loadMap(File mapLocation) throws IOException, ClusterException {
        log.info("Loading cluster map from " + mapLocation);
        if (!mapLocation.exists()) {
            log.error("The folder " + mapLocation + " does not exist");
            return false;
        }

        try {
            expandedFacets = TagHandlerFactory.getTagHandler(mapLocation,
                                            clusterDescription);
        } catch (IOException e) {
            log.error("Could not load the tag names from disk: " +
                      e.getMessage(), e);
            return false;
        }

        /* Get map information */
        log.info("Loading map information from " + MAPINFO_FILENAME);
        BufferedReader mapInfoLoader =
                ClusterCommon.stringLoader(mapLocation, MAPINFO_FILENAME);
        log.debug("  Got " + MAPINFO_FILENAME);
        mapInfoLoader.readLine(); // Read header
        String docLine = mapInfoLoader.readLine();
//        String facetLine = mapInfoLoader.readLine(); // Not used
        mapInfoLoader.close();
        String[] docTokens = docLine.split("\t");
        if (docTokens.length != 2 || !docTokens[0].equals("Documents")) {
            String error = "The line " + docLine + " dit not fit the " +
                           "expected format \"Documents\\tIntValue\" " +
                           " in " + MAPINFO_FILENAME;
            log.error(error);
            throw new IOException(error);
        }
        Integer docCount = Integer.parseInt(docTokens[1]);
        log.info("  Map should contain " + docCount + " documents");
        if (getDocCount() != docCount) {
            log.error("The stores Cluster Map only maps " + docCount +
                      " documents, while the index contains " + getDocCount() +
                      " documents. Consider rebuilding the cluster map!");
        }
        limit = docCount;

        // TODO: Make loading fail, if the count is not the same as maxdoc

        /* Create and fill map */
        log.debug("Creating core map");
        map = CoreMapFactory.getCoreMap(docCount, getFacets().size(),
                                        expandedFacets.getMaxTagCount());
        log.debug("Loading map content");
        map.load(mapLocation);

        log.info("Finished loading.");
        return true;
    }

    protected HashMap<String, List<FlexiblePair<String, Integer>>>
        indexesToString(
            HashMap<String, List<FlexiblePair<Integer, Integer>>> index) {
        HashMap<String, List<FlexiblePair<String, Integer>>> result =
                new HashMap<String,
                        List<FlexiblePair<String, Integer>>>(index.size());
        for (Map.Entry<String, List<FlexiblePair<Integer, Integer>>> entry:
                index.entrySet()) {
            ArrayList<FlexiblePair<String, Integer>> nametags =
                    new ArrayList<FlexiblePair<String, Integer>>(
                            entry.getValue().size());
            int facetID = expandedFacets.getFacetID(entry.getKey());
            for (FlexiblePair<Integer, Integer> tag: entry.getValue()) {
                try {
                    FlexiblePair<String, Integer> newNameTag =
                        new FlexiblePair<String, Integer>(
                            expandedFacets.getTagName(facetID, tag.getKey()),
                            tag.getValue(), tag.getSortType());
                    nametags.add(newNameTag);
                } catch (Exception e) {
                    log.warn("Could not expand tag #" + tag.getKey() +
                             " for facet " + entry.getKey() + " to tagname", e);
                }
            }
            result.put(entry.getKey(), nametags);
        }
        return result;
    }

    /**
     * Debugging method: Writes the amount of used memory at different stages
     * of deconstruction. Leaves the cluster map invalid.
     * @return Statiastics for the cluster map.
     */
    public String crashAndWriteMemStats() {
        StringWriter sw = new StringWriter(2000);
        System.gc();
        sw.append("Full: ").append(ClusterCommon.getMem()).append("\n");
        expandedFacets.close();
        System.gc();
        sw.append("No tag names: ").append(ClusterCommon.getMem()).append("\n");
        map = new CoreMapBitStuffedLong(1, 1);
        System.gc();
        sw.append("No tag names and no map: ").append(ClusterCommon.getMem()).append("\n");
        return sw.toString();
    }

    protected int[] getCounterList(int facetID) {
        log.trace("Creating counterlist for facet ID " + facetID + 
                  " with size " + expandedFacets.getFacetSize(facetID));
        return new int[expandedFacets.getFacetSize(facetID)];
    }
}
