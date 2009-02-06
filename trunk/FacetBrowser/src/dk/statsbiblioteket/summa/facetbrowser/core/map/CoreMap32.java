/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.facetbrowser.core.map;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.facetbrowser.Structure;

/**
 * Defines the bit-patterns for the storing of FacetID/TagID-pairs in 32 bit.
 * Also provides convenience-methods that depends on the patterns.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public abstract class CoreMap32 extends CoreMapImpl {
    public static final int FACETBITS =  5;
    public static final int FACETSHIFT = 32 - FACETBITS;
    public static final int TAG_MASK = 0xFFFFFFFF << FACETBITS >>> FACETBITS;
    /**
     * The -1 in the calculation is to make room for the emptyFacet.
     */
    public static final int FACET_LIMIT = (int) StrictMath.pow(2, FACETBITS)-1;

    /* If true, the order of the values for a given docID are sorted */
    protected static final boolean SORT_VALUES = true;

    protected CoreMap32(Configuration conf, Structure structure) {
        super(conf, structure);
    }

    /**
     * Merges the facetID and the tagID to a single 32 bit value representation.
     * @param facetID the ID for the Facet.
     * @param tagID   the ID for the Tag.
     * @return a 32 bit representation of the Facet/Tag pair.
     */
    protected int calculateValue(int facetID, int tagID) {
        if (facetID < 0) {
            throw new IllegalArgumentException(String.format(
                    "The facetID must be >= 0. It was %d", facetID));
        }
        if (tagID < 0) {
            throw new IllegalArgumentException(String.format(
                    "The tagID must be >= 0. It was %d", tagID));
        }
        return facetID << FACETSHIFT | tagID & TAG_MASK;
    }

    /**
     * Calls {@link #calculateValue(int, int)} for all tagIDs, producing an
     * array of 32 bit values, representing the facetID/tagID-pairs.
     * @param facetID the ID for the Facet.
     * @param tagIDs   the IDs for the Tags.
     * @return an array of 32 bit representation of the Facet/Tag pairs.
     */
    protected int[] calculateValues(int facetID, int[] tagIDs) {
        int[] result = new int[tagIDs.length];
        for (int i = 0 ; i < tagIDs.length ; i++) {
            result[i] = calculateValue(facetID, tagIDs[i]);
        }
        return result;
    }

}
