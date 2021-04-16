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
        author = "te",
        comment = "Needs JavaDoc before QA")
public class FacetStructure implements Serializable {
    private static final long serialVersionUID = 719987522941864L;
    private static volatile Logger log = Logger.getLogger(Structure.class);

    /**
     * The returned Tags should be sorted in order with the most popular Tags
     * first. Popularity translates directly to the number of occurences.
     */
    public static final String SORT_POPULARITY = "POPULARITY";
    public static final String SORT_POPULARITY_ASC = "POPULARITY_ASC";
    /**
     * The returned tags should be sorted in the order they are stored in the
     * underlying Tag-structure. This will normally be in (localized) alpha-
     * numeric order. Tags with 0 hits will be skipped.
     */
    public static final String SORT_ALPHA = "ALPHA";
    public static final String SORT_ALPHA_DESC = "ALPHA_DESC";
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
    public static final int DEFAULT_TAGS_MAX = 5000;
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
    private boolean reverse = false;

    public FacetStructure(String name, int id, String[] fields, int wantedTags, int maxTags, String locale,
                          String sortType, boolean reverse) {
        log.trace("Creating FacetStructure for '" + name + "'");
        if (fields == null || fields.length == 0) {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "constructor: No fields specified in facet '%s'", name));
        }
        this.id = id;
        this.name = name;
        setFields(fields);
        this.maxTags = maxTags;
        setWantedTags(wantedTags);
        setLocale(locale);
        setSortType(sortType == null ? this.sortType : sortType);
        setReverse(reverse);
    }

    /**
     * Constructs a structure from a single facet definition, defined as the
     * list of facet definitions from {@link dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys#SEARCH_FACET_FACETS}.
     * @param facetDef a single facet definition.
     * @param defaultWantedTags the number of tags if not defined in the
     *        facetDef.
     * @param id the facetID.
     */
    public FacetStructure(String facetDef, int defaultWantedTags, int id) {
        wantedTags = defaultWantedTags;
        this.id = id;
        // zoo(12 ALPHA)
        String[] subTokens = facetDef.split(" *\\(", 2);
        // zoo
        name = subTokens[0];
        fields = new String[]{name};

        if (subTokens.length == 1) {
            // Just zoo
            return;
        }

        // "  5  ALPHA)  " | "5)" | " ALPHA) | "vgfsd"
        String noParen = subTokens[1].split("\\)", 2)[0].trim();
        // "5  ALPHA" | "5" | "ALPHA" | "vgfsd"
        String[] facetArgs = noParen.split(" +", 2);
        // "5", "ALPHA" | "5" | "ALPHA" | "vgfsd"
        // TODO: Add reverse
        for (String facetArg: facetArgs) {
            switch (facetArg) {
                case FacetStructure.SORT_POPULARITY:
                    sortType = FacetStructure.SORT_POPULARITY;
                    break;
                case FacetStructure.SORT_POPULARITY_ASC:
                    sortType = FacetStructure.SORT_POPULARITY;
                    reverse = true;
                    break;
                case FacetStructure.SORT_ALPHA:
                    sortType = FacetStructure.SORT_ALPHA;
                    break;
                case FacetStructure.SORT_ALPHA_DESC:
                    sortType = FacetStructure.SORT_ALPHA;
                    reverse = true;
                    break;
                default:
                    try {
                        wantedTags = Integer.parseInt(facetArg);
                    } catch (NumberFormatException e) {
                        log.warn(String.format(Locale.ROOT, "Argument '%s' in FacetDef '%s'", facetArg, facetDef));
                    }
                    break;
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("Created " + this);
        }
    }

    /**
     * Constructs a structure from a single facet definition, defined as the
     * list of facet definitions from {@link dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys#SEARCH_FACET_FACETS}.
     * @param facetDef a single facet definition.
     * @param id the facetID.
     */
    public FacetStructure(String facetDef, int id) {
        this(facetDef, DEFAULT_TAGS_WANTED, id);
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
            if (fields == null || fields.length == 0) {
                throw new IllegalArgumentException(String.format(Locale.ROOT,
                        "Configuration-constructor: No fields specified in facet '%s'", name));
            }
            maxTags = conf.getInt(CONF_FACET_TAGS_MAX, maxTags);
            setWantedTags(conf.getInt(CONF_FACET_TAGS_WANTED, wantedTags));
            if (conf.valueExists(CONF_FACET_LOCALE)) {
                setLocale(conf.getString(CONF_FACET_LOCALE));
            }
            if (conf.valueExists(CONF_FACET_SORT_TYPE)) {
                setSortType(conf.getString(CONF_FACET_SORT_TYPE));
            }
        } catch (Exception e) {
            throw new Configurable.ConfigurationException(String.format(Locale.ROOT,
                    "Unable to construct FacetStructure for '%s'", name), e);
        }
    }


    /**
     * Construct a new FacetStructure from the existing one, optionally overriding wantedTags and sortType.
     * @param wantedTags The number of wanted tags. This is limited by {@link #maxTags}. If wantedTags is null, the
     *                   value is ignored.
     * @param sortType   The wanted sort-type. If sort-type is null, the value is ignored.
     * @return A clone of this updated with the specified values.
     */
    public FacetStructure getRequestFacet(Integer wantedTags, String sortType, Boolean reverse) {
        if (wantedTags == null) {
            wantedTags = this.wantedTags;
        }
        if (sortType == null) {
            sortType = this.sortType;
        }
        if (reverse == null) {
            reverse = this.reverse;
        }
        return new FacetStructure(name, id, fields, wantedTags, maxTags, locale, sortType, reverse);
    }

    /* Mutators and Accessors*/

    public String getName() {
        return name;
    }
    public String[] getFields() {
        return fields;
    }
    public static final String NO_FIELDS_SPECIFIED = "No fields specified, using name '";
    private void setFields(String[] fields) {
        if (fields == null || fields.length == 0) {
            log.debug(NO_FIELDS_SPECIFIED + name + "'");
            this.fields = new String[]{name};
        } else {
            this.fields = fields;
        }
    }
    private void setFields(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
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
            log.warn(String.format(Locale.ROOT, "Invalid locale '%s' specified for FacetStructure", locale));
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
        switch (sortType) {
            case SORT_ALPHA:
            case SORT_POPULARITY:
                this.sortType = sortType;
                break;
            case SORT_ALPHA_DESC:
                this.sortType = SORT_ALPHA_DESC;
                this.reverse = true;
                break;
            case SORT_POPULARITY_ASC:
                this.sortType = SORT_POPULARITY_ASC;
                this.reverse = true;
                break;
            default:
                log.warn(String.format(Locale.ROOT, "Invalid sortType '%s' specified for Facet '%s'. Using '%s' instead",
                                       sortType, name, this.sortType));
                break;
        }
        // TODO: Implement this
    }
    public int getWantedTags() {
        return wantedTags;
    }
    private void setWantedTags(int wantedTags) {
        if (wantedTags > maxTags) {
            log.warn(String.format(Locale.ROOT, "Requested %d wanted tags, but the maximum is %d. Rounding down to maximum",
                                   wantedTags, maxTags));
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

    /**
     * Pretty prints the facet structure. The string includes id, name, fields,
     * tags, sortType, and the locale.
     * @return String containing a detailed print of the facet structure. This
     * include id, name, fields, tags, sortType, and the locale.
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT,
                "FacetStructure(id=%s, name='%s', fields='%s', defaultTags=%s, maxTags=%d, sortType=%s, "
                + "sortLocale='%s', reverse=%b)",
                id, name, Strings.join(fields, ", "), wantedTags, maxTags, sortType, locale, reverse);
    }

    /**
     * Absorb secondary characteristics from other. Secondary attributes are
     * all attributes except name, fields and sortLocale.
     * @param other the FacetStructure to absorb characteristics from.
     */
    public void absorb(FacetStructure other) {
        this.setSortType(other.getSortType());
        this.setReverse(other.isReverse());
        this.setWantedTags(other.getWantedTags());
        this.setMaxTags(other.getMaxTags());
    }

    /**
     * @return true if the facet should be sorted in reverse.
     */
    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }
}