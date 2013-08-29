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
import dk.statsbiblioteket.summa.common.util.ManyToManyMapper;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubAdjusterBase;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Handles aliasing of fields and values in requests as well as responses.
 * Serves as a normalizer for uncontrolled sources.
 */
@QAInfo(level = QAInfo.Level.PEDANTIC,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrRequestAdjuster extends HubAdjusterBase {
    private static Log log = LogFactory.getLog(SolrRequestAdjuster.class);

    /**
     * Maps from field names to field names, one way when rewriting queries, the other way when adjusting the returned
     * result. This also rewrites facet fields.
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
    //public static final String SEARCH_ADJUST_FACET_TAGS = CONF_ADJUST_FACET_TAGS;

    // This is a list as multiple adjusters/facet is a valid setup
    private List<HubTagAdjuster> tagAdjusters = new ArrayList<HubTagAdjuster>();
    private final ManyToManyMapper defaultFieldMap;
    private final QueryAdjuster queryAdjuster;

    public SolrRequestAdjuster(Configuration conf) {
        super(conf);
        if (conf.containsKey(CONF_ADJUST_FACET_TAGS)) {
            List<Configuration> tagAdjusterConfs = conf.getSubConfigurations(CONF_ADJUST_FACET_TAGS);
            log.debug("Got " + tagAdjusterConfs + " tag adjuster configurations");
            for (Configuration tagAdjusterConf: tagAdjusterConfs) {
                tagAdjusters.add(new HubTagAdjuster(tagAdjusterConf));
            }
        }
        defaultFieldMap = conf.containsKey(CONF_ADJUST_DOCUMENT_FIELDS) ?
                new ManyToManyMapper(conf.getStrings(CONF_ADJUST_DOCUMENT_FIELDS)) :
                null;
        queryAdjuster = new QueryAdjuster(conf, defaultFieldMap, null);

        log.info("Created " + this);
    }

    private static final Pattern SPLIT = Pattern.compile(" +| *, *");
    @Override
    public SolrParams adjustRequest(ModifiableSolrParams request) {
        checkSubComponents();

        // Only a single query
        String query = request.get(CommonParams.Q);
        if (query != null) {
            try {
                request.set(CommonParams.Q, queryAdjuster.rewrite(query));
            } catch (ParseException e) {
                log.warn("ParseException for query '" + query + "'. Query not rewritten", e);
            }
        }

        // Multiple filters
        String[] filters = request.getParams(CommonParams.FQ);
        if (filters != null) {
            for (int i = 0 ; i < filters.length ; i++) {
                try {
                    filters[i] = queryAdjuster.rewrite(filters[i]);
                } catch (ParseException e) {
                    log.warn("ParseException for filter '" + query + "'. Filter not rewritten", e);
                }
            }
            request.set(CommonParams.FQ, filters);
        }

        // Default field
        String queryField = request.get(CommonParams.DF);
        if (queryField != null) {
            Set<String> adjustedQueryFields = defaultFieldMap.getForward().get(queryField);
            if (adjustedQueryFields != null) {
                if (adjustedQueryFields.size() != 1) {
                    log.warn(String.format(
                            "The requested default query field '%s' expanded to %d fields (%s). Only 1 is allowed",
                            queryField, adjustedQueryFields.size(), Strings.join(adjustedQueryFields)));
                }
            }
        }

        // Field list
        String[] fls = request.getParams(CommonParams.FL);
        if (fls !=  null) {
            Set<String> newFields = new HashSet<String>();
            for (String fl : fls) {
                for (String field : SPLIT.split(fl)) {
                    Set<String> replacements = defaultFieldMap.getForwardSet(field);
                    if (replacements == null) {
                        newFields.add(field);
                    } else {
                        for (String replacement : replacements) {
                            newFields.add(replacement);
                        }
                    }
                }
            }
            request.set(CommonParams.FL, Strings.join(newFields, ","));
        }


        // TODO: Facet fields
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
        return "SolrRequestAdjuster()";
    }
}
