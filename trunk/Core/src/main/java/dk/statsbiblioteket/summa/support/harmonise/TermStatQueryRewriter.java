/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.lucene.distribution.TermEntry;
import dk.statsbiblioteket.summa.common.lucene.distribution.TermStat;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.tools.QueryRewriter;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rewrites queries textually based on term statistics for given targets.
 * Terms in the queries will be assigning boosts, simulating a single
 * controlled index with regards to the scores for the returned documents.
 * </p><p>
 * Textual query rewriting is error-prone and incomplete so this should be
 * seen as a "best effort" or "better than the alternative", rather than a
 * authoritative harmonization of separate indexes.
 * </p><p>
 * The query parser is geared towards safe operation at the expense of catching
 * complex queries. If the parser is unsure about the query, it is not modified
 * at all. The choice is made as bad ranking is considered less fatal than
 * missed results.
 * </p><p>
 * The runtime arguments to the rewriter can be prefixed with the id for
 * a specific target, using the same principle as
 * {@link dk.statsbiblioteket.summa.support.harmonise.InteractionAdjuster}.
 * </p><p>
 * At the heart of the query rewriter is an assumption of the targets using
 * a Similarity that is close to Lucene's default Similarity so that the formula
 * http://lucene.apache.org/java/2_9_2/api/core/org/apache/lucene/search/Similarity.html
 * holds.
 * </p><p>
 * The query rewriter calculates a new {@code idf(t)} for all terms in the query
 * and since it cannot control {@code idf(t)}-replacing at the targets, it
 * modifies the boost for the terms so that the effect is the same. 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStatQueryRewriter implements Configurable {
    private static Log log = LogFactory.getLog(TermStatQueryRewriter.class);

    /**
     * Holds a list of configurations conforming to {@link Target}.
     */
    public static final String CONF_TARGETS = "queryrewriter.targets";

    /**
     * Overall switch for performing query rewriting. In order to disable a
     * specific target, set the target's weight to 0.0.
     * </p><p>
     * Optional. Default is true;
     * @see {@link Target#SEARCH_WEIGHT}.
     */
    public static final String CONF_TERMSTATS_ENABLED = "queryrewriter.enabled";
    public static final String SEARCH_TERMSTATS_ENABLED = CONF_TERMSTATS_ENABLED;
    public static final boolean DEFAULT_TERMSTATS_ENABLED = true;

    /**
     * If true, all terms that are to be adjusted are lowercased bafor they
     * are looked up in the term stats.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_LOWERCASE_QUERY = "queryrewriter.lowercase.query";
    public static final String SEARCH_LOWERCASE_QUERY = CONF_LOWERCASE_QUERY;
    public static final boolean DEFAULT_LOWERCASE_QUERY = true;

    /**
     * If the calculated boost is 1 +- ROUND_DELTA, it is treated as 1.
     */
    public static final double ROUND_DELTA = 0.001;

    private boolean enabled = DEFAULT_TERMSTATS_ENABLED;
    private final List<Target> targets;
    private final boolean lowercase;

    public TermStatQueryRewriter(Configuration conf) {
        enabled = conf.getBoolean(CONF_TERMSTATS_ENABLED, enabled);
        if (conf.valueExists(CONF_TARGETS)) {
            List<Configuration> targetConfs;
            try {
                targetConfs = conf.getSubConfigurations(CONF_TARGETS);
            } catch (SubConfigurationsNotSupportedException e) {
                throw new ConfigurationException(
                    "Unable to get sub configurations for key '" + CONF_TARGETS + "'", e);
            }
            targets = new ArrayList<Target>(targetConfs.size());
            for (Configuration targetConf: targetConfs) {
                targets.add(new Target(targetConf));
            }
        } else {
            log.warn("No targets specified. Rewriting will have zero effect");
            targets = new ArrayList<Target>(0);
        }
        lowercase = conf.getBoolean(CONF_LOWERCASE_QUERY, DEFAULT_LOWERCASE_QUERY);
        log.info("Constructed query rewriter with " + targets.size() + " targets. Enabled: " + enabled);
    }

    private boolean noTargetsLog = false;
    /**
     * Rewrites the given query by adjusting weights according to term stats.
     * Only the query given in {@link DocumentKeys#SEARCH_QUERY} is rewritten.
     * </p><p>
     * Example: The query {@code foo bar^0.5} is given. The rewriter determines
     * that the calculated weights for id summon should be {@code 1.2} for
     * {@code foo} and {@code 2.5} for {@code bar}. The new query for summon
     * will be {@code foo^1.2 bar^1.25}.
     * @param request a standard search request.
     * @return a map from target id to rewritten query or null if no rewriting
     * could be performed. The query in the request is left untouched.
     */
    public Map<String, String> rewrite(Request request) {
        long rewriteTime = -System.currentTimeMillis();
        if (!request.getBoolean(SEARCH_TERMSTATS_ENABLED, enabled)) {
            log.trace("Rewriting disabled. Skipping rewrite");
            return null;
        }
        String query = request.getString(DocumentKeys.SEARCH_QUERY, null);
        if (query == null) {
            log.trace("No query present. Skipping rewrite");
            return null;
        }
        if (targets.size() == 0) {
            if (!noTargetsLog) {
                log.warn(
                    "No targets specified. No further warnings will be issued "
                    + "for the duration of this runtime. Skipping rewrite");
                noTargetsLog = true;
            }
            return null;
        }

        Map<String, String> result =
            new HashMap<String, String>(targets.size());
        for (Target target: targets) {
            boolean doLowercase = request.getBoolean(SEARCH_LOWERCASE_QUERY, lowercase);
            doLowercase = request.getBoolean(
                target.getID() + "." + SEARCH_LOWERCASE_QUERY, doLowercase);
            try {
                result.put(target.getID(), rewrite(request, target, query, doLowercase));
            } catch (ParseException e) {
                log.info("ParseException while rewriting query '" + query + "' for target " + target + ": "
                         + e.getMessage());
                return null;
            }
        }
        rewriteTime += System.currentTimeMillis();
        log.debug("Finished rewriting '" + query + "' to " + targets.size()
                  + " targets in " + rewriteTime + " ms");
        return result;
    }

    /**
     * Rewrites the given query by adjusting weights according to term stats.
     * Only the query given in {@link DocumentKeys#SEARCH_QUERY} is rewritten.
     * </p><p>
     * Example: The query {@code foo bar^0.5} is given. The rewriter determines
     * that the calculated weights for id summon should be {@code 1.2} for
     * {@code foo} and {@code 2.5} for {@code bar}. The new query for summon
     * will be {@code foo^1.2 bar^1.25}.
     * @param request a standard search request.
     * @param id      the id for the Target to rewrite for.
     */
    public void rewrite(Request request, String id) {
        long rewriteTime = -System.currentTimeMillis();
        if (!request.getBoolean(SEARCH_TERMSTATS_ENABLED, enabled)) {
            log.trace("Rewriting disabled. Skipping rewrite");
            return;
        }
        String query = request.getString(DocumentKeys.SEARCH_QUERY, null);
        if (query == null) {
            log.trace("No query present. Skipping rewrite");
            return;
        }
        Target target = null;
        for (Target t: targets) {
            if (t.getID().equals(id)) {
                target = t;
            }
        }
        if (target == null) {
            log.trace("No target found for id '" + id + "'. Skipping rewrite");
            return;
        }

        boolean doLowercase = request.getBoolean(SEARCH_LOWERCASE_QUERY, lowercase);
        doLowercase = request.getBoolean(target.getID() + "." + SEARCH_LOWERCASE_QUERY, doLowercase);
        String rewritten = query;
        try {
            rewritten = rewrite(request, target, query, doLowercase);
        } catch (Exception e) {
            log.warn("The query '" + query + "' was not rewritten due to an exception. "
                     + "Returning the unmodified query", e);
        }
        request.put(DocumentKeys.SEARCH_QUERY, rewritten);
        rewriteTime += System.currentTimeMillis();
        log.debug("Finished rewriting target " + id + " '" + query + "' to '" + rewritten
                  + "' in " + rewriteTime + " ms");
    }

    private String rewrite(
        final Request request, final Target target, String query,
        final boolean doLowercase) throws ParseException {

        QueryRewriter queryRewriter =
            new QueryRewriter(new QueryRewriter.Event() {

                @Override
                public Query onQuery(TermQuery query) {
                    double docFreq = 0;
                    double numDocs = 0;

                    String term = query.getTerm().text();
                    if (doLowercase) {
                        term = term.toLowerCase();
                    }

                    for (Target t : targets) {
                        docFreq += t.getDF(request, term) * t.getWeight(request);
                        numDocs += t.getDocCount();
                    }
                    if (log.isTraceEnabled()) {
                        log.trace(String.format(
                            "rewrite: target='%s', term='%s', df=%f, docCount=%f",
                            target.getID(), term, docFreq, numDocs));
                    }

                    double idealIDFSqr = Math.pow(Math.log(numDocs / (docFreq + 1)) + 1.0, 2);
                    double targetIDFSqr = Math.pow(Math.log(
                            target.getDocCount() / (double) (target.getDF(request, term) + 1)) + 1.0, 2);
                    double boostFactor = idealIDFSqr / targetIDFSqr;
                    float finalBoost = (float) Math.min(Float.MAX_VALUE, query.getBoost() * boostFactor);

                    if (finalBoost < 1.0d - ROUND_DELTA || finalBoost > 1.0d + ROUND_DELTA) {
                        query.setBoost(finalBoost);
                    }
                    return query;
                }
            });

        return queryRewriter.rewrite(query);
    }

    /**
     * The target encapsulates a TermStat component to deliver document
     * frequencies.
     */
    public class Target implements Configurable {

        /**
         * The id for the target. Used for specifying search-time adjustments
         * and for loading term statistics.
         */
        public static final String CONF_ID = "target.id";

        /**
         * The relative weight of the stats from this target.
         * </p><p>
         * Sane values go from 0.0 to 1.0, where 0.0 is a complete disregard of
         * the targets effect on the merged term stats and 1.0 is full effect.
         * </p><p>
         * If the value is specified at runtime, it must be prefixed with the
         * target it + ".". Example: {@code sb.target.weight=1.0}.
         * </p><p>
         * Optional. Default is 1.0.
         */
        public static final String CONF_WEIGHT = "target.weight";
        public static final String SEARCH_WEIGHT = CONF_WEIGHT;
        public static final double DEFAULT_WEIGHT = 1.0d;

        /**
         * The document frequency to return when no statistics are available
         * for a given term.
         * </p><p>
         * Optional. Default is 1. As 1 gives a significant boost, it is
         * recommended to specify this value according to overall index
         * statistics.
         */
        public static final String CONF_FALLBACK_DF = "target.fallbackdf";
        public static final String SEARCH_FALLBACK_DF = CONF_FALLBACK_DF;
        public static final int DEFAULT_FALLBACK_DF = 1;

        /**
         * The index for document frequency in the termstats.
         * </p><p>
         * Optional. Default is 0.
         */
        public static final String CONF_TERMSTAT_DF_INDEX = "target.termstat.df.index";
        public static final int DEFAULT_TERMSTAT_DF_INDEX = 0;

        /**
         * The folder holding the term stat files.
         * </p><p>
         * Optional. Default is {@code PERSISTENT_ROOT/termstats/id}.
         */
        public static final String CONF_TERMSTAT_LOCATION = "target.termstat.location";

        private final String id;
        private double weight = DEFAULT_WEIGHT;
        private TermStat termStats;
        private long fallbackDF = DEFAULT_FALLBACK_DF;
        private final int dfIndex;
        private final File termstatLocation;

        public Target(Configuration conf) {
            id = conf.getString(CONF_ID);
            weight = conf.getDouble(CONF_WEIGHT, weight);
            fallbackDF = conf.getLong(CONF_FALLBACK_DF, fallbackDF);
            dfIndex = conf.getInt(CONF_TERMSTAT_DF_INDEX, DEFAULT_TERMSTAT_DF_INDEX);
            termStats = new TermStat(conf);
            termstatLocation =
                Resolver.getPersistentFile(
                    conf.valueExists(CONF_TERMSTAT_LOCATION) ?
                    new File(conf.getString(CONF_TERMSTAT_LOCATION)) :
                    new File("termstats/" + id));
            try {
                termStats.open(termstatLocation);
            } catch (IOException e) {
                throw new ConfigurationException(
                    "Unable to open term statistics from '" + termstatLocation
                    + "'. Make sure that valid term stat files are present", e);
            }
            log.debug("Created target '" + id + "' with default weight " + weight + " and term stats " + termStats);
        }

        public long getDF(Request request, final String term) {
            final TermEntry termEntry = termStats.getEntry(term);
            if (log.isTraceEnabled()) {
                if (termEntry == null) {
                    log.trace(
                        "getDF(..., " + term + ") could not locate term entry. Returning df "
                        + request.getLong(SEARCH_FALLBACK_DF, fallbackDF));
                } else {
                    log.trace(
                        "getDF(..., " + term + ") returning df " + termEntry.getStat(dfIndex));
                }
            }
            return termEntry == null ?
                   request.getLong(SEARCH_FALLBACK_DF, fallbackDF) : 
                   termEntry.getStat(dfIndex);
        }

        public double getWeight(Request request) {
            if (log.isTraceEnabled()) {
                if (request.containsKey(SEARCH_WEIGHT)) {
                    log.trace("getWeight returns search-time weight " + request.getDouble(SEARCH_WEIGHT));
                } else {
                    log.trace("getWeight returning static defined weight " + weight);
                }
            }
            return request.getDouble(id + "." + SEARCH_WEIGHT, weight);
        }

        public long getDocCount() {
            return termStats.getDocCount();
        }

        public String getID() {
            return id;
        }
    }
}
