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

import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounter;
import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounterArray;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;
import java.io.IOException;

/**
 * Provides a mapping between document-id's and facet/tags. The core map is
 * highly optimised for speed and low memory usage, some places at the cost
 * of proper OO design.
 * </p><p>
 * The map can be seen as a persistent three-dimensional array:
 * [DocID][FacetID][TagID].
 * </p><p>
 * Theoretical limitations are 2 billion docIDs, 2 billion facets,
 * 2 billion tags and (this is the bad one) 2 billion pointers from docIDs
 * to tagIDs. Implementations might introduce further constraints.
 * </p><p>
 * CoreMaps must have a constructor that takes (Configuration conf,
 * Structure structure) as arguments.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface CoreMap {
    /**
     * The index-file is part of map persistence. The index is an array of ints
     * specifying the start-position in the values-file. The array is followed
     * by an int pointing to the next free position in values. A value of
     * Integer.MAX_VALUE signifies that the entry is to be skipped.
     */
    public static final String INDEX_FILE =  "coremap.index";
    /**
     * The values-file is part of map persistence. The values is an array of
     * longs, specifying the compound pointer
     * [FacetID (32 bits)][TagID (32 bits)].
     * @see {@link #PERSISTENT_EMPTY_FACET}.
     */
    public static final String VALUES_FILE = "coremap.dat";

    public static final int PERSISTENT_FACET_BITS = 32;
    public static final int PERSISTENT_FACET_SHIFT = 64 - PERSISTENT_FACET_BITS;
    public static final long PERSISTENT_TAG_MASK =
         0xFFFFFFFFFFFFFFFFL << PERSISTENT_FACET_BITS >>> PERSISTENT_FACET_BITS;
    /**
     * The empty facet signifies that the facet/tag is to be ignored.
     */
    public static final long PERSISTENT_EMPTY_FACET = PERSISTENT_FACET_SHIFT;

    /**
     * Meta-data for the core map in
     * {@link dk.statsbiblioteket.util.XProperties}-format. The meta-data are
     * given below as META_-prefixed constants.
     */
    public static final String META_FILE =  "coremap.meta";

    /**
     * The version of the persistent data. The version refers to the format,
     * not to the concrete document-facet-tag mappings. Opening persistent
     * files with a version higher than expected will throw an Exception.
     */
    public static final String META_VERSION = "version";

    /**
     * The number of mapped documents.
     * </p><p>
     * The size of {@link #INDEX_FILE} should be 4 * documents + 4.
     */
    public static final String META_DOCUMENTS = "documents";

    /**
     * Comma + space separated list of the Facet names mapped. Example:
     * "title, author, year". When opening a previously stored core map,
     * the implementation must ensure that the wanted facet-id's are paired
     * with the stored ones.
     * </p><p>
     * This is used for sanity-checking and potentially for choosing
     * coremap-implementation based on Facet-count. 
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String META_FACETS = "facets";

    /**
     * Whether or not a {@link #remove(int)} results in the subsequent documents
     * being shifted down or not.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_SHIFT_ON_REMOVE =
            "summa.facet.core.shift-on-remove";
    public static final boolean DEFAULT_SHIFT_ON_REMOVE = false;

    /**
     * Load the core map from disk.
     * @param location the location of the core map.
     * @param forceNew ignore existing persistent data and create a new map.
     * @return if no persistent data was retrieved.
     * @throws IOException if the map could not be loaded and a new map could
     *                     not be created.
     */
    public boolean open(File location, boolean forceNew) throws IOException;

    /**
     * Store the core map on disk at the location specified in {@link #open}.
     * @throws IOException if the map could not be stored.
     */
    public void store() throws IOException;

    /**
     * Adds the tagIDs corresponding to a given facet for a given document.
     * If there is already an existing mapping of a given facet/pair for the
     * document, that pair is ignored.
     * </p><p>
     * It is expected that this method is used for building the core map.
     * @param docID   the document to update. This needs to be equal to or one
     *                more than the previous docID. The first docID must be 0.
     * @param facetID the facet to update. This needs to be greater than the
     *                previous facetID, unless the docID is one greater than
     *                the previous docID.
     * @param tagIDs  the ids for the tags in the facet for the document.
     */
    public void add(int docID, int facetID, int[] tagIDs);

    /**
     * Get an array with the tagIDs for the given facet for the given document.
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
     * map. Depending on configuration, this might result in the following
     * docIDs are shifted down by one. If shifting is not enabled, all
     * facet/tag-pairs for the docID is set to FACET_LIMIT/0 aka emptyFacet/0.
     * </p><p>
     * Note: Lucene does not shift on deletes.
     * @param docID the docID top remove.
     */
    public void remove(int docID);

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
    public void markCounterLists(TagCounter tagCounter, DocIDCollector docIDs,
                                 int startPos, int endPos);

    /**
     * Runs through all pointers to tags in facetID with a value >= position
     * and increments them by delta. This method is normally used when a
     * tag is added or removed from an associated tag handler.
     * @param facetID  the ID for the facet with affected tags.
     * @param position the lowest position for affected tags.
     * @param delta    the amount that the position should be adjusted with.
     * @see TagHandler#insertTag(int, String)
     * @see TagHandler#removeTag(int, int)
     */
    public void adjustPositions(int facetID, int position, int delta);

    /**
     * The emptyFacet is the position of the Facet responsible for counting tags
     * from deleted documents. When documents are deleted, their facets are all
     * set to this id and their tags are set to 0.
     * </p><p>
     * Note: This id must be at least the number of facets specified in 
     *       {@link Structure}.
     * </p><p>
     * Note: It is optional if the implementation uses emptyFacet for deletes.
     * @return the emptyFacet, used for tags from deleted documents.
     * @see {@link TagCounterArray#emptyFacet}.
     */
    public int getEmptyFacet();
}
