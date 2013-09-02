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
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;

import java.util.*;

/**
 * Handles aliasing of fields and values in requests. Helper class for {@link dk.statsbiblioteket.summa.support.harmonise.hub.SolrAdjuster}.
 */
@QAInfo(level = QAInfo.Level.PEDANTIC,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrResponseAdjuster {
    private static Log log = LogFactory.getLog(SolrResponseAdjuster.class);

    private final Map<String, ManyToManyMapper> tagAdjusters;
    private List<HubTagAdjuster> hubTagAdjusters = new ArrayList<HubTagAdjuster>();
    private final ManyToManyMapper fieldMap;

    @SuppressWarnings("UnusedParameters")
    public SolrResponseAdjuster(Configuration conf, ManyToManyMapper fieldMap, List<HubTagAdjuster> tagAdjusters) {
        this.fieldMap = fieldMap;
        this.hubTagAdjusters = tagAdjusters;
        this.tagAdjusters = HubTagAdjuster.merge(tagAdjusters);

        log.info("Created " + this);
    }

    public QueryResponse adjust(SolrParams request, QueryResponse response) {
        adjustDocuments(request, response);
        adjustFacets(request, response);
        // TODO: Facets (+range +date...)

        return response;
    }

    private void adjustFacets(SolrParams request, QueryResponse response) {
        List<FacetField> facetFields = response.getFacetFields();
        if (facetFields == null) {
            return;
        }

        List<FacetField> adjustedFacetFields = new ArrayList<FacetField>(facetFields.size());
        for (FacetField facetField: facetFields) {
            if (!tagAdjusters.containsKey(facetField.getName())) {
                adjustedFacetFields.add(facetField);
            } else {
                expandFacet(adjustedFacetFields, facetField);
            }
        }
        mergeAssign(facetFields, adjustedFacetFields);
    }

    private void mergeAssign(List<FacetField> facetFields, List<FacetField> adjustedFacetFields) {
        facetFields.clear();
        // TODO: Add with merging in case of same name. Remember sorting
        facetFields.addAll(adjustedFacetFields);
    }

    private void expandFacet(List<FacetField> adjustedFacetFields, FacetField facetField) {
        expandTags(facetField);
        if (fieldMap == null || !fieldMap.getReverse().containsKey(facetField.getName())) {
            adjustedFacetFields.add(facetField);
            return;
        }
        for (String adjustedField: fieldMap.getReverseSet(facetField.getName())) {
            adjustedFacetFields.add(cloneFacet(facetField, adjustedField));
        }
    }

    private void expandTags(FacetField facetField) {
        // Do we have a mapping at all?
        if (!tagAdjusters.containsKey(facetField.getName())) {
            return;
        }

        List<FacetField.Count> tags = facetField.getValues();
        for (HubTagAdjuster hta: hubTagAdjusters) {
            if (hta.getFacetNames().contains(facetField.getName())) {
                tags = expandTags(tags, hta);
            }
        }
        if (tags == facetField.getValues()) {
            return;
        }
        facetField.getValues().clear();
        for (FacetField.Count tag: tags) {
            facetField.add(tag.getName(), tag.getCount());
        }
    }

    private List<FacetField.Count> expandTags(List<FacetField.Count> tags, HubTagAdjuster hubTagAdjuster) {
        List<FacetField.Count> expandedTags = new ArrayList<FacetField.Count>((int) (tags.size() * 1.5));
        for (FacetField.Count tag: tags) {
            Set<String> ets = hubTagAdjuster.getMap().getReverseSet(tag.getName());
            if (ets == null) {
                expandedTags.add(tag);
            } else {
                for (String et: ets) {
                    expandedTags.add(new FacetField.Count(null, et, tag.getCount()));
                }
            }
        }
        return merge(expandedTags, hubTagAdjuster);
    }

    private List<FacetField.Count> merge(List<FacetField.Count> tags, HubTagAdjuster hubTagAdjuster) {
        // TODO: Implement merging
        return tags;
    }

    private FacetField cloneFacet(FacetField facetField, String newFieldName) {
        FacetField newFacet = new FacetField(newFieldName, facetField.getGap(), facetField.getEnd());
        for (FacetField.Count tag: facetField.getValues()) {
            newFacet.add(tag.getName(), tag.getCount());
        }
        return newFacet;
    }

    @SuppressWarnings("UnusedParameters")
    private void adjustDocuments(SolrParams request, QueryResponse response) {
        SolrDocumentList docs = response.getResults();
        if (docs == null) {
            return;
        }

        for (SolrDocument doc: docs) {
            final Map<String, Collection<Object>> adjustedMap = new HashMap<String, Collection<Object>>();
            for (String field: doc.getFieldNames()) {
                final Collection<Object> adjustedValues = adjustValues(field, doc.getFieldValues(field));
                Set<String> adjustedFields =
                        fieldMap == null ? new HashSet<String>(Arrays.asList(field)) : fieldMap.getReverse().get(field);
                for (String adjustedField: adjustedFields) {
                    if (adjustedMap.containsKey(adjustedField)) {
                        adjustedMap.get(adjustedField).addAll(adjustedValues);
                    } else {
                        adjustedMap.put(adjustedField, adjustedValues);
                    }
                }
            }
            doc.clear();
            for (Map.Entry<String, Collection<Object>> entry: adjustedMap.entrySet()) {
                doc.addField(entry.getKey(),
                             entry.getValue().size() == 1 ? entry.getValue().iterator().next() : entry.getValue());
            }
            // TODO: Grouped
        }

        // TODO: Sort Values
    }

    private Collection<Object> adjustValues(String field, Collection<Object> values) {
        if (tagAdjusters == null || !tagAdjusters.containsKey(field)) {
            return values;
        }
        Collection<Object> adjustedValues = new ArrayList<Object>(values.size());
        for (Object o: values) {
            if (!(o instanceof String)) {
                // TODO: Grouped here?
                adjustedValues.add(o);
            } else {
                Set<String> avs = tagAdjusters.get(field).getReverseSet((String)o);
                if (avs != null) {
                    adjustedValues.addAll(avs);
                } else {
                    adjustedValues.add(o);
                }
            }
        }
        return adjustedValues;
    }

    @Override
    public String toString() {
        return "SolrResponseAdjuster(fieldMap=" + fieldMap + ", #tagAdjusters=" + tagAdjusters.size() + ")";
    }
}
