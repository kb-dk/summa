/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.support.lucene.search;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexException;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.summa.search.document.DocumentSearcherImpl;
import dk.statsbiblioteket.summa.support.api.LuceneKeys;
import dk.statsbiblioteket.summa.support.lucene.SummaIndexReader;
import dk.statsbiblioteket.summa.support.lucene.TermProviderImpl;
import dk.statsbiblioteket.summa.support.lucene.search.sort.SortPool;
import dk.statsbiblioteket.summa.support.lucene.search.sort.SortFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
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
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanFilter;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilterClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.DocIdBitSet;
import org.apache.lucene.util.OpenBitSetDISI;

import java.io.File;
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
 * </p><p>
 * Besides the properties given in this class, the properties for
 * {@link SummaIndexReader} should be defined.
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
            "summa.support.lucene.morelikethis.maxqueryterms";

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

    /**
     * If true, an explanation for the inclusion of a document in the search
     * result is provided, unless it is explicitely disabled in the query.
     * Note that the calculation of an explanation is computationally heavy,
     * so this should only be disabled for testing purposes.
     * </p><p>
     * Optional. Default is false;
     */
    public static final String CONF_EXPLAIN = "summa.support.lucene.explain";
    public static final boolean DEFAULT_EXPLAIN = false;

    /**
     * The sub-configuration used for the distributed term stat setup.
     * If the configuration is present, distributed term stats will be enabled.
     * As there are no mandatory properties for the distributed term stats,
     * the configuration does not need to contain anything. However, it is
     * recommended to review the properties for SummaIndexreader and
     * TermProviderImpl.
     * </p><p>
     * The constructor for {@link TermProviderImpl} uses this sub configuration.
     * </p><p>
     * Optional.
     * @see {@link SummaIndexReader}.
     * @see {@link TermProviderImpl}.
     */
    public static final String CONF_TERMSTAT_CONFIGURATION =
            "summa.support.lucene.termstatconfiguration";

    /**
     * The comparator implementation to use for sorting. Possible values are
     * lucene: The build-in Comparator. Loads all terms for the given field into
     *         RAM and creates Collator-keys for them.<br />
     *         Pros: Fast startup-time, efficient on re-open.<br />
     *         Cons: Consumes a lot of memory, so-so fast on actual sort.<br />
     * localstatic: Uses an optimized Collator and creates an array with
     *         sort-order for the terms in the given field.<br />
     *         Pros: Fast startup, best actual sort-performance,<br />
     *         Cons: Temporarily consumes a lot of memory at startup.<br />
     * multipass: Uses an optimized collator and creates a structure with
     *         sort-order for the terms in the given field.<br />
     *         Pros: Customizable memory-usage at the cost of startup time,
     *               faster than build-in sort in actual sort-performance.<br/ >
     *         Cons: Long startup-time if low memory-usage is requested.
     * </p><p>
     * Optional. Default is localstatic.
     */
    public static final String CONF_SORT_COMPARATOR =
            "summa.support.lucene.sort.comparator";
    public static final String DEFAULT_SORT_COMPARATOR =
            SortFactory.DEFAULT_COMPARATOR.toString();

    /**
     * The buffer-size used by the multipass sort comparator implementation.
     * </p><p>
     * Optional. Default is 100MB.
     */
    public static final String CONF_SORT_BUFFER =
            "summa.support.lucene.sort.buffer";
    public static final int DEFAULT_SORT_BUFFER = SortFactory.DEFAULT_BUFFER;

    /**
     * If true, fields with sortLocales are warmed upon index opening. Warming a sorter takes
     * time (~3-4 minutes for 10 million documents) if there is a sort localea String-field
     * with many unique terms
     */
    public static final String CONF_WARMUP_SORTERS =
            "summa.support.lucene.warmup.warmsorters";
    public static final boolean DEFAULT_WARMUP_SORTERS = false;

    @SuppressWarnings({"FieldCanBeLocal"})
    private LuceneIndexDescriptor descriptor;
    private SortPool sortPool; // Toed to the descriptor
    private Configuration conf;
    private boolean loadDescriptorFromIndex;
    private SummaQueryParser parser;
    private IndexSearcher searcher;
    private String location = null;
    private static final long WARMUP_MAX_HITS = 50;
    private static final int COLLECTOR_REQUEST_TIMEOUT = 20 * 1000;
    private boolean explain = DEFAULT_EXPLAIN;

    private boolean mlt_enabled = DEFAULT_MORELIKETHIS_ENABLED;
    private Integer mlt_minTermFreq =   null;
    private Integer mlt_minDocFrew =    null;
    private Integer mlt_minWordLength = null;
    private Integer mlt_maxWordLength = null;
    private Integer mlt_maxQueryTerms = null;
    private Integer mlt_maxNumTokensParsed = null;
    private Set<String> mlt_stopWords = null;
    private MoreLikeThis moreLikeThis = null;
    private TermProviderImpl termProvider = null;

    private SortFactory.COMPARATOR sortComparator;
    private int sortBuffer;

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
        this.conf = conf;
        maxBooleanClauses =
                conf.getInt(CONF_MAX_BOOLEAN_CLAUSES, maxBooleanClauses);
        log.trace("Setting max boolean clauses to " + maxBooleanClauses);
        BooleanQuery.setMaxClauseCount(maxBooleanClauses);
        // TODO: Add override-switch to state where to get the descriptor
        loadDescriptorFromIndex = !conf.valueExists(
                IndexDescriptor.CONF_DESCRIPTOR);
        if (loadDescriptorFromIndex) {
            log.debug("No explicit IndexDescriptor-setup defined. The index "
                      + "description will be loaded from the index-folder upon "
                      + "calls to open");
        } else {
            log.info(String.format(
                    "The property %s was defined, so the IndexDescriptor will "
                    + "not be taken from the index-folder. Note that this makes"
                    + " it hard to coordinate major updates to the "
                    + "IndexDescriptor in a production system",
                    IndexDescriptor.CONF_DESCRIPTOR));
            setDescriptor(LuceneIndexUtils.getDescriptor(conf));
        }

        sortComparator = SortFactory.COMPARATOR.parse(conf.getString(
                CONF_SORT_COMPARATOR, DEFAULT_SORT_COMPARATOR));
        sortBuffer = conf.getInt(CONF_SORT_BUFFER, DEFAULT_SORT_BUFFER);

        // MoreLikeThis
        setupMoreLikeThis(conf);

        explain = conf.getBoolean(CONF_EXPLAIN, explain);

        if (conf.valueExists(CONF_TERMSTAT_CONFIGURATION)) {
            log.debug("Enabling distributed term stats");
            try {
                termProvider = new TermProviderImpl(
                        conf.getSubConfiguration(CONF_TERMSTAT_CONFIGURATION));
            } catch (SubConfigurationsNotSupportedException e) {
                throw new ConfigurationException(
                        "Storage doesn't support sub configurations");
            } catch (NullPointerException e) {
                throw new ConfigurationException(String.format(
                        "Exception extracting sub-configuration '%s'",
                        CONF_TERMSTAT_CONFIGURATION), e);
            }
        } else {
            log.debug(String.format("The property '%s' was not present, so no "
                                    + "distributed term stats will be used",
                                    CONF_TERMSTAT_CONFIGURATION));
        }
    }

    private void setupMoreLikeThis(Configuration conf) {
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
        } catch (SubConfigurationsNotSupportedException e) {
            throw new ConfigurationException(
                    "Storage doesn't support sub configurations");
        } catch (NullPointerException e) {
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
        String baseLocation = location;
        if (location == null || "".equals(location)) {
            log.warn("open(null) called, no index available");
            return;
        }
        location +=  "/" + LuceneIndexUtils.LUCENE_FOLDER;
        if (this.location != null) {
            close();
        }
        this.location = location;
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
        if (loadDescriptorFromIndex) {
            openDescriptor(baseLocation);
        } else {
            sortPool = new SortPool(sortComparator, sortBuffer, descriptor);
        }
        try {
            searcher = new IndexSearcher(getIndexreader(urlLocation));
            sortPool.indexChanged(searcher.getIndexReader());
            log.debug("Opened Lucene searcher for " + urlLocation
                      + " with maxDoc " + searcher.maxDoc());
            createMoreLikeThis();
        } catch (CorruptIndexException e) {
            throw new RemoteException(String.format(
                    "Corrupt index at '%s'", urlLocation), e);
        } catch (IOException e) {
            throw new RemoteException(String.format(
                    "Could not create an IndexSearcher for '%s'",
                    urlLocation), e);
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

    private IndexReader getIndexreader(URL location) throws IOException {
        IndexReader reader = IndexReader.open(new NIOFSDirectory(
                 new File(Resolver.urlToFile(location).getAbsolutePath())), true);
        return termProvider == null ? reader :
               new SummaIndexReader(reader, termProvider);
    }

    private void openDescriptor(String location) throws RemoteException {
        log.trace("Opening descriptor from '" + location + "'");
        URL urlLocation = Resolver.getURL(
                location + "/" + IndexDescriptor.DESCRIPTOR_FILENAME);
        try {
            setDescriptor(new LuceneIndexDescriptor(urlLocation));
        } catch (IOException e) {
            throw new RemoteException(String.format(
                    "Unable to create LuceneIndexDescriptor from location '%s' "
                    + "resolved to URL '%s'",
                    location, urlLocation), e);
        }
    }

    private void setDescriptor(LuceneIndexDescriptor descriptor) {
        this.descriptor = descriptor;
        sortPool = new SortPool(sortComparator, sortBuffer, descriptor);
        parser = new SummaQueryParser(conf, descriptor);
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
        return request.containsKey(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID)
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
        if (luceneQuery == null && luceneFilter == null) {
            log.debug("Both query and filter is null, returning empty result");
            return new DocumentResponse(
                    filter, query, startIndex, maxRecords, sortKey,
                    reverseSort, fallbacks, 0, 0);
        }
        log.trace("Calling private fullSearch with parsed query");
        return fullSearch(request, luceneFilter, luceneQuery, filter, query,
                          startIndex, maxRecords, sortKey, reverseSort,
                          fields, fallbacks, doLog);
    }

    private Query MATCH_ALL = new MatchAllDocsQuery();

    // Can return null on MoreLikeThis parsing
    private Query parseQuery(Request request, String query) throws
                                               RemoteException, ParseException {
        if (!isMoreLikeThisRequest(request)) {
            log.debug("parseQuery(...): Returning plain query instead of "
                      + "MoreLikeThis");
            return matchAllParse(query);
        }
        if (moreLikeThis == null) {
            throw new RemoteException(
                    "MoreLikethis not initialized (Index might not have been"
                    + " opened)");
        }

        log.debug("Performing MoreLikeThis query parsing from request");
        String recordID;
        try {
            recordID = request.getString(
                    LuceneKeys.SEARCH_MORELIKETHIS_RECORDID, null);
            if (recordID == null) {
                log.warn(String.format(
                        "Got null when requesting String for key '%s'. This "
                        + "fails sanity-checking. Switching to plain query",
                        LuceneKeys.SEARCH_MORELIKETHIS_RECORDID));
                return matchAllParse(query);
            }
        } catch (final Exception e) {
            throw new ParseException(
                    String.format("Exception while requesting String for '%s' "
                                  + "with default value null", 
                                  LuceneKeys.SEARCH_MORELIKETHIS_RECORDID)) {
                {
                    initCause(e);
                }
            };
        }
        log.trace("constructing MoreLikeThis query for '" + recordID + "'");
        if ("".equals(recordID)) {
            throw new ParseException(String.format(
                    "RecordID invalid. Expected something, got '%s'",
                    recordID));
        }
        int docID;
        Query moreLikeThisQuery;
        try {
            TermQuery q = new TermQuery(new Term(
                    IndexUtils.RECORD_FIELD, recordID));
            TopDocs recordDocs = searcher.search(q, null, 1);
            if (recordDocs.totalHits == 0) {
                throw new RemoteException(String.format(
                        "Unable to locate recordID '%s' in MoreLikeThis query",
                        recordID));
            }
            // TODO: This really needs to be updated for storage use
            // In a distributed environment, only the Searcher containing the
            // document will return any hits. Just as bad: The doc-id-trick only
            // works within the index that contains the document.
            docID = recordDocs.scoreDocs[0].doc;
            moreLikeThisQuery = moreLikeThis.like(docID);
        } catch (IOException e) {
            throw new RemoteException(String.format(
                    "Unable to create MoreLikeThis query for recordID '%s'",
                    recordID), e);
        }
        if (log.isTraceEnabled()) {
            log.trace("Created MoreLikeThis query for '" + recordID
                      + "' with docID " + docID + ": "
                      + SummaQueryParser.queryToString(moreLikeThisQuery));
        }
        return moreLikeThisQuery;
    }

    private Query matchAllParse(String query) throws ParseException {
        return query == null || "".equals(query) || "*".equals(query)
               || "(*)".equals(query)
               ? MATCH_ALL : getParser().parse(query);
    }

    private boolean isMoreLikeThisRequest(Request request) {
        return mlt_enabled &&
               request.containsKey(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID);
    }

    private Filter parseFilter(String filter) throws ParseException {
        return filter == null || "".equals(filter) || "*".equals(filter)
               ? null : new QueryWrapperFilter(getParser().parse(filter));
    }

    private DocumentResponse fullSearch(Request request,
            Filter luceneFilter, Query luceneQuery, String filter, String query,
            long startIndex, long maxRecords, String sortKey,
            boolean reverseSort, String[] fields, String[] fallbacks,
            boolean doLog) throws RemoteException {
        long startTime = System.currentTimeMillis();
        boolean mlt_request = isMoreLikeThisRequest(request);
        try {
            // MoreLikeThis needs an extra in max to compensate for self-match

            TopFieldDocs topDocs = searcher.search(
                    luceneQuery, luceneFilter,
                    (int)(startIndex + maxRecords + (mlt_request ? 1 : 0)),
                    mlt_request || sortKey == null
                    || sortKey.equals(DocumentKeys.SORT_ON_SCORE)
                    ? Sort.RELEVANCE : sortPool.getSort(sortKey, reverseSort));

            if (log.isTraceEnabled()) {
                log.trace("Got " + topDocs.totalHits + " hits for query "
                          + SummaQueryParser.queryToString(luceneQuery));
            }

            FieldSelector selector = new SetBasedFieldSelector(
                    new HashSet<String>(Arrays.asList(fields)),
                    new HashSet(5));


            if (request.getBoolean(DocumentKeys.SEARCH_EXPLAIN, explain)
                    && (Arrays.binarySearch(
                    fields, DocumentKeys.EXPLAIN_RESPONSE_FIELD) < 0)) {
                log.debug("Turning on explain for '" + query + "'");
                String[] newFields = new String[fields.length + 1];
                System.arraycopy(fields, 0, newFields, 0, fields.length);
                newFields[newFields.length - 1] =
                        DocumentKeys.EXPLAIN_RESPONSE_FIELD;
                fields = newFields;
            }

            DocumentResponse result =
                    new DocumentResponse(filter, query, startIndex, maxRecords,
                                     sortKey, reverseSort, fields, 0,
                                     topDocs.totalHits);
            // TODO: What about longs for startIndex and maxRecords?
            for (int i = (int)startIndex ;
                 i < topDocs.scoreDocs.length
                 && i < (int)(startIndex + maxRecords + (mlt_request ? 1 : 0));
                 i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                // TODO: Get a service id and the sort value
                DocumentResponse.Record record =
                        new DocumentResponse.Record(
                                Integer.toString(scoreDoc.doc), "NA",
                                scoreDoc.score, null);
                Document doc =
                     searcher.getIndexReader().document(scoreDoc.doc, selector);
                if (isMoreLikeThisSelfMatch(request, doc)) {
                    log.trace("Ignoring MoreLikeThis hit on source document");
                    continue;
                }
                for (int fieldIndex = 0; fieldIndex < fields.length;
                     fieldIndex++) {
                    String field = fields[fieldIndex];
                    if (field.equals(DocumentKeys.EXPLAIN_RESPONSE_FIELD)) {
                        String explanation =
                                explain(request, luceneQuery, scoreDoc.doc);
                        if (log.isDebugEnabled()) {
                            log.debug("Appending explanation:\n" + explanation);
                        }
                        if (explanation != null) {
                            record.addField(new DocumentResponse.Field(
                                    DocumentKeys.EXPLAIN_RESPONSE_FIELD,
                                    explanation, false));
                        }
                        continue;
                    }
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
                //noinspection DuplicateStringLiteralInspection
                log.debug("fullSearch(..., query '" + query + "', filter '"
                          + filter + ") returning " + result.size() + "/" 
                          + topDocs.totalHits + " hits found in "
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

    private String explain(Request request, Query query, int docID) {
        if (!request.getBoolean(DocumentKeys.SEARCH_EXPLAIN, explain)) {
            return null;
        }
        try {
            return String.format(
                    "    <explanation>\n<expandedquery>%s</expandedquery>\n"
                    + "    <score>%s</score></explanation>",
                    XMLUtil.encode(LuceneIndexUtils.queryToString(query)),
                    XMLUtil.encode(searcher.explain(query, docID).toString()));
        } catch (IOException e) {
            return String.format(
                    "Unable to return explanation for the inclusion of doc #%d"
                    + " in the search result due to %s", docID, e.getMessage());
        }
    }

    private boolean isMoreLikeThisSelfMatch(Request request, Document doc) {
        if (request.containsKey(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID)) {
            Field field = doc.getField(DocumentKeys.RECORD_ID);
            if (field == null) {
                return false;
            }
            String sv = field.stringValue();
            return (sv != null && sv.equals(request.get(
                    LuceneKeys.SEARCH_MORELIKETHIS_RECORDID)));
        }
        return false;
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
            collector = collectors.poll(
                    COLLECTOR_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);
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
                      + (collector.getBits().capacity() - 1));
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

    protected SummaQueryParser getParser() {
        if (parser == null) {
            throw new IllegalStateException(
                    "The parser has not been initialized. This indicates that "
                    + "the IndexDescriptor has not been resolved");
        }
        return parser;
    }

    @Override
    protected long getHitCount(
            Request request, String query, String filter) throws IOException {
        long startTime = System.currentTimeMillis();

        Filter q;
        try {
            q = parseFilter(query);
        } catch (ParseException e) {
            throw new IOException(String.format(
                    "Exception parsing query '%s'", query), e);
        }
        Filter f;
        try {
            f = parseFilter(filter);
        } catch (ParseException e) {
            throw new IOException(String.format(
                    "Exception parsing filter '%s'", query), e);
        }
        if (q == null && f == null) {
            throw new IOException(String.format(
                    "Could not parse either query '%s' nor filter '%s'",
                    query, filter));
        }

        Filter amalgam;
        if (q == null) {
            amalgam = f;
        } else if (f == null) {
            amalgam = q;
        } else {
            BooleanFilter b = new BooleanFilter();
            b.add(new FilterClause(q, BooleanClause.Occur.MUST));
            b.add(new FilterClause(f, BooleanClause.Occur.MUST));
            amalgam = b;
        }
        log.trace("getHitcount(): Created filter, performing hit count");
        DocIdSet bits = amalgam.getDocIdSet(searcher.getIndexReader());

        long count = 0;
        if (bits instanceof DocIdBitSet) { // Optimization
            count = ((DocIdBitSet)bits).getBitSet().cardinality();
        } else if (bits instanceof OpenBitSetDISI) {
            count = ((OpenBitSetDISI)bits).cardinality();
        } else {
            log.debug("getHitCount: Got bits of unknown Class "
                      + bits.getClass() + ", iterating and counting (slow)");
            // We'll have to count
            DocIdSetIterator bitIt =  bits.iterator();
            while (bitIt.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                count++;
            }
        }
        //noinspection DuplicateStringLiteralInspection
        log.debug("getHitCount(..., query '" + query + "', filter '"
                  + filter + "') " + "got hit count " + count + " in "
                  + (System.currentTimeMillis() - startTime) + " ms");
        return count;
    }
}

