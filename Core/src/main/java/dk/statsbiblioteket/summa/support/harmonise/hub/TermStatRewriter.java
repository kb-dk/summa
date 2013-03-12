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
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubAggregatorBase;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Rewrites queries according to term statistics.
 * </p><p>
 * The term stats are calculated only on the basis of active components, with the possibility of search-time adjustment
 * of the weights that each target has with respect to influencing the cumulative stats.
 * </p><p>
 *
 * </p>
 *
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStatRewriter implements Configurable {
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
    public static final String PARAM_ADJUSTMENT_ENABLED = "adjustment.enabled";

    /**
     * If true, all terms that are to be adjusted are lower cased before they
     * are looked up in the term stats.
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

    public List<HubAggregatorBase.ComponentCallable> adjustRequests(
            SolrParams params, List<HubAggregatorBase.ComponentCallable> components) {
        String query = params.get(CommonParams.Q, null);
        if (query == null) {
            return components;
        }
        List<TermStatTarget> pruned = getTargets(components);
        if (log.isDebugEnabled()) {
            log.debug("Adjusting query '" + query + "' for " + components.size() + " components and " + targets.size()
                      + " resolved targets");
        }
        comp:
        for (HubAggregatorBase.ComponentCallable component: components) {
            for (TermStatTarget target: targets) {
                if (component.getComponent().getID().equals(target.getComponentID())) {
                    adjustRequests(query, component, target, pruned, lowercase);
                    continue comp;
                }
                log.debug("Unable to adjust term stats for " + component.getComponent().getID()
                          + " as no target could be located");
            }
        }
        return components;
    }

    private void adjustRequests(String query, HubAggregatorBase.ComponentCallable component,
                                final TermStatTarget target, final List<TermStatTarget> pruned,
                                final boolean doLowercase) {
        final int fallbackDF = component.getParams().getInt(TermStatTarget.SEARCH_FALLBACK_DF, target.getFallbackDF());
        final double weight = component.getParams().getDouble(TermStatTarget.SEARCH_WEIGHT, target.getWeight());

        QueryRewriter queryRewriter = new QueryRewriter(null, null, new QueryRewriter.Event() {

            @Override
            public Query onQuery(TermQuery query) {
                double docFreq = 0;
                double numDocs = 0;

                String term = query.getTerm().text();
                if (doLowercase) {
                    term = term.toLowerCase();
                }

                for (TermStatTarget t : pruned) {
                    // TODO: We need to resolve the weights properly
                    //docFreq += t.getDF(term, fallbackDF) * t.getWeight(request);
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
        // TODO: Set the new query by copying params
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private List<TermStatTarget> getTargets(List<HubAggregatorBase.ComponentCallable> components) {
        List<TermStatTarget> pruned = new ArrayList<TermStatTarget>(targets.size());
        for (HubAggregatorBase.ComponentCallable component: components) {
            for (TermStatTarget target: targets) {
                if (component.getComponent().getID().equals(target.getComponentID())) {
                    pruned.add(target);
                    break;
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
