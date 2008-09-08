/* $Id: TagHandlerImpl.java,v 1.7 2007/10/04 13:28:18 te Exp $
 * $Revision: 1.7 $
 * $Date: 2007/10/04 13:28:18 $
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
 * CVS:  $Id: TagHandlerImpl.java,v 1.7 2007/10/04 13:28:18 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core.tags;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.build.Builder;
import dk.statsbiblioteket.summa.facetbrowser.lucene.LuceneFacetBuilder;
import dk.statsbiblioteket.summa.facetbrowser.util.pool.SortedPool;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Provides (optionally persistent) maps of tags in facets. The mapping is
 * optimised towards access-speed. Incremental updates of the map is
 * possible, although significantly slower than a complete
 * {@link Builder#build(boolean)} if the whole map needs to be build.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TagHandlerImpl implements TagHandler {
    private Log log = LogFactory.getLog(TagHandlerImpl.class);

    private Facet[] facets;
    private Structure structure;
    private boolean dirty = false;

    /**
     * Creates underlying Facets.
     * After construction, {@link #open(File)} must be called before use.
     * @param conf      the setup for the TagHandler.
     * @param structure the overall structure for the FacetBrowser.
     * @param readOnly  if true, the TagHandler will allow only requests.
     * @throws java.io.IOException if the underlying {@link Facet}s could not
     *                             be constructed.
     */
    public TagHandlerImpl(Configuration conf, Structure structure,
                          Boolean readOnly) throws IOException {
        log.debug("Creating TagHandlerImpl");
        this.structure = structure;
        facets = new Facet[structure.getFacetNames().size()];
        boolean useMemory =
                conf.getBoolean(CONF_USE_MEMORY, DEFAULT_USE_MEMORY);

        int position = 0;
        for (Map.Entry<String, FacetStructure> entry:
                structure.getFacets().entrySet()) {
            facets[position++] =
                    new Facet(entry.getValue(), useMemory, readOnly);
        }
        log.trace(String.format("TagHandlerImpl constructor finished with "
                                + "%d facets", position));
    }

    /**
     * Throws an exception of the tag handler is dirty. This is done in order to
     * make sure that the user of the tag handler knows exactly when the IDs
     * for the tags are fixed or not.
     */
    private void failIfDirty() {
        if (dirty) {
            throw new IllegalStateException("The tag handler is dirty and must "
                                            + "be cleaned before use");
        }
    }

    /**
     * @return the list of Facets making up the TagHandler. Normally used for
     *         batch-oriented updates.
     */
    public List<Facet> getFacets() {
        return Arrays.asList(facets);
    }

    /* Interface implementation */

    public int getTagID(int facetID, String tagName) {
        failIfDirty();
        return facets[facetID].indexOf(tagName);
    }

    public String getTagName(int facetID, int tagID) {
        failIfDirty();
        return facets[facetID].get(tagID);
    }

    public int getFacetID(String facetName) {
        failIfDirty();
        return structure.getFacetID(facetName);
    }

    public int getFacetSize(int facetID) {
        failIfDirty();
        return facets[facetID].size();
    }

    public int getMaxTagCount() {
        failIfDirty();
        int max = 0;
        for (SortedPool<String> tagPool: facets) {
            max = Math.max(max, tagPool.size());
        }
        log.trace("Extracted max tag count: " + max);
        return max;
    }

    public int getTagCount(String facetName) {
        failIfDirty();
        return facets[structure.getFacetID(facetName)].size();
    }

    public int getTagCount() {
        failIfDirty();
        int count = 0;
        for (SortedPool<String> tagPool: facets) {
            count += tagPool.size();
        }
        log.trace("Extracted total tag count: " + count);
        return count;
    }

    public List<String> getFacetNames() {
        return structure.getFacetNames();
    }

    // TODO: Rewrite this completely to handle missing pools.
    // TODO: Fail on changed locale or changed fields
    public List<Facet> open(File folder) throws IOException {
        log.debug("Loading tag handler data from folder '" + folder + "'");
        List<Facet> nonLoaded = new ArrayList<Facet>(facets.length);
        dirty = false;
        int facetID = 0;
        for (Facet tagPool: facets) {
            String facetName = structure.getFacet(facetID++).getName();
            log.debug("Loading tags \"" + facetName + "\"");
            if (!tagPool.open(folder)) {
                log.debug("Unable to load data for Facet '" + tagPool + "'");
                nonLoaded.add(tagPool);
            }
        }
        //noinspection DuplicateStringLiteralInspection
        log.debug("Finished loading tags for " + facets.length + " facets");
        return nonLoaded;
    }

    public void store() throws IOException {
        failIfDirty();
        log.debug("Storing tag handler data");
        long startTime = System.currentTimeMillis();
        for (SortedPool<String> tagPool: facets) {
            tagPool.store();
        }
        log.debug(String.format("Finished storing tags for %d facets in %d ms",
                                facets.length,
                                System.currentTimeMillis() - startTime));
    }

    public void close() {
        //noinspection DuplicateStringLiteralInspection
        log.debug(String.format("Closing %d facets", facets.length));
        for (SortedPool<String> tagPool: facets) {
            tagPool.close();
        }
        log.trace("Finished closing tag pools. Tag handler state is now"
                  + " unstable until open is called");
    }

    public String getStats() {
        failIfDirty();
        StringWriter sw = new StringWriter(1000);
        for (int i = 0 ; i < structure.getFacetNames().size() ; i++) {
            sw.append(structure.getFacetNames().get(i)).append(" (").
                    append(String.valueOf(facets[i].size())).append(")\n");
        }
        if (log.isTraceEnabled()) {
            log.trace("Extracted stats: " + sw.toString());
        }
        return sw.toString();
    }

    public void dirtyAddTag(int facetID, String tagName) {
        facets[facetID].add(tagName);
    }

    public void dirtyAddTag(String facetName, String tagName) {
        dirtyAddTag(structure.getFacetID(facetName), tagName);
    }

    public void cleanup() {
        //noinspection DuplicateStringLiteralInspection
        log.debug("Cleaning " + facets.length + " facets");
        long startTime = System.currentTimeMillis();
        int facetID = 0;
        for (SortedPool<String> tagPool: facets) {
            log.debug("Cleaning tagPool "
                      + structure.getFacet(facetID++).getName()
                      + " with " + tagPool.size() + " tags");
            tagPool.cleanup();
        }
        log.debug("Finished cleanup in " 
                  + (System.currentTimeMillis() - startTime) + "ms");
    }

    public int insertTag(int facetID, String tagName) {
        failIfDirty();
        if (log.isTraceEnabled()) {
            log.trace("Adding \"" + tagName + " to facet "
                      + facets[facetID].getName());
        }
        return facets[facetID].insert(tagName);
    }

    public void removeTag(int facetID, int tagID) {
        failIfDirty();
        if (log.isTraceEnabled()) {
            log.trace("Removing tag \"" + facets[facetID].get(tagID)
                      + "\" with ID " + tagID + " from facet "
                      + structure.getFacet(facetID).getName());
        }
        facets[facetID].remove(tagID);
    }

    public void clearTags() {
        log.debug("Clearing all tags");
        for (SortedPool<String> tagPool: facets) {
            tagPool.clear();
        }
    }
}
