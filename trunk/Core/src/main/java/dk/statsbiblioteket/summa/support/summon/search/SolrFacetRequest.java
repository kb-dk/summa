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
package dk.statsbiblioteket.summa.support.summon.search;

import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * A representation of a facet request. Used to bridge Summa and Solr facet handling.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrFacetRequest {
    private static Log log = LogFactory.getLog(SolrFacetRequest.class);
    private String originalRequest;
    private Structure originalStructure;
    private int minCount;
    private List<Facet> facets;

    /**
     * @param facetsDef   a comma-separated list of facets, optionally with
     *                    pageSize in parenthesis after fact name.
     * @param defaultFacetPageSize if no page size is specified for a facet,
     *                    this page size is used.
     * @param combineMode 'and' or 'or' as per the Solr API.
     */
    public SolrFacetRequest(String facetsDef, int minCount, int defaultFacetPageSize, String combineMode) {
        originalRequest = facetsDef;
        this.minCount = minCount;
        originalStructure = new Structure(facetsDef, defaultFacetPageSize);
        facets = new ArrayList<>(originalStructure.getFacetList().size());
        for (FacetStructure fc: originalStructure.getFacetList()) {
/*            if (FacetStructure.SORT_ALPHA.equals(fc.getSortType()) && fc.getLocale() != null) {
                log.warn("The facet request '" + facetsDef + "' defines sort order ALPHA with locale " + fc.getLocale()
                         + ". Locale based order is not '" + FacetStructure.SORT_POPULARITY
                         + "'. This is not supported by this faceter");
            }*/
            if (fc.getFields().length > 1 && !fieldWarningFired) {
                log.warn("The facet request '" + facetsDef + "' defines more than one field for facet '" + fc.getName()
                         + ". This is not supported by this faceter. Only the first field will be used. This warning "
                         + "will not be repeated");
                fieldWarningFired = true;
            }
            facets.add(new Facet(fc.getFields()[0], combineMode, 0, fc.getWantedTags())); // Start at 0?
        }
        log.trace("Constructed facet request from '" + facetsDef + "' with defaultFacetPageSize=" + defaultFacetPageSize
                  + " and combineMode=" + combineMode);
    }
    private static boolean fieldWarningFired = false;

    public Structure getOriginalStructure() {
        return originalStructure;
    }

    public List<Facet> getFacets() {
        return facets;
    }

    @Override
    public String toString() {
        return originalRequest;
    }

    /**
     * @return max tags aka pageSize for each facet.
     */
    public HashMap<String, Integer> getMaxTags() {
        HashMap<String, Integer> max = new HashMap<>(facets.size());
        for (Facet facet: facets) {
            max.put(facet.getField(), facet.getPageSize());
        }
        return max;
    }

    public void addFacetQueries(Map<String, List<String>> queryMap) {
        append(queryMap, "facet.mincount", Integer.toString(minCount));
        for (Facet facet: facets) {
            addFacetQuery(
                queryMap, facet.getField(), facet.getCombineMode(), facet.getStartPage(), facet.getPageSize());
        }
    }

    // Override to get search backend specific behaviour.

    /**
     * Adds a single facet query from the list of facets. This will be called once for every Facet.
     * This uses SimpleFacetParameters for standard Solr. See https://wiki.apache.org/solr/SimpleFacetParameters for
     * details. Override this method  to get search backend specific facet query syntax.
     * @param queryMap    the Sold queries.
     * @param field       the field for the facet to add.
     * @param combineMode 'or' or 'and'. While this has no influence on the display, it might be used to extract how
     *                    drill-down should be requested.
     * @param startPage   where to start in the tag list.
     * @param pageSize    the number of tags on a single page.
     */
    protected void addFacetQuery(Map<String, List<String>> queryMap, String field, String combineMode,
                                 int startPage, int pageSize) {
        append(queryMap, "facet.field", field);
        String prefix = "f." + field + ".facet.";
        // TODO: Check if startPage is counted from 0 or 1
        queryMap.put(prefix + "offset", Arrays.asList(Integer.toString(startPage * pageSize)));
        queryMap.put(prefix + "limit",  Arrays.asList(Integer.toString(pageSize)));
    }

    /**
     * If the key does not exist in the queryMap, the value is added (encapsulated in a List). If a value for the key
     * already exists, the value is added to the List corresponding to the key.
     * @param queryMap where to append the value to.
     * @param key      the key for the value.
     * @param value    the value to add.
     */
    protected void append(Map<String, List<String>> queryMap, String key, String value) {
        List<String> existing = queryMap.get(key);
        if (existing == null) {
            queryMap.put(key, Arrays.asList(value));
            return;
        }
        if (!(existing instanceof ArrayList)) {
            existing = new ArrayList<>(existing);
            queryMap.put(key, existing);
        }
        existing.add(value);
    }

    public static final class Facet {
        private String field;
        private String combineMode;
        private int startPage;
        private int pageSize;

        public Facet(String field, String combineMode, int startPage, int pageSize) {
            this.field = field;
            if (!("and".equals(combineMode) || "or".equals(combineMode))) {
                throw new IllegalArgumentException(
                    "Only 'and' and 'or' are acceptable combine modes for facets. Received '" + combineMode + "'");
            }
            this.combineMode = combineMode;
            this.startPage = startPage;
            this.pageSize = pageSize;
        }

        public String getField() {
            return field;
        }

        public String getCombineMode() {
            return combineMode;
        }

        public int getStartPage() {
            return startPage;
        }

        public int getPageSize() {
            return pageSize;
        }
    }
}
