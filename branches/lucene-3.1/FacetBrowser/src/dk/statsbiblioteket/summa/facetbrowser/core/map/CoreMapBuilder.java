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
     * Simple mapping from docID to facet/tag pairs: map[docID] -> value[].
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

    @Override
    public void add(int docID, int facetID, int[] tagIDs) {
        if (tagIDs.length == 0) {
            return;
        }
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

    // TODO: Optimize this by bypassing array creation
    public void add(int docID, int facetID, int tagID) {
        ensureSpace(docID);
        if (map[docID] == null) {
            map[docID] = new int[calculateValue(facetID, tagID)];
        } else {
            map[docID] = ArrayUtil.mergeArrays(
                    map[docID], new int[calculateValue(facetID, tagID)],
                    true, SORT_VALUES);
        }
        mapSize = Math.max(docID+1, mapSize);
    }

    @Override
    public void add(int[] docIDs, int length, int facetID, int tagID) {
        final int[] values = new int[]{calculateValue(facetID, tagID)};
        for (int i = 0; i < length; i++) {
            int docID = docIDs[i];
            ensureSpace(docID);
            if (map[docID] == null) {
                map[docID] = values;
            } else {
                map[docID] = ArrayUtil.mergeArrays(
                        map[docID], values, true, SORT_VALUES);
            }
            mapSize = Math.max(docID + 1, mapSize);
        }
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

    @Override
    public boolean hasTags(int docID) {
        return docID < getDocCount()
               && map[docID] != null && map[docID].length != 0;
    }

    @Override
    public int setValues(int docID, int[] values) {
        map[docID] = values;
        return 0; // No value-shifting is ever done in this map
    }

    @Override
    public int[] getValues(int docID) {
        return docID >= mapSize || map[docID] == null ? EMPTY : map[docID]; 
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

        //noinspection DuplicateStringLiteralInspection
        log.trace("Adjusting position for facetID " + facetID + " tags >= "
                  + position + " with delta " + delta);
        for (int docID = 0 ; docID < map.length ; docID++) {
            if (map[docID] == null) {
                continue;
            }
            for (int valuePos = 0 ; valuePos < map[docID].length ; valuePos++) {
                int value = map[docID][valuePos];
                int facet = value >>> FACETSHIFT;
                int tag = value & TAG_MASK;
                if (facet == facetID && tag >= position) {
                    map[docID][valuePos] =
                            facet << FACETSHIFT | tag + delta & TAG_MASK;
                }
            }
        }
    }
}

