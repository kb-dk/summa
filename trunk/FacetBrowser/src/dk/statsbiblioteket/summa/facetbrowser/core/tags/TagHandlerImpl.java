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

import java.util.List;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.summa.facetbrowser.util.pool.SortedPool;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Provides (optionally persistent) maps of tags in facets. The mapping is
 * optimised towards access-speed. Incremental updates of the map is
 * possible, although significantly slower than a complete
 * {@link TagHandlerFactory#fill} if the whole map needs to be build.
 */
@QAInfo(state=QAInfo.State.IN_DEVELOPMENT)
public class TagHandlerImpl implements TagHandler {
    private Log log = LogFactory.getLog(TagHandlerImpl.class);

    private Facet[] facets;
    private StructureDescription structure;
    private boolean dirty = false;
    public static final String PERSISTENCE_PREFIX = "tags_";

    /**
     * Constructs a tag handler around the given pools.
     * @param structure the description of the setup.
     * @param facets pools to hold tags. There must be a pool for each facet
     *               specified in structure.
     */
    public TagHandlerImpl(StructureDescription structure,
                          Facet[] facets) {
        this.structure = structure;
        this.facets = facets;
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
        return facets[facetID].getPosition(tagName);
    }

    public String getTagName(int facetID, int tagID) {
        failIfDirty();
        return facets[facetID].getValue(tagID);
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

    public void load(File folder) throws IOException {
        dirty = false;
        log.debug("Loading tag handler data from folder \"" + folder + "\"");
        int facetID = 0;
        for (SortedPool<String> tagPool: facets) {
            String facetName = structure.getFacetName(facetID++);
            log.debug("Loading tags \"" + facetName + "\"");
            tagPool.load(folder, PERSISTENCE_PREFIX + facetName);
        }
        //noinspection DuplicateStringLiteralInspection
        log.debug("Finished loading tags for " + facets.length + " facets");
    }

    public void store(File folder) throws IOException {
        failIfDirty();
        log.debug("Storing tag handler data to folder \"" + folder + "\"");
        int facetID = 0;
        for (SortedPool<String> tagPool: facets) {
            String facetName = structure.getFacetName(facetID++);
            log.debug("Storing tags \"" + facetName + "\"");
            tagPool.store(folder, PERSISTENCE_PREFIX + facetName);
        }
        //noinspection DuplicateStringLiteralInspection
        log.debug("Finished storing tags for " + facets.length + " facets");
    }

    public void close() {
        //noinspection DuplicateStringLiteralInspection
        log.debug("Closing " + facets.length + " facets");
        for (SortedPool<String> tagPool: facets) {
            tagPool.clear();
        }
        //noinspection AssignmentToNull
        facets = null;
        log.trace("Finished closing tag pools. Tag handler state is now"
                  + " unstable");
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
        facets[facetID].dirtyAdd(tagName);
    }

    public void cleanup() {
        //noinspection DuplicateStringLiteralInspection
        log.debug("Cleaning " + facets.length + " facets");
        int facetID = 0;
        for (SortedPool<String> tagPool: facets) {
            log.debug("Cleaning tagPool " + structure.getFacetName(facetID++)
                      + " with " + tagPool.size() + " tags");
            tagPool.cleanup();
        }
        log.debug("Finished cleaning");
    }

    public int addTag(int facetID, String tagName) {
        failIfDirty();
        if (log.isTraceEnabled()) {
            log.trace("Adding \"" + tagName + " to facet "
                      + structure.getFacetName(facetID));
        }
        return facets[facetID].add(tagName);
    }

    public void removeTag(int facetID, int tagID) {
        failIfDirty();
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("Removing tag \"" + facets[facetID].getValue(tagID)
                      + "\" with ID " + tagID + " from facet "
                      + structure.getFacetName(facetID));
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
