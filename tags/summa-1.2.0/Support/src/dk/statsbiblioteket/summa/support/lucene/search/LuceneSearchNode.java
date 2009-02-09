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
package dk.statsbiblioteket.summa.support.lucene.search;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.summa.common.index.IndexException;
import dk.statsbiblioteket.summa.search.document.DocumentSearcherImpl;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.SetBasedFieldSelector;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;

/**
 * Lucene-specific search node.
 *
 * IMPORTANT: This class is far from finished and is to be moved to another module
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: Support setMaxBooleanClauses
public class LuceneSearchNode extends DocumentSearcherImpl implements
                                                                  Configurable {
    private static Log log = LogFactory.getLog(LuceneSearchNode.class);

    /**
     * The maximum number of boolean clauses that a query can be expanded to.
     * </p><p>
     * This property is optional. Default is 10000.
     */
    public static final String CONF_MAX_BOOLEAN_CLAUSES =
            "summa.support.lucene.clauses.max";
    public static final int DEFAULT_MAX_BOOLEAN_CLAUSES = 10000;
    private int maxBooleanClauses = DEFAULT_MAX_BOOLEAN_CLAUSES;

    @SuppressWarnings({"FieldCanBeLocal"})
    private LuceneIndexDescriptor descriptor;
    private SummaQueryParser parser;
    private IndexSearcher searcher;
    private String location = null;
    private static final long WARMUP_MAX_HITS = 50;
    private static final int COLLECTOR_REQUEST_TIMEOUT = 20 * 1000;

    /**
     * Constructs a Lucene search node from the given configuration. This
     * involves the creation of an index descriptor.
     * @param conf the setup for the node. See {@link LuceneIndexUtils},
     *             {@link DocumentSearcherImpl} and {@link SearchNodeImpl} for
     *             details on keys and values.
     * @throws RemoteException if the node could not be initialized.
     */
    public LuceneSearchNode(Configuration conf) throws RemoteException {
        super(conf);
        log.info("Constructing LuceneSearchNode");
        maxBooleanClauses =
                conf.getInt(CONF_MAX_BOOLEAN_CLAUSES, maxBooleanClauses);
        log.trace("Setting max boolean clauses to " + maxBooleanClauses);
        BooleanQuery.setMaxClauseCount(maxBooleanClauses);
        descriptor = LuceneIndexUtils.getDescriptor(conf);
        parser = new SummaQueryParser(descriptor);
    }

    @Override
    public void managedOpen(String location) throws RemoteException {
        log.debug("Open called for location '" + location
                  + "'. Appending /" + LuceneIndexUtils.LUCENE_FOLDER);
        location +=  "/" + LuceneIndexUtils.LUCENE_FOLDER;
        if (this.location != null) {
            close();
        }
        this.location = location;
        if (location == null || "".equals(location)) {
            log.warn("open(null) called, no index available");
            return;
        }
        URL urlLocation = Resolver.getURL(location);
        if (urlLocation == null) {
            log.warn("Could not resolve URL for location '" + location
                     + "', no index available");
            return;
        }
        if ("".equals(urlLocation.getFile())) {
            throw new RemoteException(String.format(
                    // TODO: Consider if the exception should be eaten
                    "Could not resolve file from location '%s'", location));
        }
        try {
            searcher = new IndexSearcher(IndexReader.open(
                    FSDirectory.getDirectory(urlLocation.getFile()), true));
            log.debug("Opened Lucene searcher for " + urlLocation
                      + " with maxDoc " + searcher.maxDoc());
        } catch (CorruptIndexException e) {
            throw new RemoteException(String.format(
                    "Corrupt index at '%s'", urlLocation.getFile()), e);
        } catch (IOException e) {
            throw new RemoteException(String.format(
                    "Could not create an IndexSearcher for '%s'",
                    urlLocation.getFile()), e);
        }
        try {
            log.debug("Open finished for location '" + location
                      + "'. The searcher maxDoc is " + searcher.maxDoc());
        } catch (IOException e) {
            throw new RemoteException(String.format(
                    "Unable to determine searcher.maxDoc for searcher opened at"
                    + " location '%s'", location), e);
        }
    }

    @Override
    public void managedClose() {
        log.trace("close called");
        if (searcher != null) {
            try {
                searcher.getIndexReader().close();
                searcher.close();
            } catch (IOException e) {
                log.warn(String.format(
                        "Could not close index-connection to location '%s'. "
                        + "This will probably result in a resource-leak",
                        location), e);
            }
            //noinspection AssignmentToNull
            searcher = null;
        }
    }

    @Override
    public void managedWarmup(String query) {
        //noinspection OverlyBroadCatchBlock
        try {
            fullSearch(null, query, 0, WARMUP_MAX_HITS, null, false, null,
                       null);
        } catch (Throwable t) {
            log.warn("Throwable caught in warmup", t);
        }
    }

    public DocumentResponse fullSearch(String filter, String query,
                                       long startIndex, long maxRecords,
                                       String sortKey, boolean reverseSort,
                                       String[] fields, String[] fallbacks)
                                                        throws RemoteException {
        return fullSearch(filter, query, startIndex, maxRecords,
                          sortKey, reverseSort, fields, fallbacks, true);
    }

    private DocumentResponse fullSearch(String filter, String query,
                                    long startIndex, long maxRecords,
                                    String sortKey, boolean reverseSort,
                                    String[] fields, String[] fallbacks,
                                    boolean doLog) throws
                                                               RemoteException {
        if (searcher == null) {
            throw new IndexException("No searcher available", location, null);
        }
        if (startIndex > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "The Lucene search node does not support start indexes "
                    + "above Integer.MAX_VALUE. startIndex was " + startIndex);
        }
        if (maxRecords > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "The Lucene search node does not support max records "
                    + "above Integer.MAX_VALUE. max records was " + maxRecords);
        }
        if (startIndex + maxRecords > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "The Lucene search node does not support that start index +"
                    + " max records is above Integer.MAX_VALUE. "
                    + "start index was" + startIndex + " and max records was "
                    + maxRecords);
        }
        if (sortKey == null) {
            sortKey = getSortKey();
        }
        try {
            if (log.isTraceEnabled() && doLog) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("fullSearch('" + filter + "', '" + query + "', "
                          + startIndex + ", " + maxRecords + ", '" + sortKey
                          + "', " + reverseSort + ", " + Arrays.toString(fields)
                          + ", " + Arrays.toString(fallbacks) + ") called");
            }
            long startTime = System.currentTimeMillis();
            if (fields == null) {
                fields = getResultFields();
                fallbacks = getFallbackValues();
            }
            Filter luceneFilter = filter == null || "".equals(filter) ? null :
                             new QueryWrapperFilter(parser.parse(filter));
            Query luceneQuery = parser.parse(query);

            TopFieldDocs topDocs = searcher.search(
                    luceneQuery, luceneFilter,
                    (int)(startIndex + maxRecords), Sort.RELEVANCE);

            FieldSelector selector = new SetBasedFieldSelector(
                    new HashSet<String>(Arrays.asList(fields)),
                    new HashSet(5));

            DocumentResponse result =
                    new DocumentResponse(filter, query, startIndex, maxRecords,
                                     sortKey, reverseSort, fields, 0,
                                     topDocs.totalHits);
            // TODO: What about longs for startIndex and maxRecords?
            for (int i = (int)startIndex ;
                 i < topDocs.scoreDocs.length
                 && i < (int)(startIndex + maxRecords);
                 i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                // TODO: Get a service id and the sort value
                DocumentResponse.Record record =
                        new DocumentResponse.Record(
                                Integer.toString(scoreDoc.doc), "NA",
                                scoreDoc.score, null);
                Document doc =
                     searcher.getIndexReader().document(scoreDoc.doc, selector);
                for (int fieldIndex = 0; fieldIndex < fields.length;
                     fieldIndex++) {
                    String field = fields[fieldIndex];
                    Field iField = doc.getField(field);
                    if (iField == null || iField.stringValue() == null ||
                        "".equals(iField.stringValue())) {
                        if (fallbacks != null && fallbacks.length != 0) {
                            record.addField(new DocumentResponse.Field(
                                    field, fallbacks[fieldIndex],
                                    !nonescapedFields.contains(field)));
                        }
                    } else {
                        record.addField(new DocumentResponse.Field(
                                field, iField.stringValue(),
                                !nonescapedFields.contains(field)));
                    }
                }
                result.addRecord(record);
            }
            result.setSearchTime(System.currentTimeMillis()-startTime);
            if (doLog) {
                log.debug("fullSearch(..., '" + query + "', ...) returning "
                          + result.size() + "/" + topDocs.totalHits
                          + " hits found in "
                          + (System.currentTimeMillis()-startTime) + " ms");
            }
            return result;
        } catch (ParseException e) {
            throw new IndexException(String.format(
                    "ParseException during search for query '%s'", query),
                                     location, e);
        } catch (CorruptIndexException e) {
            throw new IndexException(String.format(
                    "CorruptIndexException during search for query '%s'",
                    query), location, e);
        } catch (RemoteException e) {
            throw new RemoteException(String.format(
                    "Inner RemoteException during search for query '%s'",
                    query), e);
        } catch (IOException e) {
            throw new IndexException(String.format(
                    "IOException during search for query '%s'", query),
                                     location, e);
        } catch (Throwable t) {
            throw new RemoteException(String.format(
                    "Exception during search for query '%s'", query), t);
        }
    }

    @Override
    protected DocIDCollector collectDocIDs(String query,
                                           String filter) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("collectDocIDs(" + filter + ", " + query + ") called");
        Filter luceneFilter;
        Query luceneQuery;
        try {
            //noinspection AssignmentToNull
            luceneFilter = filter == null || "".equals(filter) ? null :
                             new QueryWrapperFilter(parser.parse(filter));
        } catch (ParseException e) {
            throw new RemoteException(String.format(
                    "Unable to parse filter '%s'", query), e);
        }
        try {
            luceneQuery = parser.parse(query);
        } catch (ParseException e) {
            throw new RemoteException(String.format(
                    "Unable to parse query '%s'", query), e);
        }
        return collectDocIDs(luceneQuery, luceneFilter);
    }

    private DocIDCollector collectDocIDs(Query query, Filter filter) throws
                                                                   IOException {
        log.trace("collectDocIDs() called");
        long startTime = System.currentTimeMillis();
        DocIDCollector collector;
        try {
            collector = collectors.poll(COLLECTOR_REQUEST_TIMEOUT,
                                        TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RemoteException("Interrupted while requesting a "
                                      + "DocIDCollector from the queue");
        }
        if (collector == null) {
            throw new RemoteException(String.format(
                    "Timeout after %d milliseconds, while requesting a "
                    + "DocIDCollector", COLLECTOR_REQUEST_TIMEOUT));
        }
        if (filter == null) {
            //System.out.println(query);
            searcher.search(query, collector);
        } else {
            searcher.search(query, filter, collector);
        }
        if (log.isTraceEnabled()) {
            log.trace("Finished collectDocIDs in "
                      + (System.currentTimeMillis() - startTime)
                      + " ms with " + collector.getDocCount()
                      + " documents collected and the highest bit being "
                      + (collector.getBits().length() - 1));
        }
        return collector;
    }
}



