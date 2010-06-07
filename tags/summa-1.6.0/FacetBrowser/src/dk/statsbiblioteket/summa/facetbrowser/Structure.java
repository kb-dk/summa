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
package dk.statsbiblioteket.summa.facetbrowser;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Facet Structure holds top-level information, such as the Facet names
 * and the maximum number of Tags in each Facet.
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
     * Either this property or
     * {@link IndexDescriptor#CONF_DESCRIPTOR} must be specified.
     * If both properties are specified, the descriptor takes precedence.
     * See the FacetIndexDescriptor.xsd for how to setup the facets.
     * @see {@link FacetStructure}.
     * @deprecated Specify {@link IndexDescriptor#CONF_DESCRIPTOR} instead and
     *             set up the facet structure in an IndexDescriptor.
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

        if (conf.valueExists(IndexDescriptor.CONF_DESCRIPTOR)) {
            defineFacetsFromDescriptor(conf);
        } else if (conf.valueExists(CONF_FACETS)) {
            defineFacetsFromConfiguration(conf);
        } else {
            throw new ConfigurationException(String.format(
                    "Either %s or %s must be specified",
                    IndexDescriptor.CONF_DESCRIPTOR, CONF_FACETS));
        }

        freezeFacets();
        log.trace("Finished constructing Structure from configuration");
    }

    public Structure(URL descriptorLocation) {
        if (descriptorLocation == null) {
            throw new IllegalArgumentException(
                    "Got null as descriptor URL in constructor");
        }
        log.debug(String.format(
                "Constructing Structure from IndexDescriptor at location '%s'",
                descriptorLocation));
        try {
            FacetIndexDescriptor descriptor =
                    new FacetIndexDescriptor(descriptorLocation);
            facets = descriptor.getFacets();
            descriptor.close();
            freezeFacets();
        } catch (IOException e) {
            throw new ConfigurationException(String.format(
                    "Unable to construct facet structure from location '%s'",
                    descriptorLocation), e);
        }
    }

    /**
     * Checks whether the Facet Structure can be derived from the configuration.
     * @param conf is the potential holder of setup-information for Structure.
     * @return true if a Structure can be created based on conf.
     */
    public static boolean isSetupDefinedInConfiguration(Configuration conf) {
        return (conf.valueExists(IndexDescriptor.CONF_DESCRIPTOR)
                || conf.valueExists(CONF_FACETS));
    }

    private void defineFacetsFromDescriptor(Configuration conf) {
        Configuration descriptorConf;
        try {
            descriptorConf =
                    conf.getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR);
        } catch (SubConfigurationsNotSupportedException e) {
            throw new ConfigurationException(
                    "Storage doesn't support sub configurations");
        } catch (NullPointerException e) {
            //noinspection DuplicateStringLiteralInspection
            throw new ConfigurationException(String.format(
                    "Unable to extract sub configuration %s",
                    IndexDescriptor.CONF_DESCRIPTOR));
        }
        try {
            FacetIndexDescriptor descriptor =
                    new FacetIndexDescriptor(descriptorConf);
            facets = descriptor.getFacets();
            descriptor.close();
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Unable to extract facet structure from configuration", e);
        }
    }

    private void defineFacetsFromConfiguration(Configuration conf) {
        List<Configuration> facetConfs;
        try {
            facetConfs = conf.getSubConfigurations(CONF_FACETS);
        } catch (SubConfigurationsNotSupportedException e) {
            throw new ConfigurationException(
                    "Storage doesn't support sub configurations");
        } catch (ClassCastException e) {
            throw new ConfigurationException(String.format(
                    "Could not extract a list of Configurations from "
                    + "configuration with key '%s' due to a ClassCastException",
                    CONF_FACETS), e);
        } catch (NullPointerException e) {
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
                throw new ConfigurationException(
                        "Unable to extract single Facet configuration", e);
            }
        }
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
     * @return the facets that makes up the Structure.
     */
    public List<FacetStructure> getFacetList() {
        List<FacetStructure> result =
                new ArrayList<FacetStructure>(facets.size());
        for (Map.Entry<String, FacetStructure> entry: facets.entrySet()) {
            result.add(entry.getValue());
        }
        return result;
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
        try {
            return facets.get(facetName).getFacetID();
        } catch (NullPointerException e) {
            if (facets == null) {
                throw new IllegalStateException(String.format(
                        "The facets list was null when performing lookup of"
                        + " facet '%s'", facetName));
            } else {
                //noinspection unchecked
                throw new IllegalArgumentException(String.format(
                        "The facet '%s' did not exist in the facet-list %s", 
                        facetName, Logs.expand(new ArrayList(
                        facets.keySet()), 20)));
            }
        }
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

    /**
     * @return a map from Facet-names to maximum tags. The order is significant.
     */
    public HashMap<String, Integer> getMaxTags() {
        HashMap<String, Integer> map =
                new LinkedHashMap<String, Integer>(facets.size());
        for (Map.Entry<String, FacetStructure> entry: facets.entrySet()) {
            map.put(entry.getValue().getName(), entry.getValue().getMaxTags());
        }
        return map;
    }

    /**
     * @return a map from Facet-names to Facet-ids. The order is significant.
     */
    public HashMap<String, Integer> getFacetIDs() {
        HashMap<String, Integer> map =
                new LinkedHashMap<String, Integer>(facets.size());
        for (Map.Entry<String, FacetStructure> entry: facets.entrySet()) {
            map.put(entry.getValue().getName(), entry.getValue().getFacetID());
        }
        return map;
    }

    public HashMap<String, String[]> getFacetFields() {
        HashMap<String, String[]> map =
                new LinkedHashMap<String, String[]>(facets.size());
        for (Map.Entry<String, FacetStructure> entry: facets.entrySet()) {
            map.put(entry.getValue().getName(), entry.getValue().getFields());
        }
        return map;
    }

    /**
     * If possible, absorb the other Structure into this. Absorption is possible
     * if the Facets in this and other are the same, including order, fields
     * and sortLocale.
     * </p><p>
     * Absorption transfers all secondary parameters, such af maxTags and
     * similar into this structure, essentially allowing for on-the-fly tweaks.
     * @param other the Structure to absorb into this.
     * @return true if other was successfully absorbed into this.
     */
    public boolean absorb(Structure other) {
        if (!canAbsorb(other)) {
            return false;
        }
        Iterator<Map.Entry<String, FacetStructure>> thisEntries =
                this.getFacets().entrySet().iterator();
        Iterator<Map.Entry<String, FacetStructure>> otherEntries =
                other.getFacets().entrySet().iterator();
        while (thisEntries.hasNext()) {
            thisEntries.next().getValue().absorb(
                    otherEntries.next().getValue());

        }
        return true;
    }

    /**
     * Compares the facet names, fields and sort locale for equality with
     * significant ordering. If These attributes matches, true is returned.
     * @param other the Structure to compare against.
     * @return true if facet names, fields and sort locale are the same.
     */
    public boolean canAbsorb(Structure other) {
        Map<String, FacetStructure> thisFacets = getFacets();
        Map<String, FacetStructure> otherFacets = other.getFacets();
        if (thisFacets.size() != otherFacets.size()) {
            log.debug("absorb(): The number of facets differed");
            return false;
        }
        Iterator<Map.Entry<String, FacetStructure>> thisEntries =
                thisFacets.entrySet().iterator();
        Iterator<Map.Entry<String, FacetStructure>> otherEntries =
                otherFacets.entrySet().iterator();
        int counter = 0;
        while (thisEntries.hasNext()) {
            FacetStructure thisFacet = thisEntries.next().getValue();
            FacetStructure otherFacet = otherEntries.next().getValue();

            if (!thisFacet.getName().equals(otherFacet.getName())) {
                log.debug(String.format(
                        "absorb(): The facets at position %d (counting from 0)"
                        + " did not have the same name: '%s' vs. '%s'",
                        counter, thisFacet.getName(), otherFacet.getName()));
                return false;
            }
            if (!Strings.join(thisFacet.getFields(), ", ").equals(
                    Strings.join(otherFacet.getFields(), ", "))) {
                log.debug(String.format(
                        "absorb(): The fields for facet '%s' were not the "
                        + "same: '%s' vs. '%s'",
                        thisFacet.getName(),
                        Strings.join(thisFacet.getFields(), ", "),
                        Strings.join(otherFacet.getFields(), ", ")));
                return false;
            }
            if ((thisFacet.getLocale() == null
                 && otherFacet.getLocale() != null)
                || (thisFacet.getLocale() != null &&
                    !thisFacet.getLocale().equals(otherFacet.getLocale()))) {
                log.debug(String.format(
                        "absorb(): The sortLocales for facet '%s' were not the "
                        + "same: '%s' vs. '%s'",
                        thisFacet.getName(),
                        thisFacet.getLocale(), otherFacet.getLocale()));
            }
            counter++;
        }
        log.trace("Facet names, fields and sort-locale matches");
        return true;
    }

    public String toString(boolean verbose) {
        if (!verbose) {
            return toString();
        }
        return String.format("Structure(%s)",
                             join(getFacetList(), ", "));
    }

    // TODO: Should be Strings.join, but it fails?
    private String join (Collection c, String delimiter) {
        if (c == null) {
            throw new NullPointerException("Collection argument is null");
        } else if (delimiter == null) {
            throw new NullPointerException("Delimiter argument is null");
        }

        StringBuilder b = new StringBuilder();

        for (Object o : c) {
            if (b.length() == 0) {
                b.append(o == null ? "" : o.toString());
            } else {
                b.append (delimiter);
                b.append(o == null ? "" : o.toString());
            }
        }

        return b.toString();
    }

    @Override
    public String toString() {
        return "Structure(" + facets.size() + " facets)";
    }
}

