/* $Id: CoreMap.java,v 1.4 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/04 13:28:21 $
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
package dk.statsbiblioteket.summa.facetbrowser.core.map;

import java.io.IOException;
import java.io.File;

import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounter;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

/*
* The State and University Library of Denmark
* CVS:  $Id: CoreMap.java,v 1.4 2007/10/04 13:28:21 te Exp $
*/

/**
 * Provides a mapping between document-id's and facet/tags. The core map is
 * highly optimised for speed and low memory usage, some places at the cost
 * of proper OO design.
 * </p><p>
 * The map can be seen as a persistent three-dimensional array:
 * [DocID][FacetID][TagID].
 */
public interface CoreMap {
    /**
     * Set the tagIDs corresponding to a given facet for a given document.
     * Note that this must be done sequentially, which means that docID must
     * always be equal to or one greater that the previous docID (the first
     * docID must be 0). The facetID must always be greater than the previous
     * facetID, unless the docID is one greater than the previous docID, in
     * which case the only constraint is that the facetID must be valid.
     * It is expected that this method is used for building the core map.
     * @param docID   the document to update. This needs to be equal to or one
     *                more than the previous docID. The first docID must be 0.
     * @param facetID the facet to update. This needs to be greater than the
     *                previous facetID, unless the docID is one greater than
     *                the previous docID.
     * @param tagIDs  the ids for the tags in the facet for the document.
     * @see CoreMapFactory#fillMap(CoreMap,StructureDescription, TagHandler,
            dk.statsbiblioteket.summa.common.lucene.index.IndexConnector,
            Configuration, boolean).
     */
    public void add(int docID, int facetID, int[] tagIDs);

    /**
     * Get an array with the tagIDs for the given facet for the gived document.
     * A TagHandler is needed to transform the tagIDs to Strings, this can be
     * done with {@link TagHandler#getTagName(int, int)} .
     * @param docID   an id for a document.
     * @param facetID an id for a facet. The id can be retrieved from a
     *                tagHandler using the method
                      {@link TagHandler#getFacetID(String)}
     * @return the tagIDs corresponding to the given facet for the document.
     */
    public int[] get(int docID, int facetID);

    /**
     * Removes the given docID, with corresponding facet- and tagIDs from the
     * map. Any following docIDs are shifted down by one.
     * </p><p>
     * Note: This corresponds to the way Lucene handles removal of documents.
     * @param docID the docID top remove.
     */
    public void remove(int docID);

    /**
     * Store the core map on disk.
     * @param location the location of the core map.
     * @throws IOException if the map could not be stored.
     */
    public void store(File location) throws IOException;

    /**
     * Load the core map from disk.
     * @param location the location of the core map.
     * @throws IOException if the map could not be loaded.
     */
    public void load(File location) throws IOException;

    /**
     * Get the number of documents that the core map maps.
     * @return the number of mapped documents.
     */
    public int getDocCount();

    /**
     * Clear the map. This leaves the map in an unusable state and should
     * normally be followed by a rebuild of the map.
     */
    public void clear();

    /**
     * Update the tagCounter to reflect the tags that the docIDs resolves to.
     * @param tagCounter counts the occurences of the tags in the given
     *                   documents.
     * @param docIDs     ids for the documents from which the tags should be
     *                   counted.
     * @param startPos   only use the docIDs from this position (inclusive).
     * @param endPos     only use the docIDs up to this position (inclusive).
     */
    public void markCounterLists(TagCounter tagCounter, int[] docIDs,
                                 int startPos, int endPos);

    /**
     * Runs through all pointers to tags in facetID with a value >= position
     * and increments them by delta. This method is normally used when a
     * tag is added or removed from an associated tag handler.
     * @param facetID  the ID for the facet with affected tags.
     * @param position the lowest position for affected tags.
     * @param delta    the amount that the position should be adjusted with.
     * @see TagHandler#addTag(int, String)
     * @see TagHandler#removeTag(int, int)
     */
    public void adjustPositions(int facetID, int position, int delta);
}
