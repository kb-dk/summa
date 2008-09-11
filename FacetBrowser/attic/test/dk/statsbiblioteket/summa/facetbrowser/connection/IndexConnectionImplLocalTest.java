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
package dk.statsbiblioteket.summa.facetbrowser.connection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.index.TermFreqVector;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import dk.statsbiblioteket.summa.facetbrowser.IndexBuilder;
import dk.statsbiblioteket.summa.facetbrowser.util.ClusterCommon;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * IndexConnectionImplLocal Tester.
 *
 * There are a lot of performance test methods in this class. These have to
 * be started individually and manually.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection", "JavaDoc"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexConnectionImplLocalTest extends TestCase {
    private static int MAXHITS = 10000;
    /* Nanoseconds per millisecond */
    private static double NANOPERMS = 1000000.0;

    public IndexConnectionImplLocalTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        /* Set up a Lucene testindex */
        IndexBuilder.checkIndex();
    }
    public void tearDown() throws Exception {
        super.tearDown();
        // No teardown of the index, as it is too large to rebuild each time
        // we test.
    }

    /**
     * Test retrieving the topresults through IndexConnection.
     */
    public void testGetTopResults() throws Exception  {
        //TODO: searchDesc.xml (the following only works with default add to genre)
        String queryString = String.format("%s:fantasy", IndexBuilder.GENRE);
        IndexConnectionImplLocal connection =
                new IndexConnectionImplLocal(IndexBuilder.INDEX_LOCATION);
        TopDocs topDocs = connection.getTopResults(queryString, MAXHITS);
        assertEquals("Search for " + queryString + "; " + MAXHITS + " top " +
                     "results; total results = 3*REPLICATIONCOUNT",
                     3*IndexBuilder.REPLICATIONCOUNT, topDocs.totalHits);

        Document doc = connection.getDoc(topDocs.scoreDocs[0].doc);
        assertNotNull("Search for "+queryString +"; top result", doc);

        // Loop through the top docs using getDoc in index connection
        int topHitCount = Math.min(topDocs.totalHits, MAXHITS);
        for (int index=0; index < topHitCount; index++) {
            connection.getDoc(topDocs.scoreDocs[index].doc).get(IndexBuilder.TITLE);
        }

        // Loop through the top docs using the searcher directly
        Searcher searcher = connection.getSearcher();
        try {
            for (int index1 =0; index1 < topHitCount; index1++) {
                searcher.doc(topDocs.scoreDocs[index1].doc).get(IndexBuilder.TITLE);
            }
        } catch (IOException e) {
            System.out.println(e);
        }

        // Use Hits through index connection
        Hits hits = connection.getResults(queryString);
        assertTrue("There should be some hits ", hits.length() > 0);

        // Loop through the top docs using Hits
        try {
            for (int index2 = 0; index2 < topHitCount; index2++) {
                hits.doc(index2).get(IndexBuilder.TITLE);
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        connection.disconnect();
    }

    /**
     * Test IndexConnectionImplLocal against a large scale index.
     * Non-verbose version.
     */
    public void fullIndexTest() {
        for (int i = 1 ; i <= 5 ; i++) {
            System.out.println("*** Iteration " + i);
            fullIndexTest(false);
        }
    }

    public void dumpFieldsAndTerms() throws Exception {
//        IndexConnectionImplLocal connection = new IndexConnectionImplLocal();
//        new LuceneTesting().dumpFields(connection.getSearcher());
    }

    /*
    public void dumpFieldsAndTermsLimited() throws Exception {
        IndexConnectionImplLocal connection = new IndexConnectionImplLocal();
        LinkedList<Pair<String, LinkedList<ReversePair<Integer, String>>>> list;
        list = new ClusterCreator().getClusterCandidatesFGAC(
                50, 20,
                                                         connection.getSearcher().maxDoc(),
                                                         1, 2000, 500000, 10,
                                                         null, null, null, null, true);
        ClusterCreatorTest.printClusters(list);
    }
      */
    /**
     * Test IndexConnectionImplLocal against a large scale index.
     * Verbose version.
     */
    public void fullIndexTestVerbose() {
        fullIndexTest(true);
    }

    public void performanceHitsVsDocImplLocal() throws Exception {
        performanceHitsVsDoc(new IndexConnectionImplLocal());
    }
    /**
    From Toke's local machine.
*** Iteration 15/15 ***
Hit search used an average of 12.1378 ms
Top search used an average of 31.8666 ms

Hit getdoc used an average of 3810.3078 ms
Hit condoc used an average of 3965.9182 ms
Hit hiDoc2c used an average of 3822.7531 ms
Hit condoB used an average of 3841.68 ms

Top getdoc used an average of 3199.758 ms
Top getdo2 used an average of 3318.3759 ms

Hit field1 used an average of 3821.6142 ms
Top field1 used an average of 3194.4727 ms

Hit loop01 used an average of 92.8383 ms
Top loop01 used an average of 0.0453 ms
    */
    public void performanceHitsVsDocImplSumma() throws Exception {
        performanceHitsVsDoc(new IndexConnectionImplSumma());
    }
    /**
     * Performs several runs of searching and document requesting using
     * Hits and TopDocs.
     */
    @SuppressWarnings({"UnusedAssignment"})
    private void performanceHitsVsDoc(IndexConnection connection) throws
                                                                  Exception {
        String queryString = "Nielsen";
        int maxHits = 1000;
        int skipFirst = 5;
        int totalRuns = 15;

        long hsTime = 0;
        long tsTime = 0;
        long hiTime = 0;
        long tiTime = 0;
        long hi2Time = 0;
        long hi2bTime = 0;
        long ti2Time = 0;

        long htermV = 0;
        long ttermV = 0;

//        long htermcount = 0;
//        long ttermcount = 0;

        long hfTime = 0;
        long tfTime = 0;
        long hiDoc2Time = 0;

        long hloopTime = 0;
        long tloopTime = 0;

        for (int i = 1 ; i <= totalRuns ; i++) {
            System.out.println("*** Iteration " + i + "/" + totalRuns + " ***");
            System.gc();

            long startTime = System.nanoTime();
            Hits hits = connection.getResults(queryString);
            long endTime = System.nanoTime();
            if (i > skipFirst) { hsTime += endTime-startTime; }

            System.gc();
            startTime = System.nanoTime();
            TopDocs topDocs = connection.getTopResults(queryString, maxHits);
            endTime = System.nanoTime();
            if (i > skipFirst) { tsTime += endTime-startTime; }

            hits = connection.getResults(queryString);
            System.gc();
            int topHitCount = Math.min(maxHits, hits.length());
            startTime = System.nanoTime();
            for (int index=0; index < topHitCount; index++) {
                hits.doc(index);
            }
            endTime = System.nanoTime();
            if (i > skipFirst) { hiTime += endTime-startTime; }

            topDocs = connection.getTopResults(queryString, maxHits);
            System.gc();
            startTime = System.nanoTime();
            for (int index=0; index < topHitCount; index++) {
                connection.getDoc(topDocs.scoreDocs[i].doc);
            }
            endTime = System.nanoTime();
            if (i > skipFirst) { tiTime += endTime-startTime; }

            hits = connection.getResults(queryString);
            topHitCount = Math.min(maxHits, hits.length());
            System.gc();
            startTime = System.nanoTime();
            for (int index=0; index < topHitCount; index++) {
                hits.id(index);
            }
            endTime = System.nanoTime();
            if (i > skipFirst) { hloopTime += endTime-startTime; }

            topDocs = connection.getTopResults(queryString, maxHits);
            System.gc();
            startTime = System.nanoTime();
            for (int index=0; index < topHitCount; index++) {
                connection.getDoc(topDocs.scoreDocs[i].doc);
            }
            endTime = System.nanoTime();
            if (i > skipFirst) { ti2Time += endTime-startTime; }

            //noinspection UnusedDeclaration
            int dummy;
            topDocs = connection.getTopResults(queryString, maxHits);
            System.gc();
            startTime = System.nanoTime();
            for (int index=0; index < topHitCount; index++) {
                //noinspection UnusedAssignment
                dummy = topDocs.scoreDocs[i].doc;
            }
            endTime = System.nanoTime();
            if (i > skipFirst) { tloopTime += endTime-startTime; }

            topDocs = connection.getTopResults(queryString, maxHits);
            System.gc();
            startTime = System.nanoTime();
            for (int index=0; index < topHitCount; index++) {
                connection.getTermFreqVector(topDocs.scoreDocs[i].doc,
                                             ClusterCommon.FREETEXT);
            }
            endTime = System.nanoTime();
            if (i > skipFirst) { ttermV += endTime-startTime; }

            hits = connection.getResults(queryString);
            System.gc();
            startTime = System.nanoTime();
            for (int index=0; index < topHitCount; index++) {
                connection.getDoc(hits.id(index));
            }
            endTime = System.nanoTime();
            if (i > skipFirst) { hi2Time += endTime-startTime; }

            hits = connection.getResults(queryString);
            System.gc();
            startTime = System.nanoTime();
            for (int index=0; index < topHitCount; index++) {
                connection.getDoc(hits.id(index));
            }
            endTime = System.nanoTime();
            if (i > skipFirst) { hi2bTime += endTime-startTime; }

            hits = connection.getResults(queryString);
            System.gc();
            startTime = System.nanoTime();
            for (int index=0; index < topHitCount; index++) {
                connection.getTermFreqVector(hits.id(i),
                                             ClusterCommon.FREETEXT);
            }
            endTime = System.nanoTime();
            if (i > skipFirst) { htermV += endTime-startTime; }

            hits = connection.getResults(queryString);
            System.gc();
            startTime = System.nanoTime();
            for (int index=0; index < topHitCount; index++) {
                hits.doc(index).getField(ClusterCommon.SHORTFORMAT);
            }
            endTime = System.nanoTime();
            if (i > skipFirst) { hfTime += endTime-startTime; }

            hits = connection.getResults(queryString);
            System.gc();
            startTime = System.nanoTime();
            for (int index=0; index < topHitCount; index++) {
                hits.doc(index);
            }
            endTime = System.nanoTime();
            if (i > skipFirst) { hiDoc2Time += endTime-startTime; }

            topDocs = connection.getTopResults(queryString, maxHits);
            System.gc();
            startTime = System.nanoTime();
            for (int index=0; index < topHitCount; index++) {
                connection.getDoc(topDocs.scoreDocs[i].doc).
                        getField(ClusterCommon.SHORTFORMAT);
            }
            endTime = System.nanoTime();
            if (i > skipFirst) { tfTime += endTime-startTime; }

        }

        double divider = (totalRuns-skipFirst) * NANOPERMS;
        System.out.println(String.format("Hit search used an average of %s ms",
                                         hsTime / divider));
        System.out.println(String.format("Top search used an average of %s ms\n",
                                         tsTime / divider));

        System.out.println(String.format("Hit getdoc used an average of %s ms",
                                         hiTime / divider));
        System.out.println(String.format("Hit condoc used an average of %s ms",
                                         hi2Time / divider));
        System.out.println(String.format("Hit hiDoc2 used an average of %s ms",
                                         hiDoc2Time / divider));
        System.out.println(String.format("Hit condoB used an average of %s ms",
                                         hi2bTime / divider));
        System.out.println(String.format("Top getdoc used an average of %s ms",
                                         tiTime / divider));
        System.out.println(String.format("Top getdo2 used an average of %s ms\n",
                                         ti2Time / divider));

        System.out.println(String.format("Hit field1 used an average of %s ms",
                                         hfTime / divider));
        System.out.println(String.format("Top field1 used an average of %s ms\n",
                                         tfTime / divider));

        System.out.println(String.format("Hit loop01 used an average of %s ms",
                                         hloopTime / divider));
        System.out.println(String.format("Top loop01 used an average of %s ms\n",
                                         tloopTime / divider));

        System.out.println(String.format("Hit termVe used an average of %s ms",
                                         htermV / divider));
        System.out.println(String.format("Top termVe used an average of %s ms",
                                         ttermV / divider));
    }

    /**
     * Test IndexConnectionImplLocal against a large scale index.
     * The location of the index is given in cluster.properties.xml.
     */
    private void fullIndexTest(boolean verbose) {
//        String queryString = "Andersen";
//        String queryString = "Pedersen";
//        String queryString = "Jensen";
        String queryString = "Nielsen";

        long startTime = System.nanoTime();
        IndexConnectionImplLocal connection = new IndexConnectionImplLocal();
        long endTime = System.nanoTime();
        System.out.println(String.format("New index connection in %s ms.",
                                         (endTime - startTime) / NANOPERMS));

        startTime = System.nanoTime();
        TopDocs topDocs = connection.getTopResults(queryString, MAXHITS);
        endTime = System.nanoTime();
        System.out.println(String.format("Found %d/%d/millions? docs in %s ms.",
                                         MAXHITS, topDocs.totalHits, (endTime - startTime) / NANOPERMS));

        assertNotNull("Assert query result not null.", topDocs);
        assertTrue("Assert positive number of results", topDocs.totalHits>0);

        Document doc;
        if (verbose) {
            doc = connection.getDoc(topDocs.scoreDocs[0].doc);
            System.out.println("doc.get(\"shortformat\") = " + doc.get(ClusterCommon.SHORTFORMAT));
            List fields = doc.getFields();
            for (Object field: fields) {
                System.out.println(field);
            }
        }

        startTime = System.nanoTime();
        int topHitCount = Math.min(topDocs.totalHits, MAXHITS);
        for (int index=0; index < topHitCount; index++) {
            connection.getDoc(topDocs.scoreDocs[index].doc).get(ClusterCommon.SHORTFORMAT);
//            doc.get("shortformat");
        }
        endTime = System.nanoTime();
        System.out.println(String.format("Looped through %d docs in %s ms" +
                                         " using getDoc + get(shortformat).",
                                         topHitCount,
                                         (endTime - startTime) / NANOPERMS));


        startTime = System.nanoTime();
        for (int index = 0; index<topHitCount; index++) {
            connection.getDoc(topDocs.scoreDocs[index].doc);
        }
        endTime = System.nanoTime();
        System.out.println(String.format("Looped through %d docs in %s ms" +
                                         " using getDoc.", topHitCount,
                                                           (endTime - startTime) / NANOPERMS));

        startTime = System.nanoTime();
        boolean failedGetShort = false;
        for (int index = 0; index < topHitCount; index++) {
            if (connection.getShortFormatTerms(topDocs.scoreDocs[index].doc) ==
                null) {
                failedGetShort = true;
            }
        }
        endTime = System.nanoTime();
        if (failedGetShort) {
            System.out.println("Failed loop with getShortFormatTerms");
        } else {
            System.out.println(String.format("Looped through %d docs in %s ms" +
                                             " using getShortFormatTerms.",
                                             topHitCount,
                                             (endTime - startTime) / NANOPERMS));
        }

        // TODO: Try to extract without using TermVector or doc.shortFormat
        if (verbose) {
            TermFreqVector[] tfv;
            for (int index=0; index<1; index++) {
                System.out.println();
                tfv = connection.getTermFreqVectors(topDocs.scoreDocs[index].doc);
                for (TermFreqVector aTfv : tfv) {
                    System.out.println("aTfv.getField() = " + aTfv.getField() +
                                       "; aTfv.getTerms() = " + Arrays.toString(aTfv.getTerms()));
                }
            }
        }
    }

    /**
     * Test performance of TopDocs vs. Hits and direct access vs. IndexConnection.
     */
    public void performanceCombined() {
        for (int i = 0 ; i < 5 ; i++) {
            System.out.println("********** Iteration " + i + " **********");
            String queryString = "Deleurant";
            System.out.println(String.format("queryString = %s", queryString));
            performanceDirect(queryString);
            performanceIndexConnection(queryString);
            queryString = String.format("%s:fantasy", IndexBuilder.GENRE);
            System.out.println(String.format("queryString = %s", queryString));
            performanceDirect(queryString);
            performanceIndexConnection(queryString);
        }
    }
    private void performanceDirect(String queryString) {
        directUsingHits(queryString);
        directUsingTopDocs(queryString);
    }
    private void directUsingTopDocs(String queryString) {
        System.out.println("directUsingTopDocs");
        //try using TopDocs directly
        IndexSearcher is;
        try {
            is = new IndexSearcher(IndexBuilder.INDEX_LOCATION);
        } catch (IOException e) {
            System.out.println("e = " + e);
            return;
        }
        QueryParser parser = new QueryParser(IndexBuilder.AUTHOR,
                                             new StandardAnalyzer());
        Query query = null;
        try {
            query = parser.parse(queryString);
        } catch (ParseException e) {
            System.out.println("e = " + e);
        }

        long startTime = System.nanoTime();
        TopDocs topDocs;
        try {
            topDocs = is.search(query, null, MAXHITS);
        } catch (IOException e) {
            System.out.println("e = " + e);
            return;
        }
        long endTime = System.nanoTime();

        System.out.println(String.format("Found %d/%d/%d docs in %s ms " +
                                         "using TopDocs", MAXHITS, topDocs.totalHits,
                                                          IndexBuilder.comics.length*IndexBuilder.REPLICATIONCOUNT,
                                                          (endTime - startTime) / NANOPERMS));

        // try extracting the genre for the top docs
        startTime = System.nanoTime();
        int topHitCount = Math.min(topDocs.totalHits, MAXHITS);
        try {
            for (int index=0; index < topHitCount; index++) {
                is.doc(topDocs.scoreDocs[index].doc).get(IndexBuilder.GENRE);
            }
        } catch (IOException e) {
            System.out.println("e = " + e);
        }
        endTime = System.nanoTime();

        System.out.println(String.format("Looped through top %d docs in %s ms",
                                         topHitCount,
                                         (endTime - startTime) / NANOPERMS));

        try {
            is.close();
        } catch (IOException e) {
            System.out.println("e = " + e);
        }
    }
    private void directUsingHits(String queryString) {
        System.out.println("directUsingHits");
        // First we perform a standard search
        IndexSearcher is;
        try {
            is = new IndexSearcher(IndexBuilder.INDEX_LOCATION);
        } catch (IOException e) {
            System.out.println("e = " + e);
            return;
        }
        QueryParser parser = new QueryParser(IndexBuilder.AUTHOR,
                                             new StandardAnalyzer());
        Query query = null;
        try {
            query = parser.parse(queryString);
        } catch (ParseException e) {
            System.out.println("e = " + e);
        }

        long startTime = System.nanoTime();
        Hits hits;
        try {
            hits = is.search(query);
        } catch (IOException e) {
            System.out.println("e = " + e);
            return;
        }
        long endTime = System.nanoTime();

        System.out.println(String.format("Found %d/%d docs in %s ms",
                                         hits.length(),
                                         IndexBuilder.comics.length*IndexBuilder.REPLICATIONCOUNT,
                                         (endTime - startTime) / NANOPERMS));

        // Then we try to extract the genre for the top docs
        int topHitCount = Math.min(hits.length(), MAXHITS);
        startTime = System.nanoTime();
        try {
            for (int index=0; index < topHitCount; index++) {
                hits.doc(index).get(IndexBuilder.GENRE);
            }
        } catch (IOException e) {
            System.out.println("e = " + e);
        }
        endTime = System.nanoTime();

        System.out.println(String.format("Looped through top %d docs " +
                                         "in %s ms", topHitCount,
                                                     (endTime - startTime) / NANOPERMS));

        try {
            is.close();
        } catch (IOException e) {
            System.out.println("e = " + e);
        }
    }
    public void performanceIndexConnection(String queryString) {
        //TODO: searchDesc.xml (the following only works if you search for an author)
        usingTopDocs(queryString);
        usingTopDocsV2(queryString);
        usingHits(queryString);
    }
    private void usingHits(String queryString) {
        System.out.println("usingHits");
        IndexConnectionImplLocal connection =
                new IndexConnectionImplLocal(IndexBuilder.INDEX_LOCATION);

        // Use Hits through index connection
        long startTime = System.nanoTime();
        Hits hits = connection.getResults(queryString);
        long endTime = System.nanoTime();

        System.out.println(String.format("Found %d/%d docs in %s ms " +
                                         "using Hits from IndexConnection", hits.length(),
                                                                            IndexBuilder.comics.length*IndexBuilder.REPLICATIONCOUNT,
                                                                            (endTime - startTime) / NANOPERMS));

        // Loop through the top docs using Hits
        startTime = System.nanoTime();
        int topHitCount = Math.min(hits.length(), MAXHITS);
        try {
            for (int index=0; index < topHitCount; index++) {
                hits.doc(index).get(IndexBuilder.TITLE);
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        endTime = System.nanoTime();

        System.out.println(String.format("Looped through top %d docs " +
                                         "in %s ms", topHitCount,
                                                     (endTime - startTime) / NANOPERMS));

        connection.disconnect();
    }
    private void usingTopDocsV2(String queryString) {
        System.out.println("usingTopDocsV2");
        IndexConnectionImplLocal connection =
                new IndexConnectionImplLocal(IndexBuilder.INDEX_LOCATION);

        long startTime = System.nanoTime();
        TopDocs topDocs = connection.getTopResults(queryString, MAXHITS);
        long endTime = System.nanoTime();

        System.out.println(String.format("Found %d/%d/%d docs in %s ms " +
                                         "using IndexConnection", MAXHITS, topDocs.totalHits,
                                                                  IndexBuilder.comics.length*IndexBuilder.REPLICATIONCOUNT,
                                                                  (endTime - startTime) / NANOPERMS));

        // Loop through the top docs using the searcher directly
        startTime = System.nanoTime();
        Searcher searcher = connection.getSearcher();
        int topHitCount = Math.min(topDocs.totalHits, MAXHITS);
        try {
            for (int index=0; index < topHitCount; index++) {
             searcher.doc(topDocs.scoreDocs[index].doc).get(IndexBuilder.TITLE);
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        endTime = System.nanoTime();

        System.out.println(String.format("Looped through top %d docs " +
                                         "in %s ms using searcher directly",
                                         topHitCount,
                                         (endTime - startTime) / NANOPERMS));

        connection.disconnect();
    }
    private void usingTopDocs(String queryString) {
        System.out.println("usingTopDocs");
        IndexConnectionImplLocal connection =
                new IndexConnectionImplLocal(IndexBuilder.INDEX_LOCATION);

        long startTime = System.nanoTime();
        TopDocs topDocs = connection.getTopResults(queryString, MAXHITS);
        long endTime = System.nanoTime();

        System.out.println(String.format("Found %d/%d/%d docs in %s ms " +
                                         "using IndexConnection", MAXHITS, topDocs.totalHits,
                                                                  IndexBuilder.comics.length*IndexBuilder.REPLICATIONCOUNT,
                                                                  (endTime - startTime) / NANOPERMS));

        // Loop through the top docs using getDoc in index connection
        startTime = System.nanoTime();
        int topHitCount = Math.min(topDocs.totalHits, MAXHITS);
        for (int index=0; index < topHitCount; index++) {
            connection.getDoc(topDocs.scoreDocs[index].doc).get(IndexBuilder.TITLE);
        }
        endTime = System.nanoTime();

        System.out.println(String.format("Looped through top %s docs " +
                                         "in %s ms using IndexConnection",
                                         topHitCount,
                                         (endTime - startTime) / NANOPERMS));

        connection.disconnect();
    }

    public static Test suite() {
        return new TestSuite(IndexConnectionImplLocalTest.class);
    }

    public void testConnect() throws Exception {
        new IndexConnectionImplLocal(IndexBuilder.INDEX_LOCATION);
    }

    public void testDisconnect() throws Exception {
        IndexConnectionImplLocal connection;
        connection = new IndexConnectionImplLocal(IndexBuilder.INDEX_LOCATION,
                                                  IndexBuilder.AUTHOR);
        connection.disconnect();
    }
}


