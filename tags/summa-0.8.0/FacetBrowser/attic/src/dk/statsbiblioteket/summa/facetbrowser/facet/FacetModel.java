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

import java.util.LinkedHashSet;
import java.util.HashMap;

import dk.statsbiblioteket.summa.facetbrowser.util.pool.SortedPool;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * @deprecated in favor of {@link SortedPool}.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public interface FacetModel<T> {
    /**
     * Insert an object into the Tag under the Facet. Inserting null as the
     * object is permitted, but null is not added to the object-list.
     * @param facetName the name of the Facet
     * @param tagName   the name of the Tag
     * @param object    the object to store, nulls are ignored
     */
    public void put(String facetName, String tagName, T object);

    /**
     * Insert an object into the Tag under the Facet. Inserting null as the
     * object is permitted, but null is not added to the object-list.
     * @param facetName the name of the Facet
     * @param tagName   the name of the Tag
     * @param object    the object to store, nulls are ignored
     * @param score     the score for the object. Using the score for anything
     *                  is voluntary for FacetModel implementations
     */
    public void put(String facetName, String tagName, T object, Float score);

    /**
     * Assigns the custom String to the given Facet, if the Facet exists.
     * @return true if the Facet exists, false otherwise
     */
    public boolean setCustomFacetString(String facetName, String custom);

    /**
     * Assigns the custom String to the given Tag in the Facet, if the Tag exists.
     * @return true if the Tacet exists, false otherwise
     */
    public boolean setCustomTagString(String facetName, String tagName,
                                      String custom);

    /**
     * Set the properties for this FacetModel.
     * @param maxFacets  the maximum number of Facets, -1 = unlimited
     * @param maxTags    the maximum number of Tags, -1 = unlimited
     * @param maxObjects the maximum number of Objects, -1 = unlimited
     */
    public void setProperties(int maxFacets, int maxTags, int maxObjects);

    /**
     * Set the properties for this FacetModel.
     * @param maxFacets  the maximum number of Facets, -1 = unlimited
     * @param maxTags    the maximum number of Tags, -1 = unlimited
     * @param maxObjects the maximum number of Objects, -1 = unlimited
     * @param facetSortOrders  the sort order for the facets
     * @param facetCustomOrder the strings for TagSortOrder.CUSTOM. If CUSTOM is
     *                         not specified as sortOrder, this argument can
     *                         be null
     * @param tagSortOrders    the sort order for the tags
     * @param tagCustomOrder   the strings for TagSortOrder.CUSTOM. If CUSTOM is
     *                         not specified as sortOrder, this argument can
     *                         be null
     */
    public void setProperties(int maxFacets, int maxTags, int maxObjects,
                              LinkedHashSet<Element.SortOrder> facetSortOrders,
                              HashMap<String, Integer> facetCustomOrder,
                              LinkedHashSet<Element.SortOrder> tagSortOrders,
                              HashMap<String, Integer> tagCustomOrder);

    /**
     * Produce a XML representation of all the Facets, Tags and Objects in
     * this FacetModel.
     * @return a XML representation
     */
    public String toXML();

    /**
     * Indicates that there was not enough time or resources to add
     * all elements to this FacetModel.
     * @param partlyFilled true if this model is only partly filled
     */
    public void setPartlyFilled(boolean partlyFilled);
}
