/* $Id: BrowserImplTest.java,v 1.11 2007/10/04 13:28:22 te Exp $
 * $Revision: 1.11 $
 * $Date: 2007/10/04 13:28:22 $
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
package dk.statsbiblioteket.summa.facetbrowser.browse;

import java.io.StringWriter;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.facetbrowser.IndexBuilder;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.lucene.index.IndexConnector;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class BrowserImplTest extends TestCase {
    public BrowserImplTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }


    private String arrayToString(String[] strings) {
        StringWriter sw = new StringWriter(strings.length * 10);
        for (int i = 0 ; i < strings.length ; i++) {
            sw.append(strings[i]);
            if (i < strings.length - 1) {
                sw.append(", ");
            }
        }
        return sw.toString();
    }

    // TODO: Rewrite this
    private Configuration getConfiguration() {
        String[] facets = new String[]{ IndexBuilder.AUTHOR,
                                        IndexBuilder.AUTHOR_NORMALISED,
                                        IndexBuilder.TITLE,
                                        IndexBuilder.GENRE,
                                        IndexBuilder.VARIABLE };
        MemoryStorage memStore = new MemoryStorage();
//        memStore.put(StructureDescription.FACETS, arrayToString(facets));
        memStore.put(IndexConnector.INDEXROOT + IndexConnector.TYPE,
                     IndexConnector.INDEXTYPE.singleIndex);
        memStore.put(IndexConnector.INDEXROOT + IndexConnector.LINKS,
                     IndexBuilder.INDEX_LOCATION);
/*        memStore.put(TagHandlerFactory.FORCE_REBUILD, true);
        memStore.put(TagHandlerFactory.TAG_HANDLER,
                     TagHandlerFactory.TAGHANDLERS.MemoryTagHandler);
        memStore.put(TagHandlerFactory.TAG_DATA_LOCATION,
                     new File(IndexBuilder.INDEX_LOCATION, "facets").toString());*/
        return new Configuration(memStore);
    }

    /**
     * Indirect test of the browser, where all the parts that makes up a
     * Browser are handled explicitly.
     * @throws Exception if an assertion failed.
     */
    public void testParts() throws Exception {
        // TODO: Change this to use IndexConnector
/*        IndexBuilder.checkIndex();
//        IndexReader reader = IndexBuilder.getReader();
        Configuration config = getConfiguration();
        StructureDescription structure = new StructureDescription(config);
        IndexConnector connector = new IndexConnector(config);

        TagHandler tagHandler =
                TagHandlerFactory.getTagHandler(config, connector);
        assertTrue("The tag handler should not be empty",
                   tagHandler.getTagCount() > 0);
        
        CoreMap coreMap =
                CoreMapFactory.getCoreMap(connector.getReader().maxDoc(),
                                          structure.getFacetNames().size(),
                                          tagHandler.getMaxTagCount());
        // Consider adding stopwords to the configuration
        CoreMapFactory.fillMap(coreMap, structure, tagHandler,
                               connector.getReader(),
                               config, false);
        assertEquals("The core map should map all documents",
                     IndexBuilder.REPLICATIONCOUNT * IndexBuilder.comics.length,
                     coreMap.getDocCount());

        TagCounter tagCounter = new TagCounterArray(structure, tagHandler);

        int[] docIDs = new int[] {0, 1, 2, 3, 4};
        coreMap.markCounterLists(tagCounter, docIDs, 0, docIDs.length-1);
        FacetResult facetStructure =
                tagCounter.getFirst(FacetResult.TagSortOrder.popularity);
        String browseXML = facetStructure.toXML();
        assertTrue("The result should be something",
                   browseXML.length() > "<facetmodel>\n</facetmodel>".length());*/
    }

    public void dumpPerformance() throws Exception {
        // TODO: Change this to use IndexConnector
/*        System.out.println("Getting reader...");
        IndexReader reader = IndexBuilder.getReader();
        System.out.println("Setting up system...");
        Configuration config = getConfiguration();
        StructureDescription structure = new StructureDescription(config);
        IndexConnector connector = new IndexConnector(config);

        TagHandler tagHandler =
                TagHandlerFactory.getTagHandler(config, connector);
        CoreMap coreMap =
                CoreMapFactory.getCoreMap(reader.maxDoc(),
                                          structure.getFacetNames().size(),
                                          tagHandler.getMaxTagCount());
        // Consider adding stopwords to the configuration
        CoreMapFactory.fillMap(coreMap, structure, tagHandler, reader,
                               config, false);
        TagCounter tagCounter = new TagCounterArray(structure, tagHandler);

        Random random = new Random();
        int[] docCounts = new int[] {1, 100, 1000, 10000, 100000, 1000000,
                                     10000000};
        int warmup = 2;
        int runs = 3;
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(runs);
        int maxDoc = reader.maxDoc();
        System.out.println("Running tests...");
        for (int docCount: docCounts) {
//            System.out.println("Testing with " + docCount + " docIDs...");
            int[] docIDs = new int[docCount];
            for (int pos = 0 ; pos < docCount ; pos++) {
                docIDs[pos] = random.nextInt(maxDoc);
            }
            for (int i = 0 ; i < warmup ; i++) {
                coreMap.markCounterLists(tagCounter, docIDs, 0, docCount-1);
            }
            System.gc();
            profiler.reset();
            for (int i = 0 ; i < runs ; i++) {
                coreMap.markCounterLists(tagCounter, docIDs, 0, docCount-1);
           tagCounter.getFirst(FacetResult.TagSortOrder.popularity).toXML();
                profiler.beat();
            }
            System.out.println("Average time for marking and extracting tags " +
                               "for " + docCount + " documents: " 
                               + 1000 / profiler.getBps(true) + " ms");

        }*/
    }

    public void testGetReader() throws Exception {
        assertNotNull("The reader must not be null", IndexBuilder.getReader());
    }

    public void dumpPerformanceDirect() throws Exception {
        // TODO: Change this to use IndexConnector
/*        System.out.println("Getting reader...");
        IndexReader reader = IndexBuilder.getReader();
        System.out.println("Setting up system...");
        Configuration config = getConfiguration();
        config.set(FacetSearchNode.BROWSERTHREADS_PROPERTY, 2);
        config.set(FacetSearchNode.BROWSERTHREADS_TIMEOUT_PROPERTY, 5000);
        StructureDescription structure = new StructureDescription(config);
        IndexConnector connector = new IndexConnector(config);

        TagHandler tagHandler =
                TagHandlerFactory.getTagHandler(config, connector);
        CoreMap coreMap =
                CoreMapFactory.getCoreMap(reader.maxDoc(),
                                          structure.getFacetNames().size(),
                                          tagHandler.getMaxTagCount());
        // Consider adding stopwords to the configuration
        CoreMapFactory.fillMap(coreMap, structure, tagHandler, reader,
                               config, false);
//        TagCounter tagCounter = new TagCounterArray(structure, tagHandler);
        SearchDescriptor descriptor =
                new SearchDescriptor(IndexBuilder.INDEX_LOCATION);
        descriptor.loadDescription(IndexBuilder.INDEX_LOCATION);

        SummaQueryParser queryParser =
                new SummaQueryParser(new String[]{"foo", "bar"},
                                     new SimpleAnalyzer(), descriptor);

        FacetSearchNode browser = new FacetSearchNode(config, null, queryParser,
                                              tagHandler,
                                              structure, coreMap);

        Random random = new Random();
        int[] docCounts = new int[] {1, 100, 1000, 10000, 100000, 1000000,
                                     10000000};
        int warmup = 2;
        int runs = 3;
        FacetResult.TagSortOrder[] orders =
                new FacetResult.TagSortOrder[]{
                        FacetResult.TagSortOrder.popularity,
                        FacetResult.TagSortOrder.tag};
        Profiler profiler = new Profiler();
        profiler.setExpectedTotal(runs);
        int maxDoc = reader.maxDoc();

        System.out.println("Running tests...");
        SlimCollector slimCollector = new SlimCollector();
        for (int docCount: docCounts) {
            for (FacetResult.TagSortOrder order: orders) {
                slimCollector.clean();
                for (int pos = 0 ; pos < docCount ; pos++) {
                    slimCollector.collect(random.nextInt(maxDoc), 0.0f);
                }
                for (int i = 0 ; i < warmup ; i++) {
                    browser.getFacetMap("Flam", order, slimCollector);
                }
                System.gc();
                profiler.reset();
                for (int i = 0 ; i < runs ; i++) {
                    browser.getFacetMap("Flam", order, slimCollector);
                    profiler.beat();
                }
                System.out.println("Average time for marking and extracting "
                                   + "tags for " + docCount + " documents: "
                                   + 1000 / profiler.getBps(true) + " ms - "
                                   + order);
            }
        }*/
    }

    public static Test suite() {
        return new TestSuite(BrowserImplTest.class);
    }
}



