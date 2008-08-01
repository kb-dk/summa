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
import dk.statsbiblioteket.util.XProperties;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Facet Structure holds top-level information, such as the Facet names
 * and the maximum number of Tags in each Facet. Setup is done through
 * {@link Configuration}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment="Configuration isn't intuitive with the list of storages")
//FIXME: Structure relies on XProperties in configuration
public class Structure implements Configurable {
    private static Logger log = Logger.getLogger(Structure.class);

    /**
     * The returned Tags should be sorted in order with the most popular Tags
     * first. Popularity translates directly to the number of occurences.
     */
    public static final String SORT_POPULARITY = "POPULARITY";
    /**
     * The returned tags should be sorted in the order they are stored in the
     * underlying Tag-structure. This will normally be in (localized) alpha-
     * numeric order. Tags with 0 hits will be skipped.
     */
    public static final String SORT_ALPHA = "ALPHA";

    /* Configuration-related constants */

    /**
     * The property-key CONF_FACETS should contain a list of sub-properties,
     * each holding the setup for a single Facet.
     * </p><p>
     * This property is mandatory.
     */
    public static final String CONF_FACETS = "summa.facet.facets";

    /**
     * The name of the Facet.
     * </p><p>
     * This property is mandatory.
     */
    public static final String CONF_FACET_NAME = "summa.facet.name";

    /**
     * The fields that make up the Facet. Stored as a comma-separated list
     * of Strings.
     * </p><p>
     * This property is optional. Default is {@link #CONF_FACET_NAME}.
     */
    public static final String CONF_FACET_FIELDS = "summa.facet.fields";

    /**
     * The maximum number of Tags that can be requested for the Facet.
     * </p><p>
     * This property is optional. Default is {@link #DEFAULT_TAGS_MAX}.
     */
    public static final String CONF_FACET_TAGS_MAX = "summa.facet.tags.max";
    public static final int DEFAULT_TAGS_MAX = 200;

    /**
     * The default wanted number of Tags for a Facet. This number can be
     * overridden when a Facet representation is requested.
     * </p><p>
     * This property is optional. Default is {@link #DEFAULT_TAGS_WANTED}.
     */
    public static final String CONF_FACET_TAGS_WANTED =
                                                      "summa.facet.tags.wanted";
    public static final int DEFAULT_TAGS_WANTED = 15;

    /**
     * The default sort type for a Facet.
     * </p><p>
     * This property is optional. Default is {@link #DEFAULT_FACET_SORT_TYPE}.
     */
    public static final String CONF_FACET_SORT_TYPE = "summa.facet.sort.type";
    public static final String DEFAULT_FACET_SORT_TYPE = SORT_POPULARITY;

    /**
     * The locale used for sorting the Tags in the Facet.
     * </p><p>
     * Note: Specifying a locale lowers performance, so if Unicode-position
     *       sorting is satisfactory, don't specify a locale.
     * </p><p>
     * This property is optional. Default is null.
     */
    public static final String CONF_FACET_LOCALE = "summa.facet.locale";

    /**
     * The structure for a single Facet.
     */
    public class FacetStructure {
        private String name;
        private String[] fields;
        private int maxTags = DEFAULT_TAGS_MAX;
        private int wantedTags = DEFAULT_TAGS_WANTED;
        private String locale = null;
        private String sortType = DEFAULT_FACET_SORT_TYPE;

        public FacetStructure(String name, String[] fields,
                              int wantedTags, int maxTags,
                              String locale, String sortType) {
            log.trace("Creating FacetStructure for '" + name + "'");
            this.name = name;
            setFields(fields);
            setWantedTags(wantedTags);
            this.maxTags = maxTags;
            setLocale(locale);
            setSortType(sortType == null ? this.sortType : sortType);
        }

        public FacetStructure(XProperties props) {
            try {
                name = props.getString(CONF_FACET_NAME);
                if (props.containsKey(CONF_FACET_FIELDS)) {
                    setFields(props.getString(CONF_FACET_FIELDS));
                } else {
                    setFields(name);
                }
                if (props.contains(CONF_FACET_TAGS_MAX)) {
                    maxTags = props.getInteger(CONF_FACET_TAGS_MAX);
                }
                if (props.contains(CONF_FACET_TAGS_WANTED)) {
                    setWantedTags(props.getInteger(CONF_FACET_TAGS_WANTED));
                }
                if (props.containsKey(CONF_FACET_LOCALE)) {
                    setLocale(props.getString(CONF_FACET_LOCALE));
                }
                if (props.containsKey(CONF_FACET_SORT_TYPE)) {
                    setSortType(props.getString(CONF_FACET_SORT_TYPE));
                }

            } catch (Exception e) {
                throw new ConfigurationException(String.format(
                        "Unable to construct FacetStructure for '%s'", name),
                                                 e);
            }
        }


        /**
         * Construct a new FacetStructure from the existing one, optionally
         * overriding wantedTags and sortType.
         * @param wantedTags the number of wanted tags. This is limited by
         *                   {@link #maxTags}. If wantedTags is null, the
         *                   value is ignored.
         * @param sortType   the wanted sort-type. If sort-type is null, the
         *                   value is ignored.
         * @return a clone of this updated with the specified values.
         */
        public FacetStructure getRequestFacet(Integer wantedTags,
                                              String sortType) {
            if (wantedTags == null) {
                wantedTags = this.wantedTags;
            }
            if (sortType == null) {
                sortType = this.sortType;
            }
            return new FacetStructure(name, fields, wantedTags, maxTags,
                                      locale, sortType);
        }

        /* Mutators */

        public String getName() {
            return name;
        }
        public String[] getFields() {
            return fields;
        }
        private void setFields(String[] fields) {
            if (fields == null || fields.length == 0) {
                log.debug("No fields specified, using name '" + name + "'");
                this.fields = new String[]{name};
            } else {
                this.fields = fields;
            }
        }
        private void setFields(String fields) {
            if (fields == null || "".equals(fields)) {
                log.debug("No fields specified, using name '" + name + "'");
                this.fields = new String[]{name};
                return;
            }
            setFields(fields.split(" *, *"));
        }
        public String getLocale() {
            return locale;
        }
        private void setLocale(String locale) {
            // TODO: Implement this
        }
        public int getMaxTags() {
            return maxTags;
        }
        public String getSortType() {
            return sortType;
        }
        private void setSortType(String sortType) {
            if (SORT_ALPHA.equals(sortType)
                || SORT_POPULARITY.equals(sortType)) {
                this.sortType = sortType;
            } else {
                log.warn(String.format(
                        "Invalid sortType '%s' specified for Facet '%s'. "
                        + "Using '%s' instead", sortType, name, this.sortType));
            }
            // TODO: Implement this
        }
        public int getWantedTags() {
            return wantedTags;
        }
        private void setWantedTags(int wantedTags) {
            if (wantedTags > maxTags) {
                log.warn(String.format(
                        "Requested %d wanted tags, but the maximum is %d. "
                        + "Rounding down to maximum", wantedTags, maxTags));
                this.wantedTags = maxTags;
            } else {
                this.wantedTags = wantedTags;
            }
        }

    }

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
        List<XProperties> facetConfs;
        try {
            //noinspection unchecked
            facetConfs = (List<XProperties>)conf.get(CONF_FACETS);
        } catch (ClassCastException e) {
            throw new ConfigurationException(String.format(
                    "Could not extract a list of XProperties from "
                    + "configuration with key '%s'", CONF_FACETS), e);
        }
        facets = new LinkedHashMap<String, FacetStructure>(facetConfs.size());
        for (XProperties facetConf: facetConfs) {
            try {
                FacetStructure fc = new FacetStructure(facetConf);
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
        log.trace("Finished constructing Structure from configuration");
    }

    /* Getters */

    public Map<String, FacetStructure> getFacets() {
        return facets;
    }
}
