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

import java.util.*;
import java.io.StringWriter;

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
    private HashMap<String, String[]> fields;

    public FacetResultExternal(HashMap<String, Integer> maxTags,
                               HashMap<String, Integer> facetIDs,
                               HashMap<String, String[]> fields) {
        super(maxTags, facetIDs);
        this.fields = fields;
    }

    public FacetResult externalize() {
        return this;
    }

    public String getName() {
        //noinspection DuplicateStringLiteralInspection
        return "FacetResult";
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

    private HashMap<String, String[]> getFields() {
        return fields;
    }


    @Override
    public void merge(Response otherResponse) throws ClassCastException {
        super.merge(otherResponse);
        if (!(otherResponse instanceof FacetResultExternal)) {
            return;
        }
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
}

