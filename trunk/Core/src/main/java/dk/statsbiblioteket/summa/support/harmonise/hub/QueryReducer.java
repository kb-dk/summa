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

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.search.tools.QueryRewriter;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.ComponentCallable;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses queries and removes known sub-queries that are guaranteed to be match-all or match-none.
 * </p><p>
 * When queries can be fully reduced to match-none, they should be serialized to the empty String.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class QueryReducer implements Configurable, RequestAdjuster {
    private static Log log = LogFactory.getLog(QueryReducer.class);

    /**
     * Holds a list of configurations conforming to {@link ReducerTarget}.
     */
    public static final String CONF_TARGETS = "reducer.targets";

    /**
     * Whether or not reduction is enabled at all. If false, the requests are passed through unmodified.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String SEARCH_ADJUSTMENT_ENABLED = "reducer.enabled";

    private final Map<String, ReducerTarget> reducerTargets;
    private ReducerTarget defaultReducerTarget = null; // Fallback reducer

    // Used by ReducerTarget
    private final QueryRewriter queryRewriter;

    public QueryReducer(Configuration conf) {
        if (conf.valueExists(CONF_TARGETS)) {
            List<Configuration> targetConfs;
            try {
                targetConfs = conf.getSubConfigurations(CONF_TARGETS);
            } catch (SubConfigurationsNotSupportedException e) {
                throw new ConfigurationException("Unable to get sub configurations for key '" + CONF_TARGETS + "'", e);
            }
            reducerTargets = new HashMap<String, ReducerTarget>(targetConfs.size());
            for (Configuration targetConf : targetConfs) {
                ReducerTarget target = new ReducerTarget(targetConf);
                if (target.getComponentID().isEmpty()) {
                    defaultReducerTarget = target;
                } else {
                    reducerTargets.put(target.getComponentID(), target);
                }
            }
        } else {
            log.warn("No targets specified. Reduction will have zero effect");
            reducerTargets = new HashMap<String, ReducerTarget>(0);
        }
        queryRewriter = new QueryRewriter(conf, null, null);
        log.info("Created " + this);
    }

    /**
     * Reduced the search queries and filters for each component.
     *
     * @param params     incoming search parameters.
     * @param components the child-components (aka searchers) for the aggregator (includes node-specific queries).
     * @return the child-components with adjusted queries.
     */
    @Override
    public List<ComponentCallable> adjustRequests(
            SolrParams params, List<ComponentCallable> components) {
        if (!params.getBool(SEARCH_ADJUSTMENT_ENABLED, true)) {
            log.debug("Query reduction skipped as " + SEARCH_ADJUSTMENT_ENABLED + "=false");
            return components;
        }

        for (ComponentCallable component : components) {
            ReducerTarget target = reducerTargets.get(component.getComponent().getID());
            if (target == null && defaultReducerTarget != null) {
                target = defaultReducerTarget;
            }
            if (target != null) {
                log.debug("Reducing query for " + component.getComponent());
                target.reduce(component.getParams());
            }
        }
        return components;
    }

    @Override
    public String toString() {
        return String.format("QueryReducer(#reducers=%d, default=%s)", reducerTargets.size(), defaultReducerTarget);
    }

    // Consider using the callbacks from QueryRewriter
    public class ReducerTarget implements Configurable {
        /**
         * The id for the component that this target should rewrite queries for.
         * </p><p>
         * Optional. If not defined, the ReducerTarget will be used for all components.
         */
        public static final String CONF_COMPONENT_ID = "reducertarget.componentid";

        /**
         * A list of term-queries that are known not to match anything. The format is
         * {@code field:text}, {@code field:} or {code :text}.
         * </p><p>
         * Sample 1: {code Language:abcde32542f} matches the given singular term.<br/>
         * Sample 2: {code SomeSpecificField:} matches all TermQueries with the given field.<br/>
         * Sample 3: {code :magictext} matches all TermQueries with the given text, regardless of field
         * </p><p>
         * Optional but highly recommended as the ReducerTarget will have no effect without at least 1 value.
         */
        public static final String CONF_MATCH_NONES = "reducertarget.matchnones";

        private final String componentID;
        private final BlacklistMatcher blacklistMatcher;

        public ReducerTarget(Configuration conf) {
            componentID = conf.getString(CONF_COMPONENT_ID, "");

            List<String> matchNones = conf.getStrings(CONF_MATCH_NONES, Collections.<String>emptyList());
            if (matchNones.isEmpty()) {
                log.warn("The list of non-matching TermQueries for " + componentID + " is empty");
            }
            blacklistMatcher = new BlacklistMatcher(matchNones);

            log.info("Created " + this);
        }

        public void reduce(ModifiableSolrParams params) {

            // Query
            String query = params.get(CommonParams.Q, null);
            {
                if (query != null) {
                    String reduced = reduce(query);
                    if (reduced == null || reduced.isEmpty()) {
                        log.debug("The query '" + query + "' for '" + componentID + "' was reduced to match-none");
                        params.remove(CommonParams.Q);
                    } else {
                        log.debug("Reduced query '" + query + "' -> '" + reduced + "' for '" + componentID + "'");
                        params.set(CommonParams.Q, reduced);
                    }
                }
            }

            // Filters (same syntax as query, but multiple filters are possible)
            String[] filters = params.getParams(CommonParams.FQ);
            if (filters != null && filters.length != 0) {
                List<String> reducedFilters = new ArrayList<String>(filters.length);
                for (String filter : filters) {
                    String reduced = reduce(filter);
                    if (reduced == null || reduced.isEmpty()) {
                        log.debug("The filter '" + filter + "' for '" + componentID + "' was reduced to match-none");
                    } else {
                        log.debug("Reduced filter '" + query + "' -> '" + reduced + "' for '" + componentID + "'");
                        reducedFilters.add(reduced);
                    }
                }
                String[] rf = new String[reducedFilters.size()];
                rf = reducedFilters.toArray(rf);
                params.set(CommonParams.FQ, rf);
            }
        }

        private String reduce(String queryString) {
            try {
                QueryReducerViaBlackList reducer = new QueryReducerViaBlackList(blacklistMatcher);
                Query reducedQuery = reducer.reduce(queryRewriter.createQueryParser().parse(queryString));
                return reducedQuery == null ? "" : queryRewriter.toString(reducedQuery);
            } catch (ParseException e) {
                log.warn("Exception while parsing '" + queryString + "'. Returning unmodified: " + e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Detailed parse exception for '" + queryString, e);
                }
                return queryString;
            }
        }

        public String getComponentID() {
            return componentID;
        }

        public String toString() {
            return String.format("ReducerTarget(componentID='%s', matchNones=%s)",
                                 componentID, Strings.join(blacklistMatcher.getBlacklistPatterns(), 5));
        }
    }
}
