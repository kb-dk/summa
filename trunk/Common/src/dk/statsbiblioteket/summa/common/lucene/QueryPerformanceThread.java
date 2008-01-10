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
package dk.statsbiblioteket.summa.common.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import dk.statsbiblioteket.summa.common.lucene.index.IndexConnector;
import dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.TokenMgrError;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class QueryPerformanceThread extends Thread {
    private String[] queries;
    private int startPos;
    private int endPos;
    private IndexSearcher searcher;
    private SummaQueryParser queryParser;


    private static Profiler profiler;
    private static int feedback;
    private static AtomicLong hitCount = new AtomicLong();
    private static AtomicInteger docCount = new AtomicInteger();
    private static long startTime;

    public static void test(int threadCount, String[] queries,
                            IndexConnector connector,
                            SearchDescriptor descriptor) throws IOException {
        List<QueryPerformanceThread> performanceThreads =
                new ArrayList<QueryPerformanceThread>(threadCount);
        hitCount.set(0);
        docCount.set(0);

        int sliceSize = queries.length / threadCount;
        for (int i = 0 ; i < threadCount ; i++) {
            SummaQueryParser queryParser = new SummaQueryParser(new String[]{},
                                              new SimpleAnalyzer(), descriptor);
            queryParser.setDefaultFields(("au author_normalized su lsubj ti "
                                          + "freetext sort_title").split(" "));
            IndexSearcher searcher = connector.getSearcher();

            QueryPerformanceThread performanceThread =
                    new QueryPerformanceThread(queries, i * sliceSize,
                                               (i+1) * sliceSize,
                                               searcher,
                                               queryParser);
            performanceThreads.add(performanceThread);
        }

        feedback = Math.min(Math.max(10, queries.length / 100), 100);
        profiler = new Profiler();
        profiler.setBpsSpan(500);
        profiler.setExpectedTotal(queries.length);
        startTime = System.currentTimeMillis();

        for (QueryPerformanceThread performanceThread: performanceThreads) {
            performanceThread.start();
        }
        for (QueryPerformanceThread performanceThread: performanceThreads) {
            try {
                synchronized(performanceThread) {
                    performanceThread.join();
                }
            } catch (InterruptedException e) {
                System.err.println("Exception waiting for performance thread");
                e.printStackTrace();
            }
        }
        System.out.println("Tested " + queries.length
                           + " queries (" + hitCount.get() + " hits). In "
                           + (System.currentTimeMillis() - startTime)
                           / 1000 + " seconds. "
                           + "Average queries/second: "
                           + QueryPerformance.round(profiler.getBps(false))
                           + ". Total time used: " + profiler.getSpendTime());
    }

    private QueryPerformanceThread(String[] queries, int startPos, int endPos,
                                  IndexSearcher searcher,
                                  SummaQueryParser queryParser) {
        this.queries = queries;
        this.startPos = startPos;
        this.endPos = endPos;
        this.searcher = searcher;
        this.queryParser = queryParser;
    }

    private static synchronized void feedback(String message) {
        System.out.println(message);
    }

    public void run() {
        try {
            test();
        } catch (Exception e) {
            System.err.println("Exception running performance thread for query "
                               + "slice " + startPos + " to " + endPos);
            e.printStackTrace();
        }
    }

    private static void ping(long hitCount) {
        QueryPerformanceThread.hitCount.addAndGet(hitCount);
        profiler.beat();
        if (docCount.incrementAndGet() % feedback == 0) {
            feedback((System.currentTimeMillis() - startTime)
                               / 1000 + " sec. " + docCount.get() + "/"
                               + profiler.getExpectedTotal()
                               + ". Hits: "
                               + QueryPerformanceThread.hitCount.get()
                               + ". Q/sec: "
                        + QueryPerformance.round(profiler.getBps(true))
                               + " ("
                        + QueryPerformance.round(profiler.getBps(false))
                               + " total). ETA: "
                               + profiler.getETAAsString(true));
        }
    }

    public void test() throws IOException {
        for (int i = startPos ; i < endPos ; i++) {
            String query = queries[i];
            try {
                ping(test(query));
            } catch(Exception e) {
                System.err.println("Exception doing query '" + query + "'");
                e.printStackTrace();
                System.err.println("Continuing...");
            }
        }
    }

    public int test(String query) {
        if ("".equals(query)) {
            return 0;
        }
        try {
            Hits hits = searcher.search(queryParser.parse(query));
            Iterator iterator = hits.iterator();
            int counter = 0;
            while (counter++ < 20 && iterator.hasNext()) {
                Hit hit = (Hit)iterator.next();
                hit.get("shortformat");

            }
            return hits.length();
        } catch(ParseException e) {
            System.err.println("Error parsing '" + query + "'");
            return -1;
        } catch (IOException e) {
            System.err.println("IOException handling '" + query + "': "
                               + e.getMessage());
            return -1;
        } catch (TokenMgrError e) {
            System.err.println("Query parser error for '" + query + "': "
                               + e.getMessage());
            return -1;
        } catch (Exception e) {
            System.err.println("Error parsing '" + query + "': "
                               + e.getMessage());
            return -1;
        }
    }

}
