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
package dk.statsbiblioteket.summa.support.harmonise.hub;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.ManyToManyMapper;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResult.Reliability;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultImpl;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Adjusts tags in facets according to provided rules. This adjuster operates on Solr responses.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: Extend the tag format so that it handles escapes
// TODO: Implement discarding tags
public class HubTagAdjuster implements Configurable {
    private static Log log = LogFactory.getLog(HubTagAdjuster.class);

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
     * source. The source tag is removed.<br/>
     * {@code n:1} 1 new tag is created where the tag count is the sum of the
     * tag count for the sources.<br/>
     * {@code n:n} n new tag are created where the tag count for each is the sum
     * of the tag count for the sources.<br/>
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
     * Optional. Default is sum. Valid values are min, max and sum.
     */
    public static final String CONF_MERGE_MODE = "tagadjuster.merge.mode";
    public static final MERGE_MODE DEFAULT_MERGE_MODE = MERGE_MODE.sum;

    private final List<String> facetNames;
    private final MERGE_MODE mergeMode;
    private final ManyToManyMapper map;
    private String id; // An id used for timing feedback


    public HubTagAdjuster(Configuration conf) {
        facetNames = conf.getStrings(CONF_FACET_NAME);
        mergeMode = MERGE_MODE.valueOf(conf.getString(CONF_MERGE_MODE, DEFAULT_MERGE_MODE.toString()));
        map = new ManyToManyMapper(conf.getStrings(CONF_TAG_MAP));
        log.info("Created " + this);
    }

    public List<String> getFacetNames() {
        return facetNames;
    }

    // TODO: Fix super ugly FacetResultExternal-requirement

    /**
     * Adjusts the given facet result using the mapping stated in the
     * configuration.
     *
     * @param facetResult a Summa facet result.
     */
    public void adjust(FacetResultExternal facetResult) {
        //       long startTime = System.currentTimeMillis();
        for (String facetName : facetNames) {
            long singleTime = System.currentTimeMillis();
            List<FacetResultImpl.Tag<String>> oldTags = facetResult.getMap().get(facetName);
            if (oldTags == null || oldTags.isEmpty()) {
                continue;
            }
            log.trace("Transforming " + oldTags.size() + " tags for facet " + facetName);
            LinkedHashMap<String, FacetResultImpl.Tag<String>> newTags = new LinkedHashMap<String,
                    FacetResultImpl.Tag<String>>((int) (
                    oldTags.size() * 1.5));
            for (FacetResultImpl.Tag<String> oldTag : oldTags) {
                if (map.getForward().containsKey(oldTag.getKey())) {
                    for (String newName : map.getForward().get(oldTag.getKey())) {
                        mergePut(newTags, newName, oldTag);
                    }
                } else {
                    mergePut(newTags, oldTag.getKey(), oldTag);
                }
            }
            List<FacetResultImpl.Tag<String>> newListTags = new ArrayList<FacetResultImpl.Tag<String>>(newTags.size());
            for (Map.Entry<String, FacetResultImpl.Tag<String>> tag : newTags.entrySet()) {
                newListTags.add(tag.getValue());

            }
            facetResult.getMap().put(facetName, newListTags);
            facetResult.addTiming("tagadjuster." + facetName + ".adjust", System.currentTimeMillis() - singleTime);
        }
/*        facetResult.addTiming(
            getPrefix() + "tagadjuster.adjust.all." + Strings.join(facetNames, "-"),
            System.currentTimeMillis() - startTime);*/
    }

/*    private String getPrefix() {
        return id == null ? "" : id + ".";
    }*/

    /**
     * Performs reverse lookup of the source tags pointing to the given
     * destination tag. Typically used for re-writing queries.
     * If the name is unknown, it is returned directly.
     *
     * @param tagName a destination tag name.
     * @return source tag names pointing to the given name.
     */
    public Set<String> getReverse(String tagName) {
        if (log.isTraceEnabled()) {
            log.trace("getReverse(" + tagName + ") returning " +
                      (map.getReverse().containsKey(tagName) ? map.getReverse().get(tagName) : tagName));
        }
        if (map.getReverse().containsKey(tagName)) {
            return map.getReverse().get(tagName);
        }
        return new HashSet<String>(Arrays.asList(tagName));
    }


    //TODO. The following method can be improve by using Reliability more instead of just using merge mode.
    // ie MORE 4 and LESS 2 -> 4 MORE  (no matter of merge mode)
    private void mergePut(Map<String, FacetResultImpl.Tag<String>> tags, String newName,
                          FacetResultImpl.Tag<String> oldTag) {
        if (!tags.containsKey(newName)) {
            FacetResultImpl.Tag<String> newTag = new FacetResultImpl.Tag<String>(
                    newName, oldTag.getCount(), oldTag.getReliability());
            tags.put(newName, newTag);
            return;
        }

        FacetResultImpl.Tag<String> tag = tags.get(newName);
        Reliability newR = tag.getReliability();
        Reliability oldR = oldTag.getReliability();

        switch (mergeMode) {
            case min:
                tag.setCount(Math.min(tag.getCount(), oldTag.getCount()));
                if (oldR == Reliability.IMPRECISE || newR == Reliability.IMPRECISE) {
                    tag.setReliability(Reliability.IMPRECISE);
                } else {
                    tag.setReliability(Reliability.MORE);
                }
                break;
            case max:
                if (oldR == Reliability.IMPRECISE || newR == Reliability.IMPRECISE) {
                    tag.setReliability(Reliability.IMPRECISE);
                } else {
                    tag.setReliability(Reliability.MORE);
                }

                tag.setCount(Math.max(tag.getCount(), oldTag.getCount()));
                break;
            case sum:
                if (oldR == Reliability.IMPRECISE || newR == Reliability.IMPRECISE) {
                    tag.setReliability(Reliability.IMPRECISE);
                } else {
                    tag.setReliability(Reliability.LESS);
                }

                tag.setCount(tag.getCount() + oldTag.getCount());
                break;
            default:
                throw new UnsupportedOperationException(
                        "Merge mode '" + mergeMode + "' is unknown, unsuspected and unsupported");
        }
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "HubTagAdjuster(id='" + id + "', facetNames=[" + Strings.join(facetNames) + "], mergeMode=" + mergeMode
               + ", tagMap=" + map + ")";
    }
}
