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
package dk.statsbiblioteket.summa.support.lucene.search.sort;

import dk.statsbiblioteket.summa.common.util.bits.BitsArrayPacked;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Comparator;
import java.util.List;

/**
 * Sorts a list of Objects indirectly by using a pre-generated array of
 * positions. Usable for repeated sorting of subsets of a list.
 * </p><p>
 * Example: The String-list c, d, a is sorted according to natural order.
 *          The result is 1, 2, 0.
 * Usage:   If we want the sorted position of "d" from the example above, we
 *          first determine the position of "d" in the original list which is 1,
 *          after which we perform a lookup in the result from above, which
 *          gives us 2.
 * </p><p>
 * Sample use case:<br/>
 * A list of documents with integer-based IDs (Lucene docID), where each
 * document has one associated term. We want to sort a sub-sample of these
 * documents according to the terms, but we do not want to extract the terms
 * for each sub-sample we receive.
 * </p><p>
 * We create a list of the terms from the documents, with one entry/document.
 * The list is sorted with the ReferenceSorter at initialization time.
 * We then receive a subset of document-IDs. In order to sort this subset,
 * we only need to let our comparator perform a comparison of the integers
 * resolved from the pre-generated array.
 * </p><p>
 * Pros: Sorting of sublists is very fast.
 * Cons: The sorter needs to perform a complete sort on startup.
 *       There is a fixed memory usage proportionally to the number of objects.
 * Note: #documents * log2(#documents) bits are used to hold the sort-order.
 */
// TODO: Finish implementing this class
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ReferenceSorter<T> {
    private static Log log = LogFactory.getLog(ReferenceSorter.class);

    BitsArrayPacked positions;

    /**
     * Initializes the reference sorter for the given elements. Note that the
     * sorter is memory-friendly and does not copy the given list of elements
     * as Collections.sort does.
     * </p><p>
     * Note: 
     * @param elements   the elements to generate sorted position-array for.
     *                   this list must contain all possible elements.
     * @param comparator used for comparing elements.
     */
    public ReferenceSorter(List<T> elements, Comparator<T> comparator) {
        positions = new BitsArrayPacked(
                elements.size(),
                // TODO: Test the elementLength calculation
                (int)(Math.log(elements.size()) / Math.log(2)) + 1);
        for (int i = 0 ; i < elements.size() ; i++) {
            positions.set(i, i);
        }
        
    }

    /**
     * Sort the array of positions according to the comparator given in the
     * constructor.
     * @param positions the positions to sort.
     * @return positions in sorted order.
     */
    public int[] sort(int[] positions) {
        return null;
    }

}

