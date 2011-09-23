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
package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.FlexiblePair;
import dk.statsbiblioteket.summa.common.util.ManyToManyMap;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.*;

/**
 * Adjusts tags in facets according to provided rules.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: Extend the tag format so that it handles escapes
// TODO: Implement discarding tags
public class TagAdjuster implements Configurable {
    private static Log log = LogFactory.getLog(TagAdjuster.class);

    /**
     * The name of the facets to adjust tags for. Specify with a list of facet
     * names delimited by comma or a plain String list.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_FACET_NAME = "tagadjuster.facet.name";

    /**
     * The map of tag transformations. The format supports many-to-many 
     * transformations the following way where the number to the left of
     * the colon is the number of source tagnames and the number to the
     * right is the number of destination tag names:<br/>
     * {@code 1:1} Direct rename of the tag.<br/>
     * {@code 1:n} n new tags are created, each with the tag count from the
     *             source. The source tag is removed.<br/>
     * {@code n:1} 1 new tag is created where the tag count is the sum of the
     *             tag count for the sources.<br/>
     * {@code n:n} n new tag are created where the tag count for each is the sum 
     *             of the tag count for the sources.<br/>
     * If a tag is renamed to an existing tag, their tag counts are merged.
     * Rules such as {@code a-b, b-a} are handled gracefully.
     * </p><p>
     * The format of the map is<br/>
     * map: rule|rule,map<br/>
     * rule: source-destination<br/>
     * source: tagname|tagname;source<br/>
     * destination: tagname|tagname;destination<br/>
     * Blanks are trimmed from the start and the end of tagnames.
     * </p><p>
     * Example: {@code eng - english, dan;ger;hun - misc}.  
     * </p><p>
     * Mandatory. 
     */
    public static final String CONF_TAG_MAP = "tagadjuster.tags";

    /**
     * How to merge the counts for the tags when multiple source tags are mapped
     * to one destination tag (and by implication n:n).
     */
    public static enum MERGE_MODE {min, max, sum}

    /**
     * The way to merge counts for tags when more than one source tag maps to a
     * destination tag.
     * </p><p>
     * Optional. Default is max. Valid values are min, max and sum.
     */
    public static final String CONF_MERGE_MODE = "tagadjuster.merge.mode";
    public static final MERGE_MODE DEFAULT_MERGE_MODE = MERGE_MODE.max;

    private final List<String> facetNames;
    private final MERGE_MODE mergeMode;
    private final ManyToManyMap map;
    private String id; // An id used for timing feedback


    public TagAdjuster(Configuration conf) {
        facetNames = conf.getStrings(CONF_FACET_NAME);
        mergeMode = MERGE_MODE.valueOf(conf.getString(
            CONF_MERGE_MODE, DEFAULT_MERGE_MODE.toString()));
        map = new ManyToManyMap(conf.getStrings(CONF_TAG_MAP));
        log.info("Created TagAdjuster '" + facetNames + "' with " + map.size()
                 + " source->destination rules and " + map.size()
                 + " destination->source rules");
    }

    public List<String> getFacetNames() {
        return facetNames;
    }

    // TODO: Fix super ugly FacetResultExternal-requirement

    /**
     * Adjusts the given facet result using the mapping stated in the
     * configuration.
     * @param facetResult a Summa facet result.
     */
    public void adjust(FacetResultExternal facetResult) {
 //       long startTime = System.currentTimeMillis();
        for (String facetName: facetNames) {
            long singleTime = System.currentTimeMillis();
            List<FlexiblePair<String, Integer>> oldTags =
                facetResult.getMap().get(facetName);
            if (oldTags == null || oldTags.size() == 0) {
                continue;
            }
            // A bit of a hack as we are not guaranteed that the orders are the same
            FlexiblePair.SortType sortType = oldTags.get(0).getSortType();
            log.trace("Transforming " + oldTags.size() + " tags for facet "
                      + facetName);
            LinkedHashMap<String, Integer> newTags =
                new LinkedHashMap<String, Integer>((int) (oldTags.size() * 1.5));
            for (FlexiblePair<String, Integer> pair: oldTags) {
                if (map.containsKey(pair.getKey())) {
                    for (String tagName: map.get(pair.getKey())) {
                        mergePut(newTags, tagName, pair.getValue());
                    }
                } else {
                    mergePut(newTags, pair.getKey(), pair.getValue());
                }
            }
            List<FlexiblePair<String, Integer>> newListTags =
                new ArrayList<FlexiblePair<String, Integer>>(newTags.size());
            for (Map.Entry<String, Integer> tag: newTags.entrySet()) {
                newListTags.add(new FlexiblePair<String, Integer>(
                    tag.getKey(), tag.getValue(), sortType));
            }
            facetResult.getMap().put(facetName, newListTags);
            facetResult.addTiming(
                getPrefix() + "tagadjuster." + facetName + ".adjust",
                System.currentTimeMillis() - singleTime);
        }
/*        facetResult.addTiming(
            getPrefix() + "tagadjuster.adjust.all." + Strings.join(facetNames, "-"),
            System.currentTimeMillis() - startTime);*/
    }

    private String getPrefix() {
        return id == null ? "" : id + ".";
    }

    /**
     * Performs reverse lookup of the source tags pointing to the given
     * destination tag. Typically used for re-writing queries.
     * If the name is unknown, it is returned directly.
     * @param tagName a destination tag name.
     * @return source tag names pointing to the given name.
     */
    public String[] getReverse(String tagName) {
        if (log.isTraceEnabled()) {
            log.trace("getReverse(" + tagName + ") returning " +
                      (map.reverseContainsKey(tagName) ?
                       map.reverseGet(tagName) : tagName));
        }
        return map.reverseContainsKey(tagName) ?
               map.reverseGet(tagName) :
               new String[]{tagName};
    }

    private void mergePut(
        Map<String, Integer> tags, String key, Integer value) {
        if (tags.containsKey(key)) {
            switch (mergeMode) {
                case min:
                    tags.put(key, Math.min(value, tags.get(key)));
                    break;
                case max:
                    tags.put(key, Math.max(value, tags.get(key)));
                    break;
                case sum:
                    tags.put(key, value + tags.get(key));
                    break;
                default: throw new UnsupportedOperationException(
                    "Merge mode '" + mergeMode + "' is unknown, unsuspected "
                    + "and unsupported");
            }
        } else {
            tags.put(key, value);
        }
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
