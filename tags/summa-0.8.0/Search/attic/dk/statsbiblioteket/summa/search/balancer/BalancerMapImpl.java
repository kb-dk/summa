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
package dk.statsbiblioteket.summa.search.balancer;

import dk.statsbiblioteket.summa.search.SearchEngine;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.io.IOException;

import org.apache.commons.logging.Log;       import org.apache.commons.logging.LogFactory;

/**
 * The search engine load balancer distributes queries to a number of search engines.
 *
 * <p>This class implements the dk.statsbiblioteket.summa.search.balancer.Balancer
 * interface, which extends the dk.statsbiblioteket.summa.search.SearchEngine interface.</p>
 *
 * <p>Methods for adding and removing search engines to this balancer
 * are defined in the Balancer interface.</p>
 *
 * <p>The methods defined by the SearchEngine interface are requests or get methods
 * and can be performed by any one search engine. The balancer chooses one of the
 * registered search engines, forwards the request and returns the answer.</p>
 *
 * <p>This class uses HashMaps as data structure (hopefully the simple choice).</p>
 *
 * <p>TODO: Should the balancer implement SearchEngineWebService rather than SearchEngine?</p>
 *
 * <p>TODO: Should requests be queued as Lucene is not happy with parallel requests?</p>
 *
 * <p>TODO: An engine that cannot be reached, should be kept separately and tested periodically!</p>
 *
 * <p>This implementation has been postponed!</p>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class BalancerMapImpl implements Balancer {
    private static final Log log = LogFactory.getLog(BalancerMapImpl.class);

    //MAP FROM URLS TO SEARCH ENGINES (used in removeSearchModule)
    private Map<String, SearchEngine> urls_to_engines;
    //MAP FROM SEARCH ENGINES TO REQUEST COUNT
    private Map<SearchEngine, Integer> engines_to_loads;
    //MAP FROM SEARCH ENGINES TO URLS (used in reload method and error messages)
    private Map<SearchEngine, String> engines_to_urls;

    /**
     * Balancer constructor.
     */
    public BalancerMapImpl() {
        init();
    }
    /**
     * Initialise data structures.
     */
    private void init() {
        urls_to_engines = Collections.synchronizedMap(new HashMap<String, SearchEngine>());
        engines_to_loads = Collections.synchronizedMap(new HashMap<SearchEngine, Integer>());
        engines_to_urls = Collections.synchronizedMap(new HashMap<SearchEngine, String>());
        //remember synchronize over the three maps (the data structure)
        log.info("Balancer initialised");
    }

    /**
     * Register the search engine at the given url with the load balancer.
     */
    public void registerSearchModule(String url) {
        if (url==null) {return;}

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }

        try {
            SearchEngine engine = (SearchEngine) Naming.lookup(url);
            synchronized(this) {
                urls_to_engines.put(url, engine);
                engines_to_loads.put(engine, 0);//start with 0 load
                engines_to_urls.put(engine, url);
            }
            log.info("Engine at URL '"+url+"' registered with balancer.");
        } catch (NotBoundException e) {
            log.error("Engine at URL '"+url+"' NOT registered:\n\t"+e);
        } catch (IOException e) {
            log.error("Engine at URL '"+url+"' NOT registered:\n\t"+e);
        }
    }
    /**
     * Remove the search engine at the given url from the load balancer engine list.
     */
    public synchronized void removeSearchModule(String url) {
        if (url==null || !urls_to_engines.containsKey(url)) {return;}

        SearchEngine engine = urls_to_engines.remove(url);
        engines_to_loads.remove(engine);
        engines_to_urls.remove(engine);
        log.info("Engine at URL '"+url+"' removed from balancer.");
    }
    /**
     * Try reloading the engine and test if there is a connection.
     * What should we do if the engine cannot be reached?
     * Keep it separately and test it periodically!
     * This is not implemented!
     * Currently we simply log the information.
     * @return the reloaded engine if the connection is good; null otherwise
     */
    private synchronized SearchEngine reloadandIncrementEngine(SearchEngine engine) {
        if (engine==null || !engines_to_urls.containsKey(engine)) {return null;}

        String url = engines_to_urls.get(engine);

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }

        try {
            //try looking up the engine
            SearchEngine lookup = (SearchEngine) Naming.lookup(url);
            //try sending a request to the engine
            lookup.getSearchDescriptor();
            //if this works, update maps
            urls_to_engines.put(url, lookup);
            engines_to_loads.remove(engine);
            engines_to_loads.put(lookup, 1);//restart with load 1?
            engines_to_urls.remove(engine);
            engines_to_urls.put(lookup, url);
            log.info("Balancer test connection method: Engine at URL '"+url+"' 'reloaded'.");
            return lookup;
        } catch (NotBoundException e) {
            log.warn("Balancer reloadandIncrementEngine method: Engine at URL '"+url+
                    "' cannot be reached:\n\t"+e);
        } catch (IOException e) {
            log.warn("Balancer reloadandIncrementEngine method: Engine at URL '"+url+
                    "' cannot be reached:\n\t"+e);
        }

        return null;
    }

    /**
     * Find minimum load engine, increment load and return.
     * @return minimum load engine
     */
    private synchronized SearchEngine findAndIncrementEngine() {
        if (engines_to_loads.size()<1) {return null;}//the balancer knows no search engines
        Iterator<Map.Entry<SearchEngine, Integer>> entryIterator =
                engines_to_loads.entrySet().iterator();
        Map.Entry<SearchEngine, Integer> currentEntry = entryIterator.next();
        SearchEngine minimumLoadEngine = currentEntry.getKey();
        int minimumLoad = currentEntry.getValue();
        while (entryIterator.hasNext()) {
            currentEntry = entryIterator.next();
            if (currentEntry.getValue()<minimumLoad) {
                minimumLoadEngine = currentEntry.getKey();
                minimumLoad = currentEntry.getValue();
            }
        }
        engines_to_loads.put(minimumLoadEngine, minimumLoad+1);
        return minimumLoadEngine;
    }
    /**
     * Decrement load for given engine.
     */
    private synchronized void decrementEngine(SearchEngine engine) {
        if (engine!=null && engines_to_loads.containsKey(engine)) {
            int load = engines_to_loads.get(engine);
            engines_to_loads.put(engine, load-1);
        }
    }
    /**
     * Find alternative engine, i.e. not the given engine, increment load and return.
     * Currently any other engine is chosen.
     * It might be nice to choose the engine with second least load...
     * @return an alternative engine
     */
    private synchronized SearchEngine findAndIncrementAlternativeEngine(SearchEngine engine) {
        if (engines_to_loads.size()<2) {return null;}//no alternative
        Iterator<Map.Entry<SearchEngine, Integer>> entryIterator =
                engines_to_loads.entrySet().iterator();

        Map.Entry<SearchEngine, Integer> firstEntry = entryIterator.next();
        SearchEngine alternativeEngine = firstEntry.getKey();
        int load = firstEntry.getValue();

        if (alternativeEngine.equals(engine)) {
            Map.Entry<SearchEngine, Integer> secondEntry = entryIterator.next();
            alternativeEngine = secondEntry.getKey();
            load = secondEntry.getValue();
        }

        //remember to increment
        engines_to_loads.put(alternativeEngine, load +1);
        return alternativeEngine;
    }

    public String getRecord(String recordID) throws RemoteException {
        SearchEngine engine = findAndIncrementEngine();
        if (engine==null) {
            log.error("No search engines are registered; " +
                    "the query cannot be answered; null is returned.");
            return null;
        }
        String result;
        try {
            result = engine.getRecord(recordID);
        } catch (RemoteException e) {
            log.error("Balancer getRecord method: RemoteException from engine at URL '"+
                    engines_to_urls.get(engine)+"' (first try):\n\t"+e);

            //TEST CONNECTION
            SearchEngine engineReloaded = reloadandIncrementEngine(engine);
            if (engineReloaded!=null) {//RETRY

                try {
                    result = engineReloaded.getRecord(recordID);
                } catch (RemoteException e1) {
                    log.error("Balancer getRecord method: RemoteException from engine at URL '"+
                            engines_to_urls.get(engineReloaded)+"' (second try):\n\t"+e1);
                    throw e1;
                } finally {
                    decrementEngine(engineReloaded);
                }
            } else {//TRY DIFF ENGINE
                SearchEngine alternative = findAndIncrementAlternativeEngine(engine);
                if (alternative==null) {throw e;}

                try {
                    result = alternative.getRecord(recordID);
                } catch (RemoteException e1) {
                    log.error("Balancer getRecord method: RemoteException from engine at URL '"+
                            engines_to_urls.get(alternative)+"' (alternative engine):\n\t"+e1);
                    throw e1;
                } finally {
                    decrementEngine(alternative);
                }
            }
        } finally {
            decrementEngine(engine);
        }
        return result;
    }

    public String getShortRecord(String recordID) {
        return null;  //TODO
    }

    public String simpleSearch(String query, int numberOfRecords, int startIndex) {
        return null;  //TODO
    }

    public int getHitCount(String query) {
        return 0;  //TODO
    }

    public String simpleSearchSorted(String query, int numberOfRecords, int startIndex, String sortKey, boolean reverse) {
        return null;  //TODO
    }

    public String filteredSearch(String query, int numberOfRecords, int startIndex, String filterQuery) {
        return null;  //TODO
    }

    public String filteredSearchSorted(String query, int numberOfRecords, int startIndex, String filterQuery, String sortKey, boolean reverse) {
        return null;  //TODO
    }

    public String getSearchDescriptor() {
        return null;  //TODO
    }

    public String getSimilarDocuments(String recordID, int numberOfRecords, int startIndex) {
        return null;  //TODO
    }

    public SummaQueryParser getSummaQueryParser() {
        return null;  //TODO
    }

    public String getQueryLang() {
        return null;  //TODO
    }

    public String getOpenUrl(String recordID) {
        return null;  //TODO
    }

    public int getItemCount(String recordID) {
        return -1;
    }

    public int[] getItemCounts(String[] recordIDs) {
        return new int[0];
    }
}
