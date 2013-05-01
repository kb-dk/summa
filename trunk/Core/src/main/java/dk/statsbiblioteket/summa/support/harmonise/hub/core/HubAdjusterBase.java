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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class HubAdjusterBase extends HubCompositeImpl {
    private static Log log = LogFactory.getLog(HubAdjusterBase.class);

    /**
     * Whether or not adjustment is enabled. If false, the request and the response are passed through unmodified.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String PARAM_ADJUSTMENT_ENABLED = "adjustment.enabled";

    public HubAdjusterBase(Configuration conf) {
        super(conf);
        log.info("Created " + toString());
    }

    private boolean warned = false;
    @Override
    public QueryResponse search(Limit limit, SolrParams params) throws Exception {
        if (getComponents().isEmpty()) {
            if (!warned) {
                log.warn("No inner components in " + this + " so this component will always return null, " +
                         "which is probably a setup error. This warning will not be repeated");
                warned = true;
            }
            return null;
        }
        ModifiableSolrParams request = getComponentParams(params);
        if (!params.getBool(CONF_ENABLED, true)) {
            return null;
        }
        boolean rewrite = !adjustmentDisablingPossible() || params.getBool(PARAM_ADJUSTMENT_ENABLED, true);
        if (!rewrite) {
            return barrierSearch(limit, request);
        }
        SolrParams rRequest = adjustRequest(request);
        QueryResponse response = barrierSearch(limit, request);
        return response == null ? null : adjustResponse(rRequest, response);
    }

    /**
     * @return true if it should be possible to disable adjustment of parameters at search time.
     */
    protected boolean adjustmentDisablingPossible() {
        return true;
    }

    /**
     * Rewrite of the request. Note that prefixed parameters has already been handled at this point.
     * @param request the request for a search.
     * @return the request, potentially modified.
     */
    public abstract SolrParams adjustRequest(SolrParams request);

    /**
     * Rewrite of the response.
     * @param request  the prefix-adjusted request that caused the response.
     * @param response the response from th e{@link #barrierSearch}.
     * @return the response, potentially modified.
     */
    public abstract QueryResponse adjustResponse(SolrParams request, QueryResponse response);

    @Override
    protected int maxComponents() {
        return 1;
    }

    @Override
    public String toString() {
        return "HubAdjusterBase(" + super.toString() + ")";
    }
}
