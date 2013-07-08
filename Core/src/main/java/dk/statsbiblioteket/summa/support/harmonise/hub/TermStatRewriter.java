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
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubComponentImpl;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

import java.util.*;

/**
 * Rewrites queries according to term statistics.
 * </p><p>
 * The term stats are calculated only on the basis of active components, with the possibility of search-time adjustment
 * of the weights that each target has with respect to influencing the cumulative stats.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStatRewriter implements Configurable, RequestAdjuster {
    private static Log log = LogFactory.getLog(TermStatRewriter.class);

    /**
     * Holds a list of configurations conforming to {@link TermStatTarget}.
     */
    public static final String CONF_TARGETS = "termstat.targets";

    /**
     * Whether or not adjustment is enabled. If false, the request and the response are passed through unmodified.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String SEARCH_ADJUSTMENT_ENABLED = "termstatadjustment.enabled";

    /**
     * If true, all terms that are to be adjusted are lower cased before they are looked up in the term stats.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_LOWERCASE_QUERY = "termstat.lowercase.query";
    public static final String SEARCH_LOWERCASE_QUERY = CONF_LOWERCASE_QUERY;
    public static final boolean DEFAULT_LOWERCASE_QUERY = true;

    /**
     * If the calculated boost is 1 +- ROUND_DELTA, it is treated as 1.
     */
    public static final double ROUND_DELTA = 0.001;

    private final List<TermStatTarget> targets;
    private final boolean lowercase;
    private final Locale locale = new Locale("da"); // Consider making this adjustable

    public TermStatRewriter(Configuration conf) {
        if (conf.valueExists(CONF_TARGETS)) {
            List<Configuration> targetConfs;
            try {
                targetConfs = conf.getSubConfigurations(CONF_TARGETS);
            } catch (SubConfigurationsNotSupportedException e) {
                throw new ConfigurationException("Unable to get sub configurations for key '" + CONF_TARGETS + "'", e);
            }
            targets = new ArrayList<TermStatTarget>(targetConfs.size());
            for (Configuration targetConf : targetConfs) {
                targets.add(new TermStatTarget(targetConf));
            }
        } else {
            log.warn("No targets specified. Rewriting will have zero effect");
            targets = new ArrayList<TermStatTarget>(0);
        }
        lowercase = conf.getBoolean(CONF_LOWERCASE_QUERY, DEFAULT_LOWERCASE_QUERY);
        log.info("Created " + this);
    }

    /**
     * Adjusts the search queries for each component where term stats has been specified during setup, based on term
     * occurrence statistics and weight modifiers (pre-configured as well as runtime-specified).
     * @param params     incoming search parameters.
     * @param components the child-searchers for the aggregator.
     * @return the child-searchers with adjusted queries.
     */
    @Override
    public List<ComponentCallable> adjustRequests(
            SolrParams params, List<ComponentCallable> components) {
        if (!params.getBool(SEARCH_ADJUSTMENT_ENABLED, true)) {
            log.debug("TermStat adjustment skipped as " + SEARCH_ADJUSTMENT_ENABLED + "=false");
            return components;
        }
        final String query = params.get(CommonParams.Q, null);

        // Reduce the number of targets to those corresponding to the components in the search
        // This ensures that the virtual corpus used for query term weight adjustment will be equal to the real corpus
        // that the distributes search will use.
        List<TermStatTarget> pruned = getTargets(components, query);
        if (pruned.isEmpty()) {
            log.debug("adjustRequests: No general or specific queries for any sub component. Returning unchanged");
            return components;
        }
        if (log.isDebugEnabled()) {
            log.debug("adjustRequests: Adjusting query '" + query + "' for " + components.size() + " components with "
                      + pruned + " term stat collections" + (components.size() == pruned.size() ?
                    "" : ". This is a non-optimal adjustments as there should be a term stat collection for each " +
                         "component"));
        }

        // Cache query time weights for the components
        Map<String, Double> weights = new HashMap<String, Double>(components.size());
        double defaultWeight = params.getDouble(TermStatTarget.SEARCH_WEIGHT, -1.0);
        for (ComponentCallable comp: components) {
            double weight = comp.getParams().getDouble(HubComponentImpl.SPECIFIC_PARAM_PREFIX
                    + comp.getComponent().getID() + "." + TermStatTarget.SEARCH_WEIGHT, defaultWeight);
            if (weight >= 0) {
                weights.put(comp.getComponent().getID(), weight);
            }
        }

        // Iterate all components with targets and adjust their queries
        comp:
        for (ComponentCallable component: components) {
            for (TermStatTarget target: targets) {
                if (component.getComponent().getID().equals(target.getComponentID())) {
                    adjustRequests(component, target, pruned, weights);
                    continue comp;
                }
                log.debug("adjustRequests: Unable to adjust term stats for " + component.getComponent().getID()
                          + " as no target could be located");
            }
        }
        return components;
    }

    // At this point, only components with term stats are left
    private void adjustRequests(ComponentCallable component,
                                final TermStatTarget target, final List<TermStatTarget> pruned,
                                final Map<String, Double> weights) {
        final String query = component.getParams().get(CommonParams.Q, null);
        if (query == null) {
            log.debug("No query in params " + component.getParams() + " for component " + component.getComponent());
            return;
        }
        ModifiableSolrParams params = HubComponentImpl.getComponentParams(
                component.getParams(), null, component.getComponent().getID());
        final int fallbackDF = params.getInt(TermStatTarget.SEARCH_FALLBACK_DF, target.getFallbackDF());
        final boolean toLower = params.getBool(SEARCH_LOWERCASE_QUERY, lowercase);

        QueryRewriter queryRewriter = new QueryRewriter(null, null, new QueryRewriter.Event() {

            @Override
            public Query onQuery(TermQuery query) {
                double docFreq = 0;
                double numDocs = 0;

                String term = query.getTerm().text();
                if (toLower) {
                    term = term.toLowerCase(locale);
                }

                for (TermStatTarget t : pruned) {
                    Double weight = weights.get(t.getID());
                    if (weight == null) {
                        weight = t.getWeight();
                    }
                    docFreq += t.getDF(term, fallbackDF) * weight;
                    numDocs += t.getDocCount();
                }
                if (log.isTraceEnabled()) {
                    log.trace(String.format("rewrite: target='%s', term='%s', df=%f, docCount=%f",
                                            target.getID(), term, docFreq, numDocs));
                }

                double idealIDFSqr = Math.pow(Math.log(numDocs / (docFreq + 1)) + 1.0, 2);
                double targetIDFSqr = Math.pow(
                        Math.log(target.getDocCount() / (double) (target.getDF(term, fallbackDF) + 1)) + 1.0, 2);
                double boostFactor = idealIDFSqr / targetIDFSqr;
                float finalBoost = (float) Math.min(Float.MAX_VALUE, query.getBoost() * boostFactor);

                if (finalBoost < 1.0d - ROUND_DELTA || finalBoost > 1.0d + ROUND_DELTA) {
                    query.setBoost(finalBoost);
                }
                return query;
            }
        });
        String adjusted = query;
        try {
            adjusted = queryRewriter.rewrite(query);
        } catch (ParseException e) {
            log.warn("Unable to parse query '" + query + "'. Returning query unmodified");
        } catch (Exception e) {
            log.warn("Unknown exception rewriting query '" + query + "'. Returning query unmodified");
        }
        log.debug("Adjusted query for component " + component.getComponent().getID() + " from '" + query + "' to '"
                  + adjusted + "'");
        component.getParams().set(CommonParams.Q, adjusted);
    }

    /*
     * Extract all the components that has term stats and a component specific query.
     * If a general query is issued, the component specific query need no be present.
     */
    private List<TermStatTarget> getTargets(
            List<ComponentCallable> components, String query) {
        List<TermStatTarget> pruned = new ArrayList<TermStatTarget>(targets.size());
        for (ComponentCallable component: components) {
            for (TermStatTarget target: targets) {
                if (component.getComponent().getID().equals(target.getComponentID())) {
                    if (query != null || component.getParams().get(target.getComponentID() + ".q") != null) {
                        pruned.add(target);
                        break;
                    }
                }
            }
        }
        return pruned;
    }

    @Override
    public String toString() {
        return "TermStatRewriter(not properly implemented)";
    }
}
