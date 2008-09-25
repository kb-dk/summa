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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.RecordIterator;
import dk.statsbiblioteket.summa.storage.api.filter.RecordReader;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.index.IndexController;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;

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

    public void testSimpleIngest() throws Exception {
        storage.flush(new Record("foo", BASE, new byte[0]));
        storage.flush(new Record("foo2", BASE, new byte[0]));
        assertEquals("The number of records in storage should be correct",
                     2, countRecords(BASE));
    }

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

    public void testDocumentCreation() throws Exception {
        IterativeHelperDocCreator creator = new IterativeHelperDocCreator(null);
        Payload payload = getPayload("foo");
        creator.processPayload(payload);
        Document doc = (Document)payload.getData(Payload.LUCENE_DOCUMENT);
        assertEquals("The document should contain the right ID",
                     "foo",
                     doc.getField(IndexUtils.RECORD_FIELD).stringValue());
    }

    public void testSimpleIndexBuild() throws Exception {
        storage.flush(new Record("foo", BASE, new byte[0]));
        updateIndex();
        assertEquals("The number of processed ids should be correct",
                     1, IterativeHelperDocCreator.processedIDs.size());
    }

    

    /* Helpers */

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
