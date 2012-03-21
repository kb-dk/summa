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
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.support.summon.search.SummonResponseBuilder;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A wrapper for Solr web calls, transforming requests and responses from and to Summa equivalents.
 * */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class SolrSearchNode extends SearchNodeImpl {
    private static Log log = LogFactory.getLog(SolrSearchNode.class);

    // TODO: Assign mandatory ID, use it for timing and result delivery
    /**
     * The entry point for calls to Solr.
     * </p><p>
     * Optional. Default is localhost:8983 (Solr default).
     */
    public static final String CONF_SOLR_HOST = "solr.host";
    public static final String DEFAULT_SOLR_HOST = "localhost:8983";
    /**
     * The rest call at {@link #CONF_SOLR_HOST}.
     * </p><p>
     * Optional. Default is '/solr' (Solr default).
     */
    public static final String CONF_SOLR_RESTCALL = "solr.restcall";
    public static final String DEFAULT_SOLR_RESTCALL = "/solr";
    /**
     * The prefix will be added to all IDs returned by Solr.
     * </p><p>
     * Optional. Default is empty.
     */
    public static final String CONF_SOLR_IDPREFIX = "solr.id.prefix";
    public static final String DEFAULT_SOLR_IDPREFIX = "";
    /**
     * The default number of documents results to return from a search.
     * </p><p>
     * This can be overridden with {@link dk.statsbiblioteket.summa.search.api.document.DocumentKeys#SEARCH_MAX_RECORDS}.
     * </p><p>
     * Optional. Default is 15.
     */
    public static final String CONF_SOLR_DEFAULTPAGESIZE = "solr.defaultpagesize";
    public static final int DEFAULT_SOLR_DEFAULTPAGESIZE = 15;
    /**
     * The default facets if none are specified. The syntax is a comma-separated
     * list of facet names, optionally with max tags in paranthesis.
     * This can be overridden with {@link dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys#SEARCH_FACET_FACETS}.
     * Specifying the empty string turns off faceting.
     * </p><p>
     * Optional. Default is {@link #DEFAULT_SOLR_FACETS}.
     */
    public static final String CONF_SOLR_FACETS = "solr.facets";
    public static final String DEFAULT_SOLR_FACETS = "";
    /**
     * The default number of tags tags to show in a facet if not specified
     * elsewhere.
     * </p><p>
     * Optional. Default is 15.
     */
    public static final String CONF_SOLR_FACETS_DEFAULTPAGESIZE = "solr.facets.defaultpagesize";
    public static final int DEFAULT_SOLR_FACETS_DEFAULTPAGESIZE = 15;
    /**
     * Whether facets should be searched with and or or.
     * Optional. Default is 'and'. Can only be 'and' or 'or'.
     */
    public static final String CONF_SOLR_FACETS_COMBINEMODE = "solr.facets.combinemode";
    public static final String DEFAULT_SOLR_FACETS_COMBINEMODE = "and";


    /**
     * If true, calls to Solr assumes that pure negative filters (e.g. "NOT foo NOT bar") are supported.
     * If false, pure negative filters are handled by rewriting the query to "(query) filter", so if query is "baz"
     * and the filter is "NOT foo NOT bar", the result is "(baz) NOT foo NOT bar".
     * Note that rewriting also requires the {@link DocumentKeys#SEARCH_FILTER_PURE_NEGATIVE} parameter to be true.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_SUPPORTS_PURE_NEGATIVE_FILTERS = "solr.purenegativefilters.support";
    public static final boolean DEFAULT_SUPPORTS_PURE_NEGATIVE_FILTERS = false;

    /**
     * If true, the SolrSearchNode does not attempt to extract facet-query from the query and passes the query and
     * filter through unmodified. Mainly used for debugging.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String SEARCH_PASSTHROUGH_QUERY = "solr.passthrough.query";
    public static final boolean DEFAULT_PASSTHROUGH_QUERY = false;


    //    private static final DateFormat formatter =
    //        new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
    protected SummonResponseBuilder responseBuilder;
    protected final String host;
    protected final String restCall;
    protected final String idPrefix;
    protected final int defaultPageSize;
    protected final int defaultFacetPageSize;
    protected final String defaultFacets;
    protected final String combineMode;
    protected final Map<String, List<String>> summonDefaultParams;
    protected final boolean supportsPureNegative;

    public SolrSearchNode(Configuration conf) throws RemoteException {
        super(conf);
        responseBuilder = new SummonResponseBuilder(conf);
        summonDefaultParams = new HashMap<String, List<String>>();

        host = conf.getString(CONF_SOLR_HOST, DEFAULT_SOLR_HOST);
        restCall = conf.getString(CONF_SOLR_RESTCALL, DEFAULT_SOLR_RESTCALL);
        idPrefix =   conf.getString(CONF_SOLR_IDPREFIX, DEFAULT_SOLR_IDPREFIX);
        defaultPageSize = conf.getInt(CONF_SOLR_DEFAULTPAGESIZE, DEFAULT_SOLR_DEFAULTPAGESIZE);
        defaultFacetPageSize = conf.getInt(CONF_SOLR_FACETS_DEFAULTPAGESIZE, DEFAULT_SOLR_FACETS_DEFAULTPAGESIZE);
        defaultFacets = conf.getString(CONF_SOLR_FACETS, DEFAULT_SOLR_FACETS);
        combineMode = conf.getString(CONF_SOLR_FACETS_COMBINEMODE, DEFAULT_SOLR_FACETS_COMBINEMODE);
        supportsPureNegative = conf.getBoolean(
            CONF_SUPPORTS_PURE_NEGATIVE_FILTERS, DEFAULT_SUPPORTS_PURE_NEGATIVE_FILTERS);
    }


}
