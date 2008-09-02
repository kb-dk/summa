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
package dk.statsbiblioteket.summa.facetbrowser;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

/**
 * The Facet Structure holds top-level information, such as the Facet names
 * and the maximum number of Tags in each Facet. Setup is done through
 * {@link Configuration} - see {@link FacetStructure} for valid parameters.
 * </p><p>
 * Note: The Facet names and IDs in the FacetStructure does not change during an
 *       execution, nor are any Facets added or removed.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class Structure implements Configurable, Serializable {
    private static volatile Logger log = Logger.getLogger(Structure.class);

    /**
     * The property-key CONF_FACETS must contain a list of sub-properties,
     * each holding the setup for a single Facet.
     * </p><p>
     * This property is mandatory.
     * @see {@link FacetStructure}.
     */
    public static final String CONF_FACETS = "summa.facet.facets";

    /**
     * The facet structures. The Map is orderes and the order is significant
     * and used for presentation.
     */
    private Map<String, FacetStructure> facets;

    /**
     * Constructs a new Structure and empty Structure, ready to be filled with
     * FacetStructures.
     * @param facetCount the estimated number of FacetStructures that will be
     *                   used in the new Structure.
     */
    protected Structure(int facetCount) {
        facets = new LinkedHashMap<String, FacetStructure>(facetCount);
    }

    public Structure(Configuration conf) {
        log.debug("Constructing Structure from configuration");
        List<Configuration> facetConfs;
        try {
            facetConfs = conf.getSubConfigurations(CONF_FACETS);
        } catch (ClassCastException e) {
            throw new ConfigurationException(String.format(
                    "Could not extract a list of Configurations from "
                    + "configuration with key '%s' due to a ClassCastException",
                    CONF_FACETS), e);
        } catch (IOException e) {
            throw new ConfigurationException(String.format(
                    "Could not access Configuration for key '%s'", CONF_FACETS),
                                             e);
        }
        facets = new LinkedHashMap<String, FacetStructure>(facetConfs.size());
        int facetID = 0;
        for (Configuration facetConf: facetConfs) {
            try {
                FacetStructure fc = new FacetStructure(facetConf, facetID++);
                if (facets.containsKey(fc.getName())) {
                    log.warn(String.format(
                            "Facets already contain a FacetStructure named "
                            + "'%s'. The old structure will be replaced",
                            fc.getName()));
                }
                log.trace("Adding Facet '" + fc.getName() + "' to facets");
                facets.put(fc.getName(), fc);
            } catch (Exception e) {
                throw new ConfigurationException("Unable to extract single "
                                                 + "Facet configuration", e);
            }
        }
        freezeFacets();
        log.trace("Finished constructing Structure from configuration");
    }

    /**
     * Wraps the facet map as immutable, making it further updates impossible.
     * This should be done after construction, as the immutability of Structure
     * is guaranteed.
     */
    protected void freezeFacets() {
        log.debug("Making facets immutable");
        facets = Collections.unmodifiableMap(facets);
    }

    /* Getters */

    /**
     * It is recommended not to change the map of FacetStructures externally,
     * as this might result in bad sorting of facets.
     * @return the Facet-definitions. The result is an ordered map.
     *         It is expected that the order will be used in presentation.
     */
    public Map<String, FacetStructure> getFacets() {
        return facets;
    }

    /**
     * @param facetName the name of the wanted FacetStructure.
     * @return the wanted FacetStructure or null if the structure is
     *         non-existing.
     */
    public FacetStructure getFacet(String facetName) {
        return facets.get(facetName);
    }

    /**
     * The FacetID is specified in {@link FacetStructure#id}.
     * @param facetID the ID for a Facet.
     * @return the Facet with the given ID.
     * @throws NullPointerException if the Facet could not be located.
     */
    public FacetStructure getFacet(int facetID) {
        for (Map.Entry<String, FacetStructure> entry: facets.entrySet()) {
            if (entry.getValue().getFacetID() == facetID) {
                return entry.getValue();
            }
        }
        throw new NullPointerException(String.format(
                "Could not locate Facet with ID %d", facetID));
    }

    /**
     * The ID of a Facet is the same as its sort position.
     * @param facetName the Facet to retrieve the ID for.
     * @return the ID for the Facet (unique among other Facets).
     */
    public int getFacetID(String facetName) {
        return facets.get(facetName).getFacetID();
    }

    /**
     * @return a list with the names of all the facets, in facet sort order.
     */
    public List<String> getFacetNames() {
        List<String> facetNames = new ArrayList<String>(facets.size());
        for (Map.Entry<String, FacetStructure> entry: facets.entrySet()) {
            facetNames.add(entry.getValue().getName());
        }
        return facetNames;
    }

}
