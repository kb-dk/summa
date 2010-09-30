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
 * CVS:  $Id: ControlledClusterMap.java,v 1.5 2007/10/04 13:28:17 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core;

import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounter;
import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounterArray;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMapBitStuffed;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;

import java.util.ArrayList;
import java.util.List;


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
        // TODO: Implement this
        return null;//new TagHandlerImpl(new StructureDescription(facetNames),
                     //               tagNames);
    }

    public TagCounter constructTagCounter() {
        List<String> facetNames = getFacetNames();
        TagHandler tagHandler = constructTagHandler();

        int[][] tags = new int[facetNames.size()][];
        for (int counter = 0 ; counter < facetNames.size() ; counter++) {
            tags[counter] = new int[tagHandler.getFacetSize(counter)];
        }
        return new TagCounterArray(tagHandler,
                                   tagHandler.getFacetNames().size());
    }

    public CoreMap constructCoreMap(int documents) {
        throw new UnsupportedOperationException("Not implemented yet");
        /*
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
        return null; */
}
}




