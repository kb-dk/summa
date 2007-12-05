/* $Id: ClusterMapCompleteThread.java,v 1.4 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:22 $
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
package dk.statsbiblioteket.summa.facetbrowser.core;

import java.util.List;
import java.util.HashMap;

import dk.statsbiblioteket.summa.facetbrowser.browse.TagCounter;
import dk.statsbiblioteket.summa.facetbrowser.util.FlexiblePair;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

/**
 * @deprecated a suitable replacement is under development.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "te")
public class ClusterMapCompleteThread implements Runnable {
    private static Logger log =
            Logger.getLogger(ClusterMapCompleteThread.class);

    private ClusterMapComplete map;
    private TagCounter tagCounter;

    private int[] docIDs;
    private int startIndex;
    private int endIndex;
    private ClusterMapComplete.SortOrder sortOrder;
    private StructureDescription structureDescription;

    private boolean readyForRun = false;

    private HashMap<String, List<FlexiblePair<Integer, Integer>>> result;

    public ClusterMapCompleteThread(ClusterMapComplete map,
                                    TagCounter tagCounter) {
        this.map = map;
        this.tagCounter = tagCounter;
    }

/*    public ClusterMapCompleteThread(ClusterMapComplete map,
                                    int[][] counterLists) {
        this.map = map;
        this.counterLists = counterLists;
    }*/

    public void prepareRun(int[] docIDs, int startIndex, int endIndex,
                           ClusterMapComplete.SortOrder sortOrder,
                           StructureDescription structureDescription) {
        this.docIDs = docIDs;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.sortOrder = sortOrder;
        this.structureDescription = structureDescription;
        readyForRun = true;
    }

    public void run() {
        if (!readyForRun) {
            String error = "ClusterMapCompleteThread not properly initialized";
            log.error(error);
            throw new RuntimeException(error);
        }
        log.debug("Starting threaded call");
        result = map.getFirstX(docIDs, startIndex, endIndex, sortOrder,
                               structureDescription, tagCounter);
//        result = map.getFirstX(docIDs, startIndex, endIndex, sortOrder,
//                               maxFacets, maxTags, maxObjects, counterLists);
        log.debug("Finished threaded call");
        readyForRun = false;
    }

    public HashMap<String, List<FlexiblePair<Integer, Integer>>> getResult() {
        return result;
    }

    public void clearCounterLists() {
        tagCounter.reset();
//        ClusterMapCompleteArray.clearCounterLists(tagCounter);
    }
/*    public void clearCounterLists() {
        ClusterMapCompleteArray.clearCounterLists(counterLists);
    }*/
}
