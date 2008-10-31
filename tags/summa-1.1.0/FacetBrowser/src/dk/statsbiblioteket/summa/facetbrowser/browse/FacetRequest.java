/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

/**
 * A request for a facet structure, containing facet names, wanted number of
 * tags and so on.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetRequest extends Structure {
    private static Logger log = Logger.getLogger(FacetRequest.class);

    private DocIDCollector docIDs;

    /**
     * Construct a request for a Facet structure, based on the given data.
     * @param docIDs   the ids for the documents to calculate Tags for.
     * @param facets   a comma-separeted list with the names of the wanted
     *                 Facets.
     *                 Optionally, the maximum Tag-count for a given Facet can
     *                 be specified in parenthesis after the name.
     *                 Example: "Title, Author (5), City (10), Year".
     *                 If no maximum Tag-count is specified, the number is
     *                 taken from the defaults.
     *                 Optionally, the sort-type for a given Facet can be
     *                 specified in the same parenthesis. Valid values are
     *                 {@link FacetStructure#SORT_POPULARITY} and
     *                 {@link FacetStructure#SORT_ALPHA}.
     *                 If no sort-type is specified, the number is taken from
     *                 the defaults.
     *                 Example: "Title (ALPHA), Author (5 POPULARITY), City"
     * @param defaults the base for building the request.
     */
    public FacetRequest(DocIDCollector docIDs,
                   String facets, Structure defaults) {
        super(defaults.getFacets().size());
        this.docIDs = docIDs;
        parse(facets, defaults);
    }

    private void parse(String wantedFacets, Structure defaults) {
        if (wantedFacets == null || "".equals(wantedFacets)) {
            log.trace("No Facets requested, using defaults");
            getFacets().putAll(defaults.getFacets());
            return;
        }
        log.debug("Parsing '" + wantedFacets + "'");
        try {
            String[] tokens = wantedFacets.split(" *, *");
            for (String facetToken: tokens) {
                String[] subTokens = facetToken.split("\\(", 2);
                FacetStructure fc = defaults.getFacets().get(subTokens[0]);
                if (fc == null) {
                    log.warn(String.format(
                            "Could not find a Facet named '%s', parsed from "
                            + "'%s' in the default structure. Skipping Facet",
                            subTokens[0], wantedFacets));
                    continue;
                }
                if (subTokens.length == 1) {
                    getFacets().put(fc.getName(), fc);
                    continue;
                }
                // "  5  ALPHA)  " | "5)" | " ALPHA) | "vgfsd"
                String noParen = subTokens[1].split("\\)", 1)[0].trim();
                // "5  ALPHA" | "5" | "ALPHA" | "vgfsd"
                String[] facetArgs = noParen.split(" +", 2);
                // "5", "ALPHA" | "5" | "ALPHA" | "vgfsd"
                Integer maxTags = null;
                String sortType = null;
                for (String facetArg: facetArgs) {
                    if (FacetStructure.SORT_POPULARITY.equals(facetArg)) {
                        sortType = FacetStructure.SORT_POPULARITY;
                    } else if (FacetStructure.SORT_ALPHA.equals(facetArg)) {
                        sortType = FacetStructure.SORT_ALPHA;
                    } else {
                        try {
                            maxTags = Integer.parseInt(facetArg);
                        } catch (NumberFormatException e) {
                            log.warn(String.format(
                                    "Argument '%s' to Facet '%s' in request "
                                    + "'%s' could not be parsed",
                                    facetArg, facetToken, wantedFacets));
                        }
                    }
                }
                getFacets().put(fc.getName(),
                                fc.getRequestFacet(maxTags, sortType));
            }
        } catch (Exception e) {
            log.warn(String.format(
                    "Exception encountered while parsing '%s'. "
                    + "Switching to default facets", wantedFacets));
            getFacets().putAll(defaults.getFacets());
        }
        freezeFacets();
    }

    /* Getters */

    public DocIDCollector getDocIDs() {
        return docIDs;
    }
}



