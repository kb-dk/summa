/* $Id: FacetStructure.java,v 1.5 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.5 $
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
 * CVS:  $Id: FacetStructure.java,v 1.5 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The FacetStructure represents the result of facet and tag extraction from
 * a search result.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface FacetStructure<T> {
    /**
     * Sort order for tags.<br/>
     * tag:        Natural order for the tags (most probably alpha-numeric).
     * popularity: Order by number of occurences.
     */
    public enum TagSortOrder {tag, popularity}

    /**
     * Convert the internal structure to XML, suitable for output to
     * web-services and similar.
     * @return an XML-representation of the structure.
     */
    public String toXML();

    /**
     * Reduce the representation according to the limitations defined in
     * the given description. The reduction is also responsible for sorting.
     * @param tagSortOrder the order in which the tags should be sorted.
     */
    public void reduce(TagSortOrder tagSortOrder);

    /**
     * Merge the data from other to this FacetStructure. Note that merging does
     * not reduce the data, so it is advisable to run {@link #reduce} at some
     * point after merging has finished..
     * @param other data to be added to this FacetStructure.
     */
    public void merge(FacetStructure other);

    /**
     * Resolve any JVM-specific dependencies and produce a FacetStructure
     * suitable for network transfer.
     * @return a version of the FacetStructure suitable for external use.
     */
    public FacetStructure externalize();
}
