/* $Id: Scheduler.java,v 1.3 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:21 $
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
package dk.statsbiblioteket.summa.common.util.schedule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Timer;
import java.util.TimerTask;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The Scheduler sets up a {@link Timer} and {@link Schedule} on a {@link Schedulable} object.<p/>
 * The Timer is instaciated as a periodic Timer with an initial Latency.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class Scheduler extends Thread {

    /**
     * Category used for logging.
     */
    private Log log = LogFactory.getLog(this.getClass().getPackage().getName());

    /**
     * Starts the scheduler.
     *
     * @param object    the component beeing called
     * @param startup   the initial latency, messured in seconds
     * @param period    the periodic time between calling the
                        {@link Schedulable} component, messured in seconds.
     */
    public void start(final Schedulable object, final int startup,
                      final int period){
        log.info("Starting Scheduler on:" + object.getClass().getName()
                 + "latency: " + startup + "s." + "period:" +  period + "s.");
        final Timer timer = new Timer();
        final TimerTask task = new Schedule(object);
        timer.schedule(task,startup * 1000,period * 1000);
    }

}