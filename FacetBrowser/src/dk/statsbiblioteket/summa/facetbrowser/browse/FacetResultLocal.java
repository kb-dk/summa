/* $Id: FacetResultLocal.java,v 1.8 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.8 $
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
 * CVS:  $Id: FacetResultLocal.java,v 1.8 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.summa.common.util.ParseUtil;
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
        String cleanTag = ParseUtil.encode(resolveTagString(fc, tag));
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
