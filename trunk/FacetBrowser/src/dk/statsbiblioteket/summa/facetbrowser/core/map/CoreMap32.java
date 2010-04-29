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
package dk.statsbiblioteket.summa.facetbrowser.core.map;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.ArrayUtil;
import dk.statsbiblioteket.summa.facetbrowser.Structure;

import org.apache.log4j.Logger;

/**
 * Defines the bit-patterns for the storing of FacetID/TagID-pairs in 32 bit.
 * Also provides convenience-methods that depends on the patterns.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public abstract class CoreMap32 extends CoreMapImpl {
    private static Logger log = Logger.getLogger(CoreMap32.class);

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

    /**
     * This override has speed-optimization for CoreMap32.
     * @param otherCore the map to assign to.
     */
    @Override
    public void copyTo(CoreMap otherCore) {
        if (!(otherCore instanceof CoreMap32)) {
            super.copyTo(otherCore);
            return;
        }
        CoreMap32 other = (CoreMap32)otherCore;
        log.trace("Performing CoreMap32-optimized copyTo from type "
                  + this.getClass().getName()
                  + " to " + other.getClass().getName());
        
        long starttime = System.currentTimeMillis();
        other.clear();
        for (int docID = 0 ; docID < getDocCount() ; docID++) {
            if (hasTags(docID)) {
                other.setValues(docID, getValues(docID));
            }
        }
        log.debug("Finished CoreMap32-optimized copyTo from type "
                  + this.getClass().getName()
                  + " to " + other.getClass().getName() + " in "
                  + (System.currentTimeMillis() - starttime) + " ms");
    }

    /**
     * @param docID a document ID.
     * @return true if there is at least one tag for the given document ID.
     */
    public abstract boolean hasTags(int docID);

    /**
     * Assigns the given values to the docID, overwriting any existing values.
     * No sanity-checks are done to the values.
     * @param docID  a document ID.
     * @param values the values to assign to the ID.
     * @return the number of positions that the values were shiftet if the
     *         implementation keeps the values as a long list.
     */
    public abstract int setValues(int docID, int[] values);

    /**
     * Gets the values for the given document ID.
     * @param docID the document ID to get the values from.
     * @return the values from the docID or the empty list if no values exist.
     */
    public abstract int[] getValues(int docID);

    /**
     * Adds the given tags in the given facet to the given docID.
     * @param docID   the ID of a document. This needs to be
     * 0 or <= (the largest existing document-id + 1).
     * @param facetID the ID for a facet.
     * @param tagIDs an array of IDs for tags belonging to the given facet.
     */
    public void add(int docID, int facetID, int[] tagIDs) {
        if (facetID >= structure.getFacets().size()) {
            throw new IllegalArgumentException(String.format(
                    "This core map only allows %d facets. The ID for the facet "
                    + "in add was %d", FACET_LIMIT, facetID));
        }

        if (log.isTraceEnabled()) {
            log.trace(String.format("add(%d, %d, %d tagIDs) called",
                                    docID, facetID, tagIDs.length));
        }

        if (tagIDs.length == 0) { // No new tags, so we just exit
            log.trace("No tags specified for doc #" + docID
                      + " and facet #" + facetID);
            return;
        }


        /* Get the existing values for the docID and add the new values */
        int[] newValues = ArrayUtil.mergeArrays(
                getValues(docID), calculateValues(facetID, tagIDs),
                true, SORT_VALUES);

        /* Make room in values for the new data and change the index to reflect
           the new gap-size */
        setValues(docID, newValues);
    }

    public void add(int docID, int facetID, int tagID) {
        if (facetID >= structure.getFacets().size()) {
            throw new IllegalArgumentException(String.format(
                    "This core map only allows %d facets. The ID for the facet "
                    + "in add was %d", FACET_LIMIT, facetID));
        }

        if (log.isTraceEnabled()) {
            log.trace(String.format("add(%d, %d, single tag %d) called",
                                    docID, facetID, tagID));
        }

        int facetTagValue = calculateValue(facetID, tagID);
        int[] existingValues = getValues(docID);
        for (int existingValue: existingValues) {
            if (existingValue == facetTagValue) {
                return; // No merging of existing
            }
        }

        /* Get the existing values for the docID and add the new value */
        int[] newValues = ArrayUtil.mergeArrays(
                getValues(docID), new int[]{facetTagValue}, true, SORT_VALUES);

        /* Make room in values for the new data and change the index to reflect
           the new gap-size */
        setValues(docID, newValues);
    }

    // TODO: Optimize by doing calculations locally
    @Override
    public void add(int[] docIDs, int length, int facetID, int tagID) {
        for (int i = 0; i < length; i++) {
            int docID = docIDs[i];
            add(docID, facetID, tagID);
        }
    }
}

