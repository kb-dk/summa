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
package dk.statsbiblioteket.summa.common.lucene.distribution;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;

@SuppressWarnings({"ALL"})
public class TermStatClientTest extends TestCase {
    private static Log log = LogFactory.getLog(TermStatClientTest.class);

    public TermStatClientTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (TEST_DIR.exists()) {
            log.debug("Removing previous test-files from " + TEST_DIR);
            Files.delete(TEST_DIR);
        }
        TEST_DIR.mkdirs();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        try {
            Files.delete(TEST_DIR);
        } catch (IOException e) {
            fail("Unable to delete the folder " + TEST_DIR);
        }
    }

    public static Test suite() {
        return new TestSuite(TermStatClientTest.class);
    }

    public void testDumpStatsSimple() throws Exception {
        assertCount(1, 8);
        assertCount(2, 12);
        assertCount(5, 4 + (4 * 5));
    }

    public void assertCount(int docCount, int expectedEntries) throws Exception {
/*        generateIndex(docCount);
        Configuration conf = Configuration.newMemoryBased();
        TermStatClient extractor = new TermStatClient(conf);
        File dumpLocation = new File(TEST_DIR, "dump");
        extractor.dumpStats(INDEX_LOCATION, dumpLocation);
        log.info("Dump-filder contains "
                 + Strings.join(dumpLocation.listFiles(), ", "));

        TermStat termStat = new TermStat(conf);
        termStat.open(dumpLocation);
        assertEquals("The number of stored stats should be correct",
                     expectedEntries, termStat.getTermCount());
        termStat.close();*/
        // TODO: Enable this again
    }

    public void testGet() throws Exception {
        generateIndex(100);
        Configuration conf = Configuration.newMemoryBased();
        TermStatClient extractor = new TermStatClient(conf);
        File dumpLocation = new File(TEST_DIR, "dump");
        extractor.dumpStats(INDEX_LOCATION, dumpLocation);
        log.info("Dump-filder contains " + Strings.join(dumpLocation.listFiles(), ", "));

        TermStat termStat = new TermStat(conf);
        termStat.open(dumpLocation);
        for (int i = 0 ; i < termStat.size() ; i++) {
            termStat.get(i);
        }
    }

    // Disabled due to deprecation
/*    public void testOrder() throws Exception {
        generateIndex(100);
        Configuration conf = Configuration.newMemoryBased();
        TermStatClient extractor = new TermStatClient(conf);
        File dumpLocation = new File(TEST_DIR, "dump");
        extractor.dumpStats(INDEX_LOCATION, dumpLocation);

        TermStat termStat = new TermStat(conf);
        termStat.open(dumpLocation);
        TermEntry oldEntry = null;
        for (int i = 0 ; i < termStat.size() ; i++) {
            TermEntry current = termStat.get(i);
            if (oldEntry != null
                && oldEntry.getTerm().compareTo(current.getTerm()) > 0) {
                fail(String.format(
                    "Entry #%d and #%d were '%s' and '%s', but the order should be reversed",
                    i-1, i, oldEntry.getTerm(), current.getTerm()));
            }
            oldEntry = current;
        }
    }



    public void testLookup() throws Exception {
        generateIndex(100);
        Configuration conf = Configuration.newMemoryBased();
        TermStatClient extractor = new TermStatClient(conf);
        File dumpLocation = new File(TEST_DIR, "dump");
        extractor.dumpStats(INDEX_LOCATION, dumpLocation);
        log.info("Dump-folder contains " + Strings.join(dumpLocation.listFiles(), ", "));

        TermStat termStat = new TermStat(conf);
        termStat.open(dumpLocation);
        String FIXED = "fixedcontent";
        TermEntry fixed = termStat.getEntry(FIXED);
        assertNotNull("There should be an entry for '" + FIXED + "'", fixed);
        assertEquals("The doc count for " + FIXED + " should be numDocs",
                     100, fixed.getStat(0));
        assertEquals("The term count for " + FIXED + " should be numDocs",
                     100, fixed.getStat(1));
        String VARIABLE_A = "variablecontent1";
        if (termStat.getEntry(VARIABLE_A).getStat(1) != 1) {
            dumpTermStats(termStat);
        }
        assertEquals("The termcount for " + VARIABLE_A + " should be indexcount*3",
                     3, termStat.getEntry(VARIABLE_A).getStat(1));

        TermEntry world = termStat.getEntry("world");
        assertNotNull("There should be an entry for 'world'", world);

        TermEntry na = termStat.getEntry("nonexisting");
        assertNull("There should be no entry for 'nonexisting'", na);

        termStat.close();
    }
  */
    private void dumpTermStats(TermStat termStat) {
        for (int i = 0 ; i < termStat.size() ; i++) {
            TermEntry entry = termStat.get(i);
            log.debug("Entry #" + i + ": " + entry);
        }
    }


    /*
    public void testMerge() throws Exception {
        generateIndex(100, new File(TEST_DIR, "index_a"));
        generateIndex(300, new File(TEST_DIR, "index_b"));

        Configuration conf = Configuration.newMemoryBased();
        TermStatClient extractor = new TermStatClient(conf);

        extractor.dumpStats(new File(TEST_DIR, "index_a"),
                            new File(TEST_DIR, "dump_a"));
        extractor.dumpStats(new File(TEST_DIR, "index_b"),
                            new File(TEST_DIR, "dump_b"));

        extractor.mergeStats(Arrays.asList(new File(TEST_DIR, "dump_a"),
                                           new File(TEST_DIR, "dump_b")),
                             new File(TEST_DIR, "merged"));

        TermStat termStat = new TermStat(conf);
        termStat.open(new File(TEST_DIR, "merged"));
        String FIXED = "fixed:fixedcontent";
        assertEquals("The termcount for " + FIXED + " should be numDocs",
                     400, termStat.getEntry(FIXED).getStat(1));

        String VARIABLE_A = "variableshareda:variablecontent1";
        assertEquals("The termcount for " + VARIABLE_A 
                     + " should be indexcount",
                     2, termStat.getEntry(VARIABLE_A).getStat(1));
        termStat.close();
    } */

    public static final File TEST_DIR = new File("target/tmp/", "termstats");
    public static final File INDEX_LOCATION = new File(TEST_DIR, "lucene");

    private void generateIndex(int docCount) throws Exception {
        generateIndex(docCount, INDEX_LOCATION);
    }
    /*
        The number of unique terms is 4 (fixed) + (docCount * 4 (variable))
     */
    private void generateIndex(int docCount, File location) throws Exception {
        IndexWriter writer = new IndexWriter(
            new NIOFSDirectory(location),
            new IndexWriterConfig(Version.LUCENE_30, new StandardAnalyzer(Version.LUCENE_30)));
/*        IndexWriter writer = new IndexWriter(
                new NIOFSDirectory(location),
                new StandardAnalyzer(Version.LUCENE_30),
                true, new IndexWriter.MaxFieldLength(10000));*/
        for (int i = 0 ; i < docCount ; i++) {
            Document doc = new Document();
            doc.add(new Field("fixed", "fixedcontent",
                              Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new Field("duplicatefixed", "hello world hello world",
                              Field.Store.YES, Field.Index.ANALYZED));

            doc.add(new Field("duplicatemixed", "hello" + i + " world hello" + i + " world",
                              Field.Store.YES, Field.Index.ANALYZED));

            doc.add(new Field("variableshareda", "variablecontent" + i,
                              Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new Field("variablesharedb", "variablecontent" + i,
                              Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new Field("variablenonshared", "variablecontent" + i,
                              Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new Field("nonindexed", "content",
                              Field.Store.YES, Field.Index.NO));
            writer.addDocument(doc);
        }
        writer.close();
    }
}
