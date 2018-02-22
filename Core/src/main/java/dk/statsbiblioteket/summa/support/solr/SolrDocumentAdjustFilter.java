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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tweaks Solr Documents.
 */
public class SolrDocumentAdjustFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(SolrDocumentAdjustFilter.class);

    public enum ADJUSTMENT {
        /** Add Record meta-data toSolr document {@link SolrDocumentEnrich} */
        enrich,
        /** Adjust wrong date using lenient parsing {@link SolrLenientTimestamp} */
        lenient_dates
    }

    /**
     * The adjustments to perform on Solr Document XML.
     * See {@link ADJUSTMENT} for possible values.
     * </p><p>
     * Optional. Default is {@code enrich}.
     * </p>
     */
    public static final String CONF_ADJUSTMENTS = "solradjuster.adjustments";
    public static final List<ADJUSTMENT> DEFAULT_ADJUSTMENTS = Arrays.asList(ADJUSTMENT.values());

    private final List<Adjuster> adjustments;

    public SolrDocumentAdjustFilter(Configuration conf) {
        super(conf);
        final List<ADJUSTMENT> adjs;
        if (!conf.containsKey(CONF_ADJUSTMENTS)) {
            log.info("No adjustments specified with key " + CONF_ADJUSTMENTS +
                     ". Defaulting to [" + Strings.join(DEFAULT_ADJUSTMENTS) + "]");
            adjs = DEFAULT_ADJUSTMENTS;
        } else {
            adjs = new ArrayList<>();
            for (String adj: conf.getStrings(CONF_ADJUSTMENTS)) {
                adjs.add(ADJUSTMENT.valueOf(adj));
            }
        }
        adjustments = new ArrayList<>(adjs.size());
        for (ADJUSTMENT adjustment: adjs) {
            switch (adjustment)  {
                case enrich: {
                    adjustments.add(new SolrDocumentEnrich(conf));
                    break;
                }
                case lenient_dates: {
                    adjustments.add(new SolrLenientTimestamp(conf));
                    break;
                }
                default: throw new UnsupportedOperationException("Unknown adjuster '" + adjustment + "'");
            }
        }
        log.info("Created " + this);
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        for (Adjuster adjuster: adjustments) {
            adjuster.adjust(payload);
        }
        return true;
    }

    public interface Adjuster {
        /**
         * @return true if the payload was adjusted.
         */
        boolean adjust(Payload payload) throws PayloadException;
    }

    @Override
    public String toString() {
        return "SolrDocumentAdjustFilter(adjustments=[" + Strings.join(adjustments) + "])";
    }

    @Override
    public void close(boolean success) {
        super.close(success);
    }
}
