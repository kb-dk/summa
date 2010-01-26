/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
/*
 * The State and University Library of Denmark
 * CVS:  $Id: TagHandler.java,v 1.7 2007/10/04 13:28:18 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core.tags;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.List;
import java.util.Locale;
import java.io.File;
import java.io.IOException;

/**
 * A TagHandler conceptually maintains a collection of Facets, each containing
 * a collection of Tags. Normally the Tags will be words or small text snippets.
 * The TagHandler is geared towards efficient handling of large amounts of Tags
 * (10's of millions, with an upper limit above 100 million) for each Facet.
 * </p><p>
 * The TagHandler is responsible for mapping between integer and String
 * representations of tags. It enforces Strict ordering of the Tags.
 * </p><p>
 * The tags are guaranteed to be in consistent order, so order of the id of two
 * tags is equal to comparing their String representation. This is especially
 * relevant for the methods {@link #insertTag} and {@link #removeTag}, as they
 * might shift the underlying structure.
 * </p><p>
 * TagHandlers must have a constructor that takes (Configuration conf,
 * Structure structure,boolean readOnly) as arguments.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public interface TagHandler {
    /**
     * If true, the TagHandler should hold the entire structure in memory.
     * </p><p>
     * Optional. Default is false.
     * @see {@link Facet}.
     */
    public static final String CONF_USE_MEMORY =
            "summa.facet.taghandler.usememory";
    public static final boolean DEFAULT_USE_MEMORY = false;

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
     * If the tagName is present, this behaves identically to
     * {@link #getTagID(int, String)}. If not, (-(insertion point) - 1) is
     * returned, similar to Collections.binarySearch.
     * @param facetID the integer representation of a Facet (can be resolved
     *                by getFacetID).
     * @param tagName the String representation of a Tag under the given Facet.
     * @return        the integer representation for the given Tag or
     *                (-(insertion point) - 1) if the tag does not exist.
     */
    public int getNearestTagID(int facetID, String tagName);

    /**
     * If the Tags in the given Facet are sorted according to a Locale, this
     * will be returned.
     * @param facetID the integer representation of a Facet (can be resolved
     *                by getFacetID).
     * @return the Locale, if defined, for the given Facet.
     */
    public Locale getFacetLocale(int facetID);

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
     * Open the structure from disk. This needs to be called before use.
     * @param folder the folder for persistent content.
     * @throws IOException in case of major disk-related problems during store.
     * @return a list of Facets that had no existing structure on disk or had a
     *         structure that was inconsistent with the base setup.
     *         Users ofopen should perform a remove and an update of the Facets
     *         returned and of any structures using the handler (such af
     *         CoreMap), if the list is not empty.
     */
    public List<Facet> open(File folder) throws IOException;

    /**
     * Store the structure on disk at the location specified in {@link #open}.
     * @throws IOException in case of disk-related problems during store.
     */
    public void store() throws IOException;

    /**
     * Close down any open files and similar resources. The state of the
     * TagHandler is invalid after close until {@link #open} has been called.
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
     * Add a tag to the given facet. This method does not guarantee external
     * consistency for tagIDs. It should be used for quickly adding a lot
     * of tags for later use.<br />
     * A cleanup must be called after a series of calls to dirtyAddTag.
     * @see {@link #cleanup}.
     * @param facetName the name of the Facet for the Tag.
     * @param tagName the String representation of a Tag under the given Facet.
     */
    public void dirtyAddTag(String facetName, String tagName);

    /**
     * Perform a cleanup of the tagHandler, ensuring natural order and dublet
     * reduction. This must be called after calls to {@link #dirtyAddTag}.
     */
    public void cleanup();

    /**
     * Insert a tag to the given facet and return the index of the tag.<br/>
     * Note that this might shift the id of the existing tags if the new tag is
     * positioned before the last existing tag. Shifting always occur downwards
     * one position for all tags following the newly inserted tag.
     * @param facetID the integer representation of a Facet (can be resolved
     *                by getFacetID).
     * @param tagName the String representation of a Tag under the given Facet.
     * @return the id for the newly added tag. If the tag already exists,
     *         (-id-1) for the existing tag is returned.
     */
    public int insertTag(int facetID, String tagName);

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

