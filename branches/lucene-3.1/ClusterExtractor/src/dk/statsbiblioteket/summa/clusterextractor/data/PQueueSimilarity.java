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
package dk.statsbiblioteket.summa.clusterextractor.data;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * PQueueSimilarity is a similarity holder entry used in a priority queue.
 *
 * The value of a PQueueSimilarity is the similarity between two
 * {@link dk.statsbiblioteket.summa.clusterextractor.math.SparseVector}s.
 * PQueueSimilarities are used as entries in a {@link java.util.PriorityQueue}
 * in the {@link dk.statsbiblioteket.summa.clusterextractor.ClusterMergerImpl}
 * mergeCentroidSets() method.
 *
 * PQueueSimilarity implements {@link Comparable} to get the order in the
 * PriorityQueue right. The order of PQueueSimilarities is the reverse natural
 * ordering of the values.
 *
 * The PQueueSimilarity further knows the indexes of the two SparseVectors
 * in the work array (remember this is a similarity between two SparseVectors).
 * These indexes are also used as  a row index and a column index of
 * an imaginary matrix of similarities between SparseVectors. Keeping
 * this row and column index let's us work with the imaginary matrix, but only
 * keeping a work array of SparseVectors and a PriorityQueue in memory.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam",
        comment = "Class and methods needs Javadoc")
public class PQueueSimilarity implements Comparable<PQueueSimilarity> {
    /** The double value of the similarity held by this PQueueSimilarity. */
    private double value;
    /** First index of work array and row index of imaginary matrix. */
    private int i;
    /** Second index of work array and column index of imaginary matrix. */
    private int j;

    /**
     * Construct PQueueSimilarity with given similarity and indices.
     * @param similarity similarity between two SparseVectors
     * @param i index of first SparseVector in work array
     * @param j index of second SparseVector in work array
     */
    public PQueueSimilarity(double similarity, int i, int j) {
        this.value = similarity;
        this.i = i;
        this.j = j;
    }

    /**
     * Get the double value of the similarity held by this PQueueSimilarity.
     * @return value
     */
    public double getValue() {
        return value;
    }

    /**
     * Get first index of workarray / row index of imaginary matrix.
     * @return index i
     */
    public int getI() {
        return i;
    }

    /**
     * Get second index of workarray / column index of imaginary matrix.
     * @return index j
     */
    public int getJ() {
        return j;
    }

    /**
     * Compares this PQueueSimilarity with the specified PQueueSimilarity for order.
     * Returns a negative integer, zero, or a positive integer as this similarity
     * before (greater than), equal to, or after (less than) the specified similarity
     * in a PriorityQueue of PQueueSimilarity.
     * Note that the natural ordering of the double values is reversed, as we
     * want the largest value first in the priority queue.
     * @param o the similarity to be compared
     * @return a negative integer, zero, or a positive integer as this
     *         similarity is before, equal to, or after the specified similarity
     */
    public int compareTo(PQueueSimilarity o) {
        return - new Double(value).compareTo(o.getValue());
    }
}




