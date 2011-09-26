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
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A representation of a facet request. Used to bridge Summa and Summon facet
 * handling.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummonFacetRequest {
    private static Log log = LogFactory.getLog(SummonFacetRequest.class);
    private String originalRequest;
    private Structure originalStructure;
    private List<Facet> facets;

    /**
     * @param facetsDef   a comma-separated list of facets, optionally with
     *                    pageSize in parenthesis after fact name.
     * @param defaultFacetPageSize if no page size is specified for a facet,
     *                    this page size is used.
     * @param combineMode 'and' or 'or' as per the Summon API.
     */
    public SummonFacetRequest(String facetsDef, int defaultFacetPageSize,
                              String combineMode) {
        originalRequest = facetsDef;
        originalStructure = new Structure(facetsDef, defaultFacetPageSize);
        facets = new ArrayList<Facet>(originalStructure.getFacetList().size());
        for (FacetStructure fc: originalStructure.getFacetList()) {
            if (!FacetStructure.SORT_POPULARITY.equals(fc.getSortType())) {
                log.warn("The facet request '" + facetsDef + "' defines sort "
                         + "order that is not '"
                         + FacetStructure.SORT_POPULARITY + "'. This is not "
                         + "supported by this faceter");
            }
            facets.add(new Facet(
                fc.getFields()[0], combineMode, 1, fc.getWantedTags()));
        }
        log.trace("Constructed facet request from '" + facetsDef + "' with "
                  + "defaultFacetPageSize=" + defaultFacetPageSize
                  + " and combineMode=" + combineMode);
    }

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
     * @return the facet request as a list of Summon API facet definitions.
     */
    public List<String> getFacetQueries() {
        List<String> facetQueries = new ArrayList<String>(facets.size());
        for (Facet facet: facets) {
            facetQueries.add(facet.getSummonCall());
        }
        return facetQueries;
    }

    /**
     * @return max tags aka pageSize for each facet.
     */
    public HashMap<String, Integer> getMaxTags() {
        HashMap<String, Integer> max = new HashMap<String, Integer>(facets.size());
        for (Facet facet: facets) {
            max.put(facet.getField(), facet.getPageSize());
        }
        return max;
    }

    public static final class Facet {
        private String field;
        private String combineMode;
        private int startPage;
        private int pageSize;

        public Facet(String field, String combineMode,
                     int startPage, int pageSize) {
            this.field = field;
            if (!("and".equals(combineMode) || "or".equals(combineMode))) {
                throw new IllegalArgumentException(
                    "Only 'and' and 'or' are acceptable combine modes for "
                    + "facets. Received '" + combineMode + "'");
            }
            this.combineMode = combineMode;
            this.startPage = startPage;
            this.pageSize = pageSize;
        }

        public String getSummonCall() {
            return field + "," + combineMode + ",1," + pageSize;
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
