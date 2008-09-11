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
package dk.statsbiblioteket.summa.facetbrowser.build.facet;

import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Collections;

import dk.statsbiblioteket.summa.facetbrowser.util.pool.SortedPool;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Representaion of the facet in faceted modelling. A Facet contains a list
 * of one or more tags. For all practical purposes, this list will contain
 * at least one Tag.
 * @deprecated in favor of {@link SortedPool}.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public class Facet <T> extends Element{
    private static final int TAGHASHMAPINITIALSIZE = 100;


    /**
     * The default value when creating new Tags.
     */
     private int maxObjectsPerTag = -1;

    /**
     * A collection of Tags belonging to this Facet.
     */
    private LinkedHashMap<String, Tag<T>> tags =
            new LinkedHashMap<String, Tag<T>>(TAGHASHMAPINITIALSIZE);

    /**
     * Construct a Facet with the given name.
     * @param name the name of the Facet.
     */
    public Facet(String name) {
        super(name);
    }

    /**
     * Construct a Facet with the given name and add the maximum number of
     * retained tags to maxTags.
     * @param name the name of the Facet.
     * @param maxTags the maximum number of tags to store
     */
    public Facet(String name, int maxTags) {
        super(name, maxTags);
    }

    public Facet(String name, int maxTags, int maxObjects) {
        super(name, maxTags);
        maxObjectsPerTag = maxObjects;
    }

    protected void childrenSortOrderChanged() {
        for (Tag<T> tag: tags.values()) {
            tag.setSorting(childrenSortOrders, childrenCustomOrder);
        }
    }

    /**
     * Creates a new Tag with the given name and adds it to this Facet.
     * If the tag already exists, the existing tag is returned.
     * @param tagName the name of the Tag that should be created and added
     * @return the newly created tag or null, if maxTags was reached
     */
    public Tag<T> put(String tagName) {
        return put(tagName, NOSCORE);
    }

    /**
     * Creates a new Tag with the given name and adds it to this Facet.
     * If the tag already exists, the existing tag is returned.
     * @param tagName the name of the Tag that should be created and added
     * @param score   the score to assign to the tag, if one is created
     * @return the newly created tag or null, if maxTags was reached
     */
    public Tag<T> put(String tagName, float score) {
        Tag<T> tag = tags.get(tagName);
        if (tag != null) {
            return tag;
        }
        size++;
        if (maxSubElements == -1 || tags.size() < maxSubElements) {
            tag = new Tag<T>(this, tagName, maxObjectsPerTag);
            tag.setScore(score);
            tag.setSorting(childrenSortOrders, childrenCustomOrder);
            tags.put(tagName, tag);
            return tag;
        }
        return null;
    }

    /**
     * Gets a tag from this Facet.
      * @param tagName the name used to identify the Tag
     * @return a tag that matches the tagName, null if no match is found
     */
    public Tag<T> get(String tagName) {
        return tags.get(tagName);
    }

    /**
     * Produces a reduced Facet, within the specified boundaries.
     * Using -1 for any boundary means no limitation.
     * @param maxTags the maximum number of Tags to retain
     * @param maxObjectsPerTag the maximum number of objects per tag to retain
     */
    protected void sortAndReduce(int maxTags, int maxObjectsPerTag) {
        maxSubElements = maxTags;

        if (maxTags == 0) { // Optimization for special case
            tags.clear();
            return;
        }

        LinkedList<Tag<T>> sortedTags =
                new LinkedList<Tag<T>>(tags.values());
        Collections.sort(sortedTags);
//        System.out.println(tags.values().iterator().next().sortOrders);

        // Reduce
        LinkedHashMap<String, Tag<T>> newMap =
                new LinkedHashMap<String, Tag<T>>(TAGHASHMAPINITIALSIZE);
        int tagCount = 0;
        for (Tag<T> tag: sortedTags) {
            newMap.put(tag.getName(), tag.sortAndReduce(maxObjectsPerTag));
            tagCount++;
            if (tagCount == maxTags) {
                break;
            }
        }
        tags = newMap;
    }


    /* Accessors */
    public Collection<Tag<T>> getTags() {
        return tags.values();
    }

}


