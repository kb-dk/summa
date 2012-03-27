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

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.lucene.store.*;
import org.apache.lucene.util.*;

@SuppressWarnings({"ALL"})
public class TermStatExtractorTest extends TestCase {
    private static Log log = LogFactory.getLog(TermStatExtractorTest.class);

    public TermStatExtractorTest(String name) {
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
        return new TestSuite(TermStatExtractorTest.class);
    }

    public void testDumpStatsSimple() throws Exception {
        assertCount(1, 8);
        assertCount(2, 12);
        assertCount(5, 4 + (4 * 5));
    }

    public void assertCount(int docCount, int expectedEntries) throws
                                                                     Exception {
        generateIndex(docCount);
        Configuration conf = Configuration.newMemoryBased();
        TermStatExtractor extractor = new TermStatExtractor(conf);
        File dumpLocation = new File(TEST_DIR, "dump");
        extractor.dumpStats(INDEX_LOCATION, dumpLocation);
        log.info("Dump-filder contains "
                 + Strings.join(dumpLocation.listFiles(), ", "));

        TermStat termStat = new TermStat(conf);
        termStat.open(dumpLocation);
        assertEquals("The number of stored stats should be correct",
                     expectedEntries, termStat.getTermCount());
        termStat.close();
    }

    public void testLookup() throws Exception {
        generateIndex(100);
        Configuration conf = Configuration.newMemoryBased();
        TermStatExtractor extractor = new TermStatExtractor(conf);
        File dumpLocation = new File(TEST_DIR, "dump");
        extractor.dumpStats(INDEX_LOCATION, dumpLocation);
        log.info("Dump-filder contains "
                 + Strings.join(dumpLocation.listFiles(), ", "));

        TermStat termStat = new TermStat(conf);
        termStat.open(dumpLocation);
        String FIXED = "fixed:fixedcontent";
        assertEquals("The termcount for " + FIXED + " should be numDocs",
                     100, termStat.getTermCount(FIXED));

        String VARIABLE_A = "variableshareda:variablecontent1";
        assertEquals("The termcount for " + VARIABLE_A
                     + " should be indexcount",
                     1, termStat.getTermCount(VARIABLE_A));
        termStat.close();
    }

    public void testMerge() throws Exception {
        generateIndex(100, new File(TEST_DIR, "index_a"));
        generateIndex(300, new File(TEST_DIR, "index_b"));

        Configuration conf = Configuration.newMemoryBased();
        TermStatExtractor extractor = new TermStatExtractor(conf);

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
                     400, termStat.getTermCount(FIXED));

        String VARIABLE_A = "variableshareda:variablecontent1";
        assertEquals("The termcount for " + VARIABLE_A 
                     + " should be indexcount",
                     2, termStat.getTermCount(VARIABLE_A));
        termStat.close();
    }

    public static final File TEST_DIR = new File(
            "Common/tmp/", "termstats");
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
                new StandardAnalyzer(Version.LUCENE_30),
                true, new IndexWriter.MaxFieldLength(10000));
        for (int i = 0 ; i < docCount ; i++) {
            Document doc = new Document();
            doc.add(new Field("fixed", "fixedcontent",
                              Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new Field("duplicatefixed", "hello world hello world",
                              Field.Store.YES, Field.Index.ANALYZED));

            doc.add(new Field("duplicatemixed",
                              "hello" + i + " world hello" + i + " world",
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

