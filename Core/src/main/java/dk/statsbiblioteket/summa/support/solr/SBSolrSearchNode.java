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
package dk.statsbiblioteket.summa.support.solr;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.facetbrowser.api.IndexKeys;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.support.api.DidYouMeanKeys;
import dk.statsbiblioteket.summa.support.summon.search.SolrFacetRequest;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.solr.exposed.ExposedIndexLookupParams;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * SolrSearchNode with SBSolr-specific handling. This includes index lookup, hierarchical faceting and spellcheck.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SBSolrSearchNode extends SolrSearchNode {
    private static Log log = LogFactory.getLog(SBSolrSearchNode.class);

    /**
     * The handler for Exposed Index Lookup aka the Exposed module in Summa.
     * </p><p>
     * Optional. Default is '/lookup'.
     */
    public static final String CONF_LOOKUP_HANDLER = "solr.lookup.handler";
    public static final String DEFAULT_LOOKUP_HANDLER = "/lookup";

    /**
     * The spellchecker handler used for explicit DidYouMean requests.
     * </p><p>
     * Optional. Default is '', whick means that the default handler is used.
     */
    public static final String CONF_DIDYOUMEAN_HANDLER = "solr.lookup.handler";
    public static final String DEFAULT_DIDYOUMEAN_HANDLER = "";


    protected final String lookupHandlerID;
    protected final String didYouMeanHandlerID;

    public SBSolrSearchNode(Configuration conf) throws RemoteException {
        super(conf);
        lookupHandlerID = conf.getString(CONF_LOOKUP_HANDLER, DEFAULT_LOOKUP_HANDLER);
        didYouMeanHandlerID = conf.getString(CONF_DIDYOUMEAN_HANDLER, DEFAULT_DIDYOUMEAN_HANDLER);
        log.info("Created SBSolrSearchNode");
    }

    @Override
    protected Map<String, List<String>> buildSolrQuery(
        Request request, String filter, String query, Map<String, List<String>> solrParams, SolrFacetRequest facets,
        int startIndex, int maxRecords, String sortKey, boolean reverseSort) throws ParseException {
        Map<String, List<String>> solr = super.buildSolrQuery(
            request, filter, query, solrParams, facets, startIndex, maxRecords, sortKey, reverseSort);
        // IndexLookup

        buildIndexLookup(request, solr);
        buildDidYouMean(request, solr);
        return solr;
    }

    private void buildIndexLookup(Request request, Map<String, List<String>> solr) {
        if (solr.containsKey(ExposedIndexLookupParams.ELOOKUP)
            && Boolean.TRUE.toString().equals(solr.get(ExposedIndexLookupParams.ELOOKUP).get(0))) {
            solr.put("qt", Arrays.asList(lookupHandlerID));
        }
        putLookup(request, solr, IndexKeys.SEARCH_INDEX_FIELD, ExposedIndexLookupParams.ELOOKUP_FIELD);
        putLookup(request, solr,
                  IndexKeys.SEARCH_INDEX_CASE_SENSITIVE, ExposedIndexLookupParams.ELOOKUP_CASE_SENSITIVE);
        putLookup(request, solr, IndexKeys.SEARCH_INDEX_DELTA, ExposedIndexLookupParams.ELOOKUP_DELTA);
        putLookup(request, solr, IndexKeys.SEARCH_INDEX_LENGTH, ExposedIndexLookupParams.ELOOKUP_LENGTH);
        putLookup(request, solr, IndexKeys.SEARCH_INDEX_MINCOUNT, ExposedIndexLookupParams.ELOOKUP_MINCOUNT);
//        putLookup(request, queryMap, IndexKeys.SEARCH_INDEX_QUERY, ExposedIndexLookupParams.ELOOKUP_);
        // We really should have only one query
        putLookup(request, solr, IndexKeys.SEARCH_INDEX_TERM, ExposedIndexLookupParams.ELOOKUP_TERM);
//        putLookup(request, solr, IndexKeys.SEARCH_INDEX_TERM, "q");
        putLookup(request, solr, IndexKeys.SEARCH_INDEX_SORT, ExposedIndexLookupParams.ELOOKUP_SORT);
        putLookup(request, solr, IndexKeys.SEARCH_INDEX_LOCALE, ExposedIndexLookupParams.ELOOKUP_SORT_LOCALE_VALUE);
        if (!request.containsKey(DocumentKeys.SEARCH_QUERY) && !solr.containsKey("q")) {
            log.trace("No query specified for index lookup. Defaulting to *:*");
            solr.put("q", Arrays.asList("*:*"));
        }
    }

    protected boolean putLookup(Request source, Map<String, List<String>> dest, String sourceKey, String destKey) {
        if (source.containsKey(sourceKey)) {
            dest.put(destKey, Arrays.asList(source.get(sourceKey).toString()));
            dest.put(ExposedIndexLookupParams.ELOOKUP, Arrays.asList(Boolean.TRUE.toString()));
            dest.put("qt", Arrays.asList(lookupHandlerID));
            return true;
        }
        return false;
    }

    private void buildDidYouMean(Request request, Map<String, List<String>> solr) {
        putDYM(request, solr, DidYouMeanKeys.SEARCH_QUERY, "q");
        putDYM(request, solr, DidYouMeanKeys.SEARCH_MAX_RESULTS, "spellcheck.count");
    }

    protected boolean putDYM(Request source, Map<String, List<String>> dest, String sourceKey, String destKey) {
        if (source.containsKey(sourceKey)) {
            dest.put(destKey, Arrays.asList(source.get(sourceKey).toString()));
            if (!"".equals(didYouMeanHandlerID)) {
                dest.put("qt", Arrays.asList(didYouMeanHandlerID));
            }
            dest.put("spellcheck", Arrays.asList(Boolean.TRUE.toString()));
            return true;
        }
        return false;
    }

}
