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
import org.apache.solr.common.params.FacetParams;
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
    private final ManyToManyMapper fieldMap;
    private final QueryAdjuster queryAdjuster;

    public SolrRequestAdjuster(Configuration conf, ManyToManyMapper fieldMap, List<HubTagAdjuster> tagAdjusters) {
        this.fieldMap = fieldMap;
        this.tagAdjusters = HubTagAdjuster.merge(tagAdjusters);
        queryAdjuster = new QueryAdjuster(conf, this.fieldMap, this.tagAdjusters);

        log.info("Created " + this);
    }

    private static final Pattern SPLIT = Pattern.compile(" +| *, *");

    /**
     * Adjusts fields and terms in the given request, according to {@link #fieldMap} and {@link #tagAdjusters}.
     * @param request will be modified.
     * @return the modified request. This might be the request object.
     */
    public SolrParams adjust(ModifiableSolrParams request) {
        // Only a single query
        expandQuery(request, CommonParams.Q);

        // Multiple filters
        expandQuery(request, CommonParams.FQ);

        // Default field
        String queryField = request.get(CommonParams.DF);
        if (queryField != null) {
            Set<String> adjustedQueryFields = fieldMap.getForward().get(queryField);
            if (adjustedQueryFields != null) {
                if (adjustedQueryFields.size() != 1) {
                    log.warn(String.format(
                            "The requested default query field '%s' expanded to %d fields (%s). Only 1 is allowed",
                            queryField, adjustedQueryFields.size(), Strings.join(adjustedQueryFields)));
                }
            }
        }

        // Field list
        expandFields(request, CommonParams.FL, true);

        // Facets
        expandFields(request, FacetParams.FACET_FIELD, true);

        // Facet ranges
        expandFields(request, FacetParams.FACET_RANGE, true);

        // Facet dates
        expandFields(request, FacetParams.FACET_DATE, true);

        // Facet pivot
        expandFields(request, FacetParams.FACET_PIVOT, true);

        // Facet parameters (prefixed by 'f.')
        // We copy the names to avoid errors when iterating and modifying
        for (String key: new HashSet<String>(request.getParameterNames())) {
            if (!key.startsWith("f.")) {
                continue;
            }
            String[] tokens = DOT_SPLIT.split(key, 3);
            if (tokens.length == 1) {
                continue;
            }
            String field = tokens[1];
            Set<String> mappedFields = fieldMap.getForward().get(field);
            if (mappedFields == null) {
                continue;
            }
            request.remove(key);
            for (String mappedField: mappedFields) {
                String mappedKey = "f." + mappedField + (tokens.length == 2 ? "" : tokens[2]);
                if (request.get(mappedKey) != null) {
                    log.warn(String.format(
                            "Mapping '%s' to '%s' in Solr request but the destination key already exists."
                            + "The destination key will be overwritten", key, mappedKey));
                    request.remove(mappedKey);
                }
                // This always collapses to String so we lose types. That is not optimal
                request.set(mappedField, request.getParams(key));
            }
        }

        return request;
    }
    private static final Pattern DOT_SPLIT = Pattern.compile("[.]");

    /**
     * Extracts the queries for the key and rewrites them according to {@link #fieldMap} and
     * {@link #tagAdjusters}, assigning the result back into the request.
     * @param request will potentially be modified.
     * @param key     the key for the values to map.
     */
    private void expandQuery(ModifiableSolrParams request, String key) {
        String[] queries = request.getParams(key);
        if (queries != null) {
            for (int i = 0 ; i < queries.length ; i++) {
                try {
                    queries[i] = queryAdjuster.rewrite(queries[i]);
                } catch (ParseException e) {
                    log.warn("ParseException for query '" + queries[i] + "'. Query not rewritten", e);
                }
            }
            request.remove(key);
            request.set(key, queries);
        }
    }

    /**
     * If values for the given key exists, they are mapped using the {@link #fieldMap} to new fields, which
     * are assigned back into the request.
     * @param request     will potentially be modified.
     * @param key         the key for the values to map.
     * @param splitValues if true, values are assumed to be comma-separated and will be split accordingly.
     */
    private void expandFields(ModifiableSolrParams request, String key, boolean splitValues) {
        if (fieldMap == null) {
            return;
        }
        String[] fields = request.getParams(key);
        if (fields == null) {
            return;
        }

        Set<String> newFields = new HashSet<String>();
        for (String field : fields) {
            if (splitValues) {
                for (String subField : SPLIT.split(field)) {
                    expandFields(newFields, subField);
                }
            } else {
                expandFields(newFields, field);
            }
        }
        request.remove(key);
        if (newFields.size() == 1) {
            request.set(key, newFields.iterator().next());
        } else if (!newFields.isEmpty()) {
            String[] aFields = new String[newFields.size()];
            newFields.toArray(aFields);
            request.set(key, aFields);
        }
    }

    /**
     * Expand the given field according to {@link #fieldMap} and adds the result to fields.
     * @param fields the sink for the expanded field.
     * @param field the field to expand.
     */
    private void expandFields(Set<String> fields, String field) {
        Set<String> replacements = fieldMap.getForwardSet(field);
        if (replacements == null) {
            fields.add(field);
        } else {
            fields.addAll(replacements);
        }
    }

    @Override
    public String toString() {
        // TODO: Implement this
        return "SolrRequestAdjuster(fieldMap=" + fieldMap + ", #tagAdjusters=" + tagAdjusters.size() + ")";
    }
}
