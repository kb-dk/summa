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
import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounter;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.ArrayUtil;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;

/**
 * A build-oriented map, meant for batch-oriented build from scratch. The map
 * uses more memory than {@link CoreMapBitStuffed} and has slower access,
 * but is a lot faster for updates.
 * </p><p>
 * The map does not support persistence, nor marking of tag counters.
 * The map should only be used for collecting data and building an
 * access-oriented map.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class CoreMapBuilder extends CoreMap32 {
    private static Log log = LogFactory.getLog(CoreMapBuilder.class);

    private static final String NO_PERSISTENCE =
            "Persistence is not supported for this map";
    private static final double INCREMENT_FACTOR = 2.0;
    private static final int DEFAULT_SIZE = 10000;

    /**
     * Simple mapping from docID to facet/tag pairs.
     */
    private int[][] map = new int[DEFAULT_SIZE][];
    private int mapSize = 0;

    public CoreMapBuilder(Configuration conf, Structure structure) {
        super(conf, structure);
    }

    public synchronized void remove(int docID) {
        if (docID < mapSize) {
            map[docID] = null;
        }
    }

    public int getDocCount() {
        return mapSize;
    }

    public synchronized void clear() {
        map = new int[DEFAULT_SIZE][];
        mapSize = 0;
    }

    private final int[] EMPTY = new int[0];
    public synchronized int[] get(int docID, int facetID) {
        if (docID >= mapSize || map[docID] == null || map[docID].length == 0) {
            return EMPTY;
        }
        int[] values = map[docID];
        int[] result = new int[values.length];
        int resultPos = 0;
        for (int value : values) {
            if (facetID == value >>> FACETSHIFT) {
                result[resultPos++] = value & TAG_MASK;
            }
        }
        int[] reducedResult = new int[resultPos];
        System.arraycopy(result, 0, reducedResult, 0, resultPos);
        return reducedResult;
    }

    public void add(int docID, int facetID, int[] tagIDs) {
        ensureSpace(docID);
        if (map[docID] == null) {
            map[docID] = ArrayUtil.mergeArrays( // Removes doublets
                    new int[0], calculateValues(facetID, tagIDs), true,
                    SORT_VALUES);
        } else {
            map[docID] = ArrayUtil.mergeArrays(
                    map[docID], calculateValues(facetID, tagIDs), true, 
                    SORT_VALUES);
        }
        mapSize = Math.max(docID+1, mapSize);
    }

    private synchronized void ensureSpace(int docID) {
        if (docID < map.length) {
            return;
        }
        log.trace("Extending to contain " + docID);
        int newSize = Math.max((int)(map.length * INCREMENT_FACTOR), docID+1);
        int[][] newMap = new int[newSize][];
        System.arraycopy(map, 0, newMap, 0, mapSize);
        map = newMap;
    }

    /* Unsupported methods */

    @Override
    protected void putValue(int position, long value) {
        throw new UnsupportedOperationException(NO_PERSISTENCE);
    }

    @Override
    protected long getPersistentValue(int index) {
        throw new UnsupportedOperationException(NO_PERSISTENCE);
    }


    public boolean open(File location, boolean forceNew) throws IOException {
        throw new UnsupportedOperationException(NO_PERSISTENCE);
    }

    public void store() throws IOException {
        throw new UnsupportedOperationException(NO_PERSISTENCE);
    }

    public int getEmptyFacet() {
        throw new UnsupportedOperationException("No empty facet in this map");
    }

    public void markCounterLists(TagCounter tagCounter, DocIDCollector docIDs, int startPos, int endPos) {
        throw new UnsupportedOperationException(
                "No marking of counter lists in this map");
    }

    public void adjustPositions(int facetID, int position, int delta) {
        if (delta == 0) {
            return;
        }
        throw new UnsupportedOperationException(
                "adjustPositions not supported for this map");
    }
}
