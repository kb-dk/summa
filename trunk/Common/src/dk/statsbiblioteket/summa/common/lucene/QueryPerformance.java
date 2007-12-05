/* $Id: QueryPerformance.java,v 1.7 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.7 $
 * $Date: 2007/10/04 13:28:20 $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: QueryPerformance.java,v 1.7 2007/10/04 13:28:20 te Exp $
 */
package dk.statsbiblioteket.summa.common.lucene;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;

import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor;
import dk.statsbiblioteket.summa.common.lucene.index.IndexConnector;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Hit;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.TokenMgrError;

/**
 * Connects to an index and a list of queries. Steps through all the queries
 * and performs searches for them, iterating through the first 20 Hits and
 * extracting shortrecord.
 * </p><p>
 * If no queryfile is given, the program times queries entered by the user.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class QueryPerformance {
    private File indexLocation;
    private SearchDescriptor descriptor;
    private SummaQueryParser queryParser;
    private Configuration configuration;
    private IndexConnector connector;
    private IndexSearcher searcher;
    private static BufferedReader in =
            new BufferedReader(new InputStreamReader(System.in));

    public QueryPerformance(File indexLocation, boolean useRAMIndex) {
        try {
            this.indexLocation = indexLocation;
            descriptor = new SearchDescriptor(indexLocation.toString());
            descriptor.loadDescription(indexLocation.toString());

            queryParser = new SummaQueryParser(new String[]{},
                                              new SimpleAnalyzer(), descriptor);
/*        String defaultFields = p.getProperty(
                dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser.
                        DEFAULT_FIELDS).split(" ");*/
            queryParser.setDefaultFields(("au author_normalized su lsubj ti "
                                          + "freetext sort_title").split(" "));

            MemoryStorage memStore = new MemoryStorage();
            if (useRAMIndex) {
                memStore.put(IndexConnector.INDEXROOT + IndexConnector.TYPE,
                             IndexConnector.INDEXTYPE.ramIndex);
            } else {
                memStore.put(IndexConnector.INDEXROOT + IndexConnector.TYPE,
                             IndexConnector.INDEXTYPE.singleIndex);
            }
            memStore.put(IndexConnector.INDEXROOT + IndexConnector.LINKS,
                         indexLocation.toString());
            configuration = new Configuration(memStore);
            connector = new IndexConnector(configuration);
            searcher = connector.getSearcher();
        } catch (Exception e) {
            throw new RuntimeException("Could not connect to index at '"
                                       + indexLocation + "'", e);
        }
        }

    private String round(double v) {
        return Double.toString(Math.round(v * 10) / 10.0);
    }

    public void test(String[] queries) throws IOException {
        Profiler profiler = new Profiler();
        profiler.setBpsSpan(500);
        profiler.setExpectedTotal(queries.length);
        int feedback = Math.min(Math.max(10, queries.length / 100), 100);
        int counter = 0;
        long totalhits = 0;
        long startTime = System.currentTimeMillis();
        for (String query: queries) {
            try {
                totalhits += test(query);
                profiler.beat();
                if (counter++ % feedback == 0) {
                    System.out.println((System.currentTimeMillis() - startTime)
                                       / 1000 + " sec. " + counter + "/"
                                       + queries.length
                                       + ". Hits: " + totalhits
                                       + ". Q/sec: "
                                       + round(profiler.getBps(true))
                                       + " ("
                                       + round(profiler.getBps(false))
                                       + " total). ETA: "
                                       + profiler.getETAAsString(true));
                }
            } catch(Exception e) {
                System.err.println("Exception doing query");
                e.printStackTrace();
                System.err.println("Continuing...");
            }
        }
        System.out.println("Tested " + queries.length
                           + " queries (" + totalhits + " hits). In "
                           + (System.currentTimeMillis() - startTime) 
                           / 1000 + " seconds. "
                           + "Average queries/second: "
                           + round(profiler.getBps(false))
                           + ". Total time used: " + profiler.getSpendTime());
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

    /**
     * If a query file is given, performance is tested by performing searches
     * for all queries, as described for this class.<br />
     * If no query file, the program switches to interactive mode.<br />
     * If the argument <code>-r</code> is given, the index is loaded into RAM.
     * Note that this requires as much memory as the index requires disk space.
     */
    public static void main(String[] args) throws IOException {
        if (!(args.length >= 1 && args.length <= 3)) {
            System.err.println("Usage: QueryPerformance [-r] indexlocation "
                               + "[queryfile]");
            System.exit(-1);
        }
        QueryPerformance tester;
        if ("-r".equals(args[0])) {
            tester = new QueryPerformance(new File(args[1]), true);
        } else {
            tester = new QueryPerformance(new File(args[0]), false);
        }
        if (args.length == 3 || args.length == 2 && !"-r".equals(args[0])) {
            String[] queries;
            if ("-r".equals(args[0])) {
                queries = Files.loadString(new File(args[2])).split("\n");
            } else {
                queries = Files.loadString(new File(args[1])).split("\n");
            }
            tester.test(queries);
        } else {
            tester.interactive();
        }
    }

    private void interactive() throws IOException {
        Profiler profiler = new Profiler();
        while(true) {
            System.out.print("Enter a query, HITS divisions query or QUIT: ");
            String query = in.readLine();
            if ("QUIT".equals(query)) {
                break;
            } else if (query.startsWith("HITS")) {
                query = query.substring(5, query.length());
                String[] tokens = query.split(" ", 2);
                int divisions = Integer.parseInt(tokens[0]);
                query = tokens[1];
                hitPerformance(query, divisions);
            } else {
                profiler.reset();
                int hitcount = test(query);
                System.out.println("Got " + hitcount + " hits in "
                                   + profiler.getSpendTime());
            }
        }
    }

    /**
     * Performs a search, the iterates through the hits, extracting shortformat
     * every (hits.total / divisions) hits.
     */
    private void hitPerformance(String query, int divisions)
            throws IOException {
//        System.out.println("Iterating through all hits for '"
//                           + query + "'");
        Profiler profiler1 = new Profiler();
        Profiler profiler2 = new Profiler();
        Hits hits;
        try {
            profiler1.reset();
            hits = searcher.search(queryParser.parse(query));
        } catch (ParseException e) {
            System.err.println("Exception getting hits for '" + query + "': ");
            e.printStackTrace();
            return;
        }
        assert hits != null;
//        System.out.println("Got " + hits.length() + " in "
//                           + profiler1.getSpendTime());
        int every = hits.length() / divisions;
/*        Iterator iterator = hits.iterator();
        for (int i = 0 ; i < hits.length() ; i++) {
            Hit hit = (Hit)iterator.next();
            if (i % every == 0) {
                hit.get("shortformat");
            }
        }*/
        for (int i = 0 ; i < hits.length() ; i += every) {
            hits.doc(i).getField("shortformat");
        }
        System.out.println("Got shortformat for "
                           + Math.min(hits.length(), divisions)
                           + "/" + hits.length() + " hits in "
                           + profiler2.getSpendTime());
        System.out.println("Total time: " + profiler1.getSpendTime());
    }

}
