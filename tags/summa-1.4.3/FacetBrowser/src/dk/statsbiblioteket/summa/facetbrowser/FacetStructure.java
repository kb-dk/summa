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
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

/**
 * The structure for a single Facet.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class FacetStructure implements Serializable {
    private static volatile Logger log = Logger.getLogger(Structure.class);

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
    /**
     * The name of the Facet.
     * </p><p>
     * This property is mandatory.
     */
    public static final String CONF_FACET_NAME = "summa.facet.name";
    /**
     * The fields that make up the Facet. Stored as a list of Strings.
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

    private String name;
    private String[] fields;
    private int maxTags = DEFAULT_TAGS_MAX;
    private int wantedTags = DEFAULT_TAGS_WANTED;
    private String locale = null;
    private String sortType = DEFAULT_FACET_SORT_TYPE;
    private Integer id = null;

    public FacetStructure(String name, int id, String[] fields,
                          int wantedTags, int maxTags,
                          String locale, String sortType) {
        log.trace("Creating FacetStructure for '" + name + "'");
        this.id = id;
        this.name = name;
        setFields(fields);
        setWantedTags(wantedTags);
        this.maxTags = maxTags;
        setLocale(locale);
        setSortType(sortType == null ? this.sortType : sortType);
    }

    public FacetStructure(Configuration conf, int id) {
        this.id = id;
        try {
            name = conf.getString(CONF_FACET_NAME);
            if (conf.valueExists(CONF_FACET_FIELDS)) {
                setFields(conf.getStrings(CONF_FACET_FIELDS));
            } else {
                setFields(name);
            }
            maxTags = conf.getInt(CONF_FACET_TAGS_MAX, maxTags);
            wantedTags = conf.getInt(CONF_FACET_TAGS_WANTED, wantedTags);
            if (conf.valueExists(CONF_FACET_LOCALE)) {
                setLocale(conf.getString(CONF_FACET_LOCALE));
            }
            if (conf.valueExists(CONF_FACET_SORT_TYPE)) {
                setSortType(conf.getString(CONF_FACET_SORT_TYPE));
            }
        } catch (Exception e) {
            throw new Configurable.ConfigurationException(String.format(
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
        return new FacetStructure(name, id, fields, wantedTags, maxTags,
                                  locale, sortType);
    }

    /* Mutators */

    public String getName() {
        return name;
    }
    public String[] getFields() {
        return fields;
    }
    public static final String NO_FIELDS_SPECIFIED =
            "No fields specified, using name '";
    private void setFields(String[] fields) {
        if (fields == null || fields.length == 0) {
            log.debug(NO_FIELDS_SPECIFIED + name + "'");
            this.fields = new String[]{name};
        } else {
            this.fields = fields;
        }
    }
    private void setFields(List<String> fields) {
        if (fields == null || fields.size() == 0) {
            log.debug(NO_FIELDS_SPECIFIED + name + "'");
            this.fields = new String[]{name};
        } else {
            this.fields = fields.toArray(new String[fields.size()]);
        }
    }
    private void setFields(String fields) {
        if (fields == null || "".equals(fields)) {
            log.debug(NO_FIELDS_SPECIFIED + name + "'");
            this.fields = new String[]{name};
            return;
        }
        setFields(fields.split(" *, *"));
    }
    public String getLocale() {
        return locale;
    }
    private void setLocale(String locale) {
        if (locale == null) {
            this.locale = locale;
            return;
        }
        try {
            new Locale(locale);
            this.locale = locale;
        } catch (Exception e) {
            log.warn(String.format("Invalid locale '%s' specified for "
                                   + "FacetStructure", locale));
            //noinspection AssignmentToNull
            this.locale = null;
        }
    }
    public int getMaxTags() {
        return maxTags;
    }

    public void setMaxTags(int maxTags) {
        this.maxTags = maxTags;
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

    /**
     * @return the id of the Facet is also its position relative to other
     *         FacetStructures, with regard to presentation and sorting.
     *         The id will be 0 or more and is unique among FacetStructures.
     */
    public Integer getFacetID() {
        return id;
    }

    @Override
    public String toString() {
        return String.format(
                "FacetStructure(id=%s, name='%s', fields='%s', defaultTags=%s, "
                + "maxTags=%d, sortType=%s, sortLocale='%s')",
                id, name, Strings.join(fields, ", "), wantedTags, maxTags,
                sortType, locale);
    }

    /**
     * Absorb secondary characteristica from other. Secondary attributes are
     * all attributes except name, fields and sortLocale.
     * @param other the FacetStructure to absorb characteristica from.
     */
    public void absorb(FacetStructure other) {
        this.setSortType(other.getSortType());
        this.setWantedTags(other.getWantedTags());
        this.setMaxTags(other.getMaxTags());
    }
}
