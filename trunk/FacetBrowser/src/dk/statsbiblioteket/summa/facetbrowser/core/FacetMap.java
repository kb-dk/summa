/* $Id: FacetMap.java,v 1.6 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.6 $
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
 * CVS:  $Id: FacetMap.java,v 1.6 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core;

import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a mapping between document-IDs and facet/tags. Basic addition
 * and removal of document-IDs is possible, allowing for iterative updates.
 * </p><p>
 * The FacetMap is the main interaction-interface to Facet updating and takes 
 * care on keeping TagHandler and CoreMap in sync.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetMap {
    private Log log = LogFactory.getLog(FacetMap.class);

    private CoreMap coreMap;
    private TagHandler tagHandler;
    private Structure structure;
    private boolean readOnly;

    public FacetMap(Structure structure,
                    CoreMap coreMap, TagHandler tagHandler, boolean readOnly) {
        log.debug("Constructing facet map");
        this.structure = structure;
        this.coreMap = coreMap;
        this.tagHandler = tagHandler;
        this.readOnly = readOnly;
    }

    /**
     * Removes a document and its corresponding facet/tag pairs from the map.
     * This operation should be fast enough for real-time updates.
     * Note: Only the mapping from document-IDs to facet/tags is changed by
     *       this method: No optimisation of the facet/tags storage is
     *       performed. If an optimisation is needed, call {@link #optimise}.
     * @param docID the document to remove.
     */
    public void removeDocument(int docID) {
        checkReadOnly();
        log.debug("Removing document " + docID);
        coreMap.remove(docID);
        log.trace("Removed document " + docID);
    }

    private void checkReadOnly() {
        if (readOnly) {
            throw new IllegalStateException(
                    "The CoreMap is marked as read-only");
        }
    }

    /**
     * Assigns facetTags to the given document. If the document already exists
     * in the map, the facetTags are merged with existing facetTags.<br />
     * See {@link #addToDocumentByID} for a faster alternative.
     * @param docID     the ID of the document to add, this needs to be either
     *                  0 or <= docCount.
     * @param facetTags a map with facet/tag pairs. The keys in the map are
     *                  facets names, while the values are tag names.
     */
    public synchronized void addToDocument(int docID, Map<String,
            List<String>> facetTags) {
        checkReadOnly();
        if (facetTags.size() == 0) {
            log.trace("addToDocument: No Facet/Tag pairs received for doc "
                      + docID);
            return;
        }
        log.trace("addToDocument: Adding " + facetTags.size()
                  + " String-based facet/tags to document " + docID);
        Map<Integer, List<Integer>> facetTagIDs =
                new HashMap<Integer, List<Integer>>(facetTags.size());
        for (Map.Entry<String, List<String>> facet: facetTags.entrySet()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("Adding " + facet.getValue().size() + " tags for facet \""
                      + facet.getKey() + "\" to tag handler");
            int facetID = structure.getFacetID(facet.getKey());
            List<Integer> tagIDs =
                    new ArrayList<Integer>(facet.getValue().size());
            for (String tag: facet.getValue()) {
                int tagID = tagHandler.insertTag(facetID, tag);
                if (tagID >= 0) {
                    coreMap.adjustPositions(facetID, tagID, 1);
                    // Adjust already resolved tagIDs
                    for (int i = 0 ; i < tagIDs.size() ; i++) {
                        if (tagIDs.get(i) >= tagID) {
                            tagIDs.set(i, tagIDs.get(i) + 1);
                        }
                    }
                } else { // Tag already exists
                    tagID = -1 * (tagID + 1);
                }
                tagIDs.add(tagID);
            }
            facetTagIDs.put(facetID, tagIDs);
        }
        log.trace("Finished adding Strings for document " + docID
                  + ", switching to map update");
        addToDocumentByID(docID, facetTagIDs);
    }

    public void addToDocument(int docID, String facet, List<String> tags) {
        Map<String, List<String>> facetTags =
                new HashMap<String, List<String>>(1);
        facetTags.put(facet, tags);
        addToDocument(docID, facetTags);
    }

    public void add(int docID, String facet, String tag) throws IOException {
        int facetID = structure.getFacetID(facet);
        int tagID = tagHandler.insertTag(facetID, tag);
        if (tagID >= 0) {
            coreMap.adjustPositions(facetID, tagID, 1);
        } else {
            tagID = (tagID * -1) - 1;
        }
        coreMap.add(docID, facetID, new int[]{tagID});
    }

    /**
     * Assigns facetTags to the given document. If the document already exists
     * in the map, the facetTags are merged with existing facetTags.<br />
     * This method differs from {@link #addToDocument} by using IDs for
     * facets and tags. This requires that the facet/tags has previously been
     * added. This method is considerably faster than addToDocument.
     * @param docID     the ID of the document to add, this needs to be either
     *                  0 or <= docCount.
     * @param facetTags a map with facet/tag pairs. The keys in the map are
     *                  facetIDs, while the values are tagIDs.
     */
    public void addToDocumentByID(int docID,
                                  Map<Integer, List<Integer>> facetTags) {
        checkReadOnly();
        log.trace("Adding ID-based facet/tags to document " + docID);
        for (Map.Entry<Integer, List<Integer>> facet: facetTags.entrySet()) {
            int[] tagIDs = new int[facet.getValue().size()];
            int counter = 0;
            for (Integer tagID: facet.getValue()) {
                tagIDs[counter++] = tagID;
            }
            coreMap.add(docID, facet.getKey(), tagIDs);
        }
        log.trace("Finished adding ID-based facet/tags to document " + docID);
    }

    /**
     * Assigns facetTags to the given document. If the document already exists
     * in the map, the facetTags are merged with existing facetTags.<br />
     * This method differs from {@link #addToDocument} by using IDs for
     * facets and tags. This requires that the facet/tags has previously been
     * added. This method is considerably faster than addToDocument and a bit
     * faster than {@link #addToDocumentByID} .
     * @param docID     the ID of the document to add, this needs to be either
     *                  0 or <= docCount.
     * @param facetTags a map with facet/tag pairs. The keys in the map are
     *                  facetIDs, while the values are tagIDs.
     */
    public void addToDocumentByArray(int docID, Map<Integer, int[]> facetTags) {
        checkReadOnly();
        log.trace("Adding ID-based array-stored facet/tags to doc " + docID);
        for (Map.Entry<Integer, int[]> facet: facetTags.entrySet()) {
            coreMap.add(docID, facet.getKey(), facet.getValue());
        }
        log.debug("Finished adding ID-based array-stored facet/tags to doc "
                  + docID);
    }

    /**
     * Ensures that only reachable facet/tag pairs are stored. A pair is
     * reachable if at least one document points to it.<br />
     * Note 1: If a disk-based tag handler is used, this will only have
     *         an effect when the facets and tags are explicitly stored.
     * Note 2: This takes a considerable amount of time.
     */
    public void optimise() {
        checkReadOnly();
        log.warn("Optimising facet map not implemented yet");
    }

    /**
     * Stores the map at the given location.
     * @throws IOException if an I/O error occured.
     */
    public void store() throws IOException {
        coreMap.store();
        tagHandler.store();
    }

    /**
     * @return the number of mapped documents.
     */
    public int getDocCount() {
        return coreMap.getDocCount();
    }

    public void close() {
        // TODO: Consider adding close to CoreMap
        coreMap.clear();
        tagHandler.close();
    }

    public void open(File location) throws IOException {
        tagHandler.open(location);
        coreMap.open(location, false);
    }

    public TagHandler getTagHandler() {
        return tagHandler;
    }

    public Structure getStructure() {
        return structure;
    }

    public CoreMap getCoreMap() {
        return coreMap;
    }
}



