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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.lucene.index.IndexConnector;
import dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.TokenMgrError;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;

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
    private SearchDescriptor descriptor;
    private SummaQueryParser queryParser;
    private Configuration configuration;
    private IndexConnector connector;
    private IndexSearcher searcher;
    private static BufferedReader in =
            new BufferedReader(new InputStreamReader(System.in));

    public QueryPerformance(File indexLocation, boolean useRAMIndex) {
        // TODO: Re-implement this
/*        try {
            descriptor = new SearchDescriptor(indexLocation.toString());
            descriptor.loadDescription(indexLocation.toString());

            queryParser = new SummaQueryParser(new String[]{},
                                              new SimpleAnalyzer(), descriptor);
//        String defaultFields = p.getProperty(
//                dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser.
//                        DEFAULT_FIELDS).split(" ");
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
        } catch (Exception e) {
            throw new RuntimeException("Could not connect to index at '"
                                       + indexLocation + "'", e);
        }*/
        }

    public static String round(double v) {
        return Double.toString(Math.round(v * 10) / 10.0);
    }

    public void test(String[] queries, int threadCount, boolean simulate,
                     boolean uniqueSearchers, int maxHits) throws IOException {
        QueryPerformanceThread.test(threadCount, queries,
                                    connector, descriptor,
                                    simulate, uniqueSearchers, maxHits);
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
        if (!(args.length >= 1)) {
            System.err.println("Usage: QueryPerformance [-s] [-r]"
                               + " [-t threadcount] [-h maxHits] "
                               + "indexlocation [queryfile]");
            System.err.println("-s\tSimulate (parses queries, but skips "
                               + "searching)");
            System.err.println("-r\tLoad index into RAM");
            System.err.println("-t threadcount\tUse threadcount threads");
            System.err.println("-u\tUse unique searchers for each thread");
            System.err.println("-h maxHits\tThe number of hits to extract "
                               + "shortformat for");
            System.exit(-1);
        }
        int threadCount = 1;
        boolean simulate = false;
        boolean useRAM = false;
        boolean uniqueSearchers = false;
        String indexLocation = null;
        String queryfile = null;
        int maxHits = 20;

        List<String> arguments = new LinkedList<String>(Arrays.asList(args));
        while (arguments.size() > 0) {
            if ("-r".equals(arguments.get(0))) {
                useRAM = true;
                arguments.remove(0);
            } else if ("-s".equals(arguments.get(0))) {
                arguments.remove(0);
                simulate = true;
            } else if ("-u".equals(arguments.get(0))) {
                arguments.remove(0);
                uniqueSearchers = true;
            } else if ("-t".equals(arguments.get(0))) {
                arguments.remove(0);
                try {
                    threadCount = Integer.parseInt(arguments.get(0));
                } catch (NumberFormatException e) {
                    System.err.println("Expected threadCount (an integer)."
                                       + " Got '"
                                       + arguments.get(0) + "'. Exiting");
                    System.exit(-1);
                }
                arguments.remove(0);
            } else if ("-h".equals(arguments.get(0))) {
                arguments.remove(0);
                try {
                    maxHits = Integer.parseInt(arguments.get(0));
                } catch (NumberFormatException e) {
                    System.err.println("Expected maxHits (an integer). Got '"
                                       + arguments.get(0) + "'. Exiting");
                    System.exit(-1);
                }
                arguments.remove(0);
            } else if (indexLocation == null) {
                indexLocation = arguments.remove(0);
            } else if (queryfile == null) {
                queryfile = arguments.remove(0);
            } else {
                System.err.println("Unexpected token: '" + arguments.get(0)
                                   + "'. Exiting");
                System.exit(-1);
            }
        }

        QueryPerformance tester
                = new QueryPerformance(new File(indexLocation), useRAM);
        if (queryfile == null) {
            tester.interactive();
        } else {
            String[] queries
                    = Files.loadString(new File(queryfile)).split("\n");
            tester.test(queries, threadCount, simulate, uniqueSearchers,
                        maxHits);
        }
    }

    private void interactive() throws IOException {
        searcher = connector.getSearcher();
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



