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
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.*;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: Extend the tag format so that it handles escapes
// TODO: Implement discarding tags
public class TagAdjuster implements Configurable {
    private static Log log = LogFactory.getLog(TagAdjuster.class);

    /**
     * The name of the facet to adjust tags for.
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
     * {@code 1:n} n new tags are created, each the tag count from the source.
     *             The source tag is removed.<br/>
     * {@code n:1} 1 new tag are created where the tag count is the sum of the
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

    private final String facetName;
    private final Map<String, String[]> map;

    public TagAdjuster(Configuration conf) {
        facetName = conf.getString(CONF_FACET_NAME);
        String[] rules = conf.getString(CONF_TAG_MAP).split(" *, *");
        map = new HashMap<String, String[]>((int)(rules.length * 1.5));
        for (String rule: rules) {
            String[] parts = rule.split(" *- *");
            if (parts.length != 2) {
                throw new ConfigurationException(
                    "Expected two parts by splitting '" + rule
                    + "' with delimiter '-' but got " + parts.length);
            }
            String[] sources = parts[0].split(" *; *");
            String[] destinations = parts[1].split(" *; *");
            for (String source: sources) {
                map.put(source, destinations);
            }
        }
    }

    public String getFacetName() {
        return facetName;
    }

    // TODO: Fix super ugly FacetResultExternal-requirement
    public void adjust(FacetResultExternal facetResult) {
        if (!facetResult.getMap().containsKey(facetName)) {
            return;
        }
        List<FlexiblePair<String, Integer>> oldTags =
            facetResult.getMap().get(facetName);
        if (oldTags.size() == 0) {
            return;
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
                    additivePut(newTags, tagName, pair.getValue());
                }
            } else {
                additivePut(newTags, pair.getKey(), pair.getValue());
            }
        }
        List<FlexiblePair<String, Integer>> newListTags =
            new ArrayList<FlexiblePair<String, Integer>>(newTags.size());
        for (Map.Entry<String, Integer> tag: newTags.entrySet()) {
            newListTags.add(new FlexiblePair<String, Integer>(
                tag.getKey(), tag.getValue(), sortType));
        }
        facetResult.getMap().put(facetName, newListTags);
    }

    private void additivePut(
        Map<String, Integer> tags, String key, Integer value) {
        if (tags.containsKey(key)) {
            tags.put(key, value + tags.get(key));
        } else {
            tags.put(key, value);
        }
    }
}
