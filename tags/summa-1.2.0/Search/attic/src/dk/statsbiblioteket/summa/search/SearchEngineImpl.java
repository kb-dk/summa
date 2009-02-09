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
package dk.statsbiblioteket.summa.search;

import javax.xml.xpath.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;

import dk.statsbiblioteket.summa.storage.io.AccessRead;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaSortKeyAnalyzer;
import dk.statsbiblioteket.summa.common.lucene.index.OldIndexField;
import dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexAlias;
import dk.statsbiblioteket.summa.common.lucene.AnalyzerFactory;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryFilter;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.store.FSDirectory;

import org.xml.sax.InputSource;

/**
 * @deprecated in favor of {@link LuceneSearcher}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class SearchEngineImpl implements SearchEngineImplMBean, SearchEngine, LowLevelSearchEngine {

    private Properties p;

    private SearchDescriptor descriptor;
    private String queryLang = "";
    private String[] defaultFields;

    private static SearchEngineImpl _instance = null;
    private IndexSearcher searcher;
    //private MoreLikeThis similar;

    private int lastResponseTime;

    private URL proplocation;

    private static final Log log = LogFactory.getLog(SearchEngineImpl.class);
    private static final Log logPer = LogFactory.getLog("dk.statsbiblioteke.performance.logger");
    private static final Log logSearch = LogFactory.getLog("dk.statsbiblioteket.summa.search.Log");

    private static final boolean isTraceEnabled = log.isTraceEnabled();
    private static final boolean isDebugEnabled = log.isDebugEnabled();
    private static final boolean isInfoEnabled = log.isInfoEnabled();
    private static final boolean isWarnEnabled = log.isWarnEnabled();


    private static XPathExpression countItems;
    private AccessRead io;
    private String indexPath;
    private List<String> parallelIndexPaths;
    private int retries;

    //private static final String DEFAULT_FIELD = "freetext"; // TODO: What?
    public SummaQueryParser summaQueryParser;
    private final String storageURL;
    private Analyzer analyzer;

    /**
     * Loads properties from classpath, and initialises search descriptor.
     *
     * @throws IOException If lucene searcher cannot be initialised
     */
    private SearchEngineImpl() throws IOException {
        long start = System.currentTimeMillis();
        if (isInfoEnabled) {log.info("in constructor");}
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        proplocation = loader.getResource(CONFIG);

        p = new Properties();
        try {
            log.info("Loading proeprties from " + proplocation);
            p.loadFromXML(loader.getResourceAsStream(CONFIG));
        } catch (IOException e) {
            log.error(e);
        }

        try {
            retries = Integer.parseInt(p.getProperty(CONF_RETRIES, "2"));
        } catch (NumberFormatException e) {
            log.warn(CONF_RETRIES + " was not a number. Defaulting to 2 retries.", e);
        }
        BooleanQuery.setMaxClauseCount(Integer.parseInt(p.getProperty(MAX_BOOLEAN_CLAUSES, "10000")));

        indexPath = p.getProperty(INDEX_MAIN);
        parallelIndexPaths = new LinkedList<String>();
        int count = 1;
        String parallelIndexPath = p.getProperty(INDEX_PARALLEL + "." + count);
        while (parallelIndexPath!=null) {
            parallelIndexPaths.add(parallelIndexPath);
            count++;
            parallelIndexPath = p.getProperty(INDEX_PARALLEL + "." + count);
        }

        descriptor = new SearchDescriptor(indexPath);
        descriptor.loadDescription(indexPath);


        log.info("Got search descriptor:\n" + descriptor.getDescription());

        analyzer = AnalyzerFactory.buildAnalyzer(descriptor);
        log.info("Analyzer created");

        queryLang = searchDescriptorToSmallXML();
        log.info ("Query language analyzed");

        lastResponseTime = 0;
        storageURL = p.getProperty(IO_SERVICE);
        try {
            io = (AccessRead) Naming.lookup(storageURL);
        } catch (NotBoundException e) {
            log.error(e);
        } catch (MalformedURLException e) {
            log.error(e);
        } catch (RemoteException e) {
            log.error(e);
        }
        log.info("Got storage connection");
                
        reinitSearcher();
        log.info("Searcher initialized");

        // TODO: Create the query parser here
//        summaQueryParser = new SummaQueryParser(new String[]{}, analyzer, descriptor);
        log.info("SummaQueryParser created");

/*        StringTokenizer defaultToken = new StringTokenizer(p.getProperty(dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser.DEFAULT_FIELDS), " ", false);
        defaultFields = new String[1000];
        int k = 0;
        while (defaultToken.hasMoreElements()) {
            String[] buf = summaQueryParser.getFields(defaultToken.nextToken());
            System.arraycopy(buf, 0, defaultFields, k, buf.length);
            k += buf.length;
        }
        String[] trimed = new String[k];
        System.arraycopy(defaultFields, 0, trimed, 0, k);
        summaQueryParser.setDefaultFields(trimed);*/

        if (isInfoEnabled) {log.info("default fields ok"
                                     + defaultFields.length
                                     + " instance ok");}

        XPathFactory fac = XPathFactory.newInstance();
        XPath xp = fac.newXPath();

        try {
            countItems = xp.compile("count(record/datafield[@tag='096']" +
                  "[subfield[@code='i' and (text()='vu' or text()='cu' or text()='fu' or text()='kort' or text()='lu' or text()='m' or text()='nod-st')]]" +
                    "[subfield[@code='z' and (starts-with(text(), 'SB-') or starts-with(text(), 'SVB'))]])");
        } catch (XPathExpressionException e) {
            log.error("Could not compile countItem xpath");
        }
        logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.SearchEngineImpl  in:" + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * Get the singleton instance of this searcher.
     *
     * @return The singleton, or null on errors
     */
    public static synchronized SearchEngineImpl getInstance() {
        long start = System.currentTimeMillis();
        log.trace("Get instance called");
        if (_instance == null) {

            try {
                _instance = new SearchEngineImpl();
            } catch (IOException e) {
                log.error("unable to make instance", e);
            }
            if (isInfoEnabled) {log.info("engine instansiated");}
        }
        logPer.info( "dk.statsbiblioteket.summa.search.SearchEngineImpl.getInstance  in:" + (System.currentTimeMillis() - start) + "ms");
        return _instance;

    }

    /**
     * Returns the response time of the last executed query.
     *
     * @return Last known response tim in milliseconds. 0 if no queries
     *         performed yet.
     */
    public int getLastResponseTime() {
        return lastResponseTime;
    }

    /**
     * Get the registry server used when talken to storage RMI service.
     *
     * @return The registry server, or "Unknown".
     */
    public String getRegistryServer() {
        try {
            return new URL(storageURL).getHost();
        } catch (MalformedURLException e) {
            return "Unknown";
        }
    }

    /**
     * Get the registry port used when talking to storage RMI service.
     *
     * @return The registry port, or -1 for unknown
     */
    public int getRegistryPort() {
        try {
            return new URL(storageURL).getPort();
        } catch (MalformedURLException e) {
            return -1;
        }
    }

    /**
     * Get the RMI service name used when talking to storage RMI service.
     *
     * @return The rmi service name, or "Unknown".
     */
    public String getRegistryServiceName() {
        try {
            return new URL(storageURL).getPath();
        } catch (MalformedURLException e) {
            return "Unknown";
        }
    }

    /**
     * Write current properties to a file. Will write to original location
     * if it was a file, otherwise to current directory.
     *
     * @throws IOException on trouble writing file
     */
    public void writeProperties() throws IOException {
        File f;
        if (proplocation.getProtocol().equals("file")) {
            f = new File(proplocation.getFile());
        } else {
            f = new File(".", CONFIG);
        }
        p.storeToXML(new FileOutputStream(f), null);
    }

    /**
     * Read a record from storage with a given ID.
     *
     * @param recordID of the record to read
     * @return The record
     * @throws RemoteException On trouble communicating with storage.
     */
    public String getRecord(String recordID) throws RemoteException {
        long start = System.currentTimeMillis();
        try {
            Record r = io.getRecord(recordID);
            String rec = new String(r.getContent(), "utf-8");

            //todo: remove this ugly hack;
            if (rec != null){
                rec = rec.replace("xmlns=\"http://www.loc.gov/MARC21/slim\"", "");
                rec = rec.replace("xmlns=\"http://www.openarchives.org/OAI/2.0/\"" , "");
            }
            logPer.info( "dk.statsbiblioteket.summa.search.SearchEngineImpl.getRecord  in:" + (System.currentTimeMillis() - start) + "ms");
            return rec;
            //return r != null ? new String(r.getUTF8Content(), "utf-8") : null;
        } catch (UnsupportedEncodingException e) {
                log.error("The recordContent for record: " + recordID + " is not supported", e);
            return null;
        }
    }


    /**
     * Read a shortRecord from Index
     *
     * @param recordID of the record to read
     * @return The record in short format
     */
    public String getShortRecord(String recordID) {
        long start = System.currentTimeMillis();
        TermQuery q = new TermQuery(new Term("recordID", recordID));
        try {
            Hits h = searcher.search(q);
            logPer.info( "dk.statsbiblioteket.summa.search.SearchEngineImpl.getShortRecord  in:" + (System.currentTimeMillis() - start) + "ms");
            return h.length() > 0 ? "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" + getShortRecord(h.doc(0)) : null;
        } catch (IOException e) {
            log.error(e);
            return null;
        }
    }

    /**
     * Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset.
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     * <p/>
     * This method also updates lastResponseTime.
     *
     * @param query           Query string
     * @param numberOfRecords The number of records to return
     * @param startIndex      The offset to start at
     * @return A string encoding the results in xml,
     *         or a specific xml doc on trouble
     */
    public String simpleSearch(String query, int numberOfRecords, int startIndex) {
        long start = System.currentTimeMillis();
        Query luceneQuery;
        if (isInfoEnabled) {log.info("Performing simplesearch on '" + query + "'");}
        try {
            luceneQuery = summaQueryParser.parse(query);
        } catch (ParseException e) {
            log.warn("Cannot parse query: '" + query + "'", e);
            return makeError("forstår ikke søgningen").toString();
        }
        Hits hits;
        try {
            hits = search(luceneQuery);
        } catch (IOException e) {
            log.warn("Error performing query: '" + query + "'", e);
            logSearch.warn("Error performing query: '" + query + "'", e);
            return makeError("søgningen nede").toString();
        } catch (BooleanQuery.TooManyClauses e) {
            log.warn("Error performing query: '" + query + "'", e);
            logSearch.warn("Error performing query: '" + query + "'", e);
            return makeError("søgningen kan ikke gennemføres").toString();
        }

        String result = makeResult(hits, numberOfRecords, startIndex).toString();
        lastResponseTime = (int) (System.currentTimeMillis() - start);
        logSearch.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.simpleSearch  on:"
                    + "\"" + query +"\""
                +" returned:" + hits.length() + " hits in "
                + lastResponseTime +  "ms");

        logPer.info( "dk.statsbiblioteket.summa.search.SearchEngineImpl.simpleSearch  in:" + lastResponseTime + "ms");
        return result;
    }

    /**
     * @param query
     * @return returns the number of hits from a simple search
     * @deprecated only used by suggestion-engine.
     */
    public int getHitCount(String query) {
        long start = System.currentTimeMillis();
        CountCollector col = new CountCollector();
        if (searchWithCollector(query,col)){
            return col.getCount();
        }
        return 0;
    }

    /**
     * Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset, results will be sorted by
     * given key.
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     * <p/>
     * This method also updates lastResponseTime.
     *
     * @param query           Query string
     * @param numberOfRecords The number of records to return
     * @param startIndex      The offset to start at
     * @return A string encoding the results in xml,
     *         or a specific xml doc on trouble
     */
    public String simpleSearchSorted(String query, int numberOfRecords, int startIndex, String sortKey, boolean reverse) {
        long start = System.currentTimeMillis();
        Query luceneQuery;
        if (isDebugEnabled){ log.debug("Performing simplesearchsorted on '" + query + "'");}
        try {
            luceneQuery = summaQueryParser.parse(query);
        } catch (ParseException e) {
            log.warn("Cannot parse query: '" + query + "'", e);
            return makeError("forstår ikke søgningen").toString();
        }

        Sort sort = new Sort(sortKey, reverse);
        Hits hits;
        try {
            hits = search(luceneQuery, sort);
        } catch (IOException e) {
            log.warn("Error performing query: '" + query + "'", e);
            return makeError("søgningen nede").toString();
        } catch (BooleanQuery.TooManyClauses e) {
            log.warn("Error performing query: '" + query + "'", e);
            return makeError("søgningen kan ikke gennemføres").toString();
        }
        String result = makeResult(hits, numberOfRecords, startIndex).toString();
        logPer.info( "dk.statsbiblioteket.summa.search.SearchEngineImpl.simpleSearchSorted  in:" + (System.currentTimeMillis() - start) + "ms");
        lastResponseTime = (int) (System.currentTimeMillis() - start);
        return result;
    }

    /**
     * Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset, results will be filtered with
     * the given filter.
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     * <p/>
     * This method also updates lastResponseTime.
     *
     * @param query           Query string
     * @param numberOfRecords The number of records to return
     * @param startIndex      The offset to start at
     * @return A string encoding the results in xml,
     *         or a specific xml doc on trouble
     */
    public String filteredSearch(String query, int numberOfRecords, int startIndex, String filterQuery) {
        long start = System.currentTimeMillis();
        Query luceneQuery;
        Query luceneFilterQuery;
        if (isDebugEnabled) {log.debug("Performing filteredsearch on '" + query + "'");}
        try {
            luceneQuery = summaQueryParser.parse(query);
            // TODO: Implement this
            luceneFilterQuery = summaQueryParser.parse(filterQuery);
//            luceneFilterQuery = summaQueryParser.parse(filterQuery, QueryParser.OR_OPERATOR);
        } catch (ParseException e) {
            if (isWarnEnabled) {log.warn("Cannot parse query: '" + query + "'", e);}
            return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><searchresult totalhits=\"0\" firstRecord=\"0\"  error=\"forstår ikke søgningen\" />";
        }
        BooleanQuery queryFilter = new BooleanQuery();
        //queryFilter.add(luceneQuery[1], BooleanClause.Occur.MUST);
        queryFilter.add(luceneFilterQuery, BooleanClause.Occur.MUST);
        Filter filter = new QueryFilter(queryFilter);
        if (isInfoEnabled){log.info("filter: " + queryFilter.toString());}
        Hits hits;
        try {
            hits = search(luceneQuery, filter);
        } catch (IOException e) {
            log.warn("Error performing query: '" + query + "'", e);
            return makeError("søgningen nede").toString();
        } catch (BooleanQuery.TooManyClauses e) {
            log.warn("Error performing query: '" + query + "'", e);
            return makeError("søgningen kan ikke gennemføres").toString();
        }
        String result = makeResult(hits, numberOfRecords, startIndex).toString();
        logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.filteredSearch(" + query + "," + numberOfRecords + "," + startIndex + "," + filterQuery + ") in:" + (System.currentTimeMillis() - start) + "ms");
        lastResponseTime = (int) (System.currentTimeMillis() - start);
        return result;
    }

    /**
     * Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset.
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     * <p/>
     * This method also updates lastResponseTime.
     *
     * @param query           Query string
     * @param numberOfRecords The number of records to return
     * @param startIndex      The offset to start at
     * @return A string encoding the results in xml,
     *         or a specific xml doc on trouble
     */
    public String filteredSearchSorted(String query, int numberOfRecords, int startIndex, String filterQuery, String sortKey, boolean reverse) {
        long start = System.currentTimeMillis();
        Query luceneQuery;
        Query luceneFilterQuery;
        if (isDebugEnabled){log.debug("Performing filteredsearchsorted on '" + query + "'");}
        try {
            luceneQuery = summaQueryParser.parse(query);
            // TODO: Implement this
            luceneFilterQuery = summaQueryParser.parse(filterQuery);
//            luceneFilterQuery = summaQueryParser.parse(filterQuery, QueryParser.OR_OPERATOR);
        } catch (ParseException e) {
            log.warn("Cannot parse query: '" + query + "'", e);
            return makeError("forstår ikke søgningen").toString();
        }
        Sort sort = new Sort(sortKey, reverse);
        BooleanQuery queryFilter = new BooleanQuery();
        //queryFilter.add(luceneQuery[1], BooleanClause.Occur.MUST);
        queryFilter.add(luceneFilterQuery, BooleanClause.Occur.MUST);
        Filter filter = new QueryFilter(queryFilter);
        Hits hits;
        try {
            hits = search(luceneQuery, filter, sort);
        } catch (IOException e) {
            log.warn("Error performing query: '" + query + "'", e);
            return makeError("søgningen nede").toString();
        } catch (BooleanQuery.TooManyClauses e) {
            log.warn("Error performing query: '" + query + "'", e);
            return makeError("søgningen kan ikke gennemføres").toString();
        }
        String result = makeResult(hits, numberOfRecords, startIndex).toString();
        lastResponseTime = (int) (System.currentTimeMillis() - start);
        logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.filteredSearch(" + query + "," + numberOfRecords + "," + startIndex + "," + filterQuery + "," + sortKey + ") in:" + (System.currentTimeMillis() - start) + "ms");
        return result;
    }

    /**
     * Get the search descriptor in an XML string
     *
     * @return The XML search descriptor, or null if impossible.
     */
    public String getSearchDescriptor() {
        return queryLang;
    }

    /**
     * Wrapper for search method that retries a number of times before giving
     * up. The index will be reinitialised on trouble
     * <p/>
     * The number of times to retry is read from property searchRetries,
     * defaults to 2.
     *
     * @param query The Lucene query
     * @return The hits for query.
     * @throws IOException                 if Lucene search fails repeatedly
     * @throws BooleanQuery.TooManyClauses On too many clauses
     * @see IndexSearcher#search(Query)
     */
    private synchronized Hits search(Query query) throws IOException {
        long start = System.currentTimeMillis();
        int i = 0;
        Hits hits = null;
        IOException lastException = null;
        do {
            try {

                hits = searcher.search(query);
                // todo : remove this
                if (isInfoEnabled) { log.info("top hit:"  + searcher.explain(query, 0));}
                if (isInfoEnabled) { log.info("2. hit: " + searcher.explain(query, 1));}
            } catch (IOException e) {
                log.error("Error searching index for query " + query, e);
                reinitSearcher();
                lastException = e;
            }
        } while ((hits == null) && (++i < retries));
        if (hits == null && lastException != null) {
            throw lastException;
        }
        logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.search(" + query.toString() + ")  in:" + (System.currentTimeMillis() - start) + "ms");
        return hits;
    }

    /**
     * Wrapper for search method that retries a number of times before giving
     * up. The index will be reinitialised on trouble
     * <p/>
     * The number of times to retry is read from property searchRetries,
     * defaults to 2.
     *
     * @param query  The Lucene query
     * @param filter Search filter
     * @return The hits for query.
     * @throws IOException                 if Lucene search fails repeatedly
     * @throws BooleanQuery.TooManyClauses On too many clauses
     * @see IndexSearcher#search(Query, Filter)
     */
    private synchronized Hits search(Query query, Filter filter) throws IOException {
        long start = System.currentTimeMillis();
        int i = 0;
        Hits hits = null;
        IOException lastException = null;
        do {
            try {
                hits = searcher.search(query, filter);
            } catch (IOException e) {
                log.error("Error searching index for query " + query, e);
                reinitSearcher();
                lastException = e;
            }
        } while ((hits == null) && (++i < retries));
        if (hits == null && lastException != null) {
            throw lastException;
        }
        logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.search(" + query.toString() + ","  + filter.toString() + ")  in:" + (System.currentTimeMillis() - start) + "ms");
        return hits;
    }

    /**
     * Wrapper for search method that retries a number of times before giving
     * up. The index will be reinitialised on trouble
     * <p/>
     * The number of times to retry is read from property searchRetries,
     * defaults to 2.
     *
     * @param query The Lucene query
     * @param sort  Sort key
     * @return The hits for query.
     * @throws IOException                 if Lucene search fails repeatedly
     * @throws BooleanQuery.TooManyClauses On too many clauses
     * @see IndexSearcher#search(Query, Sort)
     */
    private synchronized Hits search(Query query, Sort sort) throws IOException {
        long start = System.currentTimeMillis();
        int i = 0;
        Hits hits = null;
        IOException lastException = null;
        do {
            try {
                hits = searcher.search(query, sort);
            } catch (IOException e) {
                log.error("Error searching index for query " + query, e);
                reinitSearcher();
                lastException = e;
            }
        } while ((hits == null) && (++i < retries));
        if (hits == null && lastException != null) {
            throw lastException;
        }
        logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.search(" + query.toString() + ","  + sort.toString() + ")  in:" + (System.currentTimeMillis() - start) + "ms");
        return hits;
    }

    /**
     * Wrapper for search method that retries a number of times before giving
     * up. The index will be reinitialised on trouble
     * <p/>
     * The number of times to retry is read from property searchRetries,
     * defaults to 2.
     *
     * @param query The Lucene query
     * @param sort  Sort key
     * @return The hits for query.
     * @throws IOException                 if Lucene search fails repeatedly
     * @throws BooleanQuery.TooManyClauses On too many clauses
     * @see IndexSearcher#search(Query, Filter, Sort)
     */
    private synchronized Hits search(Query query, Filter filter, Sort sort) throws IOException {
        long start = System.currentTimeMillis();
        int i = 0;
        
        Hits hits = null;
        IOException lastException = null;
        do {
            try {
                hits = searcher.search(query, filter, sort);
            } catch (IOException e) {
                log.error("Error searching index for query " + query, e);
                reinitSearcher();
                lastException = e;
            }
        } while ((hits == null) && (++i < retries));
        if (hits == null && lastException != null) {
            throw lastException;
        }
        logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.search(" + query.toString() + ","  + filter.toString() + "," + sort.toString() + ")  in:" + (System.currentTimeMillis() - start) + "ms");
        return hits;
    }

    public String getSimilarDocuments(String recordID, int numberOfRecords, int startIndex) {
        long start = System.currentTimeMillis();
        TermQuery q = new TermQuery(new Term("recordID", recordID));
        Hits hits;
        try {
            hits = searcher.search(q);
      //      Query qM = similar.like(hits.id(0));
      //      hits = searcher.search(qM);
      //      return makeSimilarResults(hits, numberOfRecords, startIndex, recordID).toString();
        } catch (IOException e) {
            log.error(e);
            return null;
        }
        return null;
    }

    private static StringWriter makeSimilarResults(Hits docs, int numberOfRecords, int startIndex, String recordID) {
        long start = System.currentTimeMillis();
        int hitCount = docs.length() - 1;
        StringWriter w = new StringWriter();
        w.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><searchresult totalhits=\"" + hitCount + "\" firstRecord=\"" + startIndex + "\" >");
        try {
            for (int i = startIndex; i < startIndex + numberOfRecords && i < hitCount; i++) {
                Document d = docs.doc(i);
                if (!recordID.equals(d.get("recordID"))) {
                    w.append("<record searchscore=\"" + docs.score(i) + "\" recordID=\"" + d.get("recordID") + "\">");
                    //w.append(d.get("shortformat"));
                    w.append(getShortRecord(d));
                    w.append("</record>");
                }
            }
        } catch (IOException e) {
            log.error(e);
        }
        w.append("</searchresult>");
        logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.makeSimilarResults  in:" + (System.currentTimeMillis() - start) + "ms");
        return w;

    }


    //todo: remove this hack asap
    private static String getShortRecord(Document d) {
        long start = System.currentTimeMillis();
        if (d.get("shortformat") != null){
            logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.getShortRecord  in:" + (System.currentTimeMillis() - start) + "ms");
            return d.get("shortformat").replace("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">",
                    "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
        }
        return "";
    }

    /**
     * Given a set of hit, wrap a certain interval in an XML document
     * and return it as a StringWriter.
     * <p/>
     * Form of XML:
     * <searchresult totalhits="..." firstrecord="...">
     * <record seachscore="..." recordID="...">
     * ...
     * </record>
     * ...
     * </searchresult>
     * <p/>
     * Note: Errors in getting range are silently ignored.
     *
     * @param docs            The hits to wrap in xml
     * @param numberOfRecords The number of hits to wrap
     * @param startIndex      The first hit to wrap
     * @return The requested hits wrapped in XML
     */
    private static StringWriter makeResult(Hits docs, int numberOfRecords,
                                           int startIndex) {
        long start = System.currentTimeMillis();
        int hitCount = docs.length();
        StringWriter w = new StringWriter();
        w.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><searchresult totalhits=\"" + hitCount + "\" firstRecord=\"" + startIndex + "\" >");
        try {
            for (int i = startIndex; i < startIndex + numberOfRecords && i < hitCount; i++) {

                Document d = docs.doc(i);
                if (getShortRecord(d) != null){
                    w.append("<record searchscore=\"" + docs.score(i) + "\" recordID=\"" + d.get("recordID") + "\">");
                    w.append(getShortRecord(d));
                    w.append("</record>");
                }
            }
        } catch (IOException e) {
            log.error(e);
        }
        w.append("</searchresult>");
        logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.makeResult  in:" + (System.currentTimeMillis() - start) + "ms");
        return w;
    }

    /**
     * Given an error string, make an XML document describing it, and return it
     * as a StringWriter.
     * <p/>
     * Form of XML:
     * <searchresult totalhits="0" firstrecord="0" error="..."/>
     *
     * @param errorMsg
     * @return The requested hits wrapped in XML
     */
    private static StringWriter makeError(String errorMsg) {
        StringWriter w = new StringWriter();
        w.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><searchresult totalhits=\"0\" firstRecord=\"0\"  error=\"" + errorMsg + "\" />");
        return w;
    }

    /**
     * Tries to re-initialise the IndexReader, by unlocking the FSDirectory and
     * reopening the IndexReader.
     *
     * @throws IOException if the index cannot be opened.
     */
    private void reinitSearcher() throws IOException {
        log.warn("Reinitializing IndexSearcher on index: " + indexPath);
        File f = new File(indexPath);
        IndexReader r;
//        if (f.isDirectory()){
//            File[] indexex = f.listFiles();
//            IndexReader[] indexReaders = new IndexReader[indexex.length -1];
//            int j = 0;
//            for (File _file : indexex){
//                if (_file.isDirectory()){
//                    //todo: each instance has to be a parallel indexreader
//                    FSDirectory fs = FSDirectory.getDirectory(_file.getAbsolutePath(),false);
//                    IndexReader.unlock(fs);
//                    indexReaders[j++] = IndexReader.open(fs);
//                }
//            }
//            r = new MultiReader(indexReaders);
//        } else {


        FSDirectory fs = FSDirectory.getDirectory(indexPath, false);
        IndexReader.unlock(fs);
        r = IndexReader.open(fs);

        if (!parallelIndexPaths.isEmpty()) {
            if (isInfoEnabled) { log.info("has parallelIndexParths");}
            try {
                ParallelReader parallelReader = new ParallelReader();

                parallelReader.add(r);

                for (String parallelIndex: parallelIndexPaths) {
                    FSDirectory parallelDir = FSDirectory.getDirectory(parallelIndex, false);
                    IndexReader.unlock(parallelDir);//is this necessary?
                    IndexReader parallelIndexReader = IndexReader.open(parallelDir);
                    if (r.maxDoc()==parallelIndexReader.maxDoc()) {
                        parallelReader.add(parallelIndexReader);
                    } else {
                        if (isInfoEnabled) { log.info("Parallel index '"+parallelIndex+"' NOT up to date?");}
                    }
                }
                r = parallelReader;
            } catch (FileNotFoundException e) {
                if (isInfoEnabled) { log.info("Parallel index FileNotFoundException.");}
            } catch (IOException e) {
                if (isInfoEnabled) { log.info("Parallel index IOException.");}
            }
        }
        //}


        searcher = new IndexSearcher(r);
    }

    private String searchDescriptorToSmallXML() {
        if (isDebugEnabled){ log.debug("making queryLanguage description");}
        HashMap<String, Set<String>[]> singlefields = new HashMap<String, Set<String>[]>();
        HashMap<String, HashMap<String, Set<String>[]>> grp = new HashMap<String, HashMap<String, Set<String>[]>>();

        String returnVal = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n";
        TreeSet<OldIndexField> fields = descriptor.getSingleFields();
        HashMap<String, SearchDescriptor.Group> groups = descriptor.getGroups();
        returnVal += "<SummaQueryDescriptor>\n";

        if (isDebugEnabled){ log.debug("queryLanguage, populating singleFields");}
        for (OldIndexField f : fields) {
            if (f.getType().getIndex() != Field.Index.NO  && !(f.getType().getAnalyzer() instanceof SummaSortKeyAnalyzer)){
                if (singlefields.containsKey(f.getName())) {
                    if (isTraceEnabled) {log.trace("Adding resolver to singleField: " + f.getName() + ", " + f.getResolver());}
                    singlefields.get(f.getName())[0].add(f.getResolver());
                    for (IndexAlias a : f.getAliases()) {
                        singlefields.get(f.getName())[1].add(a.getLang() + ":" + a.getName());
                        if (isTraceEnabled) {log.trace("Adding alias to singleField: " + a.getLang() + ":" + a.getName());}
                    }
                } else {
                    Set[] s = new Set[]{new HashSet<String>(), new HashSet<String>()};
                    s[0].add(f.getResolver());
                    for (IndexAlias a : f.getAliases()) {
                        s[1].add(a.getLang() + ":" + a.getName());
                    }
                    singlefields.put(f.getName(), s);
                }
            }
        }
        if (isDebugEnabled){log.debug("singleFields populated");}


        for (SearchDescriptor.Group g : groups.values()) {
            TreeSet<OldIndexField> grpF = g.getFields();
            if (grp.containsKey(g.getName())) {
                HashMap<String, Set<String>[]> m = grp.get(g.getName());
                for (OldIndexField f : grpF) {
                    if (f.getType().getIndex() != Field.Index.NO && !(f.getType().getAnalyzer() instanceof SummaSortKeyAnalyzer)){
                        if (m.containsKey(f.getName())) {
                            m.get(f.getName())[0].add(f.getResolver());
                            for (IndexAlias a : f.getAliases()) {
                                m.get(f.getName())[1].add(a.getLang() + ":" + a.getName());
                            }
                        } else {
                            Set<String>[] s = new HashSet[]{new HashSet<String>(), new HashSet<String>()};
                            s[0].add(f.getResolver());
                            for (IndexAlias a : f.getAliases()) {
                                s[1].add(a.getLang() + ":" + a.getName());
                            }
                            m.put(f.getName(), s);
                        }
                    }
                }

            } else {
                HashMap<String, Set<String>[]> h = new HashMap<String, Set<String>[]>();
               // Set<String>[] s;

                for (OldIndexField f : grpF) {
                    if (f.getType().getIndex() != Field.Index.NO && !(f.getType().getAnalyzer() instanceof SummaSortKeyAnalyzer)){
                        if (h.get(f.getName()) == null) {
                            h.put(f.getName(),  new Set[]{new HashSet<String>(), new HashSet<String>()});
                        }
                        h.get(f.getName())[0].add(f.getResolver());
                        for (IndexAlias a : f.getAliases()) {
                            Set set = h.get(f.getName())[1];
                            h.get(f.getName())[1].add(a.getLang() + ":" + a.getName());
                        }
                    }
                }
                grp.put(g.getName(), h);
            }
        }
        if (isDebugEnabled){log.debug("done populating groups");}

        returnVal += "\t<singleFields>\n";
        Set<Map.Entry<String, Set<String>[]>> single = singlefields.entrySet();
        for (Map.Entry<String, Set<String>[]> fi : single) {
            returnVal += "\t\t<field name=\"" + fi.getKey() + "\">\n";
            for (String ali : fi.getValue()[1]) {
                returnVal += "\t\t\t<alias xml:lang=\"" + ali.substring(0, ali.indexOf(":")) + "\">" + ali.substring(ali.indexOf(":") + 1)  + "</alias>\n";
            }
            for (String ba : fi.getValue()[0]) {
                returnVal += "\t\t\t<source> " + ba + "</source>\n";
            }
            returnVal += "\t\t</field>\n";
        }
        returnVal += "\t</singleFields>\n";

        if (isDebugEnabled){log.debug("done writing singleFields: \n\n" + returnVal);}

        Set<Map.Entry<String, HashMap<String, Set<String>[]>>> gp = grp.entrySet();
        for (Map.Entry<String, HashMap<String, Set<String>[]>> g : gp) {
            returnVal += "\t<group name=\"" + g.getKey() + "\">\n";
            SearchDescriptor.Group qaz = groups.get(g.getKey());
            for (IndexAlias a : qaz.getAliases()) {
                returnVal += "\t\t<alias xml:lang=\"" + a.getLang() + "\">" + a.getName() + "</alias>\n";
            }
            if (isDebugEnabled){ log.debug("wrote group def:"  + returnVal);}
            returnVal += "\t\t<fields>\n";
            HashMap<String, Set<String>[]> flds = g.getValue();
            Set<Map.Entry<String, Set<String>[]>> gflds = flds.entrySet();
            for (Map.Entry<String, Set<String>[]> fi : gflds) {
                returnVal += "\t\t\t<field name=\"" + fi.getKey() + "\">\n";
                for (String ali : fi.getValue()[1]) {
                    returnVal += "\t\t\t\t<alias xml:lang=\"" + ali.substring(0, ali.indexOf(":")) + "\">" +   ali.substring(ali.indexOf(":") + 1)+ "</alias>\n";
                }
                for (String ba : fi.getValue()[0]) {
                    returnVal += "\t\t\t\t<source> " + ba + "</source>\n";
                }
                returnVal += "\t\t\t</field>\n";
            }
            returnVal += "\t\t</fields>\n";
            returnVal += "\t</group>\n";
        }
        returnVal += "</SummaQueryDescriptor>\n";

        if (isDebugEnabled){ log.debug("done writing groups:" + returnVal);}

        return returnVal;

    }

    /**
     * Constructs a query based on queryString, performs a search and returns
     * all the Hits.
     *
     * @param queryString converted to a Query
     * @return all Hits, containing Document-IDs
     * @depricated
     */
    public Hits getHits(String queryString) {
        long start = System.currentTimeMillis();
        Query luceneQuery;
        try {
            luceneQuery = summaQueryParser.parse(queryString);
        } catch (ParseException e) {
            log.warn("Cannot parse query: '" + queryString + "'", e);
            return null;
        }
        Hits hits;
        try {
            hits = search(luceneQuery);
        } catch (IOException e) {
            log.warn("Error performing query: '" + queryString + "'", e);
            return null;
        } catch (BooleanQuery.TooManyClauses e) {
            log.warn("Error performing query: '" + queryString + "'", e);
            return null;
        }
        logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.getHits  in:" + (System.currentTimeMillis() - start) + "ms");
        return hits;
    }

    /**
     * Constructs a query based on queryString, performs a search and returns
     * the highest ranking numberOfResults Document-IDs.
     *
     * @param queryString  converted to a Query
     * @param numberOfDocs the maximum number of Documents-IDs to return
     * @return a TopDoc, containing Document-IDs
     */
    public TopDocs getTopDocs(String queryString, int numberOfDocs) {
        long start = System.currentTimeMillis();
        Query luceneQuery;
        try {
            luceneQuery = summaQueryParser.parse(queryString);
        } catch (ParseException e) {
            log.warn("Cannot parse query: '" + queryString + "'", e);
            return null;
        }
        TopDocs topDocs;
        try {
            topDocs = search(luceneQuery, numberOfDocs);
        } catch (IOException e) {
            log.warn("Error performing query: '" + queryString + "'", e);
            return null;
        } catch (BooleanQuery.TooManyClauses e) {
            log.warn("Error performing query: '" + queryString + "'", e);
            return null;
        }
        logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.getTopDocs( "+ queryString + "," + numberOfDocs + ")  in:" + (System.currentTimeMillis() - start) + "ms");
        return topDocs;
    }

    /**
     * Constructs a query, based on queryString, performs a search with the
     * given collector.
     * @param queryString converted to a Query
     * @param collector   the collector responsible for handling the collected
     *                    documents and scores
     * @return true if there were no errors
     */
    public boolean searchWithCollector(String queryString,
                                       HitCollector collector) {
        long start = System.currentTimeMillis();
        int i = 0;
        TopDocs topDocs = null;
        IOException lastException = null;

        Query luceneQuery;
        try {
            luceneQuery = summaQueryParser.parse(queryString);
        } catch (ParseException e) {
            log.warn("Cannot parse query: '" + queryString + "'", e);
            return false;
        }

        try {
            boolean foundSomething = false;
            do {
                try {
                    searcher.search(luceneQuery, collector);
                    foundSomething = true;
                } catch (IOException e) {
                    log.error("Error searching index for query " + luceneQuery,
                              e);
                    reinitSearcher();
                    lastException = e;
                }
            } while (!foundSomething && ++i < retries);
            if (!foundSomething && lastException != null) {
                throw lastException;
            }
        } catch (IOException e) {
            log.warn("Error performing query: '" + queryString + "'", e);
            return false;
        } catch (BooleanQuery.TooManyClauses e) {
            log.warn("Error performing query: '" + queryString + "'", e);
            return false;
        }
        logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.searchWithCollector( "+ queryString + "," + collector.getClass().getCanonicalName() + ")  in:" + (System.currentTimeMillis() - start) + "ms");
        return true;
    }

    /**
     * Wrapper for search method that retries a number of times before giving
     * up. The index will be reinitialised on trouble
     * <p/>
     * The number of times to retry is read from property searchRetries,
     * defaults to 2.
     *
     * @param query        The Lucene query
     * @param numberOfDocs The number of Docs to be returned
     * @return The TopDocs for query.
     * @throws IOException                 if Lucene search fails repeatedly
     * @throws BooleanQuery.TooManyClauses On too many clauses
     * @see IndexSearcher#search(Query, Filter)
     */
    private synchronized TopDocs seadrch(Query query,
                                         int numberOfDocs)
            throws IOException {
        int i = 0;
        TopDocs topDocs = null;
        IOException lastException = null;
        do {
            try {
                topDocs = searcher.search(query, null, numberOfDocs);
            } catch (IOException e) {
                log.error("Error searching index for query " + query, e);
                reinitSearcher();
                lastException = e;
            }
        } while ((topDocs == null) && (++i < retries));
        if (topDocs == null && lastException != null) {
            throw lastException;
        }
        return topDocs;
    }


    /**
     * Requests a Document from the Lucene index.
     *
     * @param docNumber the identifier for the Document
     * @return the document, corresponding to the docNumber, if it exists
     */
    public Document getDoc(int docNumber) {
        long start = System.currentTimeMillis();
        try {
            logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl." +
                        "getDoc( " + docNumber + ")  in:" +
                        (System.currentTimeMillis() - start) + "ms");
            return searcher.doc(docNumber);
        } catch (IOException e) {
            log.error("Error getting document with docNumber " + docNumber, e);
            return null;
        }
    }

    /**
     * Telegram a vector of terms for a specific Lucene Document Field.
     *
     * @param docNumber the ID for the document
     * @param field     the Field-name
     * @return A space-separated string of terms
     */
    public TermFreqVector getTermFreqVector(int docNumber, String field) {
        long start = System.currentTimeMillis();
        try {
            logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl." +
                        "getTermFreqVector(" + docNumber + ", " + field + ")" +
                        "  in:" + (System.currentTimeMillis() - start) + "ms");
            return searcher.getIndexReader().getTermFreqVector(docNumber, field);
        } catch (IOException ex) {
            log.error("Error getting term freq vector with docNumber " +
                      docNumber, ex);
            return null;
        }
    }

    /**
     * Wrapper for search method that retries a number of times before giving
     * up. The index will be reinitialised on trouble
     * <p/>
     * The number of times to retry is read from property searchRetries,
     * defaults to 2.
     *
     * @param query        The Lucene query
     * @param numberOfDocs The number of Docs to be returned
     * @return The TopDocs for query.
     * @throws IOException                 if Lucene search fails repeatedly
     * @throws BooleanQuery.TooManyClauses On too many clauses
     * @see IndexSearcher#search(Query, Filter)
     */
    private synchronized TopDocs search(Query query,
                                        int numberOfDocs)
            throws IOException {
        long start = System.currentTimeMillis();
        int i = 0;
        TopDocs topDocs = null;
        IOException lastException = null;
        do {
            try {
                topDocs = searcher.search(query, null, numberOfDocs);
            } catch (IOException e) {
                log.error("Error searching index for query " + query, e);
                reinitSearcher();
                lastException = e;
            }
        } while ((topDocs == null) && (++i < retries));
        if (topDocs == null && lastException != null) {
            throw lastException;
        }
        return topDocs;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public IndexReader getIndexReader() {
        return searcher.getIndexReader();
    }

    public SummaQueryParser getSummaQueryParser() {
        return summaQueryParser;
    }

    public String getQueryLang(){
        return queryLang;
    }

    public String getOpenUrl(String recordID) {
        long start = System.currentTimeMillis();
        TermQuery q = new TermQuery(new Term("recordID", recordID));
        try {
            Hits h = searcher.search(q);
            logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.getOpenUrl  in:" + (System.currentTimeMillis() - start) + "ms");
            return h.length() == 1 && h.doc(0).get("openUrl") != null ? h.doc(0).get("openUrl") : null;
        } catch (IOException e) {
            log.error(e);
            return null;
        }

    }

    public int getItemCount(String recordID) {
        long start = System.currentTimeMillis();
        try {
           String record = getRecord(recordID);
             logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.getItemCount( + " + recordID + ")  in:" + (System.currentTimeMillis() - start) + "ms");
            return record != null ?  ((Double)countItems.evaluate(new InputSource(new StringReader(record)), XPathConstants.NUMBER)).intValue() : 0;
        } catch (XPathExpressionException e) {
            log.warn("something wrong with xpath", e);
            return 0;
        } catch (RemoteException e) {
            log.warn("something wrong with xpath", e);
            return 0;
        }
    }

    public int[] getItemCounts(String[] recordIDs) {
        long start = System.currentTimeMillis();
        int[] j = new int[recordIDs.length];
        for (int i = 0; i< recordIDs.length; i++){
             j[i]= getItemCount(recordIDs[i]);
        }
         logPer.info("dk.statsbiblioteket.summa.search.SearchEngineImpl.getItemCounts  in:" + (System.currentTimeMillis() - start) + "ms");
        return j;
    }

}


