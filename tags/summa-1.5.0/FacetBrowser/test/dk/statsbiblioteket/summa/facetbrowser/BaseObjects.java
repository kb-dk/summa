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
package dk.statsbiblioteket.summa.facetbrowser;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounter;
import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounterArray;
import dk.statsbiblioteket.summa.facetbrowser.core.FacetMap;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMapBitStuffed;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandlerImpl;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;

import java.io.IOException;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;

/**
 * A helper class for some of the unit-tests for the Facet-framework.
 * Basic objects with consistent states are constructed.
 * </p><p>
 * All returned objects are singletons, except for TagCounters.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BaseObjects {
    public String[] facetNames = new String[]{
            IndexBuilder.ID, IndexBuilder.AUTHOR, IndexBuilder.GENRE,
            IndexBuilder.TITLE, IndexBuilder.FREETEXT,
            IndexBuilder.VARIABLE};

    // Deleted upon close
    private List<File> openedFiles = new ArrayList<File>(10);

    public List<String> getFacetNames() {
        return Arrays.asList(facetNames);
    }

    private Structure structure = null;
    public Structure getStructure() throws IOException {
        if (structure == null) {
            Configuration conf = Configuration.newMemoryBased();
            List<Configuration> facets = conf.createSubConfigurations(
                    Structure.CONF_FACETS, facetNames.length);
            for (int i = 0 ; i < facetNames.length ; i++) {
                Configuration facet = facets.get(i);
                facet.set(FacetStructure.CONF_FACET_NAME, facetNames[i]);
                if (i == 0) {
                    facet.set(FacetStructure.CONF_FACET_LOCALE, "da"); // Author
                }
            }
            structure = new Structure(conf);
        }
        return structure;
    }

    public TagHandler getTagHandler() throws IOException {
        return getMemoryTagHandler();
    }

    private TagHandler memoryTagHandler = null;
    public TagHandler getMemoryTagHandler() throws IOException {
        Random random = new Random();
        if (memoryTagHandler == null) {
            memoryTagHandler = getMemoryTagHandler(random.nextInt());
            memoryTagHandler.clearTags();
        }
        return memoryTagHandler;
    }

    public TagHandler getMemoryTagHandler(int id) throws IOException {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(TagHandler.CONF_USE_MEMORY, true);
        TagHandler memoryTagHandler =
                new TagHandlerImpl(conf, getStructure(), false);
        File location = new File(System.getProperty("java.io.tmpdir"),
                                 "tagHandlerTest_" + id);
        openedFiles.add(location);
        memoryTagHandler.open(location);
        return memoryTagHandler;
    }

    private Random random = new Random();

    private TagHandler diskTagHandler = null;
    public TagHandler getDiskTagHandler() throws IOException {
        if (diskTagHandler == null) {
            Configuration conf = Configuration.newMemoryBased();
            conf.set(TagHandler.CONF_USE_MEMORY, false);
            diskTagHandler = new TagHandlerImpl(conf, getStructure(), false);
            File location = new File(System.getProperty("java.io.tmpdir"),
                                     "tagHandlerTest_" + random.nextInt());
            openedFiles.add(location);
            diskTagHandler.open(location);
        }
        return diskTagHandler;
    }

    private CoreMap coreMap = null;
    public CoreMap getCoreMap() throws IOException {
        if (coreMap == null) {
            coreMap = getCoreMap(random.nextInt(), true);
        }
        return coreMap;
    }
    public CoreMap getCoreMap(int id, boolean forceNew) throws IOException {
        if (coreMap == null) {
            Configuration conf = Configuration.newMemoryBased();
            coreMap = new CoreMapBitStuffed(conf, getStructure());
            File location = new File(System.getProperty("java.io.tmpdir"),
                                     "coreMapTest_" + id);
            openedFiles.add(location);
            coreMap.open(location, forceNew);
        }
        return coreMap;
    }

    private FacetMap facetMap = null;
    public FacetMap getFacetMap() throws IOException {
        if (facetMap == null) {
            facetMap = new FacetMap(getStructure(), getCoreMap(),
                                    getMemoryTagHandler(), false);
        }
        return facetMap;
    }

    public TagCounter getTagCounter() throws IOException {
        return new TagCounterArray(getTagHandler(),
                                   getCoreMap().getEmptyFacet());
    }

    /**
     * Close all open file handles.
     */
    public void close() {
        if (diskTagHandler != null) {
            diskTagHandler.close();
        }
        for (File file: openedFiles) {
            try {
                Files.delete(file);
            } catch (IOException e) {
                System.err.println("Could not delete '" + file
                                   + "': " + e.getMessage());
            }
        }
    }
}




