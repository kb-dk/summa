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
 * CVS:  $Id: FacetResultExternal.java,v 1.7 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.api;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.text.Collator;

import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.util.xml.XMLUtil;

/**
 * This facet structure representation is suitable for serializing and other
 * external use. It does not rely on String pool or similar index-specific
 * resources.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetResultExternal extends FacetResultImpl<String> {
    private static final long serialVersionUID = 7879716843L; // 20110822
    private HashMap<String, String[]> fields;
    private Structure structure;

    public static final String NAME = "FacetResult";

    public FacetResultExternal(HashMap<String, Integer> maxTags,
                               HashMap<String, Integer> facetIDs,
                               HashMap<String, String[]> fields,
                               Structure structure) {
        super(maxTags, facetIDs);
        this.fields = fields;
        this.structure = structure;
    }

    @Override
    public FacetResultExternal externalize() {
        return this;
    }

    @Override
    public String getName() {
        //noinspection DuplicateStringLiteralInspection
        return NAME;
    }

    @Override
    protected String getQueryString(String facet, String tag) {
        if (fields.get(facet) == null) {
            throw new IllegalStateException(String.format(
                    "No fields specified in facet '%s'", facet));
        }

        // TODO: Should # be excaped too?
        String cleanTag = getTagString(facet, tag);
        StringWriter sw = new StringWriter(100);
        String[] fields = this.fields.get(facet);
        if (fields.length > 1) {
            sw.append("(");
        }
        for (int i = 0 ; i < fields.length ; i++) {
            sw.append(fields[i]);
            sw.append(":\"");
            sw.append(XMLUtil.encode(queryEscapeTag(cleanTag)));
            sw.append("\"");
            if (i < fields.length - 1) {
                sw.append(" OR ");
            }
        }
        if (fields.length > 1) {
            sw.append(")");
        }

        return sw.toString();
    }

    public HashMap<String, String[]> getFields() {
        return fields;
    }

    public void setFields(HashMap<String, String[]> fields) {
        this.fields = fields;
    }

    /**
     * Reduces the number of tags in the facets, as per the original request
     * structure. This is called automatically by {@link #toXML()}.
     */
    public void reduce() {
        reduce(structure);
    }

    @Override
    public String toXML() {
        reduce();
        return super.toXML();
    }

    @Override
    public void merge(Response otherResponse) throws ClassCastException {
        if (!(otherResponse instanceof FacetResultExternal)) {
            return;
        }
        super.merge(otherResponse);
        for (Map.Entry<String, String[]> otherField:
                ((FacetResultExternal)otherResponse).getFields().entrySet()) {
            if (!fields.containsKey(otherField.getKey())) {
                fields.put(otherField.getKey(), otherField.getValue());
            } else {
                fields.put(otherField.getKey(),
                           mergeArrays(fields.get(otherField.getKey()),
                                       otherField.getValue()));
            }
        }
    }

    private String[] mergeArrays(String[] one, String[] two) {
        if (Arrays.equals(one, two)) {
            return one;
        }
        List<String> merged = Arrays.asList(one);
        for (String t: two) {
            if (!merged.contains(t)) {
                merged.add(t);
            }
        }
        String[] result = new String[merged.size()];
        merged.toArray(result);
        return result;
    }

    /**
     * Renames the facet. and the field-names according to the map.
     * @param map oldName -> newName for facets and fields.
     */
    @Override
    public void renameFacetsAndFields(Map<String, String> map) {
        HashMap<String, String[]> newFields =
            new HashMap<String, String[]>(fields.size());
        for (Map.Entry<String, String[]> entry: fields.entrySet()) {
            newFields.put(adjust(map, entry.getKey()),
                          adjust(map, entry.getValue()));
        }
        fields = newFields;
        super.renameFacetsAndFields(map);
    }

    private static class AlphaComparator implements Comparator<Tag<String>>{
    	private Collator collator = null;
    	public AlphaComparator(String locale) {
    		if (locale != null) {
    			collator = Collator.getInstance(new Locale(locale)); 
    		}
    	}
		@Override
		public int compare(Tag<String> t1,Tag<String> t2) {
			if (collator == null) {
				return t1.getKey().compareTo(t2.getKey());
			}
			return collator.compare(t1.getKey(), t2.getKey());
		}
    }

    private static class PopularityComparator implements Comparator<Tag<String>>{

		@Override
		public int compare(
				Tag<String> t1,
				Tag<String> t2) {
			return t2.getCount()-t1.getCount();  //Highest number first. (descending)
		}
    }
	@Override
	protected Comparator<Tag<String>> getTagComparator(String facet) {
		FacetStructure fc = structure.getFacet(facet);
		if (fc == null || !FacetStructure.SORT_ALPHA.equals(fc.getSortType())) {
			return new PopularityComparator();
		}
		return new AlphaComparator(fc.getLocale());
	}

	@Override
	protected List<String> getFacetNames() {
		return structure.getFacetNames();
	}
    
    
}

