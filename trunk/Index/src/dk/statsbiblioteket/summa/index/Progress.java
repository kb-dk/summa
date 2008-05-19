/* $Id: Progress.java,v 1.3 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.3 $
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
package dk.statsbiblioteket.summa.index;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;

/**
 * @deprecated use {@link Profiler} from sbutil instead.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "hal")
public class Progress implements ProgressMBean {


    private static int recordCount;

    //private static int currentQueueSize;

    private static int currentIndexRate;
    private static int meanIndexRate;

    private long lastTime;
    private int lastRecordCount;

    private long firstTime;

    private static String currentOperation;


    public Progress() {

        recordCount = 0;
        lastRecordCount = 0;
        lastTime = System.currentTimeMillis();
        currentIndexRate = 0;
        meanIndexRate = 0;
        firstTime = lastTime;
        currentOperation = "Initializing";
    }

    public int getCurrentIndexRate() {
        long time = System.currentTimeMillis();
        int rec = recordCount;
        int rate;
        if (recordCount > 0) {
            double timediff = (time - lastTime) / (1000.0 * 60.0);
            int recorddiff = rec - lastRecordCount;
            rate = new Double(Math.ceil(recorddiff / timediff)).intValue();
            lastTime = time;
            lastRecordCount = rec;
            currentIndexRate = rate;
        }
        return currentIndexRate;
    }

    public int getMeanIndexRate() {
        long time = System.currentTimeMillis();
        if (recordCount > 0 && recordCount > lastRecordCount) {
            double timediff = (time - firstTime) / (1000.0 * 60.0);
            meanIndexRate = new Double(Math.ceil(recordCount / timediff)).intValue();
        }
        return meanIndexRate;
    }

    public static void count() {
        recordCount++;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public int[] getCurrentQueueSizes() {
        int[] ret = new int[Workflow.numberOfServices];
        for (int i = 0; i < Workflow.numberOfServices; i++) {
            ret[i] = Workflow.queues[i].size();
        }
        return ret;
    }

    public String getCurrentOperation() {
        return currentOperation;
    }

    public static void setCurrentOperation(String currentOperation) {
        Progress.currentOperation = currentOperation;
    }

}
