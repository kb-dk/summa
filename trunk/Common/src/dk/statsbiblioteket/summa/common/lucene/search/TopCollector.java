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

import java.io.StringWriter;

import org.apache.lucene.search.HitCollector;

/**
 * TopCollector that uses simple array-copying instead of the more fancy
 * PriorityQueue. For some cases, the array-juggling is slightly faster.
 * Use this as an alternative to TopDocs-returning searches.
 */
public class TopCollector extends HitCollector {
    int maxHits;
    int[] ids;
    float[] scores;
    int count = 0;
    float min = -1;

    /**
     * @param maxHits the maximum number of hits to return.
     */
    public TopCollector(int maxHits) {
        this.maxHits = maxHits;
        ids = new int[maxHits+1];
        scores = new float[maxHits+1];
    }

    public void collect(int i, float v) {
        if (v < min) {
            return;
        }
//        System.out.println("min " + min + ", val " + v);
        for (int pos = 0 ; pos < count ; pos++) {
            if (v > scores[pos]) {
                System.arraycopy(ids, pos, ids, pos+1, maxHits-pos-1);
                ids[pos] = i;
                System.arraycopy(scores, pos, scores, pos+1, maxHits-pos-1);
                scores[pos] = v;
                count = Math.min(maxHits, ++count);
                min = scores[count-1];
                return;
            }
        }
        count = Math.min(maxHits, ++count);
        ids[count-1] = i;
        scores[count-1] = v;
        min = v;
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
}




