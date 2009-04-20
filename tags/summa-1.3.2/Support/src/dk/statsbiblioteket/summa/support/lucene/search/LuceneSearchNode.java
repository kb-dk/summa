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

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.index.IndexException;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.summa.search.document.DocumentSearcherImpl;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.SetBasedFieldSelector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

    /**
     * If present, normal search will be skipped an a MoreLikeThis-search will
     * be performed. The recordid is verbatim for the record (document) that
     * should be used as base for the MoreLikethis-functionality.
     * </p><p>
     * Optional. If no value is present, MoreLikeThis will not be active.
     * @see {@link dk.statsbiblioteket.summa.search.api.document.DocumentKeys#SEARCH_START_INDEX}.
     * @see {@link dk.statsbiblioteket.summa.search.api.document.DocumentKeys#SEARCH_MAX_RECORDS}.
     */
    public static final String SEARCH_MORELIKETHIS_RECORDID =
            "search.document.lucene.morelikethis.recordid";

    /**
     * A sub-configuration for the MoreLikeThis functionality. All tweaks to
     * MoreLikeThis must go into this sub configuration.
     * </p><p>
     * Optional. If no sub configuration is present, MoreLikeThis uses default
     * values.
     */
    public static final String CONF_MORELIKETHIS_CONF =
            "summa.support.lucene.morelikethis.configuration";

    /**
     * If true, the MoreLikeThis-functionality is enabled. MoreLikeThis
     * co-exists peacefully with the standard search, although only one the
     * results from one of the modes can be returned at a time.
     * </p><p>
     * Optional. Default is false;
     */
    public static final String CONF_MORELIKETHIS_ENABLED =
            "summa.support.lucene.morelikethis.enabled";
    public static final boolean DEFAULT_MORELIKETHIS_ENABLED = true;

    /* http://lucene.apache.org/java/2_4_0/api/contrib-queries/org/apache/lucene/search/similar/MoreLikeThis.html */

    /**
     * Lucene MoreLikeThis property.<br />
     * "Sets the frequency below which terms will be ignored in the source doc".
     * </p><p>
     * Optional. If not defined, Lucene MoreLikeThis defaults will be used.
     */
    public static final String CONF_MORELIKETHIS_MINTERMFREQ =
            "summa.support.lucene.morelikethis.mintermfreq";

    /**
     * Lucene MoreLikeThis property.<br />
     * "Sets the frequency at which words will be ignored which do not occur in
     * at least this many docs".
     * </p><p>
     * Optional. If not defined, Lucene MoreLikeThis defaults will be used.
     */
    public static final String CONF_MORELIKETHIS_MINDOCFREQ =
            "summa.support.lucene.morelikethis.mindocfreq";

    /**
     * Lucene MoreLikeThis property.<br />
     * "Sets the minimum word length below which words will be ignored".
     * </p><p>
     * Optional. If not defined, Lucene MoreLikeThis defaults will be used.
     */
    public static final String CONF_MORELIKETHIS_MINWORDLENGTH =
            "summa.support.lucene.morelikethis.minwordlength";

    /**
     * Lucene MoreLikeThis property.<br />
     * "Returns the maximum word length above which words will be ignored.
     *  Set this to 0 for no maximum word length. The default is
     * {@link MoreLikeThis#DEFAULT_MAX_WORD_LENGTH"}..
     * </p><p>
     * Optional. If not defined, Lucene MoreLikeThis defaults will be used.
     */
    public static final String CONF_MORELIKETHIS_MAXWORDLENGTH =
            "summa.support.lucene.morelikethis.maxwordlength";

    /**
     * Lucene MoreLikeThis property.<br />
     * "Sets the maximum number of query terms that will be included in any
     *  generated query".
     * </p><p>
     * Optional. If not defined, Lucene MoreLikeThis defaults will be used.
     */
    public static final String CONF_MORELIKETHIS_MAXQUERYTERMS =
            "summa.support.lucene.morelikethis.maxwueryterms";

    /**
     * Lucene MoreLikeThis property.<br />
     * "The maximum number of tokens to parse in each example doc field that is
     *  not stored with TermVector support".
     * </p><p>
     * Optional. If not defined, Lucene MoreLikeThis defaults will be used.
     */
    public static final String CONF_MORELIKETHIS_MAXNUMTOKENSPARSED =
            "summa.support.lucene.morelikethis.maxnumtokensparsed";

    /**
     * Lucene MoreLikeThis property.<br />
     * "Set the set of stopwords. Any word in this set is considered
     *  'uninteresting' and ignored. Even if your Analyzer allows stopwords,
     *  you might want to tell the MoreLikeThis code to ignore them, as for the
     *  purposes of document similarity it seems reasonable to assume that
     * 'a stop word is never interesting'".
     * </p><p>
     * The stop words is given as a list of Strings.
     * </p><p>
     * Optional. If not defined, Lucene MoreLikeThis defaults will be used.
     */
    public static final String CONF_MORELIKETHIS_STOPWORDS =
            "summa.support.lucene.morelikethis.stopwords";

    @SuppressWarnings({"FieldCanBeLocal"})
    private LuceneIndexDescriptor descriptor;
    private SummaQueryParser parser;
    private IndexSearcher searcher;
    private String location = null;
    private static final long WARMUP_MAX_HITS = 50;
    private static final int COLLECTOR_REQUEST_TIMEOUT = 20 * 1000;

    private boolean mlt_enabled = DEFAULT_MORELIKETHIS_ENABLED;
    private Integer mlt_minTermFreq = null;
    private Integer mlt_minDocFrew = null;
    private Integer mlt_minWordLength = null;
    private Integer mlt_maxWordLength = null;
    private Integer mlt_maxQueryTerms = null;
    private Integer mlt_maxNumTokensParsed = null;
    private Set<String> mlt_stopWords = null;
    private MoreLikeThis moreLikeThis = null;

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

        // MoreLikeThis
        if (!conf.valueExists(CONF_MORELIKETHIS_CONF)) {
            log.debug("No MoreLikeThis configuration present, skipping with "
                      + "MoreLikeThis.enabled == " + mlt_enabled);
            return;
        }
        log.debug("Opening and extracting MoreLikeThis-config");
        Configuration mltConf;
        try {
            mltConf = conf.getSubConfiguration(CONF_MORELIKETHIS_CONF);
            if (mltConf == null) {
                log.debug("No MoreLikeThis sub configuration present at '"
                          + CONF_MORELIKETHIS_CONF + "'");
                return;
            }
        } catch (IOException e) {
            log.error(String.format(
                    "The key '%s' existed, but did not resolve to a sub "
                    + "configuration. The configuration for MoreLikeThis "
                    + "will be ignored", CONF_MORELIKETHIS_CONF), e);
            return;
        }
        mlt_enabled = mltConf.valueExists(CONF_MORELIKETHIS_ENABLED) ?
                      mltConf.getBoolean(CONF_MORELIKETHIS_ENABLED) :
                      mlt_enabled;
        mlt_minTermFreq = getIntOrNull(mltConf, CONF_MORELIKETHIS_MINTERMFREQ);
        mlt_minDocFrew = getIntOrNull(mltConf, CONF_MORELIKETHIS_MINDOCFREQ);
        mlt_minWordLength =
                getIntOrNull(mltConf, CONF_MORELIKETHIS_MINWORDLENGTH);
        mlt_maxWordLength =
                getIntOrNull(mltConf, CONF_MORELIKETHIS_MAXWORDLENGTH);
        mlt_maxQueryTerms =
                getIntOrNull(mltConf, CONF_MORELIKETHIS_MAXQUERYTERMS);
        mlt_maxNumTokensParsed =
                getIntOrNull(mltConf, CONF_MORELIKETHIS_MAXNUMTOKENSPARSED);
        List<String> stopWords = mltConf.getStrings(
                CONF_MORELIKETHIS_STOPWORDS, (List<String>)null);
        if (stopWords != null) {
            mlt_stopWords = new HashSet<String>(stopWords);
        }
        log.debug(String.format(
                "Finished setting up MoreLikeThis with enabled=%s, "
                + "minTermFreq=%s, minDocFreq=%s, minWordLength=%s, "
                + "maxWordLength=%s, maxQueryTerms=%s, maxNumTokensParsed=%s, "
                + "stopWords-count=%s",
                mlt_enabled, mlt_minTermFreq, mlt_minDocFrew, mlt_minWordLength,
                mlt_minWordLength, mlt_maxQueryTerms, mlt_maxNumTokensParsed,
                mlt_stopWords == null ? "[None]" : mlt_stopWords.size()));
    }

    private Integer getIntOrNull(Configuration conf, String key) {
        return conf.valueExists(key) ? conf.getInt(key) : null;
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
            createMoreLikeThis();
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

    private void createMoreLikeThis() {
        if (!mlt_enabled) {
            log.trace("MoreLikethis disabled");
            return;
        }
        log.trace("Opening MoreLikeThis");

        moreLikeThis = new MoreLikeThis(searcher.getIndexReader());
        if (mlt_minTermFreq != null) {
            moreLikeThis.setMinTermFreq(mlt_minTermFreq);
        }
        if (mlt_minDocFrew != null) {
            moreLikeThis.setMinDocFreq(mlt_minDocFrew);
        }
        if (mlt_minWordLength != null) {
            moreLikeThis.setMinWordLen(mlt_minWordLength);
        }
        if (mlt_maxWordLength != null) {
            moreLikeThis.setMaxWordLen(mlt_maxWordLength);
        }
        if (mlt_maxQueryTerms != null) {
            moreLikeThis.setMaxQueryTerms(mlt_maxQueryTerms);
        }
        if (mlt_maxNumTokensParsed != null) {
            moreLikeThis.setMaxNumTokensParsed(mlt_maxNumTokensParsed);
        }
        if (mlt_stopWords != null) {
            moreLikeThis.setStopWords(mlt_stopWords);
        }
        if (descriptor.getMoreLikethisFields().size() == 0) {
            log.warn("No MoreLikethis-fields defined in LuceneIndexDescriptor. "
                     + "MoreLikethis probably won't return any results");
        } else {
            moreLikeThis.setFieldNames(
                    descriptor.getMoreLikethisFields().toArray(new String[
                            descriptor.getMoreLikethisFields().size()]));
        }
        log.debug("MoreLikeThis created for reader for '" + location + "'");
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
            fullSearch(null, null, query, 0, WARMUP_MAX_HITS, null, false, null,
                       null);
        } catch (Throwable t) {
            log.warn("Throwable caught in warmup", t);
        }
    }

    @Override
    protected boolean isRequestUsable(Request request) {
        return request.containsKey(SEARCH_MORELIKETHIS_RECORDID)
               || super.isRequestUsable(request);
    }

    public DocumentResponse fullSearch(Request request,
            String filter, String query, long startIndex, long maxRecords,
            String sortKey, boolean reverseSort,
            String[] fields, String[] fallbacks) throws RemoteException {
        return fullSearch(request, filter, query, startIndex, maxRecords,
                          sortKey, reverseSort, fields, fallbacks, true);
    }

    private DocumentResponse fullSearch(
            Request request, String filter, String query,
            long startIndex, long maxRecords,
            String sortKey, boolean reverseSort,
            String[] fields, String[] fallbacks,
            boolean doLog) throws RemoteException {
        sanityCheck(startIndex, maxRecords);
        if (sortKey == null) {
            sortKey = getSortKey();
        }
        Filter luceneFilter;
        Query luceneQuery;
        try {
            if (log.isTraceEnabled() && doLog) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("fullSearch('" + filter + "', '" + query + "', "
                          + startIndex + ", " + maxRecords + ", '" + sortKey
                          + "', " + reverseSort + ", " + Arrays.toString(fields)
                          + ", " + Arrays.toString(fallbacks) + ") called");
            }
            if (fields == null) {
                fields = getResultFields();
                fallbacks = getFallbackValues();
            }
            luceneFilter = parseFilter(filter);
            luceneQuery = parseQuery(request, query);
        } catch (ParseException e) {
            throw new IndexException(String.format(
                    "ParseException during fullSearch for query '%s'", query),
                                     location, e);
        }
        if (luceneQuery == null) {
            return new DocumentResponse(
                    filter, query, startIndex, maxRecords, sortKey,
                    reverseSort, fallbacks, 0, 0);
        }
        log.trace("Calling private fullSearch with parsed query");
        return fullSearch(luceneFilter, luceneQuery, filter, query,
                          startIndex, maxRecords, sortKey, reverseSort,
                          fields, fallbacks, doLog);
    }

    // Can return null on MoreLikeThis parsing
    private Query parseQuery(Request request, String query) throws
                                                            RemoteException,
                                                            ParseException {
        if (!mlt_enabled
            || !request.containsKey(SEARCH_MORELIKETHIS_RECORDID)) {
            return query == null ? null : parser.parse(query);
        }
        if (moreLikeThis == null) {
            throw new RemoteException(
                    "MoreLikethis not initialized (Index might not have been"
                    + " opened)");
        }

        String recordID = request.getString(SEARCH_MORELIKETHIS_RECORDID);
        log.trace("constructing MoreLikeThis query for '" + recordID + "'");
        if (recordID == null || "".equals(recordID)) {
            throw new ParseException(
                    "RecordID invalid. Expected something, got '" + recordID
                    + "'");
        }
        int docID;
        Query moreLikeThisQuery;
        try {
            TermQuery q = new TermQuery(new Term(
                    IndexUtils.RECORD_FIELD, recordID));
            TopDocs recordDocs = searcher.search(q, null, 1);
            if (recordDocs.totalHits == 0) {
                log.debug("Unable to locate recordID in MoreLikeThis query "
                          + "resolving for '" + recordID + "'");
                return null;
            }
            // TODO: This really needs to be updated for storage use
            // In a distributed environment, only the Searcher containing the
            // document will return any hits. Just as bad: The doc-id-trick only
            // works within the index that contains the document.
            docID = recordDocs.scoreDocs[0].doc;
            moreLikeThisQuery = moreLikeThis.like(docID);
        } catch (IOException e) {
            log.error("Unable to create MoreLikeThis query for "
                      + recordID + "'", e);
            return null;
        }
        if (log.isTraceEnabled()) {
            log.trace("Created MoreLikeThis query for '" + recordID
                      + "' with docID " + docID + ": "
                      + SummaQueryParser.queryToString(moreLikeThisQuery));
        }
        return moreLikeThisQuery;
    }

    private Filter parseFilter(String filter) throws ParseException {
        return filter == null || "".equals(filter) ? null :
               new QueryWrapperFilter(parser.parse(filter));
    }

    private DocumentResponse fullSearch(
            Filter luceneFilter, Query luceneQuery, String filter, String query,
            long startIndex, long maxRecords, String sortKey,
            boolean reverseSort, String[] fields, String[] fallbacks,
            boolean doLog) throws RemoteException {
        long startTime = System.currentTimeMillis();
        try {
            TopFieldDocs topDocs = searcher.search(
                    luceneQuery, luceneFilter,
                    (int)(startIndex + maxRecords), Sort.RELEVANCE);

            if (log.isTraceEnabled()) {
                log.trace("Got " + topDocs.totalHits + " hits for query "
                          + SummaQueryParser.queryToString(luceneQuery));
            }

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

    private void sanityCheck(long startIndex, long maxRecords) throws
                                                                IndexException {
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
    }

    @Override
    protected DocIDCollector collectDocIDs(
            Request request, String query, String filter) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("collectDocIDs(" + filter + ", " + query + ") called");
        Filter luceneFilter;
        Query luceneQuery;
        try {
            //noinspection AssignmentToNull
            luceneFilter = parseFilter(filter);
        } catch (ParseException e) {
            throw new RemoteException(String.format(
                    "Unable to parse filter '%s'", query), e);
        }
        log.trace("Parsing collectDocID query '" + query + "'");
        try {
            luceneQuery = parseQuery(request, query);
        } catch (ParseException e) {
            throw new RemoteException(String.format(
                    "Unable to parse query '%s'", query), e);
        }
        if (luceneQuery == null) {
            throw new RemoteException(String.format(
                    "The query '%s' parsed to null", query));
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

    /**
     * @return the docCount for the currently opened index or -1 if no index is
     * opened.
     * @throws java.io.IOException if the index query failed.
     */
    public int getDocCount() throws IOException {
        return searcher == null ? -1 : searcher.maxDoc();
    }
}
