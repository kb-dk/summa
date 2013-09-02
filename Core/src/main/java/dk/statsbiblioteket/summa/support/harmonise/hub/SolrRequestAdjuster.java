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
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Handles aliasing of fields and values in requests. Helper class for {@link SolrAdjuster}.
 */
@QAInfo(level = QAInfo.Level.PEDANTIC,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrRequestAdjuster {
    private static Log log = LogFactory.getLog(SolrRequestAdjuster.class);

    private final Map<String, ManyToManyMapper> tagAdjusters;
    //private List<HubTagAdjuster> tagAdjusters = new ArrayList<HubTagAdjuster>();
    private final ManyToManyMapper defaultFieldMap;
    private final QueryAdjuster queryAdjuster;

    public SolrRequestAdjuster(Configuration conf, ManyToManyMapper fieldMap, List<HubTagAdjuster> tagAdjusters) {
        defaultFieldMap = fieldMap;
        this.tagAdjusters = HubTagAdjuster.merge(tagAdjusters);
        queryAdjuster = new QueryAdjuster(conf, defaultFieldMap, this.tagAdjusters);

        log.info("Created " + this);
    }

    private static final Pattern SPLIT = Pattern.compile(" +| *, *");

    public SolrParams adjust(ModifiableSolrParams request) {
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

    @Override
    public String toString() {
        // TODO: Implement this
        return "SolrRequestAdjuster(fieldMap=" + defaultFieldMap + ", #tagAdjusters=" + tagAdjusters.size() + ")";
    }
}
