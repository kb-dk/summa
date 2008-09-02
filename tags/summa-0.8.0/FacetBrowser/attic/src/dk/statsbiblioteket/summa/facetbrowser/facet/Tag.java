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

import java.util.LinkedList;
import java.util.List;
import java.util.Collections;

import dk.statsbiblioteket.summa.facetbrowser.util.pool.SortedPool;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The State and University Library of Denmark
 * Representaion of the tag (aka cluster) in faceted modelling. A Tag is always
 * connected to one and only one Facet.
 * @deprecated in favor of {@link SortedPool}.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public class Tag<T> extends Element {
    /**
     * The Facet that contains this Tag. */
    private Facet<T> facet;

    private LinkedList<T> objects = new LinkedList<T>();

    /**
     * Create a new Tag with the given name, belonging to the given Facet.
     * @param facet the Facet which contains this Tag
     * @param name the name of this Tag
     */
    public Tag(Facet<T> facet, String name) {
        super(name);
        this.facet = facet;
    }

    /**
     * Create a new Tag with the given name, belonging to the given Facet.
     * Set the maximum number of retained objects to maxObjects.
     * @param facet the Facet which contains this Tag
     * @param name the name of this Tag
     * @param maxObjects the maximum number of objects to store
     */
    public Tag(Facet<T> facet, String name, int maxObjects) {
        super(name, maxObjects);
        this.facet = facet;
    }

    /**
     * Create a new Tag with the given name, belonging to the given Facet.
     * Set the maximum number of retained objects to maxObjects.
     * @param facet the Facet which contains this Tag
     * @param name the name of this Tag
     * @param maxObjects the maximum number of objects to store
     * @param initialSize the initial size for this Tag
     */
    public Tag(Facet<T> facet, String name, int maxObjects, int initialSize) {
        super(name, maxObjects, initialSize);
        this.facet = facet;
    }

    /**
     * Add an object to this Tag. Adding an object increases size by 1.
     * Objects are stored, if the number of stored objects are below maxObjects.
     * @param object the object to add to this Tag
     */
    public void put(T object) {
        if ((maxSubElements == -1 || objects.size() < maxSubElements) &&
            object != null) {
            objects.add(object);
        }
        size++;
    }

    /**
     * "Clones" the other Tag, but retains only maxObjects objects.
     * @param other an other Tag to sortAndReduce from
     * @param maxObjects the maximum number of objects to retain
     */
    protected void reduceFrom(Tag<T> other, int maxObjects) {
        // TODO: Optimize this
        name = other.getName();
        size = other.getSize();
        setCustomString(other.getCustomString());
        setScore(other.getScore());
        maxSubElements = maxObjects;
        objects.clear();
        // TODO: Implement sorting of objects
        switch (maxObjects) {
            case -1: {
                objects.addAll(other.getObjects());
                break;
            }
            case 0: {
                // Do nothing
                break;
            }
            default: {
                List<T> otherObjects = other.getObjects();
                objects.addAll(otherObjects.subList(0,
                                                    Math.min(otherObjects.size(), maxObjects)));
            }
        }
    }

    /**
     * Produces a reduced Tag, within the specified boundaries.
     * Using -1 for any boundary means no limitation. In this case, the current
     * Tag is returned directly.
     * @param maxObjects the maximum number of objects to keep
     * @return a reduced copy of this Tag
     */
    public Tag<T> sortAndReduce(int maxObjects) {
        if (maxObjects == -1) { // Optimization for special case
            return this;
        }
        Tag<T> newTag = new Tag<T>(facet, name, maxObjects, size);
        newTag.reduceFrom(this, maxObjects);
        return newTag;
    }

    /* Accessors */
    public Facet<T> getFacet() {
        return facet;
    }
    public List<T> getObjects() {
        return objects;
    }

    @SuppressWarnings({"unchecked"})
    protected void childrenSortOrderChanged() {
        for (T object: objects) {
            if (object instanceof Element) {
                ((Element)object).setSorting(childrenSortOrders,
                                             childrenCustomOrder);
            }
        }
    }
}
