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
package dk.statsbiblioteket.summa.common.lucene.search;

import org.apache.lucene.index.*;
import org.apache.lucene.search.*;

import java.io.*;

/**
 * TopCollector that uses simple binary-search with arrays instead of
 * PriorityQueue. For some cases, the binary-search is slightly faster.
 * Use this as an alternative to TopDocs-returning searches.
 */
public class BinaryCollector extends Collector {
    int maxHits;
    int[] ids;
    float[] scores;
    int count = 0;
    float min = -1;

    /**
        * @param maxHits the maximum number of hits to return.
        */
    public BinaryCollector(int maxHits) {
        this.maxHits = maxHits;
        ids = new int[maxHits+1];
        scores = new float[maxHits+1];
    }

    public void collect(int docID, float score) {
        if (score < min) {
            return;
        }
        int pos = binarySearch(scores, score);
        if (pos == count && count == maxHits) { // lowest
            ids[maxHits-1] = docID;
            scores[maxHits-1] = score;
            min = score;
            return;
        }
        System.arraycopy(ids, pos, ids, pos+1, maxHits-pos-1);
        ids[pos] = docID;
        System.arraycopy(scores, pos, scores, pos+1, maxHits-pos-1);
        scores[pos] = score;
        count = Math.min(maxHits, ++count);
        min = scores[count-1];
    }


    // Simple reverse binary searcher that assumes values >= 0.0 and !NaN.
    // The searcher returns insert position.
    private static int binarySearch(float[] values, float key) {
        int low = 0;
        int high = values.length;
        while (low <= high) {
            int middle = low + high >>> 1;
            float midVal = values[middle];
            if (midVal > key) {
                low = middle + 1;
            } else if (midVal < key) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
        return low;
    }

    /**
     * Reset the collector, so that it is ready for another run.
     */
    public void reset() {
        count = 0;
    }

    /**
     * @return the number of collected elements.
     */
    public int size() {
        return count;
    }

    /**
     * @param i the index in the element list.
     * @return the doc-id for the element.
     */
    public int getPosition(int i) {
        if (i >= count) {
            throw new ArrayIndexOutOfBoundsException("There is only " + count
                                                     + " elements and element #"
                                                     + i + " was requested");
        }
        return ids[i];
    }

    /**
     * @param i the index in the element list.
     * @return the score for the element.
     */
    public float getValue(int i) {
        if (i >= count) {
            throw new ArrayIndexOutOfBoundsException("There is only " + count
                                                     + " elements and element #"
                                                     + i + " was requested");
        }
        return scores[i];
    }

    public String toString() {
        int MAX = 10;
        StringWriter sw = new StringWriter();
        for (int i = 0 ; i < count && i < MAX ; i++) {
            sw.append("(").append(Integer.toString(ids[i])).append(", ");
            sw.append(Float.toString(scores[i])).append(")");
            if (i < count-1) {
                sw.append(" ");
            }
        }
        if (count > MAX) {
            sw.append("...");
        }
        return sw.toString();
    }

    // TODO check if these "implementains" make sense.
    @Override
    public void setScorer(Scorer scorer) throws IOException {
        // ignore score
    }

    @Override
    public void collect(int i) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setNextReader(IndexReader indexReader, int i)
            throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
        return false;
    }
}




