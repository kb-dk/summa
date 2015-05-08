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
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.lucene.distribution.TermEntry;
import dk.statsbiblioteket.summa.common.lucene.distribution.TermStat;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;

/**
 * The target encapsulates a TermStat component to deliver document frequencies.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStatTarget implements Configurable {
    private static Log log = LogFactory.getLog(TermStatTarget.class);
    /**
     * The id for the target. Used for specifying search-time adjustments
     * and for loading term statistics.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_ID = "target.id";

    /**
     * The id for the component that this target should rewrite queries for. this will normally be the id of a direct
     * child of {@link TermStatRewriter}.
     * </p><p>
     * Optional. If not defined, {@link #CONF_ID} is used.
     */
    public static final String CONF_COMPONENT_ID = "target.componentid";

    /**
     * The relative weight of the stats from this target.
     * </p><p>
     * Sane values go from 0.0 to 1.0, where 0.0 is a complete disregard of
     * the targets effect on the merged term stats and 1.0 is full effect.
     * </p><p>
     * If the value is specified at runtime, it must be prefixed with the
     * target ID + ".". Example: {@code sb.target.weight=1.0}.
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
    private final String componentID;
    private double weight;
    private TermStat termStats;
    private long fallbackDF;
    private final int dfIndex;
    private final File termstatLocation;

    public TermStatTarget(Configuration conf) {
        id = conf.getString(CONF_ID);
        componentID = conf.getString(CONF_COMPONENT_ID, id);
        weight = conf.getDouble(CONF_WEIGHT, DEFAULT_WEIGHT);
        fallbackDF = conf.getLong(CONF_FALLBACK_DF, DEFAULT_FALLBACK_DF);
        dfIndex = conf.getInt(CONF_TERMSTAT_DF_INDEX, DEFAULT_TERMSTAT_DF_INDEX);
        termStats = new TermStat(conf);
        termstatLocation = Resolver.getPersistentFile(
                conf.valueExists(CONF_TERMSTAT_LOCATION) ?
                        new File(conf.getString(CONF_TERMSTAT_LOCATION)) :
                        new File("termstats/" + id));
        try {
            termStats.open(termstatLocation);
        } catch (IOException e) {
            throw new ConfigurationException("Unable to open term statistics from '" + termstatLocation
                                             + "'. Make sure that valid term stat files are present", e);
        }
        log.info("Created " + this);
    }

    /**
     * Return the document frequency for the given term or {@link #fallbackDF} if the term is not present.
     * @param term the term to resolve.
     * @return a document frequency for the given term.
     */
    @SuppressWarnings("UnnecessaryParentheses")
    public long getDF(final String term) {
        return getDF(term, (int)fallbackDF);
    }

    /**
     * Return the document frequency for the given term.
     * @param term the term to resolve.
     * @param defaultDF if the term is not in the termstats file, the defaultDF is returned unless defaultDF == -1,
     *                  in which case the fallbackDF is returned.
     * @return a document frequency for the given term.
     */
    @SuppressWarnings("UnnecessaryParentheses")
    public long getDF(final String term, int defaultDF) {
        final TermEntry termEntry = termStats.getEntry(term);
        if (log.isTraceEnabled()) {
            if (termEntry == null) {
                log.trace("getDF(..., " + term + ") could not locate term entry. Returning defaultDF=" + defaultDF);
            } else {
                log.trace("getDF(..., " + term + ") returning df " + termEntry.getStat(dfIndex));
            }
        }
        return termEntry == null ?
                defaultDF == -1 ? fallbackDF : defaultDF :
                termEntry.getStat(dfIndex);
    }

    public double getWeight() {
        return weight;
    }
    public long getDocCount() {
        return termStats.getDocCount();
    }

    public String getID() {
        return id;
    }

    public String getComponentID() {
        return componentID;
    }

    @Override
    public String toString() {
        return String.format("TermStatTarget(id=%s, componentID=%s, termStatFile=%s, fallbackDF=%d, weight=%s)",
                             id, componentID, termstatLocation, fallbackDF, weight);
    }

    public int getFallbackDF() {
        // TODO: Hates this, but the SolrParams does not support longs. Make a long-getter for SolrParams (and upstream)
        return (int) fallbackDF;
    }

    public void setFallbackDF(long fallbackDF) {
        this.fallbackDF = fallbackDF;
    }

}
