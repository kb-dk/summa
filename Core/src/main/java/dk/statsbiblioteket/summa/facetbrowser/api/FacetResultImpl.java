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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: FacetResultImpl.java,v 1.10 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.api;

import dk.statsbiblioteket.summa.common.util.CollatorFactory;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.common.util.FlexiblePair;
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.search.api.ResponseImpl;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.io.StringWriter;
import com.ibm.icu.text.Collator;
import java.util.*;

/**
 * Base implementation of a facet structure, where the tags are generic.
 * Resolving tags to queries and representations are delegated to implementing
 * classes. The same goes for sort-order of the tags.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class FacetResultImpl<T extends Comparable<T>>
    extends ResponseImpl implements FacetResult<T> {
    private static final transient Log log =
            LogFactory.getLog(FacetResultImpl.class);
    private static final long serialVersionUID = 7879716850L;

    private int DEFAULTFACETCAPACITY = 64;
    private static final int DEFAULT_MAXTAGS = 100;

    /**
     * If false, tags containing the empty String will not be part of the
     * generated XML. Note that this might result in a reduction of the
     * number of returned tags by 1.
     * </p><p>
     * Default: false.
     */
    public boolean emptyTagsValid = false;

    /**
     * Pseudo-code: Map<FacetName, FlexiblePair<Tag, TagCount>>.
     * We use a linked map so that the order of the Facets will be
     * significant.
     */
    protected LinkedHashMap<String, List<Tag<T>>> map;
    protected HashMap<String, Integer> maxTags;
    protected HashMap<String, Integer> facetIDs;

  
   public static class Tag<S> implements Serializable {
		private S key;
    	private int count;
    	private Reliability reliability;     
    	private static final long serialVersionUID = 101L;    	 
    	
    	public Tag(S key, int count, Reliability reliability) {
			this.key = key;
			this.count = count;
			this.reliability = reliability;
		}
    	
            
		public S getKey() {
			return key;
		}
		public void setKey(S key) {
			this.key = key;
		}
		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
		public Reliability getReliability() {
			return reliability;
		}
		public void setReliability(Reliability reliability) {
			this.reliability = reliability;
		}
		
		
    }
    
    /**
     * @param maxTags  a map from Facet-name to max tags for the facet.
     * @param facetIDs a map from Facet-name to facetID.
     */
    public FacetResultImpl(HashMap<String, Integer> maxTags,
                           HashMap<String, Integer> facetIDs) {
        map = new LinkedHashMap<String, List<Tag<T>>>(DEFAULTFACETCAPACITY);
        this.maxTags = maxTags;
        this.facetIDs = facetIDs;
    }

    /**
     * It is advisable to call reduce before calling toXML, to ensure that
     * all elements are trimmed and sorted.
     * @return an XML representation of the facet browser structure.
     */
    @Override
    // TODO: Switch to XMLOutputStream
    public synchronized String toXML() {
        log.trace("Entering toXML");
        StringWriter sw = new StringWriter(10000);

        sw.write("<facetmodel timing=\"");
        sw.write(XMLUtil.encode(getTiming()));
        sw.write("\">\n");
        for (Map.Entry<String, List<Tag<T>>> facet: map.entrySet()) {
            if (facet.getValue().size() > 0) {
                sw.write("  <facet name=\"");
                sw.write(XMLUtil.encode(facet.getKey()));
                // TODO: Preserve scoring
                sw.write("\">\n");

    //            sw.write(facet.getCustomString());
                int tagCount = 0;

                Integer maxTags = this.maxTags.get(facet.getKey());
                if (maxTags == null) {
                    maxTags = DEFAULT_MAXTAGS;
                }
//                        structure.getFacets().get(facet.getKey()).getMaxTags();
                for (Tag<T> tag: facet.getValue()) {
                    String tagString = getTagString(
                        facet.getKey(), tag.getKey());
                    if (!emptyTagsValid && "".equals(tagString)) {
                        log.trace("Skipping empty tag from " + facet.getKey()
                                  + " with tag count " + tag.getCount());

                        continue;
                    }
                    if (tagCount++ < maxTags) {
                        sw.write("    <tag name=\"");
                        sw.write(XMLUtil.encode(tagString));
        /*                if (!Element.NOSCORE.equals(tag.getScore())) {
                            sw.write("\" score=\"");
                            sw.write(Float.toString(tag.getScore()));
                        }*/
                        sw.write("\"" +
                        		" addedobjects=\"");
                        sw.write(Integer.toString(tag.getCount()));
                        sw.write("\"");
                        sw.write(" reliability=\"");
                        sw.write(tag.getReliability().toString());
                        sw.write("\"");                                                
                        sw.write(">\n");
                        
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

    /**
     * Reduces the number of tags in the facets, as per the given request
     * structure.
     * @param request a request structure describing the facet setup.
     */
    public synchronized void reduce(Structure request) {
        LinkedHashMap<String, List<Tag<T>>> newMap =
                new LinkedHashMap<String,
                        List<Tag<T>>>(map.size());
        sortFacets();
        for (Map.Entry<String, List<Tag<T>>> facet:
                map.entrySet()) {
            String facetName = facet.getKey();
            List<Tag<T>> tags = facet.getValue();

            Integer maxTags = request.getMaxTags().get(facetName);
            if (maxTags == null) { // Fallback 1
                maxTags = this.maxTags.get(facetName);
            }
            if (maxTags == null) { // Fallback 2
                maxTags = DEFAULT_MAXTAGS;
            }
            if (!emptyTagsValid) {
                List<Tag<T>> tagList = facet.getValue();
                for (int i = 0 ; i < tagList.size() ; i++) {
                    if ("".equals(getTagString(
                        facetName, tagList.get(i).getKey()))) {
                        log.trace("Removing empty tag from " + facetName
                                  + " with tag count "
                                  + tagList.get(i).getCount());
                        tagList.remove(i);
                        break;
                    }
                }
            }
            if (facet.getValue().size() <= maxTags) {
                newMap.put(facet.getKey(), facet.getValue());
            } else {
                newMap.put(facet.getKey(),
                           new ArrayList<Tag<T>>(
                                   facet.getValue().subList(0, maxTags)));
            }
        }
        map = newMap;
    }

    /**
     * Used by the default merge.
     * @return the internal map.
     */
    public Map<String, List<Tag<T>>> getMap() {
        return map;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void merge(Response otherResponse) throws ClassCastException {
        if (!(otherResponse instanceof FacetResult)) {
            //noinspection ProhibitedExceptionThrown
            throw new ClassCastException(String.format(
                    "Expected a FacetResult, but go '%s'",
                    otherResponse.getClass().getName()));
        }
        FacetResult other = (FacetResult)otherResponse;
        super.merge(other);
        if (other == null) {
            log.warn("Attempted to merge with null");
        }
        String typeProblem = "The FacetResultImpl<T> default merger can only"
                             + " handle FacetResultImpl<T> as input";
        if (!(other instanceof FacetResultImpl)) {
            throw new IllegalArgumentException(typeProblem);
        }
        Map<String, List<Tag<T>>> otherMap;
        try  {
            //noinspection unchecked
            otherMap = ((FacetResultImpl<T>)other).getMap();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(typeProblem, e);
        }

        // other is source and this is destination
        //noinspection unchecked
        mergeMaxTags((FacetResultImpl<T>) other);
        for (Map.Entry<String, List<Tag<T>>> sEntry:
                otherMap.entrySet()) {
            List<Tag<T>> dList = map.get(sEntry.getKey());
            if (dList == null) { // Just add the taglist
                map.put(sEntry.getKey(), sEntry.getValue());
            } else { // Merge the tags
                List<Tag<T>> sList = sEntry.getValue();
                for (Tag<T> sPair: sList) {
                    boolean found = false;
                    for (Tag<T> dPair: dList) {
                        if (sPair.getKey().equals(dPair.getKey())) { // Merge
                            mergeTag(sPair, dPair);                                                        
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

    /*  
     * Merge two tags:
     * 1: Add total count
     * 2: 'Add' Reliability
     * 
     * Only dPair will be modified
     *     
     * @param sPair Will not modified
     * @param dPair Will be modified  
     */
	private void mergeTag(Tag<T> sPair, Tag<T> dPair) {
		dPair.setCount(sPair.getCount() + dPair.getCount());
		Reliability rD = dPair.getReliability();
		Reliability rS = sPair.getReliability();
		
		switch (rD) {
		case IMPRECISE :
		    dPair.setReliability(Reliability.IMPRECISE); 
			break;
		case LESS:
			if (rS == Reliability.LESS || rS==Reliability.PRECISE) { 
		        dPair.setReliability(Reliability.LESS);
			}
		    else { dPair.setReliability(Reliability.IMPRECISE);                                
		    }                            	
		break;
		
		case MORE:
			if (rS == Reliability.MORE || rS==Reliability.PRECISE) { 
		        dPair.setReliability(Reliability.MORE);
			}
		    else { dPair.setReliability(Reliability.IMPRECISE);                                
		    }                            	
		break;
			                            
		case PRECISE:
		    dPair.setReliability(sPair.getReliability());                            	
		break;
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
     * Compare the tag keys according to specified order.
     * @param facet the container for the tags.
     * @param tag1 first tag.
     * @param tag2 second tag.
     * @return {@code CustomComparator.compare(tag1, tag2)}.
     */
    protected abstract Comparator<Tag<T>> getTagComparator(String facet);

    /**
     * @return an ordered list of facet names.
     */
    protected abstract List<String> getFacetNames();

    public void sortFacets() {
    	// Sort on tag level
        for (Map.Entry<String, List<Tag<T>>> facet:
            map.entrySet()) {
        	Collections.sort(facet.getValue(), getTagComparator(facet.getKey()));
        }
        
        // Sort on facet level
        LinkedHashMap<String, List<Tag<T>>> newMap = 
        		new LinkedHashMap<String, List<Tag<T>>>();
        for (String facetName: getFacetNames()) {
        	List<Tag<T>> tags = map.remove(facetName);
        	if (tags != null) {
        		newMap.put(facetName, tags);
        	}
        }
        newMap.putAll(map);
        map = newMap;
    }

    /**
     * This should be overridden when extending, if the tags does not resolve
     * naturally to Strings.
     * @param facet The facet that contains the tag.
     * @param tag The tag to convert to String.
     * @return a String-representation of the Tag.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected String getTagString(String facet, T tag) {
        log.trace("Default-implementation of getTagString called with Tag "
                  + tag);
        return String.valueOf(tag);
    }
    /**
     * This should be overridden when extending, if the tags does not resolve
     * naturally to queries.
     * @param facet The facet that contains the tag.
     * @param tag The tag to convert to a query.
     * @return a query for the tag in the facet.
     */
    protected String getQueryString(String facet, T tag) {
        return facet + ":\""
               + XMLUtil.encode(queryEscapeTag(String.valueOf(tag))) + "\"";
    }

    /**
     * Assign the list of tags to the given Facet. Any existing Facet will be
     * overwritten.
     * @param facet The Facet to assign to.
     * @param tags a list of pars, where the first element is the tag and the
     *             second element is the tag-count.
     */
    public void assignTags(String facet, List<Tag<T>> tags) {
        map.put(facet, tags);
    }

    /**
     * Adds the list of tags to the given Facet. If the Facet does not exist, it
     * will be created. Each Tag will be added to the tags for the Facet.
     * If a Tag already exists in the list, the tagCounts will be added for
     * that Tag. This requires iteration through the tags, so consider using
     * {@link #assignTags} if the uniqueness of the Tags is known.<br />
     * Note that the SortOrder for the FlexiblePairs may be reset by this
     * method.
     * @param facet The Facet to assign to.
     * @param tags A list of pars, where the first element is the tag and the
     *             second element is the tag-count.
     */
    public void addTags(String facet, List<Tag<T>> tags) {
        if (map.containsKey(facet)) {
            for (Tag<T> tag: tags) {
                addTag(facet, tag);
            }
        } else {
            assignTags(facet, tags);
        }
    }

    
    public void addTag(String facet, T tagKey, int count) {
    	addTag(facet, new Tag<T>(tagKey, count, Reliability.PRECISE));
    }

        
    /**
     * Add a given Tag to a given Facet. If the Tag already exists, the tagCount
     * is added to the existing tagCount. Note that this iterates through all
     * Tags in the given Facet, thus being somewhat inefficient. Consider using
     * {@link #assignTag} if the Tag is known to be unique within the Facet.
     * @param facet    The Facet to add the Tag to. If it does not exist, a new
     *                 Facet is created.
     * @param tagKey      The Tag to add to the Facet.
     * @param count The tagCount for the Tag.
     * @param reliability  
     */
    public void addTag(String facet, T tagKey, int count, Reliability reliability) {
    	addTag(facet, new Tag<T>(tagKey, count, reliability));
    }

    public void addTag(String facet, Tag tag) {
        List<Tag<T>> tags = map.get(facet);
        if (tags == null) {
            tags = new ArrayList<Tag<T>>(
                    DEFAULTFACETCAPACITY);
            map.put(facet, tags);
        }
        for (Tag<T> tPair: tags) {
            if (tPair.getKey().equals(tag.getKey())) {
            	mergeTag(tag, tPair); //tPair will be modified with new count and reliability
            	return;
            }
        }
        tags.add(tag);
    }

    /**
     * Assigns a given Tag to a given Facet. There is no checking for duplicate
     * Tags, so ensuring consistency is up to the caller. Consider using
     * {@link #addTag} if it is unknown whether the tag is unique.
     * @param facet    The Facet to assign the Tag to. If it does not exist,
     *                 a new Facet is created.
     * @param tag      The Tag to assign to the Facet.
     * @param tagCount The tagCount for the Tag.
     */
/*    public void assignTag(String facet, T tag, int tagCount) {
        List<Tag<T>> tags = map.get(facet);
        if (tags == null) {
            tags = new ArrayList<Tag<T>>(
                    DEFAULTFACETCAPACITY);
            map.put(facet, tags);
        }
        tags.add(new Tag<T>(tag, tagCount, Reliability.PRECISE ));
    }
*/
    public static String urlEntityEscape(String text) {
        return text.replace("&",  "&amp;").
                    replace("<",  "&lt;").
                    replace(">",  "&gt;").
                    replace("#",  "%23"). // Escaping for URL
                    replace("\"", "&quot;");
    }

    /**
     * Constructs a list of the Tags under the given Facet.
     * @param facet The facet to get Tags for.
     * @return all the Tags under the given Facet or null if the Facet does not
     *         exist.
     */
    public List<String> getTags(String facet) {
        if (!map.containsKey(facet)) {
            log.debug("getTags(" + facet + "): Could not locate facet");
            return null;
        }
        List<String> result = new ArrayList<String>(map.get(facet).size());
        for (Tag<T> pair: map.get(facet)) {
            result.add(getTagString(facet, pair.getKey()));
        }
        return result;
    }

    /**
     * Escape the tag for use in a query. Currently this means placing a
     * backslash in front of quotes.
     * @param cleanTag The tag to escape.
     * @return the escaped tag.
     */
    protected String queryEscapeTag(String cleanTag) {
        return cleanTag.replace("\"", "\\\"");
    }

    protected String adjust(Map<String, String> replacements, String s) {
        return replacements.containsKey(s) ? replacements.get(s) : s;
    }

    protected String[] adjust(Map<String, String> replacements, String[] s) {
        for (int i = 0 ; i < s.length ; i++) {
            s[i] = adjust(replacements, s[i]);
        }
        return s;
    }

    /**
     * Renames the facet. and the field-names according to the map.
     * @param replacements oldName -> newName for facets and fields.
     */
    public synchronized void renameFacetsAndFields(
                                             Map<String, String> replacements) {
        LinkedHashMap<String, List<Tag<T>>> newTags =
            new LinkedHashMap<String, List<Tag<T>>>(map.size());
        for (Map.Entry<String, List<Tag<T>>> entry:
            map.entrySet()) {
            newTags.put(adjust(replacements, entry.getKey()), entry.getValue());
        }
        map = newTags;

        maxTags = adjust(replacements, maxTags);
        facetIDs = adjust(replacements, facetIDs);
    }

    private HashMap<String, Integer> adjust(
        Map<String, String> replacements, HashMap<String, Integer> map) {
        HashMap<String, Integer> adjusted = new HashMap<String, Integer>(map.size());
        for (Map.Entry<String, Integer> entry: map.entrySet()) {
            adjusted.put(adjust(
                replacements, entry.getKey()), entry.getValue());
        }
        return adjusted;
    }

    public boolean isEmptyTagsValid() {
        return emptyTagsValid;
    }

    public void setEmptyTagsValid(boolean emptyTagsValid) {
        this.emptyTagsValid = emptyTagsValid;
    }
}

