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

import dk.statsbiblioteket.summa.facetbrowser.util.ClusterCommon;
import dk.statsbiblioteket.summa.facetbrowser.connection.analysis.LocalFreeTextAnalyzer;
import dk.statsbiblioteket.summa.common.lucene.search.SlimCollector;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.ParallelReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Connection to the search index used by the cluster module.
 * The location of the index and the default search field should be given
 * in the file cluster.properties.xml or as parameters to the constructor.
 *
 * Updated for parallel indices.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexConnectionImplLocal implements IndexConnection {
    //LOG
    private static final Logger log = Logger.getLogger(IndexConnectionImplLocal.class);

    //PROPERTIES
    private String index_directory_name;
    private List<String> parallel_index_directories;
    private String default_field;//TODO: remove?

    //MAGIC CONSTANTS
    private static final String default_field_string = "default_field";
    private static final String default_default_field = ClusterCommon.FREETEXT;
    //TODO: loose some of these magic constants

    //PARSER AND SEARCHER
    private IndexSearcher searcher;

    /**
     * Connect to the Lucene index specified in the properties file.
     * The properties file is 'cluster.properties.xml'.
     * The key for the index location directory name is 'directory_name'.
     * The key for the default field is 'default_field'.
     */
    public IndexConnectionImplLocal() {
        init();

        try {
            IndexReader reader = IndexReader.open(index_directory_name);

            if (!parallel_index_directories.isEmpty()) {
                ParallelReader parallelReader = new ParallelReader();
                parallelReader.add(reader);
                for (String parallelIndexDirectoryName: parallel_index_directories) {
                    parallelReader.add(IndexReader.open(parallelIndexDirectoryName));
                }
                reader = parallelReader;
            }

            searcher = new IndexSearcher(reader);
            //TODO: do we need a reInitSearcher method?
        } catch (IOException e) {
            log.error(e); //TODO
        }

    }
    /**
     * Load properties and add index_directory_name and default_field.
     */
    private void init() {
        log.info("init: load properties.");
        index_directory_name = ClusterCommon.getProperty(ClusterCommon.INDEX_LOCATION);
        if (index_directory_name==null) {
            log.fatal("No directory name for an index is known.");
            System.exit(1);
        }

        parallel_index_directories = new LinkedList<String>();
        int count = 1;
        String parallelIndexPath =
                ClusterCommon.getProperty(ClusterCommon.INDEX_LOCATION+count);
        while (parallelIndexPath!=null) {
            parallel_index_directories.add(parallelIndexPath);
            count++;
            parallelIndexPath =
                    ClusterCommon.getProperty(ClusterCommon.INDEX_LOCATION+count);
        }

        default_field = ClusterCommon.getProperty(default_field_string);
        if (default_field==null) {
            default_field = default_default_field;
            log.warn("Default field add to default value.");
        }

        log.info("init: done.");
    }

    /**
     * Connect to the Lucene index specified by the given directory name.
     * Note that this method connects to a single index given by the parameter.
     * @param directoryName the location of the index
     * @param defaultField the default field for searches in the index
     */
    public IndexConnectionImplLocal(String directoryName, String defaultField) {
        index_directory_name = directoryName;

        try {
            searcher = new IndexSearcher(index_directory_name);
        } catch (IOException e) {
            log.error(e);
        }

        if (defaultField!=null) {
            default_field = defaultField;
        } else {
            default_field = default_default_field;
            log.info("Default field add to default value.");
        }

    }
    /**
     * Connect to the Lucene index specified by the given directory name.
     * Note that this method connects to a single index given by the parameter.
     * @param directoryName the location of the index
     */
    public IndexConnectionImplLocal(String directoryName) {
        this(directoryName, default_default_field);
    }

    /**
     * Disconnect from the index.
     */
    public void disconnect() {
        try {
            searcher.close();
        } catch (IOException e) {
            log.error(e);
        }
        searcher = null;
    }

    /**
     * Get the 'number of results' top results for the given query string.
     * @return TopDocs
     */
    public TopDocs getTopResults(String queryString, int numberOfResults) {
        Query luceneQuery;
        QueryParser summaQueryParser =
                new QueryParser("freetext",
                                new PerFieldAnalyzerWrapper(
                                        new LocalFreeTextAnalyzer()));
//        SummaQueryParser summaQueryParser =
//                SearchEngineImpl.getInstance().getSummaQueryParser();
        try {
            luceneQuery = summaQueryParser.parse(queryString);
        } catch (ParseException e) {
            log.warn("Cannot parse query: '" + queryString + "'", e);
            return null;
        }
        TopDocs topDocs;
        try {
            topDocs = searcher.search(luceneQuery, null, numberOfResults);
        } catch (IOException e) {
            log.warn("Error performing query: '" + queryString + "'", e);
            return null;
        } catch (BooleanQuery.TooManyClauses e) {
            log.warn("Error performing query: '" + queryString + "'", e);
            return null;
        }
        return topDocs;
    }

    /**
     * Get the document with the given doc id.
     * Note that the documents only contains the stored fields.
     * @return the requested Document; null on invalid id
     */
    public Document getDoc(int id) {
        Document doc = null;
        if (searcher!=null) {
            try {
                doc = searcher.doc(id);
            } catch (IOException e) {
                log.error(e);
            }
        }
        return doc;
    }

    
    /**
     * Get the index searcher used by this object.
     * @return the Searcher
     */
    public IndexSearcher getSearcher() {
        return searcher;
    }

    public TermFreqVector[] getTermFreqVectors(int id) {
        IndexReader reader = searcher.getIndexReader();
        try {
            return reader.getTermFreqVectors(id);
        } catch (IOException e) {
            log.error(e);
            return null;
        }
    }

    public String[] getShortFormatTerms(int id) {
        return getTerms(id, ClusterCommon.SHORTFORMAT);
    }
    public String[] getTerms(int id, String field) {
        IndexReader reader = searcher.getIndexReader();
        try {
            TermFreqVector tfv = reader.getTermFreqVector(id, field);
            if (tfv==null) {return null;}
            return tfv.getTerms();
        } catch (IOException e) {
            log.error(e);
            return null;
        }
    }

    /**
     * Get the results for the given query string.
     * @return Hits
     */
    public Hits getResults(String queryString) {
        Query luceneQuery;
        QueryParser summaQueryParser =
                new QueryParser("freetext",
                                new PerFieldAnalyzerWrapper(
                                        new LocalFreeTextAnalyzer()));
//        SummaQueryParser summaQueryParser =
//                SearchEngineImpl.getInstance().getSummaQueryParser();
        try {
            luceneQuery = summaQueryParser.parse(queryString);
        } catch (ParseException e) {
            log.warn("Cannot parse query: '" + queryString + "'", e);
            return null;
        }
        Hits hits;
        try {
            hits = searcher.search(luceneQuery);
        } catch (IOException e) {
            log.warn("Error performing query: '" + queryString + "'", e);
            return null;
        } catch (BooleanQuery.TooManyClauses e) {
            log.warn("Error performing query: '" + queryString + "'", e);
            return null;
        }
        return hits;
    }

    public TermFreqVector getTermFreqVector(int docNumber, String field) {
        try {
            return searcher.getIndexReader().getTermFreqVector(docNumber, field);
        } catch (IOException ex) {
            log.error(ex);
            return null;
        }
    }

    public SlimCollector getSlimDocs(String query) {
        SlimCollector collector = new SlimCollector();
        Query luceneQuery;
        QueryParser queryParser = new QueryParser("freetext",
                                                  new PerFieldAnalyzerWrapper(new LocalFreeTextAnalyzer()));
//                SearchEngineImpl.getInstance().getSummaQueryParser();
        try {
            luceneQuery = queryParser.parse(query);// queryParser.parse(query);
        } catch (ParseException e) {
            log.warn("Cannot parse query: '" + query + "'", e);
            return null;
        }

        try {
            searcher.search(luceneQuery, collector);
        } catch (IOException e) {
            log.warn("Could not run collector search with " + query);
            return null;
        }
        return collector;
    }

    public IndexReader getIndexReader() {
        return searcher.getIndexReader();
    }

    public void releaseSlimCollector(SlimCollector collector) {
        // TODO: Implement this
    }
}


