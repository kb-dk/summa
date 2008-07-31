/* $Id: StructureDescription.java,v 1.11 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.11 $
 * $Date: 2007/10/05 10:20:23 $
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
 * CVS:  $Id: StructureDescription.java,v 1.11 2007/10/05 10:20:23 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import dk.statsbiblioteket.summa.facetbrowser.util.ClusterCommon;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

/**
 * This describes how the complete cluster map should be build and displayed.
 * A core property is the facet names.
 * @deprecated use {@link Structure} instead.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StructureDescription {
    private static Logger log = Logger.getLogger(StructureDescription.class);
    /**
     * The names of the Facets, which is also the order in which they should
     * be sorted.
     */
    private List<String> facetNames;
    private Map<String, Integer> facetIDs;

    private int maxTagsDefault =    5;

    private Map<String, Integer> tagsPerFacetMap;
    private int[] tagsPerFacetArray;
    private Map<String, Integer> facetSortOrder;

    /**
     * The names of the facets for the FacetBrowser. These correspond directly
     * to Field-names in the Lucene index. The value is a comma-separated list
     * of Field-names, optionally with the maximum number of tags in
     * parenthesises. If no maximum number of tags is specified for a given
     * facet, the {@link #MAX_TAGS_PER_FACET} is used for that facet.
     * Sample: lma_long, author_normalised (5), sort_title (10)
     */
    public static final String FACETS =
            "facetbrowser.Facets";
    /**
     * The maximum number of tags
     */
    public static final String MAX_TAGS_PER_FACET =
            "facetbrowser.MaxTagsPerFacets";
    public static final String DEFAULT_MAX_CLUSTER_TAGS =
            "default_maxClusterTags";


    /**
     * Constructs a StructureDescription where the values are fetched from
     * properties.
     * @deprecated use {@link StructureDescription(Configuration)} instead.
     */
    public StructureDescription() {
        try {
            maxTagsDefault =
                    ClusterCommon.getPropertyInt(DEFAULT_MAX_CLUSTER_TAGS);
        } catch (Exception e) {
            log.error("Could not fill all default max values from properties",
                      e);
        }

        String[] facets = ClusterCommon.getFacetsFromProperties();
        facetNames = new ArrayList<String>(Arrays.asList(facets));
        facetIDs = new HashMap<String, Integer>(facetNames.size());
        int facetIDCounter = 0;
        for (String facetName: facetNames) {
            facetIDs.put(facetName, facetIDCounter++);
        }

        facetSortOrder = facetIDs;

        tagsPerFacetArray =
                ClusterCommon.getValuesFromProperties(ClusterCommon.FACET_NAMES,
                                                      maxTagsDefault);
        if (facets.length != tagsPerFacetArray.length) {
            log.warn("Could not extract sizes for facets from " +
                     ClusterCommon.FACET_NAMES);
            return;
        }
        tagsPerFacetMap =
                new HashMap<String, Integer>(tagsPerFacetArray.length);
        for (int i = 0 ; i < facets.length ; i++) {
            tagsPerFacetMap.put(facets[i], tagsPerFacetArray[i]);
        }
    }

    public StructureDescription(Configuration configuration) {
        try {
            maxTagsDefault = configuration.getInt(MAX_TAGS_PER_FACET);
        } catch (Exception e) {
            log.warn("Could not fill tags default max value from properties",
                     e);
        }
        List<Configuration.Pair<String, Integer>> facets;
        try {
            facets = configuration.getIntValues(FACETS, maxTagsDefault);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not get facets from " +
                                               "properties with key " + FACETS);
        }
        facetNames = new ArrayList<String>(facets.size());
        for (Configuration.Pair<String, Integer> facet: facets) {
            facetNames.add(facet.getFirst());
        }
        init(facetNames);
        facetIDs = new HashMap<String, Integer>(facets.size());
        tagsPerFacetArray = new int[facets.size()];
        tagsPerFacetMap = new HashMap<String, Integer>(facets.size());
        int facetID = 0;
        for (Configuration.Pair<String, Integer> facet: facets) {
            facetIDs.put(facet.getFirst(), facetID);
            tagsPerFacetArray[facetID] = facet.getSecond();
            tagsPerFacetMap.put(facet.getFirst(), facet.getSecond());
            facetID++;
        }
    }


    /**
     * Shorthand for constructing a proper list of facetNames and calling the
     * constructor {@link #StructureDescription(List<String>)}
     * @param facetNames the wanted facets.
     */
    public StructureDescription(String[] facetNames) {
        this(Arrays.asList(facetNames));
    }
    /**
     * Basic constructor with just the facet names and everything else left
     * to default values.
     * @param facetNames the names of the facets for the browser.
     */
    public StructureDescription(List<String> facetNames) {
        init(facetNames);
    }

    private void init(List<String> facetNames) {
        this.facetNames = new ArrayList<String>(facetNames);
        facetIDs = new HashMap<String, Integer>(facetNames.size());
        int facetIDCounter = 0;
        for (String facetName: facetNames) {
            facetIDs.put(facetName, facetIDCounter++);
        }
        facetSortOrder = facetIDs;

        tagsPerFacetArray = new int[facetNames.size()];
        for (int i = 0 ; i < facetNames.size() ; i++) {
            tagsPerFacetArray[i] = maxTagsDefault;
        }
        tagsPerFacetMap =
                new HashMap<String, Integer>(tagsPerFacetArray.length);
        for (int i = 0 ; i < facetNames.size() ; i++) {
            tagsPerFacetMap.put(facetNames.get(i), tagsPerFacetArray[i]);
        }
    }

    /* Accessors */
    public int getMaxTags(int facetID) {
        if (tagsPerFacetMap == null) {
            return getMaxTags();
        }
        return tagsPerFacetArray[facetID];
    }
    public int getMaxTags(String facetName) {
        if (tagsPerFacetMap == null) {
            return getMaxTags();
        }
        return tagsPerFacetMap.get(facetName);
    }
    public int getMaxTags() {
        return maxTagsDefault;
    }

    /**
     * @return the maximum number of objects in a tag.
     * @deprecated always returns 1.
     */
    public int getMaxObjects() {
        return 1;
    }

    public List<String> getFacetNames() {
        return facetNames;
    }
    public String getFacetName(int facetID) {
        return facetNames.get(facetID);
    }
    public int getFacetID(String facetName) {
        return facetIDs.get(facetName);
    }
    public int getFacetSortOrder(String facetName) {
        return facetSortOrder.get(facetName);
    }
}
