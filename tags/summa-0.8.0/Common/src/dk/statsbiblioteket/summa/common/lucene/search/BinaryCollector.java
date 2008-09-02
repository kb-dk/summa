/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.common.lucene.search;

import java.io.StringWriter;
import java.util.Arrays;

import org.apache.lucene.search.HitCollector;

/**
 * TopCollector that uses simple binary-search with arrays instead of
 * PriorityQueue. For some cases, the binary-search is slightly faster.
 * Use this as an alternative to TopDocs-returning searches.
 */
public class BinaryCollector extends HitCollector {
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
}
