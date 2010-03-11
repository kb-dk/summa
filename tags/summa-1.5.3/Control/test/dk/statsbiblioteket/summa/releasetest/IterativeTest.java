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
package dk.statsbiblioteket.summa.releasetest;

import java.io.IOException;
import java.io.File;
import java.util.Iterator;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.api.filter.RecordReader;
import dk.statsbiblioteket.summa.facetbrowser.core.FacetMap;
import dk.statsbiblioteket.summa.facetbrowser.core.FacetCore;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMapFactory;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandlerFactory;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultImpl;
import dk.statsbiblioteket.summa.facetbrowser.browse.Browser;
import dk.statsbiblioteket.summa.facetbrowser.FacetSearchNode;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.index.lucene.LuceneManipulator;

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

    @Override
    public void setUp () throws Exception {
        super.setUp();
        IngestTest.deleteOldStorages();
        if (IndexTest.INDEX_ROOT.exists()) {
            Files.delete(IndexTest.INDEX_ROOT);
        }
        IndexTest.INDEX_ROOT.mkdirs();
        IterativeHelperDocCreator.processedIDs.clear();

        Configuration storageConf = IngestTest.getStorageConfiguration();
        storage = StorageFactory.createStorage(storageConf);
    }

    @Override
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
        IterativeHelperDocCreator creator =
                new IterativeHelperDocCreator(Configuration.newMemoryBased());
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
                     4, facetMap.getTagHandler().getFacets().size());
        assertEquals("The coreMap should have the correct number of docs",
                     1, facetMap.getCoreMap().getDocCount());
        facetMap.close();

        assertIndexEquals("The index should contain a single document",
                          Arrays.asList("foo"), 0);
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
                          Arrays.asList("foo1", "foo2", "foo3"), 0);
        assertTagsEquals("The index should contain multiple Title tags",
                          "Title", Arrays.asList("Title_foo1", "Title_foo2",
                                                 "Title_foo3"));
    }

    /*
    Updates an index two times with separate indexers.
     */
    public void testMultipartUpdate() throws Exception {
        assertEquals("No ids should be processed before start of test",
                     0, IterativeHelperDocCreator.processedIDs.size());
        storage.flush(new Record("foo1", BASE, new byte[0]));
        updateIndex();
        assertEquals("The number of processed ids should be correct at first",
                     1, IterativeHelperDocCreator.processedIDs.size());
        assertIndexEquals("The index should contain a single document",
                          Arrays.asList("foo1"), 0);
        assertTagsEquals("The index should contain a single Title tag",
                          "Title", Arrays.asList("Title_foo1"));

        storage.flush(new Record("foo2", BASE, new byte[0]));
        updateIndex();
        assertEquals("The number of processed ids should be correct at last",
                     2, IterativeHelperDocCreator.processedIDs.size());
        assertIndexEquals("The index should contain multiple documents",
                          Arrays.asList("foo1", "foo2"), 0);
        assertTagsEquals("The index should contain multiple Title tags",
                          "Title", Arrays.asList("Title_foo1", "Title_foo2"));

    }

    /*
    Build an index with 2 Records, then delete the last one
     */
    public void testDeleteEnd() throws Exception {
        storage.flush(new Record("foo1", BASE, new byte[0]));
        storage.flush(new Record("foo2", BASE, new byte[0]));
        updateIndex();
        Record deletedRecord = new Record("foo2", BASE, new byte[0]);
        deletedRecord.setDeleted(true);
        Thread.sleep(10); // Hack to make createdTime != modifiedTime
        deletedRecord.touch();
        storage.flush(deletedRecord);
        updateIndex();

        assertEquals("The number of processed ids should be correct",
                     3, IterativeHelperDocCreator.processedIDs.size());
        assertIndexEquals("The index should contain a single document",
                          Arrays.asList("foo1"), 1);
        assertTagsEquals("The index should contain a single Title tag",
                          "Title", Arrays.asList("Title_foo1"));
    }

    /*
    Build an index with 2 Records, then delete the first one
     */
    public void testDeleteBeginning() throws Exception {
        storage.flush(new Record("foo1", BASE, new byte[0]));
        storage.flush(new Record("foo2", BASE, new byte[0]));
        updateIndex();
        Record deletedRecord = new Record("foo1", BASE, new byte[0]);
        deletedRecord.setDeleted(true);
        Thread.sleep(10); // Hack to make createdTime != modifiedTime
        deletedRecord.touch();
        storage.flush(deletedRecord);
        updateIndex();

        assertEquals("The number of processed ids should be correct",
                     3, IterativeHelperDocCreator.processedIDs.size());
        assertIndexEquals("The index should contain a single document",
                          Arrays.asList("foo2"), 1);
        assertTagsEquals("The index should contain a single Title tag",
                          "Title", Arrays.asList("Title_foo2"));
    }

    /*
    Build an index with 2 Records, then delete the first one and add a third one
     */
    public void testDeleteAdd() throws Exception {
        storage.flush(new Record("foo1", BASE, new byte[0]));
        storage.flush(new Record("foo2", BASE, new byte[0]));
        updateIndex();

        Record deletedRecord = new Record("foo1", BASE, new byte[0]);
        deletedRecord.setDeleted(true);
        Thread.sleep(10); // Hack to make createdTime != modifiedTime
        deletedRecord.touch();
        storage.flush(deletedRecord);
        updateIndex();

        storage.flush(new Record("foo3", BASE, new byte[0]));
        updateIndex();

        assertIndexEquals("The index should contain a single document",
                          Arrays.asList("foo2", "foo3"), 1);
        assertTagsEquals("The index should contain multiple Title tag",
                          "Title", Arrays.asList("Title_foo2", "Title_foo3"));
    }

    /*
    Build an index with 2 Records, then delete the first one without doing
    a new index-round
     */
    public void testDeleteOneGo() throws Exception {
        storage.flush(new Record("foo1", BASE, new byte[0]));
        storage.flush(new Record("foo2", BASE, new byte[0]));
        Record deletedRecord = new Record("foo2", BASE, new byte[0]);
        deletedRecord.setDeleted(true);
        Thread.sleep(10); // Hack to make createdTime != modifiedTime
        deletedRecord.touch();
        storage.flush(deletedRecord);
        updateIndex();

        assertEquals("The number of processed ids should be correct",
                     2, IterativeHelperDocCreator.processedIDs.size());
        assertIndexEquals("The index should contain a single document",
                          Arrays.asList("foo1"), 0);
        assertTagsEquals("The index should contain a single Title tag",
                          "Title", Arrays.asList("Title_foo1"));
    }

    /*
    Update a record among multiple records.
     */
    public void testUpdate() throws Exception {
        storage.flush(new Record("foo1", BASE, new byte[0]));
        storage.flush(new Record("foo2", BASE, new byte[0]));
        updateIndex();
        Record updatedRecord = new Record("foo1", BASE, new byte[0]);
        Thread.sleep(10); // Hack to make createdTime != modifiedTime
        updatedRecord.touch();
        storage.flush(updatedRecord);
        updateIndex();

        assertIndexEquals("The index should contain multiple document",
                          Arrays.asList("foo2", "foo1"), 1);
        assertTagsEquals("The index should contain multiple Title tags",
                          "Title", Arrays.asList("Title_foo1", "Title_foo2"));
    }

    /*
    Update a record.
     */
    public void testUpdateSingleRecord() throws Exception {
        storage.flush(new Record("foo1", BASE, new byte[0]));
        updateIndex();
        Record updatedRecord = new Record("foo1", BASE, new byte[0]);
        Thread.sleep(10); // Hack to make createdTime != modifiedTime
        updatedRecord.touch();
        storage.flush(updatedRecord);
        updateIndex();

        assertIndexEquals("The index should contain a single document",
                          Arrays.asList("foo1"), 1);
        assertTagsEquals("The index should contain a single Title tag",
                          "Title", Arrays.asList("Title_foo1"));
    }

    /*
    Tests whether consolidate properly performs a clean up of the Lucene index
    and a rebuild of the Facet structure
     */
    public void testConsolidate() throws Exception {
        storage.flush(new Record("foo1", BASE, new byte[0]));
        storage.flush(new Record("foo2", BASE, new byte[0]));
        storage.flush(new Record("foo3", BASE, new byte[0]));
        updateIndex();
        assertTagsEquals("Stored only should work with iterative",
                          "onlystore", Arrays.asList(
                "Stored_foo1", "Stored_foo2", "Stored_foo3"));
        assertTagsEquals("Indexed only should work with iterative",
                          "onlyindex", Arrays.asList(
                "Indexed_foo1", "Indexed_foo2", "Indexed_foo3"));

        Record deletedRecord = new Record("foo1", BASE, new byte[0]);
        deletedRecord.setDeleted(true);
        Thread.sleep(10); // Hack to make createdTime != modifiedTime
        deletedRecord.touch();
        storage.flush(deletedRecord);
        updateIndex();

        storage.flush(new Record("foo4", BASE, new byte[0]));
        updateIndexConsolidate();

        assertIndexEquals("The index should contain no deletions",
                          Arrays.asList("foo2", "foo3", "foo4"), 0);
        assertTagsEquals("The Facet index should contain multiple Title tags",
                          "Title", Arrays.asList(
                "Title_foo2", "Title_foo3", "Title_foo4"));

        assertTagsEquals("Stored only should work with consolidate",
                          "onlystore", Arrays.asList(
                "Stored_foo2", "Stored_foo3", "Stored_foo4"));
        assertTagsEquals("Indexed only should work with consolidate",
                          "onlyindex", Arrays.asList(
                "Indexed_foo2", "Indexed_foo3", "Indexed_foo4"));
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
                     4, structure.getFacetNames().size());
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
                     4, new Structure(conf).getFacetNames().size());
        Browser browser = new FacetSearchNode(conf);
        browser.open(getIndexLocation());
        return browser;
    }

    /*
    All recordIDs must be present in the latest index in the given order.
     */
    private void assertIndexEquals(String message, List<String> recordIDs,
                                   int expectedDeleted) throws Exception {
        if (recordIDs == null) {
            recordIDs = new ArrayList<String>(0);
        }
        IndexReader ir = getIndexReader();
        List<String> foundIDs = new ArrayList<String>(ir.maxDoc());
        int deletedCount = 0;
        for (int i = 0 ; i < ir.maxDoc() ; i++) {
            if (ir.isDeleted(i)) {
                deletedCount++;
                log.trace("assertIndexEquals: Found deleted at pos " + i);
            } else {
                String id = ir.document(i).getField(IndexUtils.RECORD_FIELD).
                        stringValue();
                foundIDs.add(id);
                log.trace("assertIndexEquals: Found id '" + id + "'");
            }
        }
        ir.close();

        if (!recordIDs.equals(foundIDs)) {
            fail(message + ". The standard documents in the index did not "
                 + "match the expected ids. Expected: " + recordIDs
                 + ", actual: " + foundIDs);
        }
        if (expectedDeleted != deletedCount) {
            fail(message + ". The number of deleted documents in the index did"
                 + "not match the expected ids. Expected: " + expectedDeleted
                 + ", actual: " + deletedCount);
        }
    }

    private IndexReader getIndexReader() throws IOException {
        File indexLocation = getIndexLocation();
        return IndexReader.open(new File(indexLocation, "lucene"));
    }

    private File getIndexLocation() {
        File[] subFolders = IndexTest.INDEX_ROOT.listFiles();
        return subFolders[subFolders.length-1];
    }

    private void updateIndex() throws IOException, InterruptedException {
        Configuration conf = getIndexConfiguration();
        updateIndex(conf);
    }

    private void updateIndexConsolidate() throws IOException,
                                                 InterruptedException {
        Configuration conf = getIndexConfiguration();
        // Ensure that consolidate is called
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(2).
//                getSubConfiguration("IndexUpdate").
                set(IndexControllerImpl.CONF_CONSOLIDATE_MAX_DOCUMENTS, 1);
        // Ensure that deletes are removed
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(2).
//                getSubConfiguration("IndexUpdate").
             getSubConfigurations(IndexControllerImpl.CONF_MANIPULATORS).get(0).
//                getSubConfiguration("LuceneUpdater").
                set(LuceneManipulator.CONF_MAX_SEGMENTS_ON_CONSOLIDATE, 1);
        // Sanity-check
        assertEquals("The value for consolidatetimeout should be present",
                     -1,
                     conf.getSubConfigurations(FilterControl.CONF_CHAINS).
                             get(0).
                             getSubConfigurations(FilterSequence.CONF_FILTERS).
                             get(2).
//                             getSubConfiguration("IndexUpdate").
                             getInt(IndexControllerImpl.
                             CONF_CONSOLIDATE_TIMEOUT));
        updateIndex(conf);
    }

    private void updateIndex(Configuration conf) throws InterruptedException {
        FilterControl filters = new FilterControl(conf);
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
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);
        Iterator<Record> iterator = new StorageIterator(storage, iterKey); 
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
        ms.put(ConnectionConsumer.CONF_RPC_TARGET,
               "//localhost:28000/summa-storage");
        ms.put(RecordReader.CONF_BASE, BASE);
        return new RecordReader(new Configuration(ms));
    }

}

