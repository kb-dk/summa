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
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Collections;
import java.text.ParseException;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import dk.statsbiblioteket.summa.facetbrowser.util.pool.SortedPool;
import dk.statsbiblioteket.util.qa.QAInfo;
//import dk.statsbiblioteket.commons.XmlOperations;

/**
 * The outer container for faceted modelling, containing Facets and their
 * underlying tags. The FacetModelImplOO containg convenience methods for adding
 * new Facets and new Tags.
 *
 * The type for the FacetModelImplOO refers to the objects that are to be stored in
 * the Tags.
 * @deprecated in favor of {@link SortedPool}.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public class FacetModelImplOO <T> extends Element implements FacetModel<T> {
    private static final String FACET_MODEL = "FacetModel";
    private static final int FACETHASHMAPINITIALSIZE = 10;
    private Logger log = Logger.getLogger(FacetModelImplOO.class);
    private static final int XMLWRITERINITIALSIZE = 1000;

    private boolean partlyFilled = false;

    public enum ExtractionOrder {TAGINSERT, FACETINSERT}

    /**
     * The maximum number of Facets.
     * Setting this to -1 (the default) means that there is no limit.
     */
    // TODO: Make this do something
    private int maxFacets = -1;
    /**
     * The maximum number of Tags that newly created Facets should keep.
     * Setting this to -1 (the default) means that there is no limit.
     */
    private int maxTagsPerFacet = -1;
    /**
     * The maximum number of Objects that the newly created Tags should keep.
     * Setting this to -1 (default) means that there is no limit.
     */
    private int maxObjectsPerTag = -1;

    protected LinkedHashSet<SortOrder> tagSortOrders;
    protected HashMap<String, Integer> tagCustomOrder;

    /**
     * Something has been added to this FacetModel, since the last sortAndReduce().
     */
    private boolean dirty = false;

    /**
     * The collection of Facets in this facetting model. Each Facet contains
     * one or more Tags.
     */
    private LinkedHashMap<String, Facet<T>> facets =
            new LinkedHashMap<String, Facet<T>>(FACETHASHMAPINITIALSIZE);
    /**
     * The collection of Tags in this facetting model. The same tags can be
     * found indirectly by iterating through the Facets. The tags list is
     * maintained to preserve the order of tag-creation.
     */
    // TODO: Solve problem with duplicate names
//    private LinkedHashMap<String, Tag<T>> tags =
//            new LinkedHashMap<String, Tag<T>>(TAGHASHMAPINITIALSIZE);

    /**
     * The empty constructor means that there are no limit on the number of
     * Tags and objects.
     */
    public FacetModelImplOO() {
        super(FACET_MODEL);
        log.trace("FacetModel with no limits on tags and objects created");
        // Nothing to do here, but keep the empty constructer for the
        // default values
    }
    /**
     * Constructor for FacetModelImplOO with constraints on the tags and object
     * collections.
     * @param maxTags the maximum number of tags for each Facet
     * @param maxObjects the maximum number of objects in each Tag
     */
    public FacetModelImplOO(int maxTags, int maxObjects) {
        super(FACET_MODEL);
        setProperties(-1, maxTags, maxObjects);
        log.trace(String.format("FacetModelImplOO with maxTagsPerFacet %d and " +
                                "maxObjectsPerTag %d created",
                                maxTags, maxObjects));
    }

    /**
     * Splits a combined string of facet name and tag name and adds the
     * combination to this model.
     * @param combined a combination of facet name and tag name of the form
     * "facet_name:tag_name"
     */
    public void put(String combined) throws ParseException {
        put(combined, (T)null);
    }
    /**
     * Creates a new Facet, if it does not exists and creates and adds the
     * Tag, if it does not exist, to that Facet.
     * @param facetName the Facet to use or create
     * @param tagName the tagName to create and put
     */
    public void put(String facetName, String tagName) {
        put(facetName, tagName, null);
    }
    /**
     * Splits a combined string of facet name and tag name and adds the
     * combination to this model. Also adds the given object to the tag.
     * @param combined a combination of facet name and tag name of the form
     * "facet_name:tag_name"
     * @param object the object to add to the facet. nulls are not added, but
     * they do increase the size for the tag.
     * @throws ParseException
     */
    public void put(String combined, T object) throws ParseException {
        String[] tokens = combined.split(":", 2);
        if (tokens.length != 2) {
            throw new ParseException("No tag specified in "+ combined, 0);
        }
        put(tokens[0], tokens[1], object);
    }
    /**
     * Puts an object in the given tag in the given facet, creating the tag
     * and/or the facet, if they do not exist.
     * @param facetName the facet to use or create
     * @param tagName the tag to use or create
     * @param object the object to add to the facet. nulls are not added, but
     * they do increase the size for the tag.
     */
    public void put(String facetName, String tagName, T object) {
        put(facetName, tagName, object, NOSCORE);
    }

    public void put(String facetName, String tagName, T object, Float score) {
        log.trace("Putting " + facetName + ":" + tagName + " in the model");

        Facet<T> facet = facets.get(facetName);
        if (facet == null) {
            facet = new Facet<T>(facetName, maxTagsPerFacet, maxObjectsPerTag);
            facet.setSorting(childrenSortOrders, childrenCustomOrder);
            facet.setChildrenSorting(tagSortOrders, tagCustomOrder);
            facets.put(facet.getName(), facet);
        }
        if (NOSCORE.equals(facet.getScore()) ||
            facet.getScore() < score) {
            facet.setScore(score);
        }
        Tag<T> tag = facet.put(tagName);
        if (tag != null) {
            tag.put(object);
            if (NOSCORE.equals(tag.getScore()) ||
                tag.getScore() < score) {
                tag.setScore(score);
            }
        }
        dirty = true;
    }

    public boolean setCustomFacetString(String facetName, String custom) {
        Facet facet = facets.get(facetName);
        if (facet != null) {
            facet.setCustomString(custom);
            return true;
        }
        return false;
    }

    public boolean setCustomTagString(String facetName, String tagName,
                                      String custom) {
        Facet facet = facets.get(facetName);
        if (facet != null) {
            Tag tag = facet.get(tagName);
            if (tag != null) {
                tag.setCustomString(custom);
                return true;
            }
        }
        return false;
    }

    protected void sortAndReduce() {
        sortAndReduce(maxFacets, maxTagsPerFacet, maxObjectsPerTag);
    }
    /**
     * Produces a reduced FacetModelImplOO, within the specified boundaries.
     * Using -1 for any boundary means no limitation.
     * Note that the order of the Tags returned by getTags(TagSortOrder.INSERT) are
     * not guaranteed after a call to reduceFrom.
     * @param maxFacets the maximum number of Facets to keep
     * @param maxTagsPerFacet the maximum number of Tags to keep for each Facet
     * @param maxObjectsPerTag the maximum number of objects to keep/Tag
     */
    protected void sortAndReduce(int maxFacets, int maxTagsPerFacet,
                                 int maxObjectsPerTag) {
        dirty = false;
        if (maxFacets == 0) { // Optimization for special case
            facets.clear();
            return;
        }

        // Recursive sortAndReduce
        for (Facet<T> facet: facets.values()) {
            facet.sortAndReduce(maxTagsPerFacet, maxObjectsPerTag);
        }

        LinkedList<Facet<T>> sortedFacets =
                new LinkedList<Facet<T>>(facets.values());
        Collections.sort(sortedFacets);

        // Reduce
        LinkedHashMap<String, Facet<T>> newFacets =
                   new LinkedHashMap<String, Facet<T>>(FACETHASHMAPINITIALSIZE);
        int facetCount = 0;
        for (Facet<T> facet: sortedFacets) {
            newFacets.put(facet.getName(), facet);
            facetCount++;
            if (facetCount == maxFacets) {
                break;
            }
        }
        facets = newFacets;
    }

    public int getFacetCount() {
        return facets.size();
    }
    /**
     * Iterates through the Facets and returns the sum of added Tags. Note that
     * this can be a lot higher than the number of stored Tags.
     * @return the number of added Tags
     */
    public int getTagSize() {
        int total = 0;
        for (Facet<T> facet: facets.values()) {
            total += facet.getSize();
        }
        return total;
    }
    public Collection<Facet<T>> getFacets() {
        return facets.values();
    }

    /* Interface implementation */

    public void setProperties(int maxFacets, int maxTags, int maxObjects) {
        setProperties(maxFacets, maxTags, maxObjects,  null, null, null, null);
    }

    public void setPropertiesArrays(int maxFacets, int maxTags, int maxObjects,
                                    SortOrder[] facetSortOrders,
                                    String[] facetCustomOrder,
                                    SortOrder[] tagSortOrders,
                                    String[] tagCustomOrder) {
        LinkedHashSet<SortOrder> facetSortOrdersMap =
                FacetModelFactory.expandSort(facetSortOrders);
        HashMap<String, Integer> facetCustomOrderMap =
                FacetModelFactory.expandCustom(facetCustomOrder);
        LinkedHashSet<SortOrder> tagSortOrdersMap =
                FacetModelFactory.expandSort(tagSortOrders);
        HashMap<String, Integer> tagCustomOrderMap =
                FacetModelFactory.expandCustom(tagCustomOrder);
        setProperties(maxFacets, maxTags,  maxObjects,
                      facetSortOrdersMap, facetCustomOrderMap,
                      tagSortOrdersMap, tagCustomOrderMap);
    }

    public void setProperties(int maxFacets, int maxTags, int maxObjects,
                              LinkedHashSet<SortOrder> facetSortOrders,
                              HashMap<String, Integer> facetCustomOrder,
                              LinkedHashSet<SortOrder> tagSortOrders,
                              HashMap<String, Integer> tagCustomOrder) {
        log.trace("setProperties called");
        this.maxFacets = maxFacets;
        maxTagsPerFacet = maxTags;
        maxObjectsPerTag = maxObjects;
        setChildrenSorting(facetSortOrders, facetCustomOrder);
        setTagSorting(tagSortOrders, tagCustomOrder);
        dirty = true;
    }

    public String toXML() {
        if (dirty) {
            sortAndReduce();
        }
        StringWriter sw = new StringWriter(XMLWRITERINITIALSIZE);

        sw.write("<facetmodel>\n");
        for (Map.Entry<String, Facet<T>> facetSet : facets.entrySet()) {
            Facet<T> facet = facetSet.getValue();
            sw.write("  <facet name=\"");
            sw.write(entityEncode(facetSet.getKey()));
            if (!NOSCORE.equals(facet.getScore())) {
                sw.write("\" score=\"");
                sw.write(Float.toString(facet.getScore()));
            }
            sw.write("\">\n");
            sw.write(facet.getCustomString());
            for (Tag<T> tag: facet.getTags()) {
                sw.write("    <tag name=\"");
                sw.write(entityEncode(tag.getName()));
                if (!NOSCORE.equals(tag.getScore())) {
                    sw.write("\" score=\"");
                    sw.write(Float.toString(tag.getScore()));
                }
                sw.write("\" addedobjects=\"");
                sw.write(Integer.toString(tag.getSize()));
                sw.write(partlyFilled ? "+" : "");
                sw.write("\">\n");
                sw.write(tag.getCustomString());
                for (T object: tag.getObjects()) {
                    sw.write("      <object>");
                    sw.write(object.toString());
                    sw.write("</object>\n");
                }
                sw.write("    </tag>\n");
            }
            sw.write("  </facet>\n");
        }
        sw.write("</facetmodel>\n");
        return sw.toString();
    }

    private String entityEncode(String name) {
        log.warn("Entity-encode has not been implemented yet. "
                 + "Returning input verbatim");
        return name;
        // TODO: Implement this
    }

    public void setPartlyFilled(boolean partlyFilled) {
        this.partlyFilled = partlyFilled;
    }

    public int facetCount() {
        return facets.size();
    }

    public int totalTagCount() {
        int count = 0;
        for (Facet<T> facet: facets.values()) {
            count += facet.getTags().size();
        }
        return count;
    }

    protected void childrenSortOrderChanged() {
        for (Facet<T> facet: facets.values()) {
            facet.setSorting(childrenSortOrders, childrenCustomOrder);
        }
    }

    protected void setTagSorting(LinkedHashSet<SortOrder> tagSortOrders,
                                 HashMap<String, Integer> tagCustomOrder) {
        this.tagSortOrders = tagSortOrders;
        this.tagCustomOrder = tagCustomOrder;
        for (Facet facet: facets.values()) {
            facet.setChildrenSorting(tagSortOrders, tagCustomOrder);
        }
    }

    public Facet<T> getFacet(String facetName) {
        return facets.get(facetName);
    }
}



