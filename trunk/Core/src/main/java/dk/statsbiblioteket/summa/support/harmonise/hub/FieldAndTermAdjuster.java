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
package dk.statsbiblioteket.summa.support.harmonise.hub;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubAdjusterBase;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles aliasing of fields and values in requests as well as responses.
 * Serves as a normalizer for uncontrolled sources.
 */
@QAInfo(level = QAInfo.Level.PEDANTIC,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FieldAndTermAdjuster extends HubAdjusterBase {
    private static Log log = LogFactory.getLog(FieldAndTermAdjuster.class);

    /**
     * Maps from field names to field names, one way when rewriting queries,
     * the other way when adjusting the returned result.
     * </p><p>
     * This is a comma-separated list of rewrite-rules. The format of a single rule is
     * {@code outer_name - inner_fieldname}.
     * Example: {@code author - AuthorField, title - main_title}.
     * </p><p>
     * This option is not cumulative: Search-time overrides base configuration.
     * </p><p>
     * Optional. Default is no rewriting.
     */
    // TODO: Handle many-to-many re-writing
    public static final String CONF_ADJUST_DOCUMENT_FIELDS = "adjuster.document.fields";
    public static final String SEARCH_ADJUST_DOCUMENT_FIELDS = CONF_ADJUST_DOCUMENT_FIELDS;

    /**
     * Maps facet values in responses.
     * </p><p>
     * This is a list of sub configurations, each holding a {@link HubTagAdjuster} configuration.
     * </p><p>
     * Note: Mapping of facet values is performed after field name mapping.
     */
    public static final String CONF_ADJUST_FACET_TAGS = "adjuster.facet.tags";
    public static final String SEARCH_ADJUST_FACET_TAGS = CONF_ADJUST_FACET_TAGS;

    // This is a list as multiple adjusters/facet is a valid setup
    private List<HubTagAdjuster> tagAdjusters = new ArrayList<HubTagAdjuster>();

    public FieldAndTermAdjuster(Configuration conf) {
        super(conf);
        if (conf.containsKey(CONF_ADJUST_FACET_TAGS)) {
            List<Configuration> tagAdjusterConfs = conf.getSubConfigurations(CONF_ADJUST_FACET_TAGS);
            log.debug("Got " + tagAdjusterConfs + " tag adjuster configurations");
            for (Configuration tagAdjusterConf: tagAdjusterConfs) {
                tagAdjusters.add(new HubTagAdjuster(tagAdjusterConf));
            }
        }

        log.info("Created " + this);
    }

    @Override
    public SolrParams adjustRequest(ModifiableSolrParams request) {
        checkSubComponents();
        // TODO: Implement this
        return request;
    }

    private void checkSubComponents() {
        if (getComponents().size() != 1) {
            throw new IllegalStateException(
                    "The RequestPruner must have exactly 1 sub component but had " + getComponents().size());
        }
    }

    @Override
    public QueryResponse adjustResponse(SolrParams request, QueryResponse response) {
        // TODO: Implement this

        return response;
    }

    // We do not want bypassing of a white lister
    @Override
    protected boolean adjustmentDisablingPossible() {
        return true;
    }

    @Override
    public QueryResponse barrierSearch(Limit limit, ModifiableSolrParams params) throws Exception {
        return getComponents().get(0).search(limit, params);
    }

    @Override
    public String toString() {
        // TODO: Implement this
        return "FieldAndTermAdjuster()";
    }
}
