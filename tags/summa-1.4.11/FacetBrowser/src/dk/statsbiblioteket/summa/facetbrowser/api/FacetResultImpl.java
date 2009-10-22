/* $Id: FacetResultImpl.java,v 1.10 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.10 $
 * $Date: 2007/10/05 10:20:22 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: FacetResultImpl.java,v 1.10 2007/10/05 10:20:22 te Exp $
 */
/* $Id: FacetResultImpl.java,v 1.10 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.10 $
 * $Date: 2007/10/05 10:20:22 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: FacetResultImpl.java,v 1.10 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.api;

import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.common.util.FlexiblePair;
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.StringWriter;
import java.util.*;

/**
 * Base implementation of a facet structure, where the tags are generic.
 * Resolving tags to queryes and representations are delegated to implementing
 * classes. The same goes for sort-order of the tags.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class FacetResultImpl<T extends Comparable<T>>
        implements FacetResult<T> {
    private static final transient Log log =
            LogFactory.getLog(FacetResultImpl.class);

    private int DEFAULTFACETCAPACITY = 20;
    private static final int DEFAULT_MAXTAGS = 100;

    /**
     * Pseudo-code: Map<FacetName, FlexiblePair<Tag, TagCount>>.
     * We use a linked map so that the order of the Facets will be
     * significant.
     */
    protected LinkedHashMap<String, List<FlexiblePair<T, Integer>>> map;
    protected HashMap<String, Integer> maxTags;
    protected HashMap<String, Integer> facetIDs;

    /**
     * @param maxTags  a map from Facet-name to max tags for the facet.
     * @param facetIDs a map from Facet-name to facetID.
     */
    public FacetResultImpl(HashMap<String, Integer> maxTags,
                           HashMap<String, Integer> facetIDs) {
        map = new LinkedHashMap<String, List<FlexiblePair<T, Integer>>>(
                DEFAULTFACETCAPACITY);
        this.maxTags = maxTags;
        this.facetIDs = facetIDs;
    }

    /**
     * It is advisable to call reduce before calling toXML, to ensure that
     * all elements are trimmed and sorted.
     * @return an XML representation of the facet browser structure.
     */
    public synchronized String toXML() {
        log.trace("Entering toXML");
        StringWriter sw = new StringWriter(10000);

        sw.write("<facetmodel>\n");
        for (Map.Entry<String, List<FlexiblePair<T, Integer>>> facet:
                map.entrySet()) {
            if (facet.getValue().size() > 0) {
                sw.write("  <facet name=\"");
                sw.write(urlEntityEscape(facet.getKey()));
                // TODO: Preserve scoring
                sw.write("\">\n");

    //            sw.write(facet.getCustomString());
                int tagCount = 0;

                Integer maxTags = this.maxTags.get(facet.getKey());
                if (maxTags == null) {
                    maxTags = DEFAULT_MAXTAGS;
                }
//                        structure.getFacets().get(facet.getKey()).getMaxTags();
                for (FlexiblePair<T, Integer> tag: facet.getValue()) {
                    if (tagCount++ < maxTags) {
                        sw.write("    <tag name=\"");
                        sw.write(urlEntityEscape(getTagString(facet.getKey(),
                                                              tag.getKey())));
        /*                if (!Element.NOSCORE.equals(tag.getScore())) {
                            sw.write("\" score=\"");
                            sw.write(Float.toString(tag.getScore()));
                        }*/
                        sw.write("\" addedobjects=\"");
                        sw.write(Integer.toString(tag.getValue()));
                        sw.write("\">\n");
                        //noinspection DuplicateStringLiteralInspection
                        sw.write("    <query>"
                                 + getQueryString(facet.getKey(), tag.getKey())
                                 + "</query>\n");
        /*                for (T object: tag.getObjects()) {
                            sw.write("      <object>");
                            sw.write(object.toString());
                            sw.write("</object>\n");
                        }*/
                        sw.write("    </tag>\n");
                    }
                }
                sw.write("  </facet>\n");
            } else {
                log.trace("Skipped \"" + facet.getKey() + "\" as it did not "
                          + "contain any tags");
            }
        }
        sw.write("</facetmodel>\n");
        return sw.toString();
    }

    public synchronized void reduce(TagSortOrder tagSortOrder) {
        LinkedHashMap<String, List<FlexiblePair<T, Integer>>> newMap =
                new LinkedHashMap<String,
                        List<FlexiblePair<T, Integer>>>(map.size());
        for (Map.Entry<String, List<FlexiblePair<T, Integer>>> entry:
                map.entrySet()) {
            Integer maxTags = this.maxTags.get(entry.getKey());
            if (maxTags == null) {
                maxTags = DEFAULT_MAXTAGS;
            }
//                    structure.getFacets().get(entry.getKey()).getMaxTags();
            if (entry.getValue().size() <= maxTags) {
                newMap.put(entry.getKey(), entry.getValue());
            } else {
                newMap.put(entry.getKey(),
                           new ArrayList<FlexiblePair<T, Integer>>(
                                   entry.getValue().subList(0, maxTags)));
            }
        }
        map = newMap;
        sort(tagSortOrder);
    }

    /**
     * Used by the default merge.
     * @return the internal map.
     */
    protected Map<String, List<FlexiblePair<T, Integer>>> getMap() {
        return map;
    }

    public void merge(Response otherResponse) throws ClassCastException {
        if (!(otherResponse instanceof FacetResult)) {
            throw new ClassCastException(String.format(
                    "Expected a FacetResult, but go '%s'",
                    otherResponse.getClass().getName()));
        }
        FacetResult other = (FacetResult)otherResponse;
        if (other == null) {
            log.warn("Attempted to merge with null");
        }
        String typeProblem = "The FacetResultImpl<T> default merger can only"
                             + " handle FacetResultImpl<T> as input";
        if (!(other instanceof FacetResultImpl)) {
            throw new IllegalArgumentException(typeProblem);
        }
        Map<String, List<FlexiblePair<T, Integer>>> otherMap;
        try  {
            //noinspection unchecked
            otherMap = ((FacetResultImpl<T>)other).getMap();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(typeProblem, e);
        }

        // other is source and this is destination
        //noinspection unchecked
        mergeMaxTags((FacetResultImpl<T>)other);
        for (Map.Entry<String, List<FlexiblePair<T, Integer>>> sEntry:
                otherMap.entrySet()) {
            List<FlexiblePair<T, Integer>> dList = map.get(sEntry.getKey());
            if (dList == null) { // Just add the taglist
                map.put(sEntry.getKey(), sEntry.getValue());
            } else { // Merge the tags
                List<FlexiblePair<T, Integer>> sList = sEntry.getValue();
                for (FlexiblePair<T, Integer> sPair: sList) {
                    boolean found = false;
                    for (FlexiblePair<T, Integer> dPair: dList) {
                        if (sPair.getKey().equals(dPair.getKey())) { // Merge
                            dPair.setValue(sPair.getValue() + dPair.getValue());
                            found = true;
                            break;
                        }
                    }
                    if (!found) { // Add non-existing
                        dList.add(sPair);
                    }
                }
            }
        }
    }

    private void mergeMaxTags(FacetResultImpl<T> otherResult) {
        log.trace("Merging maxTags");
        for (Map.Entry<String, Integer> maxTag: otherResult.maxTags.entrySet()){
            if (!maxTags.containsKey(maxTag.getKey())) {
                maxTags.put(maxTag.getKey(), maxTag.getValue());
            }
        }
    }

    /**
     * Shortcut for sortTags and sortFacets methods.
     * @param tagSortOrder the sort order for the tags.
     */
    protected void sort(TagSortOrder tagSortOrder) {
        sortTags(tagSortOrder);
        sortFacets();
    }

    protected void sortTags(TagSortOrder tagSortOrder) {
        for (Map.Entry<String, List<FlexiblePair<T, Integer>>> facet:
                map.entrySet()) {
            switch (tagSortOrder) {
                case tag:
                    Collections.sort(facet.getValue(),
                        new Comparator<FlexiblePair<T, Integer>>() {
                            public int compare(FlexiblePair<T, Integer> o1,
                                               FlexiblePair<T, Integer> o2) {
                                return compareTags(o1, o2);
                            }
                        });
                    break;
                case popularity:
                    Collections.sort(facet.getValue(),
                        new Comparator<FlexiblePair<T, Integer>>() {
                            public int compare(FlexiblePair<T, Integer> o1,
                                               FlexiblePair<T, Integer> o2) {
                                return o1.getValue().compareTo(o2.getValue());
                            }
                        });
                    break;
                default:
                    log.error("Unknown tag sort order in sortTags: " +
                              tagSortOrder);
            }
        }
    }

    protected void sortFacets() {
        //final Structure s2 = structure;
        // construct list
        List<Pair<String, List<FlexiblePair<T, Integer>>>> ordered =
                new ArrayList<Pair<String, List<FlexiblePair<T, Integer>>>>(
                        map.size());
        for (Map.Entry<String, List<FlexiblePair<T, Integer>>> facet:
                map.entrySet()) {
            ordered.add(new Pair<String, List<FlexiblePair<T, Integer>>>(
                    facet.getKey(), facet.getValue()));
        }
        // Sort it
        Collections.sort(ordered,
            new Comparator<Pair<String, List<FlexiblePair<T, Integer>>>>() {
                public int compare(Pair<String,
                                        List<FlexiblePair<T, Integer>>> o1,
                                   Pair<String,
                                        List<FlexiblePair<T, Integer>>> o2) {
               // TODO: This way of sorting does not work for different sources
                    Integer score1 = facetIDs.get(o1.getKey());
//                            s2.getFacet(o1.getKey()).getFacetID();
                    Integer score2 = facetIDs.get(o2.getKey());
//                            s2.getFacet(o2.getKey()).getFacetID();
                    if (score1 != null && score2 != null) {
                        return score1.compareTo(score2);
                    } else if (score1 != null) {
                        return -1;
                    } else if (score2 != null) {
                        return 1;
                    } else {
                        return o1.getKey().compareTo(o2.getKey());
                    }
                }
            });
    }

    /**
     * This should be overridet when subclassing, if the tags order is not
     * natural.
     * @param o1 the first object to compare.
     * @param o2 the second object to compare.
     * @return -1, 0 or 1, depending on order.
     */
    protected int compareTags(FlexiblePair<T, Integer> o1,
                              FlexiblePair<T, Integer> o2) {
        return o1.compareTo(o2);
    }

    /**
     * This should be overridden when subclassing, if the tags does not resolve
     * naturally to Strings.
     * @param facet the facet that contains the tag.
     * @param tag the tag to convert to String.
     * @return a String-representation of the Tag.
     */
    protected String getTagString(String facet, T tag) {
        log.trace("Default-implementation of getTagString called with Tag " 
                  + tag);
        return urlEntityEscape(String.valueOf(tag));
    }
    /**
     * This should be overridet when subclassing, if the tags does not resolve
     * naturally to queries.
     * @param facet the facet that contains the tag.
     * @param tag the tag to convert to a query.
     * @return a query for the tag in the facet.
     */
    protected String getQueryString(String facet, T tag) {
        return facet + ":\"" +
               urlEntityEscape(String.valueOf(tag)) + "\"";
    }

    /**
     * Assign the list of tags to the given Facet. Any existing Facet will be
     * overwritten.
     * @param facet the Facet to assign to.
     * @param tags a list of pars, where the first element is the tag and the
     *             second element is the tag-count.
     */
    public void assignTags(String facet, List<FlexiblePair<T, Integer>> tags) {
        map.put(facet, tags);
    }

    /**
     * Adds the list of tags to the given Facet. If the Facet does not exist, it
     * will be created. Each Tag will be added to the tags for the Facet.
     * If a Tag already exists in the list, the tagCounts will be added for
     * that Tag. This requires iteration throgh the tags, so consider using
     * {@link #assignTags} if the uniqueness of the Tags is known.<br />
     * Note that the SortOrder for the FlexiblePairs may be reset by this
     * method.
     * @param facet the Facet to assign to.
     * @param tags a list of pars, where the first element is the tag and the
     *             second element is the tag-count.
     */
    public void addTags(String facet, List<FlexiblePair<T, Integer>> tags) {
        if (map.containsKey(facet)) {
            for (FlexiblePair<T, Integer> tag: tags) {
                addTag(facet, tag.getKey(), tag.getValue());
            }
        } else {
            assignTags(facet, tags);
        }
    }

    /**
     * Add a given Tag to a given Facet. If the Tag already exists, the tagCount
     * is added to the existing tagCount. Note that this iterates through all
     * Tags in the given Facet, thus being somewhat inefficient. Consider using
     * {@link #assignTag} if the Tag is known to be unique within the Facet.
     * @param facet    the Facet to add the Tag to. If it does not exist, a new
     *                 Facet is created.
     * @param tag      the Tag to add to the Facet.
     * @param tagCount the tagCount for the Tag.
     */
    public void addTag(String facet, T tag, int tagCount) {
        List<FlexiblePair<T, Integer>> tags = map.get(facet);
        if (tags == null) {
            tags = new ArrayList<FlexiblePair<T, Integer>>(
                    DEFAULTFACETCAPACITY);
            map.put(facet, tags);
        }
        for (FlexiblePair<T, Integer> tPair: tags) {
            if (tPair.getValue().equals(tag)) {
                tPair.setValue(tPair.getValue() + tagCount);
                return;
            }
        }
        // TODO: Remove SortType?
        tags.add(new FlexiblePair<T, Integer>(tag, tagCount,
                                   FlexiblePair.SortType.SECONDARY_DESCENDING));
    }

    /**
     * Assigns a given Tag to a given Facet. There is no checking for duplicate
     * Tags, so ensuring consistency is up to the caller. Consider using
     * {@link #addTag} if it is unknown whether the tag is unique.
     * @param facet    the Facet to assign the Tag to. If it does not exist,
     *                 a new Facet is created.
     * @param tag      the Tag to assign to the Facet.
     * @param tagCount the tagCount for the Tag.
     */
    public void assignTag(String facet, T tag, int tagCount) {
        List<FlexiblePair<T, Integer>> tags = map.get(facet);
        if (tags == null) {
            tags = new ArrayList<FlexiblePair<T, Integer>>(
                    DEFAULTFACETCAPACITY);
            map.put(facet, tags);
        }
        // TODO: Remove SortType?
        tags.add(new FlexiblePair<T, Integer>(tag, tagCount,
                                   FlexiblePair.SortType.SECONDARY_DESCENDING));
    }

    public static String urlEntityEscape(String text) {
        return text.replaceAll("&",  "&amp;").
                    replaceAll("<",  "&lt;").
                    replaceAll(">",  "&gt;").
                    replaceAll("#",  "%23"). // Escaping for URL
                    replaceAll("\"", "&quot;");
    }

    /**
     * Constructs a list of the Tags under the given Facet.
     * @param facet the facet to get Tags for.
     * @return all the Tags under the given Facet or null if the Facet does not
     *         exist.
     */
    public List<String> getTags(String facet) {
        if (!map.containsKey(facet)) {
            log.debug("getTags(" + facet + "): Could not locate facet");
            return null;
        }
        List<String> result = new ArrayList<String>(map.get(facet).size());
        for (FlexiblePair<T, Integer> pair: map.get(facet)) {
            result.add(getTagString(facet, pair.getKey()));
        }
        return result;
    }

    /**
     * Escape the tag for use in a query. Currently this means placing a
     * backslash in front of quotes.
     * @param cleanTag the tag to escape.
     * @return the escaped tag.
     */
    protected String queryEscapeTag(String cleanTag) {
        return cleanTag.replace("\"", "\\\"");
    }
}
