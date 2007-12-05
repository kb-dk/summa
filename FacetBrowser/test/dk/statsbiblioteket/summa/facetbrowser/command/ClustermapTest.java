/* $Id: ClustermapTest.java,v 1.5 2007/10/11 12:56:25 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/11 12:56:25 $
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
package dk.statsbiblioteket.summa.facetbrowser.command;

import java.util.Random;

import dk.statsbiblioteket.summa.facetbrowser.core.ClusterMapCompleteArray;
import dk.statsbiblioteket.summa.facetbrowser.core.ClusterMapComplete;
import dk.statsbiblioteket.summa.facetbrowser.util.ClusterCommon;
import dk.statsbiblioteket.summa.facetbrowser.connection.IndexConnectionFactory;
import dk.statsbiblioteket.summa.facetbrowser.connection.IndexConnectionFactoryController;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ClustermapTest extends TestCase {
    public static void main(String[] args) throws Exception {
        IndexConnectionFactory.getIndexConnection();
        ClustermapTest test = new ClustermapTest();
        boolean load = args.length > 0;
        int[] limits = new int[]{Integer.MAX_VALUE};

        for (int limit: limits) {
            test.testGetFirstX(limit, 10, load);
        }
        System.out.println("Finished");
    }

    public void testAllControlledIndex() throws Exception {
        IndexConnectionFactoryController.setControlledIndex();
        testAll();
    }

    public void testAll() throws Exception {
        int[] limits = new int[] {100, 1000};
        boolean load = false;

        IndexConnectionFactory.getIndexConnection();
        for (int limit: limits) {
            testGetFirstX(limit, 10, load);
        }
    }

    public void testGetFirstX(int limit, int maxTagsPerFacet, boolean loadMap)
            throws Exception {
        System.out.println("*** Limited to " + limit + " unique documents, " +
                           maxTagsPerFacet + " tags/facet ***");
        Random random = new Random();
        Profiler progress = new Profiler();
        ClusterMapCompleteArray raw =
                new ClusterMapCompleteArray(limit);
        if (loadMap) {
            raw.loadMap();
        } else {
            raw.fillMap();
        }

        System.out.print(raw.getStats());
        System.out.println("Build time: " +
                           Profiler.millisecondsToString(progress.
                                   getSpendMilliseconds()));
        int[] threadCounts = new int[]{1, 2, 3, 4, 5};
        int[] searchCounts = new int[]{100,
                                       1000,
                                       10000,
                                       100000,
                                       1000000,
                                       2000000,
                                       5000000,
                                       10000000};
//        int[] searchCounts = new int[]{1000000, 1000000};
        int maxDoc = raw.getDocCount();
        for (int threadCount: threadCounts) {
            System.out.println("Testing with " + threadCount + " threads");
            for (int searchCount: searchCounts) {
                System.gc();
                int[] hits = new int[searchCount];
                for (int i = 0 ; i < searchCount ; i++) {
                    hits[i] = random.nextInt(maxDoc);
                }
                progress = new Profiler();
//                Arrays.sort(hits);
                raw.getFirstXThreaded(hits,
                                      ClusterMapComplete.SortOrder.ALPHA,
                                      threadCount);
                String spend = progress.getSpendTime();
                String memB = ClusterCommon.getMem();
                System.gc();
                String memA = ClusterCommon.getMem();
                String message = "Processed " + searchCount +
                                 " recordIDs  in " + spend + " using " +
                                 memB + " before GC and " + memA + " after";
                System.out.println(message);
            }
        }
        System.gc();
        System.out.println("Memory usage incl. Strings: " +
                           ClusterCommon.getMem());
        //noinspection AssignmentToNull
/*        raw.expandedFacets = null;
        System.gc();
        System.out.println("Memory usage excl. Strings: " +
                           ClusterCommon.getMem());
        if (raw.toString().equals("Blam")) {
            System.out.println("Dummy, just to ensure no clever GC");
        }*/
    }

}
