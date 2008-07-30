/* $Id: CoreMapFactory.java,v 1.5 2007/10/05 10:20:22 te Exp $
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
 * CVS:  $Id: CoreMapFactory.java,v 1.5 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core.map;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.index.IndexConnector;
import dk.statsbiblioteket.summa.facetbrowser.util.ClusterCommon;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.ClusterException;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class CoreMapFactory {
    private static Logger log = Logger.getLogger(CoreMapFactory.class);

    /**
     * Stopwords are tags that should not be reflected in the core map. They
     * are Field-specific. One example would be to have the stopwords "a" and
     * "the" for the field Subject. This would be stated by
     * FacetBrowser.STOPWORDS_Subject=a, the
     * in the configuration.
     * If stopwords for a given field are unspecified, the stopwords from
     * FacetBrowser.STOPWORDS_Fallback
     * will be used.
     * If there are no stopwords for a given field, the empty list should be
     * specified:
     * FacetBrowser.STOPWORDS_Title=
     * This is used by {@link #fillMap}.
     */
    private static final String STOPWORDSPREFIX =
            "FacetBrowser.STOPWORDS";
    /*
     * If the stopwords for a given field are unspecified, the stopwords defined
     * in LuceneFacetBrowser.STOPWORDS_Fallback are used for that field.
     * This is used by {@link #fillMap}.
     */
//    private static final String STOPWORDSFALLBACK =
//            STOPWORDSPREFIX + "_Fallback";

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static CoreMap getCoreMap(int documents, int facets, int maxTags)
            throws ClusterException {
        if (CoreMapBitStuffed.canHandle(facets, maxTags)) {
            log.info("Creating " + CoreMapBitStuffed.class.getName() +
                     " based on " + facets +
                     " facets with a maximum of " + maxTags + " tags");
            return new CoreMapBitStuffed(documents, facets);
        } else if (CoreMapBitStuffedLong.canHandle(facets, maxTags)) {
            log.info("Creating " + CoreMapBitStuffedLong.class.getName() +
                     " based on " + facets +
                     " facets with a maximum of " + maxTags + " tags");
            return new CoreMapBitStuffedLong(documents, facets);
        } else {
            throw new ClusterException("No CoreMap capable of handling " +
                                       facets + " facets, " +
                                       "where at least one facet has " +
                                       maxTags +
                                       " tags");
        }
    }

    /**
     * Updates the given map to reflect the link between docIDs and facet/tags.
     * @param map           the map to update.
     * @param structure     the structure for the facet browser (facet names and
     *                      such).
     * @param tagHandler    the resolver for tags from IDs.
     * @param indexConnector a connection to a Lucene index.
     * @param configuration the configuration for this map.
     * @param incremental   attempt to update the map incrementally. Only new
     *                      documents will be reflected in the map, while
     *                      changed documents are ignored.
     *                      Note: This is not supported yet. Setting this to
     *                      true means that the filling will be skipped if the
     *                      map is up to date and that the map will be reset
     *                      and filled from the start, if it is not up to date.
     * @return true if the index was updated.
     * @throws IOException if there was a problem accessing the index.
     */
    @QAInfo(state=QAInfo.State.QA_NEEDED,
            level=QAInfo.Level.FINE,
            comment="Iterative updates should be checked especially well")
    public static boolean fillMap(CoreMap map,
                                  StructureDescription structure,
                                  TagHandler tagHandler,
                                  IndexConnector indexConnector,
                                  Configuration configuration,
                                  boolean incremental) throws IOException {
        log.info("Filling core map for facet browsing");
        IndexReader indexReader = indexConnector.getReader();
        if (incremental && indexReader.maxDoc() == map.getDocCount()) {
            log.info("Core map is up-to-date. No updates performed");
            return false;
        }
        if (!incremental) {
            map.clear();
        }

        Map<String, Set<String>> stopWords = getStopwords(configuration,
                                                          structure);
        Profiler totalProgress = new Profiler();
        int maxDoc = indexReader.maxDoc();

        Profiler progress = new Profiler();
        progress.setExpectedTotal(maxDoc);
        int feedbackEvery =
                Math.min(10000,
                         Math.max(1000, (maxDoc - map.getDocCount()) / 1000));
        progress.setBpsSpan(feedbackEvery * 5);
        for (int docID = map.getDocCount() ; docID < maxDoc ; docID++) {
            if (docID % feedbackEvery == 0) {
                //noinspection DuplicateStringLiteralInspection
                log.debug("Mapped " + docID + "/" + maxDoc +
                          " ETA: " +  progress.getETAAsString(true) +
                          ". " + ClusterCommon.getMem());
            }
            fillFromRecord(docID, map, structure, tagHandler, indexReader,
                           stopWords);
            progress.beat();
        }
        log.info("Core map created in " + totalProgress.getSpendTime());
        return true;
    }

    /* Constructs a map with stopwords belonging to the different fields.
       All fields stated in the structure are guaranteed to be present.
     */
    private static Map<String, Set<String>> getStopwords(Configuration
                                                         configuration,
                                                         StructureDescription
                                                         structure)
            throws IOException {
        Map<String, Set<String>> stopwords =
            new HashMap<String, Set<String>>(structure.getFacetNames().size());
/*        List<String> fallbackS = configuration.valueExists(STOPWORDSFALLBACK) ?
                                 configuration.getStrings(STOPWORDSFALLBACK) :
                                 new ArrayList<String>(1);
        Set<String> fallback = fallbackS == null ?
                               new HashSet<String>(1) :
                               new HashSet<String>(fallbackS);*/
        for (String facetName: structure.getFacetNames()) {
            String key = STOPWORDSPREFIX + "_" + facetName;
            stopwords.put(facetName, configuration.valueExists(key) ?
                            new HashSet<String>(configuration.getStrings(key)) :
                            new HashSet<String>(1));
        }
        return stopwords;
    }

    private static void fillFromRecord(int docID,
                                       CoreMap map,
                                       StructureDescription structure,
                                       TagHandler tagHandler,
                                       IndexReader indexReader,
                                       Map<String, Set<String>> stopWords)
            throws IOException {
        for (Map.Entry<String, Set<String>> facetEntry: stopWords.entrySet()) {
            String facetName = facetEntry.getKey();
            int facetID = structure.getFacetID(facetName);
            Set<String> stops = facetEntry.getValue();
            TermFreqVector concreteVector =
                            indexReader.getTermFreqVector(docID, facetName);
            if (concreteVector == null) {
                log.debug("No termFreqVector found for facet " + facetName
                          + " with document " + docID);
                map.add(docID, facetID, new int[0]); // Do we need to do this?
            } else {
                String[] terms = concreteVector.getTerms();
                List<Integer> tags = new ArrayList<Integer>(terms.length);
                for (String term: terms) {
                    if (!stops.contains(term)) {
                        if (log.isTraceEnabled()) {
                            //noinspection DuplicateStringLiteralInspection
                            log.trace("Adding " + term + " to " + facetName);
                        }
                        try {
                            tags.add(tagHandler.getTagID(facetID, term));
                        } catch (Exception e) {
                            //noinspection DuplicateStringLiteralInspection
                            log.warn("Could not find ID for tag " + term
                                      + " from document " + docID
                                      + " in facet " + facetName);
                        }
                    }
                }
                int[] tagIDs = new int[tags.size()];
                int counter = 0;
                for (int tagID: tags) {
                    tagIDs[counter++] = tagID;
                }
                map.add(docID, facetID, tagIDs);
            }
        }
    }
}
