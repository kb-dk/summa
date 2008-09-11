/* $Id: EmployerBaseMBean.java,v 1.2 2007/10/04 13:28:19 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:19 $
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
package dk.statsbiblioteket.summa.dice;

import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface EmployerBaseMBean {

    /**
     * Stop the Employers job fetching thread and allways
     * return null on getJob. This should cause all Workers
     * with keepAlive=false to stop too (when their local job queue is empty).
     */
    public void forceStop ();

    public int getQueueSize ();

    public int getNumRunning ();

    public int getNumConsumerEnqueued ();

    public int getNumFinished ();

    public String[] getWorkerInfo ();

    public String getStartDate ();

    /**
     * Average job processing time in seconds.
     * A job is considered processed when it is
     * registered by the consumer
     */
    public double getMeanProcessingSpeed ();

    /**
     * Weighted processing speed calculated by
     *
     *   lastSpeed = (newSpeed + lastSpeed)/2
     *
     * thereby significantly weighing recent speed counts to be more important
     * and older speed counts to be "forgotten" fast.
     * @return
     */
    public double getProcessingSpeed ();

    /**
     * Average running time for the fetchJob method
     * @return
     */
    public double getMeanFetchingSpeed ();
}



