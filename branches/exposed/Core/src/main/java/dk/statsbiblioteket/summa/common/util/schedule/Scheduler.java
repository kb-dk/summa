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



