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
package dk.statsbiblioteket.summa.releasetest;

import java.io.File;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import dk.statsbiblioteket.summa.ingest.stream.XMLSplitterFilter;
import dk.statsbiblioteket.summa.storage.StorageFactory;
import dk.statsbiblioteket.summa.storage.database.DatabaseControl;
import dk.statsbiblioteket.summa.storage.io.Control;
import dk.statsbiblioteket.summa.storage.io.RecordIterator;
import dk.statsbiblioteket.util.Files;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The purpose of this class is to test "files => ingest-chain => storage".
 * It relies on the modules Common, Ingest and Storage.
 */
public class IngestTest extends TestCase {
    private static Log log = LogFactory.getLog(IngestTest.class);

    private final static String HEADER =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
    + "<outer xmlns=\"http://www.example.com/mynamespace/\""
    + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
    + " xmlns:foo=\"http://example.com/somename\">\n"
    + "<sometag>Hello World</sometag>\n"
    + "<innerrecords>\n";
    private static final String FOOTER =
    "</innerrecords>\n"
    + "</outer>\n";

    private final static String THREE_RECORDS =
    HEADER
    + "<record id=\"recordA\">\n"
    + "<mycontent atag=\"tagvalueA\">contentA</mycontent>\n"
    + "<morecontent><subcontent>subA</subcontent></morecontent>\n"
    + "</record>\n"

    + "<record id=\"recordB\">\n"
    + "<mycontent atag=\"tagvalueB\">contentB</mycontent>\n"
    + "<morecontent><subcontent>subB</subcontent></morecontent>\n"
    + "</record>\n"

    + "<record id=\"recordC\">\n"
    + "<mycontent atag=\"tagvalueC\">contentC</mycontent>\n"
    + "<morecontent><subcontent>subC</subcontent></morecontent>\n"
    + "</record>\n"
    + FOOTER;

    private final static String TWO_RECORDS =
    HEADER
    + "<record id=\"record2A\">\n"
    + "<mycontent atag=\"tagvalue2A\">content2A</mycontent>\n"
    + "<morecontent><subcontent>sub2A</subcontent></morecontent>\n"
    + "</record>\n"

    + "<record id=\"record2B\">\n"
    + "<mycontent atag=\"tagvalue2B\">content2B</mycontent>\n"
    + "<morecontent><subcontent>sub2B</subcontent></morecontent>\n"
    + "</record>\n"
    + FOOTER;

    private final static String NO_RECORDS = HEADER + FOOTER;

    private final static String INVALID_RECORDS = "Not XML!";

    private static int storageCounter = 0;

    public void setUp () throws Exception {
        super.setUp();
        if (sourceRoot.exists()) {
            Files.delete(sourceRoot);
        }
        assertTrue("The root '" + sourceRoot + "' should have be created",
                   sourceRoot.mkdirs());
        Files.saveString(THREE_RECORDS, new File(sourceRoot, "three.xml"));
        Files.saveString(TWO_RECORDS, new File(sourceRoot, "two.xml"));
        Files.saveString(NO_RECORDS, new File(sourceRoot, "none.xml"));
        Files.saveString(INVALID_RECORDS, new File(sourceRoot, "invalid.xml"));
        Files.saveString("Dummy content", new File(sourceRoot, "dummy.dummy"));
        for (int i = 0 ; i < 40 ; i++) { // Up to the number of distinct tests
            File attempDelete =
                    new File(System.getProperty("java.io.tmpdir"),
                             "storage" + i);
            if (attempDelete.exists()) {
                try {
                    Files.delete(attempDelete);
                    log.debug("Deleted '" + attempDelete + "'");
                } catch (Exception e) {
                    log.warn("Could not delete '" + attempDelete + "'");
                }
            }
        }
        storageLocation =
            new File(System.getProperty("java.io.tmpdir"),
                     "storage" + storageCounter++);
    }
    public void tearDown() throws Exception {
        super.tearDown();
        Files.delete(sourceRoot);
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final File sourceRoot =
            new File(new File(System.getProperty("java.io.tmpdir")),
                     "IngestTest");


    public File storageLocation;

    public void testBasicLoad() throws Exception {
        Configuration conf = getReaderConfiguration();
        FileReader reader = new FileReader(conf);
        for (int i = 0 ; i < 4 ; i++) {
            assertTrue("Reader should have next for file #" + (i+1),
                       reader.hasNext());
            assertNotNull("The next should give a payload", reader.next());
        }
        assertFalse("Reader should only have 4 payloads",
                   reader.hasNext());
        reader.close(true);
    }

    private static final String TESTBASE = "testbase";
    public void testRecordExtraction() throws Exception {
        Configuration readerConf = getReaderConfiguration();
        Configuration splitterConf = getSplitterConfiguration();

        FileReader reader = new FileReader(readerConf);
        XMLSplitterFilter splitter = new XMLSplitterFilter(splitterConf);
        splitter.setSource(reader);
        int NUM_RECORDS = 5;
        for (int i = 0 ; i < NUM_RECORDS ; i++) {
            assertTrue("Splitter should have next for record #" + (i+1),
                       splitter.hasNext());
            Payload payload = splitter.next();
            assertNotNull("The next should give a payload", payload);
        }
        assertFalse("Splitter should only have " + NUM_RECORDS + " payloads",
                   splitter.hasNext());
    }

    private Configuration getSplitterConfiguration() {
        Configuration splitterConf = Configuration.newMemoryBased();
        splitterConf.set(XMLSplitterFilter.CONF_BASE, TESTBASE);
        splitterConf.set(XMLSplitterFilter.CONF_COLLAPSE_PREFIX, "true");
        splitterConf.set(XMLSplitterFilter.CONF_ID_ELEMENT, "record#id");
        splitterConf.set(XMLSplitterFilter.CONF_ID_PREFIX, "myprefix");
        splitterConf.set(XMLSplitterFilter.CONF_PRESERVE_NAMESPACES, "true");
        splitterConf.set(XMLSplitterFilter.CONF_RECORD_ELEMENT, "record");
        splitterConf.set(XMLSplitterFilter.CONF_REQUIRE_VALID, "false");
        return splitterConf;
    }

    private Configuration getReaderConfiguration() {
        Configuration readerConf = Configuration.newMemoryBased();
        readerConf.set(FileReader.CONF_ROOT_FOLDER, sourceRoot.toString());
        readerConf.set(FileReader.CONF_RECURSIVE, true);
        readerConf.set(FileReader.CONF_FILE_PATTERN, ".*\\.xml");
        readerConf.set(FileReader.CONF_COMPLETED_POSTFIX, ".processed");
        return readerConf;
    }

    private Configuration getStorageConfiguration() {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(DatabaseControl.PROP_LOCATION, storageLocation.toString());
        return conf;
    }

    private Configuration getWriterConfiguration() {
        Configuration writerConf = Configuration.newMemoryBased();
        writerConf.set(FileReader.CONF_ROOT_FOLDER, sourceRoot.toString());
        writerConf.set(FileReader.CONF_RECURSIVE, true);
        writerConf.set(FileReader.CONF_FILE_PATTERN, ".*\\.xml");
        writerConf.set(FileReader.CONF_COMPLETED_POSTFIX, ".processed");
        return writerConf;
    }

    public void testStorage() throws Exception {
        Configuration storageConf = getStorageConfiguration();

        Control control = StorageFactory.createController(storageConf);

        RecordIterator iterator = control.getRecordsModifiedAfter(0, TESTBASE);
        assertFalse("The Storage should be empty", iterator.hasNext());

        Record record = new Record("foo", TESTBASE, new byte[0]);
        control.flush(record);

        iterator = control.getRecordsModifiedAfter(0, TESTBASE);
        assertTrue("The Storage should contain something", iterator.hasNext());
        assertEquals("The first record should have id as expected",
                     "foo", iterator.next().getId());
        assertFalse("After extraction of a Record, "
                    + "the iterator should be empty",
                   iterator.hasNext());
    }

    public void testIngestToStorage() throws Exception {
        // TODO: Implement this
        
/*        Configuration storageConf = getStorageConfiguration();
        Control control = StorageFactory.createController(storageConf);

        Configuration writerConf = getWriterConfiguration();

        FileReader reader = new FileReader(readerConf);
        XMLSplitterFilter splitter = new XMLSplitterFilter(splitterConf);
        splitter.setSource(reader);
  */
    }

    public void testFullIngestWorkflow() throws Exception {
        // TODO: Implement this. Remember to use a proper filter chain

    }

}