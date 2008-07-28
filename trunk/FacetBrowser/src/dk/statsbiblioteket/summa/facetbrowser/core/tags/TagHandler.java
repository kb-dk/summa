/* $Id: TagHandler.java,v 1.7 2007/10/04 13:28:18 te Exp $
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
 * CVS:  $Id: TagHandler.java,v 1.7 2007/10/04 13:28:18 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core.tags;

import dk.statsbiblioteket.summa.facetbrowser.util.pool.SortedPool;
import dk.statsbiblioteket.summa.common.lucene.index.IndexConnector;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.List;
import java.io.File;
import java.io.IOException;

/**
 * The TagHandler is responsible for mapping between integer and String
 * representations of tags.
 * </p><p>
 * The tags are guaranteed to be in consistent order, so order of the id of two
 * tags is equal to comparing their String representation. This is especially
 * relevant for the methods {@link #addTag} and {@link #removeTag}, as they
 * might shift the underlying structure.
 */
@QAInfo(state=QAInfo.State.QA_NEEDED,
        level=QAInfo.Level.NORMAL)
public interface TagHandler {

    /**
     * Resolve the id (integer representation) for a given tag name. This method
     * does not need to be particulary fast, as it should only be used for
     * building facet information based on an index.
     * @param facetID the integer representation of a Facet (can be resolved
     *                by getFacetID).
     * @param tagName the String representation of a Tag under the given Facet.
     * @return        the integer representation for the given Tag or -1 if the
     *                tag does not exist.
     */
    public int getTagID(int facetID, String tagName);

    /**
     * Resolve the name (String representation) for a given tag id. This should
     * be a time-optimised method, as it is expected to be called 1-300 times
     * for each generation of a facet browser result.
     * @param facetID the integer representation of a Facet (can be resolved
     *                by getFacetID).
     * @param tagID   the integer representation of a Tag under the given Facet.
     * @return        the String representation for the given Tag.
     */
    public String getTagName(int facetID, int tagID);

    /**
     * Resolve the id (integer representation) for a given facet name.
     * @param facetName the String representation of the wanted Facet.
     * @return          the integer representation of the Facet, -1 if the
     *                  Facet could not be found.
     */
    public int getFacetID(String facetName);

    /**
     * Calculate the number of Tags in the given Facet.
     * @param facetID the integer representation of a Facet (can be resolved
     *                by getFacetID).
     * @return        the number of Tags in the Facet.
     */
    public int getFacetSize(int facetID);

    /**
     * Iterate through the taglists and return the maximum tagcount.
     * @return the maximum number of tags in any facet
     */
    public int getMaxTagCount();

    /**
     * Get the number of tags in a given facet.
     * @param facetName the name of the facet to get the tag count fo.
     * @return the number of tags in the given facet.
     */
    public int getTagCount(String facetName);

    /**
     * Get the sum of the tags in all facets.
     * @return the total number of Tags.
     */
    public int getTagCount();

    /**
     * Get the Facet names.
     * @return the Facet names.
     */
    public List<String> getFacetNames();

    /**
     * @return the list of Facets making up the TagHandler. Normally used for
     *         batch-oriented updates.
     */
    public List<Facet> getFacets();

    /**
     * Load the structure from disk.
     * @param folder       the folder to store the content in.
     * @throws IOException in case of disk-related problems during store.
     */
    public void load(File folder) throws IOException;

    /**
     * Store the structure on disk.
     * @param folder       the folder to store the content in.
     * @throws IOException in case of disk-related problems during store.
     */
    public void store(File folder) throws IOException;

    /**
     * Close down any open files and similar resources. The state of the
     * TagHandler is invalid after close.
     */
    public void close();

    /**
     * Create human readable statistics for the facets and tags.
     * @return human readable statistics.
     */
    public String getStats();

    /**
     * Add a tag to the given facet. This method does not guarantee external
     * consistency for tagIDs. It should be used for quickly adding a lot
     * of tags for later use.<br />
     * A cleanup must be called after a series of calls to dirtyAddTag.
     * @see {@link #cleanup}.
     * @param facetID the integer representation of a Facet (can be resolved
     *                by getFacetID).
     * @param tagName the String representation of a Tag under the given Facet.
     */
    public void dirtyAddTag(int facetID, String tagName);

    /**
     * Perform a cleanup of the tagHandler, ensuring natural order and dublet
     * reduction. This must be called after calls to {@link #dirtyAddTag}.
     */
    public void cleanup();

    /**
     * Add a tag to the given facet and return the index of the tag.<br/>
     * Note that this might shift the id of the existing tags if the new tag is
     * positioned before the last existing tag. Shifting always occur downwards
     * one position for all tags following the newly inserted tag.
     * @param facetID the integer representation of a Facet (can be resolved
     *                by getFacetID).
     * @param tagName the String representation of a Tag under the given Facet.
     * @return the id for the newly added tag. If the tag already exists, the
     *         id for the existing tag is returned.
     */
    public int addTag(int facetID, String tagName);

    /**
     * Remove a tag from the given facet.<br/>
     * Note that this might shift the id of the existing tags, if the removed
     * tag is positioned before the last existing tag. Shiftig always occur
     * upwards for all tags following the removed tag.
     * @param facetID the integer representation of a Facet (can be resolved
     *                by getFacetID).
     * @param tagID the ID for the tag to remove.
     */
    public void removeTag(int facetID, int tagID);

    /**
     * Clear all tags from the facet. Normally used before a complete rebuild.
     */
    public void clearTags();

}
