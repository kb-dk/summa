/* $Id: SlimCollectorTest.java,v 1.9 2007/10/11 12:56:25 te Exp $
 * $Revision: 1.9 $
 * $Date: 2007/10/11 12:56:25 $
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
package dk.statsbiblioteket.summa.facetbrowser.connection;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.facetbrowser.IndexBuilder;
import dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.summa.common.lucene.search.SlimCollector;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Hits;

/**
 * Primarily speed-measurements of using a Collector when searching. The speed-
 * tests are not run as part of the normal Unit-tests and must be started
 * explicitly..
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SlimCollectorTest extends TestCase {
    public SlimCollectorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void dumpPerformance() throws Exception {
        int reruns = 3;
        int limit = 3700000;
        int requests = 100;


        Profiler pf = new Profiler();
        for (int rerun = 0 ; rerun < reruns ; rerun++) {
            System.gc();

            pf.reset();
            SlimCollector slim = new SlimCollector();
            for (int i = 0 ; i < limit ; i++) {
                slim.collect(i, 0.0f);
            }
            System.out.println("Collected " + limit +
                               " in " + pf.getSpendTime());

            pf.reset();
            slim.clean();
            for (int i = 0 ; i < limit ; i++) {
                slim.collect(i, 0.0f);
            }
            System.out.println("Collected " + limit +
                               " by reusing the previous collector" +
                               " in " + pf.getSpendTime());

            pf.reset();
            slim = new SlimCollector(limit);
            for (int i = 0 ; i < limit ; i++) {
                slim.collect(i, 0.0f);
            }
            System.out.println("Collected " + limit +
                               " with pre-allocated size of " + limit +
                               " in " + pf.getSpendTime());

            pf.reset();
            pf.setExpectedTotal(requests);
            for (int i = 0 ; i < requests ; i++) {
                slim.getDocumentIDsOversize();
                pf.beat();
            }
            System.out.println("Requested the result " + requests + " times" +
                               " in " + pf.getSpendTime() +
                               ". Average time: " + 1000 / pf.getBps(false) +
                               " ms/request");

            pf.reset();
            pf.setExpectedTotal(requests);
            for (int i = 0 ; i < requests ; i++) {
                slim.getDocumentIDsOversize();
                pf.beat();
            }
            System.out.println("Requested the oversize result " + requests
                               + " times" +
                               " in " + pf.getSpendTime() +
                               ". Average time: " + 1000 / pf.getBps(false) +
                               " ms/request");
        }
    }

    public void testSearch() throws Exception {
        SlimCollector slimCollector = new SlimCollector();
        IndexBuilder.checkIndex();
        SearchDescriptor descriptor =
                new SearchDescriptor(IndexBuilder.INDEXLOCATION);
        descriptor.loadDescription(IndexBuilder.INDEXLOCATION);

        SummaQueryParser queryParser =
                new SummaQueryParser(new String[]{"foo", "bar"},
                                     new SimpleAnalyzer(), descriptor);
        IndexSearcher searcher = new IndexSearcher(IndexBuilder.INDEXLOCATION);

        Query query = queryParser.parse(IndexBuilder.STATIC
                                        + ":" + IndexBuilder.STATIC_CONTENT);
        searcher.search(query, slimCollector);
        assertEquals("Searching for the static field should give all documents",
                     IndexBuilder.getDocumentCount(),
                     slimCollector.getDocumentCount());
    }

    public void dumpSearchSpeed() throws Exception {
        int warmup = 3;
        int reruns = 30;

        SlimCollector slimCollector = new SlimCollector();
        IndexBuilder.checkIndex();
        SearchDescriptor descriptor =
                new SearchDescriptor(IndexBuilder.INDEXLOCATION);
        descriptor.loadDescription(IndexBuilder.INDEXLOCATION);

        SummaQueryParser queryParser =
                new SummaQueryParser(new String[]{"foo", "bar"},
                                     new SimpleAnalyzer(), descriptor);
        IndexSearcher searcher = new IndexSearcher(IndexBuilder.INDEXLOCATION);

        for (int i = 0 ; i < warmup ; i++) {
            slimCollector.clean();
            Query query = queryParser.parse(IndexBuilder.STATIC
                                            + ":" + IndexBuilder.STATIC_CONTENT);
            searcher.search(query, slimCollector);
        }
        System.gc();
        Profiler pf = new Profiler();
        pf.setExpectedTotal(reruns);
        for (int i = 0 ; i < reruns ; i++) {
            slimCollector.clean();
            Query query = queryParser.parse(IndexBuilder.STATIC
                                           + ":" + IndexBuilder.STATIC_CONTENT);
            searcher.search(query, slimCollector);
            pf.beat();
        }
        System.out.println("Searched " + IndexBuilder.getDocumentCount()
                           + " documents with just as many hits "
                           + reruns + " times in an average of " 
                           + 1000 / pf.getBps(true) + " ms");

        pf.reset();
        pf.setExpectedTotal(reruns);
        for (int i = 0 ; i < reruns ; i++) {
            Query query = queryParser.parse(IndexBuilder.STATIC
                                           + ":" + IndexBuilder.STATIC_CONTENT);
            Hits hits = searcher.search(query);
            for (int h = 0 ; h < 20 ; h++) {
                hits.id(h);
            }
            pf.beat();
        }
        System.out.println("Searched " + IndexBuilder.getDocumentCount()
                           + " documents with just as many hits of which "
                           + "the first 20 was extracted "
                           + reruns + " times in an average of "
                           + 1000 / pf.getBps(true) + " ms");

    }

    public static Test suite() {
        return new TestSuite(SlimCollectorTest.class);
    }
}
