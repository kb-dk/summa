/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.releasetest;

import java.io.IOException;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.index.IndexReader;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.RecordIterator;
import dk.statsbiblioteket.summa.storage.api.filter.RecordReader;
import dk.statsbiblioteket.summa.facetbrowser.core.FacetMap;
import dk.statsbiblioteket.summa.facetbrowser.core.FacetCore;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMapFactory;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandlerFactory;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResult;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultImpl;
import dk.statsbiblioteket.summa.facetbrowser.browse.Browser;
import dk.statsbiblioteket.summa.facetbrowser.browse.BrowserImpl;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;

/**
 * Index-building unit-test with focus on iterative building, including
 * deletion and updates.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IterativeTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(IterativeTest.class);

    private Storage storage;
    public static final String BASE = "bar";

    public void setUp () throws Exception {
        super.setUp();
        IngestTest.deleteOldStorages();
        if (IndexTest.INDEX_ROOT.exists()) {
            Files.delete(IndexTest.INDEX_ROOT);
        }
        IndexTest.INDEX_ROOT.mkdirs();

        Configuration storageConf = IngestTest.getStorageConfiguration();
        storage = StorageFactory.createStorage(storageConf);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        storage.close();
    }

    /*
    Plain & simple ingestion of 2 Records into a Storage.
     */
    public void testSimpleIngest() throws Exception {
        storage.flush(new Record("foo", BASE, new byte[0]));
        storage.flush(new Record("foo2", BASE, new byte[0]));
        assertEquals("The number of records in storage should be correct",
                     2, countRecords(BASE));
    }


    /*
    Plain read of a Records from Storage and verification of EOF after that.
     */
    public void testSimpleRead() throws Exception {
        RecordReader reader = getRecordReader();
        assertFalse("The reader should have nothing for an empty storage",
                    reader.hasNext());
        reader.close(true);

        storage.flush(new Record("foo", BASE, new byte[0]));
        reader = getRecordReader();
        assertTrue("The reader should have something for a non-empty storage",
                   reader.hasNext());
        reader.next();
        assertFalse("The reader should be exhausted after a single record",
                   reader.hasNext());
        reader.close(true);
    }

    /*
    Tests whether the helper class is capable of creating a Lucene Document.
     */
    public void testDocumentCreation() throws Exception {
        IterativeHelperDocCreator creator = new IterativeHelperDocCreator(null);
        Payload payload = getPayload("foo");
        creator.processPayload(payload);
        Document doc = (Document)payload.getData(Payload.LUCENE_DOCUMENT);
        assertEquals("The document should contain the right ID",
                     "foo",
                     doc.getField(IndexUtils.RECORD_FIELD).stringValue());
    }

    /*
    Build an index with a single Records.
     */
    public void testSimpleIndexBuild() throws Exception {
        storage.flush(new Record("foo", BASE, new byte[0]));
        updateIndex();
        assertEquals("The number of processed ids should be correct",
                     1, IterativeHelperDocCreator.processedIDs.size());
        FacetMap facetMap = getFacetMap();
        assertEquals("The TagHandler should have the correct number of Facets",
                     2, facetMap.getTagHandler().getFacets().size());
        assertEquals("The coreMap should have the correct number of docs",
                     1, facetMap.getCoreMap().getDocCount());
        facetMap.close();

        assertIndexEquals("The index should contain a single document",
                          Arrays.asList("foo"), null);
        assertTagsEquals("The index should contain a single Title tag",
                          "Title", Arrays.asList("Title_foo"));
    }

    /*
    Build an index with 3 records
     */
    public void testMultipleRecords() throws Exception {
        storage.flush(new Record("foo1", BASE, new byte[0]));
        storage.flush(new Record("foo2", BASE, new byte[0]));
        storage.flush(new Record("foo3", BASE, new byte[0]));
        updateIndex();
        assertEquals("The number of processed ids should be correct",
                     3, IterativeHelperDocCreator.processedIDs.size());

        assertIndexEquals("The index should contain multiple document",
                          Arrays.asList("foo1", "foo2", "foo3"), null);
        assertTagsEquals("The index should contain multiple Title tags",
                          "Title", Arrays.asList("Title_foo1", "Title_foo2",
                                                 "Title_foo3"));
    }

    /*
    Updates an index two times with separate indexers.
     */
    public void testMultipartUpdate() throws Exception {
        storage.flush(new Record("foo1", BASE, new byte[0]));
        updateIndex();
        assertEquals("The number of processed ids should be correct at first",
                     1, IterativeHelperDocCreator.processedIDs.size());
        assertIndexEquals("The index should contain a single document",
                          Arrays.asList("foo1"), null);
        assertTagsEquals("The index should contain a single Title tag",
                          "Title", Arrays.asList("Title_foo1"));

        storage.flush(new Record("foo2", BASE, new byte[0]));
        updateIndex();
        assertEquals("The number of processed ids should be correct at last",
                     2, IterativeHelperDocCreator.processedIDs.size());
        assertIndexEquals("The index should contain multiple documents",
                          Arrays.asList("foo1", "foo2"), null);
        assertTagsEquals("The index should contain multiple Title tags",
                          "Title", Arrays.asList("Title_foo1", "Title_foo2"));

    }

    /*
    Build an index with 2 Records, then delete the last one
     */
    public void testDeleteEnd() throws Exception {

    }

    /*
    Build an index with 2 Records, then delete the first one
     */
    public void testDeleteBeginning() throws Exception {

    }

    /* Helpers */

    /*
    Verifies that the tags for the given facet is as expected.
     */
    private void assertTagsEquals(String message, String facet,
                                  List<String> expectedTags) throws Exception {
        Browser browser = getBrowser();
        DocIDCollector ids = new DocIDCollector();
        IndexReader ir = getIndexReader();
        for (int i = 0 ; i < ir.maxDoc() ; i++) {
            if (!ir.isDeleted(i)) {
                ids.collect(i, 1.0f);
            }
        }
        ir.close();
        FacetResultImpl result =
                (FacetResultImpl)browser.getFacetMap(ids, facet);
        if (!expectedTags.equals(result.getTags(facet))) {
            fail(message + ". The Tags for facet " + facet
                 + " did not match. Expected: " + expectedTags + ", actual: "
                 + result.getTags(facet));
        }
    }

    private FacetMap getFacetMap() throws Exception {
        Configuration conf = Configuration.load(
                "data/iterative/IterativeTest_FacetSearchConfiguration.xml");
        Structure structure = new Structure(conf);
        assertEquals("There should be the right number of Facets defined",
                     2, structure.getFacetNames().size());
        TagHandler tagHandler =
                TagHandlerFactory.getTagHandler(conf, structure, false);
        CoreMap coreMap = CoreMapFactory.getCoreMap(conf, structure);
        FacetMap facetMap =  new FacetMap(structure, coreMap, tagHandler, true);
        facetMap.open(new File(getIndexLocation(), FacetCore.FACET_FOLDER));
        return facetMap;
    }

    private Browser getBrowser() throws Exception {
        Configuration conf = Configuration.load(
                "data/iterative/IterativeTest_FacetSearchConfiguration.xml");
        assertEquals("There should be the right number of Facets defined",
                     2, new Structure(conf).getFacetNames().size());
        Browser browser = new BrowserImpl(conf);
        browser.open(getIndexLocation());
        return browser;
    }

    /*
    All recordIDs must be present in the latest index in the given order.
     */
    private void assertIndexEquals(String message, List<String> recordIDs,
                                   List<String> deletedRecordIDs) throws
                                                                     Exception {
        if (recordIDs == null) {
            recordIDs = new ArrayList<String>(0);
        }
        if (deletedRecordIDs == null) {
            deletedRecordIDs = new ArrayList<String>(0);
        }
        IndexReader ir = getIndexReader();
        List<String> foundIDs = new ArrayList<String>(ir.maxDoc());
        List<String> foundDeletedIDs = new ArrayList<String>(ir.maxDoc());
        for (int i = 0 ; i < ir.maxDoc() ; i++) {
            String id = ir.document(i).getField(IndexUtils.RECORD_FIELD).
                    stringValue();
            log.trace("assertIndexEquals: Found id '" + id + "'"
                      + (ir.isDeleted(i) ? " (deleted)" : " (standard)"));
            if (ir.isDeleted(i)) {
                foundDeletedIDs.add(id);
            } else {
                foundIDs.add(id);
            }
        }
        ir.close();

        if (!recordIDs.equals(foundIDs)) {
            fail(message + ". The standard documents in the index did not "
                 + "match the expected ids. Expected: " + recordIDs
                 + ", actual: " + foundIDs);
        }
        if (!deletedRecordIDs.equals(foundDeletedIDs)) {
            fail(message + ". The deleted documents in the index did not "
                 + "match the expected ids. Expected: " + deletedRecordIDs
                 + ", actual: " + foundDeletedIDs);
        }
    }

    private IndexReader getIndexReader() throws IOException {
        File indexLocation = getIndexLocation();
        IndexReader ir = IndexReader.open(new File(indexLocation, "lucene"));
        return ir;
    }

    private File getIndexLocation() {
        File[] subFolders = IndexTest.INDEX_ROOT.listFiles();
        return subFolders[subFolders.length-1];
    }

    private void updateIndex() throws IOException, InterruptedException {
        FilterControl filters = new FilterControl(getIndexConfiguration());
        log.debug("Starting filter");
        filters.start();
        log.debug("Waiting for finish");
        filters.waitForFinish();
        filters.stop();
    }

    private Configuration getIndexConfiguration() throws IOException {
        String descriptorLocation = Resolver.getURL(
                "data/iterative/IterativeTest_IndexDescriptor.xml")
                .getFile();
        String confString = Resolver.getUTF8Content(
                "data/iterative/IterativeTest_IndexConfiguration.xml");
        log.debug("Replacing [IndexDescriptorLocation] with "
                  + descriptorLocation);
        confString = confString.replaceAll("\\[IndexDescriptorLocation\\]",
                                           descriptorLocation);
        log.debug("Replacing [IndexRootLocation] with "
                  + IndexTest.INDEX_ROOT.getPath());
        confString = confString.replaceAll("\\[IndexRootLocation\\]",
                                           IndexTest.INDEX_ROOT.getPath());
        File confFile = File.createTempFile("IndexConfiguration", ".xml");
        Files.saveString(confString, confFile);
        System.out.println(confFile);
        return Configuration.load(confFile.getPath());
    }

    private Payload getPayload(String id) {
        return new Payload(new Record(id, BASE, new byte[0]));
    }

    private int countRecords(String base) throws IOException {
        RecordIterator iterator = storage.getRecords(base);
        int counter = 0;
        while (iterator.hasNext()) {
            counter++;
            iterator.next();
        }
        return counter;
    }

    public RecordReader getRecordReader() throws IOException {
        MemoryStorage ms = new MemoryStorage();
        ms.put(RecordReader.CONF_START_FROM_SCRATCH, true);
        ms.put(RecordReader.CONF_BASE, BASE);
        return new RecordReader(new Configuration(ms));
    }

}
