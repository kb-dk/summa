/* $Id: SlimCollector.java,v 1.2 2007/10/04 13:28:22 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:22 $
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
 * CVS:  $Id: SlimCollector.java,v 1.2 2007/10/04 13:28:22 te Exp $
 */
package dk.statsbiblioteket.summa.common.lucene.search;

import org.apache.lucene.search.HitCollector;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A Lucene HitCollector, that only collects document IDs. This collector
 * dynamically expands to contain the encountered data.
 *
 * As there is no sorting of the search-result when a collector is used, the
 * SlimCollector is comparable to the speed of a standard Hits-returning search,
 * with the difference that all hits are extracted with the SlimCollector.
 *
 * TODO: Figure out how to skip the expensive calculation of score, as it is discarded
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class SlimCollector extends HitCollector {
    private static final double growthFactor = 2;
    private int[] content;
    private int pos = 0;
    private static final int DEFAULT_INITIAL_CAPACITY = 10000;

    public SlimCollector() {
        content = new int[DEFAULT_INITIAL_CAPACITY];
    }
    public SlimCollector(int initialCapacity) {
        content = new int[initialCapacity];
    }

    public void collect(int i, float v) {
        if (pos == content.length) {
            int[] temp = new int[(int)(content.length*growthFactor)];
            System.arraycopy(content, 0, temp, 0, content.length);
            content = temp;
        }
        content[pos++] = i;
    }

    /**
     * Returns the collected document IDs as an array. Note that this involves
     * an memory allocation and an Array-copy.
     * @return an array with the collected IDs
     * @deprecated use {@link #getDocumentIDsOversize()} instead, to avoid an
     *             array copy.
     */
    public int[] getDocumentIDs() {
        int[] result = new int[pos];
        System.arraycopy(content, 0, result, 0, pos);
        return result;
    }

    /**
     * Returns the internal representation of the slimCollector. This is ugly,
     * but it avoids an array copy. Note that only the ids from position 0 to
     * documentCount-1 are the result from the last collection.
     * @return an array with document IDs from the last collest, plus garbage
     *         at the end. Use {@link #getDocumentCount()} to determine how
     *         many of the IDs are valid.
     */
    public int[] getDocumentIDsOversize() {
        return content;
    }

    /**
     * @return the number of documents that was collected.
     */
    public int getDocumentCount() {
        return pos;
    }

    /**
     * Resets the SlimCollector, so that it is ready for another collection
     * run. The internal arrays are not reduced. This is usable for a pool
     * of SlimCollectors, in order to avoid GC.
     */
    public void clean() {
        pos = 0;
    }
}



