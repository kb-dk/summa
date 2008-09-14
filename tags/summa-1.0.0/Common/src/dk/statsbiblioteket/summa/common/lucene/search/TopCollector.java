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



