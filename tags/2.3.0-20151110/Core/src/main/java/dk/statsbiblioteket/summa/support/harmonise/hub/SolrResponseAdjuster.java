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
    private List<HubTagAdjuster> hubTagAdjusters = new ArrayList<>();
    private final ManyToManyMapper fieldMap;

    @SuppressWarnings("UnusedParameters")
    public SolrResponseAdjuster(Configuration conf, ManyToManyMapper fieldMap, List<HubTagAdjuster> tagAdjusters) {
        this.fieldMap = fieldMap;
        this.hubTagAdjusters = tagAdjusters;
        this.tagAdjusters = HubTagAdjuster.merge(tagAdjusters);

        log.info("Created " + this);
    }

    /**
     * Perform all necessary adjustments of the given response.
     * @param request  the originating request that lead to the response.
     * @param response a non-mapped Solr response.
     * @return the response mapped as specified when creating the SolrResponseAdjuster.
     */
    public QueryResponse adjust(SolrParams request, QueryResponse response) {
        adjustDocuments(request, response);
        for (HubTagAdjuster hubTagAdjuster: hubTagAdjusters) {
            hubTagAdjuster.adjust(request, response);
        }
        adjustFacetFields(request, response);
        return response;
    }

    /**
     * Adjust field names as well as tag content for the facets in the given response.
     * @param request  the originating request that lead to the given response.
     * @param response normally with facet structures.
     */
    private void adjustFacetFields(SolrParams request, QueryResponse response) {
        if (fieldMap == null) {
            return;
        }
        List<FacetField> facetFields = response.getFacetFields();
        if (facetFields == null) {
            return;
        }
        List<FacetField> adjustedFacetFields = new ArrayList<>(facetFields.size());
        for (FacetField facetField: facetFields) {
            expandFacet(adjustedFacetFields, facetField);
        }
        merge(request, facetFields, adjustedFacetFields);
    }

    /**
     * Merges the source facet fields into the destination list. Duplicate facet fields, identified by name, are
     * handled by copying the tags from the source into the destination facet field.
     * @param request     the request that lead to the given source facet fields.
     * @param destination the facet fields from source are added here.
     * @param source      facet fields to merge into destination.
     */
    private void merge(SolrParams request, List<FacetField> destination, List<FacetField> source) {
        destination.clear();

        for (FacetField adjusted: source) {
            FacetField existing = getMatchingField(destination, adjusted);
            if (existing == null) {
                destination.add(adjusted);
                continue;
            }
            merge(request, existing, adjusted);
        }
    }

    /**
     * Merge the Counts from the source facet field into the existing facet field.
     * @param request  the request that lead to the facet fields.
     * @param existing receiving field for the Counts from source.
     * @param source   the provider of extra Counts.
     */
    @SuppressWarnings("UnusedParameters")
    private void merge(SolrParams request, FacetField existing, FacetField source) {
        for (FacetField.Count count: source.getValues()) {
            // TODO: Merge counts - what about min/max/sum?
            existing.add(count.getName(), count.getCount());
        }
    }

    /**
     * Seeks through existing fields and return the field matching the name in wanted field.
     * @param existing a list of fields to search through.
     * @param wanted   the field to search for.
     * @return a matching field from existing or null if there was no match.
     */
    private FacetField getMatchingField(List<FacetField> existing, FacetField wanted) {
        for (FacetField current: existing) {
            if (wanted.getName().equals(current.getName())) {
                return current;
            }
        }
        return null;
    }

    /**
     * Resolve the mapped name(s) for facetField and create new facet field(s) with the name(s), adding them to
     * adjustedFacetFields.
     * @param adjustedFacetFields the destination for the mapped facetField.
     * @param facetField a non-mapped facet field.
     */
    private void expandFacet(List<FacetField> adjustedFacetFields, FacetField facetField) {
        if (fieldMap == null || !fieldMap.getReverse().containsKey(facetField.getName())) {
            adjustedFacetFields.add(facetField);
            return;
        }
        for (String adjustedField: fieldMap.getReverseSet(facetField.getName())) {
            adjustedFacetFields.add(cloneFacet(facetField, adjustedField));
        }
    }

    /**
     * Utility-method for cloning a facet field with a new name.
     * @param facetField the facet field to clone.
     * @param newFieldName the new name.
     * @return the cloned facet field with the new name, fully independent from the old facet field.
     */
    private FacetField cloneFacet(FacetField facetField, String newFieldName) {
        FacetField newFacet = new FacetField(newFieldName, facetField.getGap(), facetField.getEnd());
        for (FacetField.Count tag: facetField.getValues()) {
            newFacet.add(tag.getName(), tag.getCount());
        }
        return newFacet;
    }

    /**
     * Adjust fields and text in documents according to the original configuration.
     * @param request  the request that led to the given response.
     * @param response the document structures in this will be adjusted.
     */
    @SuppressWarnings("UnusedParameters")
    private void adjustDocuments(SolrParams request, QueryResponse response) {
        SolrDocumentList docs = response.getResults();
        if (docs == null) {
            return;
        }

        for (SolrDocument doc: docs) {
            final Map<String, Collection<Object>> adjustedMap = new HashMap<>();
            for (String field: doc.getFieldNames()) {
                final Collection<Object> adjustedValues = adjustValues(field, doc.getFieldValues(field));
                Set<String> adjustedFields =
                        fieldMap == null ? new HashSet<>(Arrays.asList(field)) : fieldMap.getReverse().get(field);
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
            // TODO: Adjust scores
        }

        // TODO: Sort Values
    }

    private Collection<Object> adjustValues(String field, Collection<Object> values) {
        if (tagAdjusters == null || !tagAdjusters.containsKey(field)) {
            return values;
        }
        Collection<Object> adjustedValues = new ArrayList<>(values.size());
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
