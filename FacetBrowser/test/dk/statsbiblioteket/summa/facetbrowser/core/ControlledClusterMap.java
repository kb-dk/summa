/* $Id: ControlledClusterMap.java,v 1.5 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/04 13:28:17 $
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
 * CVS:  $Id: ControlledClusterMap.java,v 1.5 2007/10/04 13:28:17 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core;

import java.util.List;
import java.util.ArrayList;

import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounter;
import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounterArray;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.MemoryTagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMapBitStuffed;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;


/**
 * This class is responsible for constructing a controlled map with controlled
 * facets and tags. This can be used by JUnit tests.
 */
public class ControlledClusterMap {
    private int documents;

    public ControlledClusterMap(int documents) {
        this.documents = documents;
    }

    private String[][][] facets = new String[][][]{
            {{"FacetUnique"}, {}},
            {{"FacetA"}, {"TagA1", "TagA3", "TagA2"}},
            {{"FacetC"}, {"TagC1", "TagC2", "TagC3"}},
            {{"FacetB"}, {}},
            {{"FacetD"}, {"TagD\twith tab and space"}}};

    public List<String> getFacetNames() {
        facets[0][0] = new String[documents];
        for (int i = 0 ; i < documents ; i++) {
            facets[0][0][i] = "Title" + i;
        }

        List<String> facetNames = new ArrayList<String>(facets.length);
        for (String[][] facetName: facets) {
            facetNames.add(facetName[0][0]);
        }
        return facetNames;
    }

    public TagHandler constructTagHandler() {
        String[] facetNames = new String[facets.length];
        String[][] tagNames = new String[facets.length][];
        for (int i = 0 ; i < facets.length ; i++) {
            tagNames[i] = facets[i][1];
        }
        return new MemoryTagHandler(new StructureDescription(facetNames),
                                    tagNames);
    }

    public TagCounter constructTagCounter() {
        List<String> facetNames = getFacetNames();
        TagHandler tagHandler = constructTagHandler();

        int[][] tags = new int[facetNames.size()][];
        for (int counter = 0 ; counter < facetNames.size() ; counter++) {
            tags[counter] = new int[tagHandler.getFacetSize(counter)];
        }
        return new TagCounterArray(facetNames, tags);
    }

    public CoreMap constructCoreMap(int documents) {
        List<String> facetNames = getFacetNames();
        CoreMap coreMap = new CoreMapBitStuffed(documents, facetNames.size());
        for (int document = 0 ; document < documents ; document++) {
            for (int facet = 0 ; facet < facetNames.size() ; facet++) {
                if (facet == 0) {
                    int[] content = new int[1];
                    content[0] = document;
                    coreMap.add(document, facet, content);
                } else {
                    int chooser = document % 5;
                    if (chooser != 0) {
                        int[] content = new int[facets[chooser][1].length];
                        for (int i = 0 ; i < facets[chooser][1].length ; i++) {
                            content[i] =  i; // All
                            // Wrong
                        }
                    }
                }
            }
        }
//            coreMap.add();
        return null;
}
}
