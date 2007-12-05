/* $Id: FacetStructureLocal.java,v 1.8 2007/10/05 10:20:22 te Exp $
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
 * CVS:  $Id: FacetStructureLocal.java,v 1.8 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.browse;

import java.util.List;
import java.util.Map;

import dk.statsbiblioteket.summa.facetbrowser.util.FlexiblePair;
import dk.statsbiblioteket.summa.facetbrowser.util.ClusterCommon;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The local FacetStructure is optimized towards compact and fast implementation
 * at the cost of serializability. Down to earth, this means that Strings are
 * represented with integers (pointers to a String-pool).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetStructureLocal extends FacetStructureImpl<Integer> {
    private TagHandler tagHandler;

    public FacetStructureLocal(StructureDescription structureDescription,
                               TagHandler tagHandler) {
        super(structureDescription);
        this.tagHandler = tagHandler;
    }

    protected int compareTags(FlexiblePair<Integer, Integer> o1,
                              FlexiblePair<Integer, Integer> o2) {
        return o1.compareTo(o2);
    }

    protected String getTagString(String facet, Integer tag) {
        return ClusterCommon.simpleEntityEscape(resolveTagString(facet, tag));
    }

    protected String getQueryString(String facet, Integer tag) {
        return facet + ":\"" +
               ClusterCommon.simpleEntityEscape(resolveTagString(facet, tag))
               + "\"";
    }

    protected String resolveTagString(String facet, Integer tag) {
        return tagHandler.getTagName(tagHandler.getFacetID(facet), tag);
    }

    /**
     * Resolve the internal tag-id's to Strings and produce a serializable
     * version of this FacetStructure, suitable for network-transfer.
     * @return a version of the FacetStructure suitable for external use.
     */
    public FacetStructure externalize() {
        FacetStructureExternal external =
                new FacetStructureExternal(structureDescription);
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

}
