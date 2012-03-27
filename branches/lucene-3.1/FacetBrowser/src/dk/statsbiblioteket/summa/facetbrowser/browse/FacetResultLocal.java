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
 * CVS:  $Id: FacetResultLocal.java,v 1.8 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResult;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultImpl;
import dk.statsbiblioteket.summa.common.util.FlexiblePair;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.log4j.Logger;

/**
 * The local FacetStructure is optimized towards compact and fast implementation
 * at the cost of serializability. Down to earth, this means that Strings are
 * represented with integers that are pointers to Tags in a pool.
 */
// TODO: Consider if this can be auto-externalized in case of serializing
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetResultLocal extends FacetResultImpl<Integer> {
    private static final long serialVersionUID = 5648138168468L;
    private static Logger log = Logger.getLogger(FacetResultLocal.class);

    private TagHandler tagHandler;
    private Structure structure;

    public FacetResultLocal(Structure structure, TagHandler tagHandler) {
        super(structure.getMaxTags(), structure.getFacetIDs());
        this.structure = structure;
        this.tagHandler = tagHandler;
    }

    @Override
    protected int compareTags(FlexiblePair<Integer, Integer> o1,
                              FlexiblePair<Integer, Integer> o2) {
        return o1.compareTo(o2);
    }

    @Override
    protected String getQueryString(String facet, Integer tag) {
        FacetStructure fc = structure.getFacet(facet);
        if (fc == null) {
            throw new IllegalStateException(String.format(
                    "The requested facet '%s' was not present in the structure",
                    facet));
        }
        if (fc.getFields() == null || fc.getFields().length == 0) {
            throw new IllegalStateException(String.format(
                    "No fields specified in facet structure '%s'",
                    fc.getName()));
        }

        // TODO: Should # be excaped too?
        String cleanTag = XMLUtil.encode(resolveTagString(fc, tag));
        StringWriter sw = new StringWriter(100);
        if (fc.getFields().length > 1) {
            sw.append("(");
        }
        for (int i = 0 ; i < fc.getFields().length ; i++) {
            sw.append(fc.getFields()[i]);
            sw.append(":\"");
            sw.append(XMLUtil.encode(queryEscapeTag(cleanTag)));
            sw.append("\"");
            if (i < fc.getFields().length - 1) {
                sw.append(" OR ");
            }
        }
        if (fc.getFields().length > 1) {
            sw.append(")");
        }

        return sw.toString();
    }

    @Override
    protected String getTagString(String facet, Integer tag) {
        return resolveTagString(structure.getFacet(facet), tag);
    }

    protected String getTagString(FacetStructure facet, Integer tag) {
        return resolveTagString(facet, tag);
    }

    protected String resolveTagString(FacetStructure facet, Integer tag) {
        return tagHandler.getTagName(facet.getFacetID(), tag);
    }

    /**
     * Resolve the internal tag-ids to Strings and produce a serializable
     * version of this FacetStructure, suitable for network-transfer.
     * @return a version of the FacetStructure suitable for external use.
     */
    public FacetResult externalize() {
        log.trace("externalize() called");
        FacetResultExternal external = new FacetResultExternal(
                structure.getMaxTags(), structure.getFacetIDs(),
                structure.getFacetFields());
        for (Map.Entry<String, List<FlexiblePair<Integer, Integer>>> entry:
                map.entrySet()) {
            String facet = entry.getKey();
            for (FlexiblePair<Integer, Integer> tagPair: entry.getValue()) {
                external.assignTag(facet, getTagString(facet, tagPair.getKey()),
                                   tagPair.getValue());
            }
        }
        return external;
    }

    /* Response interface */

    public String getName() {
        return "FacetResultLocal";
    }
}

