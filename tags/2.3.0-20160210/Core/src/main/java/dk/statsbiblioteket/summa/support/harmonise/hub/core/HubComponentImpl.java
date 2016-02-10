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
package dk.statsbiblioteket.summa.support.harmonise.hub.core;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.response.XMLResponseWriter;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Handles setup of default parameters and stripping of {@code id_<id>.} from parameters,
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class HubComponentImpl implements HubComponent, Configurable {
    private static Log log = LogFactory.getLog(HubComponentImpl.class);

    /**
     * Parameters for a specific component must be prefixed with this + id + ".", for example "id_sb.", if the component
     * in questing has id "sb". The parameters will have the prefix stripped before being passed on to
     * {@link #barrierSearch}.
     */
    public static final String SPECIFIC_PARAM_PREFIX = "id_";

    /**
     * Whether or not this component is enabled. When using this parameter, it will nearly always be prefixed.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_ENABLED = "enabled";

    /**
     * Sub-storage with key-value pairs of atomics, Strings and list of atomics or Strings. When a search is performed,
     * these pairs will be used as base and overwritten by the query params where the keys matches.
     * </p><p>
     * Note: Defaults and explicit params are not additive. If an explicit param is provided, it will clear all
     * instances of default values for the given key before it is added.
     * </p><p>
     * Optional.
     */
    public static final String CONF_DEFAULTS = "defaults";

    /**
     * The ID for this node.
     * </p><p>
     * Highly recommended. Default is class name.
     */
    public static final String CONF_ID = "node.id";

    private final String id;
    private final String prefix;
    private final SolrParams defaultParams;

    public HubComponentImpl(Configuration conf) {
        id = conf.getString(CONF_ID, getClass().getSimpleName());
        prefix = SPECIFIC_PARAM_PREFIX + id + ".";
        defaultParams = conf.containsKey(CONF_DEFAULTS) ? parseDefaults(conf.getSubConfiguration(CONF_DEFAULTS)) : null;
        log.info("Created " + toString());
    }

    /**
     * Converts all entries from the given configuration to SolrParam-representation.
     * @param conf default values for this or another component.
     * @return static SolrParams with the defaults.
     */
    protected static SolrParams parseDefaults(Configuration conf) {
        ModifiableSolrParams params = new ModifiableSolrParams();
        for (Map.Entry<String, Serializable> entry: conf) {
            String key = entry.getKey();
            Serializable value = entry.getValue();
            put(key, value, params);
        }
        return params;
    }

    @SuppressWarnings("unchecked")
    private static void put(String key, Serializable value, ModifiableSolrParams params) {
        if (value instanceof List) {
            List<Serializable> list = (List<Serializable>) value;
            for (Serializable v: list) {
                put(key, v, params);
            }
            return;
        }
        params.add(key, value.toString());
    }

    @Override
    public QueryResponse search(Limit limit, SolrParams params) throws Exception {
        ModifiableSolrParams request = getComponentParams(params);
        if (!params.getBool(CONF_ENABLED, true)) {
            return null;
        }
        return barrierSearch(limit, request);
    }

    /**
     * Rewrites all parameters prefixed as described in {@link #SPECIFIC_PARAM_PREFIX},adds them to the
     * default parameters and returns the merged parameters.
     * @param params the input parameters.
     * @return parameters for search, potentially adjusted.
     */
    protected ModifiableSolrParams getComponentParams(SolrParams params) {
        // Locate all properly prefixed, remove prefix, clear all existing values for the keys, all new key-value pairs
        return getComponentParams(params, defaultParams, prefix);
    }

    /**
     * Rewrites all parameters prefixed as described in {@link #SPECIFIC_PARAM_PREFIX}, but using the provided prefix.
     * @param params the input parameters.
     * @param defaults the base parameters, which will not be prefix-adjusted. Adjusted params are added to these and
     *                 the result returned. If null, only params is used for the result.
     * @param prefix the prefix to adjust to. This should normally be of the form {@code id_foo.}
     * @return parameters for search, potentially adjusted.
     */
    public static ModifiableSolrParams getComponentParams(SolrParams params, SolrParams defaults, String prefix) {
        ModifiableSolrParams adjusted = new ModifiableSolrParams(defaults);
        Iterator<String> pNames = params.getParameterNamesIterator();
        while (pNames.hasNext()) {
            String pName = pNames.next();
                adjusted.set(pName.startsWith(prefix) ? pName.substring(prefix.length()) : pName,
                             params.getParams(pName));
        }
        return adjusted;
    }

    /**
     * The barrierSearch works like {@link #search} with the difference that arguments prefixed with
     * {@link #SPECIFIC_PARAM_PREFIX} + id + "." will have said prefix removed and default values will have been added.
     * Previously prefixed params will override existing params by clearing all instances of those before being added to
     * params.
     * @param limit optional limitation on the HubComponents and bases that are to be searched.
     * @param params HubComponent-specific parameters.
     * @return the result from a search or null if the request is not applicable.
     * @throws Exception if an error occurred.
     */
    protected abstract QueryResponse barrierSearch(Limit limit, ModifiableSolrParams params) throws Exception;

    /**
     * Converts the given request/response pair into Solr-compliant XML.
     * @param request  a Solr request.
     * @param response a Solr response.
     * @return Solr-compliant XML.
     */
    public static String toXML(SolrParams request, QueryResponse response) {
        XMLResponseWriter xmlWriter = new XMLResponseWriter();
        StringWriter w = new StringWriter();
        SolrQueryResponse sResponse = new SolrQueryResponse();
        if (response == null || response.getResponse() == null) {
            String message = (response == null ? "response" : "response.getResponse()")
                             + " == null for " + request.toNamedList();
            log.warn(message);
            sResponse.setException(new NullPointerException(message));
        } else {
            sResponse.setAllValues(response.getResponse());
        }

        try {
            xmlWriter.write(w, new LocalSolrQueryRequest(null, request), sResponse);
        } catch (IOException e) {
            throw new RuntimeException("Unable to convert Solr response into XML", e);
        }
        return w.toString();
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public String toString() {
        return "HubComponentImpl(id='" + getID()+ ')';
    }
}
