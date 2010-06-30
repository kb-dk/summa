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
package dk.statsbiblioteket.summa.common.util;

import java.util.Date;
import java.text.DateFormat;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Simple active profiler. This class does not do any monitoring and you have
 * to manually update it with the {@link #update} method.
 *
 * Use case 1:<br />
 * Profile each iteration of a loop. Call {@link #start} before looping and call
 * {@link #update} at the end of each iteration.
 *</p><p>
 * Use case 2:<br />
 * Profile a specific event occuring sporadically. Call {@link #start} at the
 * beginning of each event and call {@link #update} at the end of the event.
 * @deprecated use {@link dk.statsbiblioteket.util.Profiler} instead.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
@Deprecated
public class Profiler {

    private Date startDate;
    private Date stopDate;
    private static final DateFormat dateFormatter =
            DateFormat.getDateTimeInstance();
    private double numUpdates;
    private long lastUpdate;
    private double meanSpeed;
    private double speed;
    private double lastSpeed;

    public Profiler() {
        meanSpeed = 0;
        speed = 0;
        lastUpdate = 0;
        numUpdates = 0;
        stopDate = null;
        startDate = null;
    }

    /**
     * Start the profiler. You need to call this before starting to {@link #update} it.
     *
     * Calling start again will not reset the profiler , just set start the timer running ftom the
     * current system time.
     */
    public void start () {
        lastUpdate = System.currentTimeMillis();
        if (startDate == null) {
            startDate = new Date();
        }
    }

    /**
     * Update all stats for this profiler. Make sure that you call {@link #start}
     * to initialize the Profiler.
     *
     * @throws NullPointerException if the Profiler has not been started yet
     */
    public void update () {
        if (startDate == null) {
            throw new NullPointerException ("Profiler updated without being started");
        }

        numUpdates = numUpdates + 1;
        double delta_t = System.currentTimeMillis() - lastUpdate;

        meanSpeed = ((numUpdates-1)/numUpdates)*meanSpeed + (1/numUpdates)*delta_t;

        if  (speed < 1) {speed = delta_t;}
        speed = (speed + delta_t)/2;

        lastUpdate = System.currentTimeMillis();
        lastSpeed = delta_t;
    }

    /**
     * Stop the Profiler
     */
    public void stop () {
        update();
        stopDate = new Date();
    }

    /**
     * Get the actual last speed measured in milliseconds. This number does not
     * take any history into account.
     * @return
     */
    public double getLastSpeed () {
        return lastSpeed;
    }

    /**
     * Mean speed  in milliseconds, updated by
     *
     *   meanSpeed = ((numUpdates-1)/numUpdates)*meanSpeed + (1/numUpdates)*delta_t;
     *
     * @return
     */
    public double getMeanSpeed () {
        return meanSpeed;
    }

    /**
     * Weighted speed  in milliseconds, updated by
     *
     *   speed = (newSpeed + speed)/2
     *
     * This significantly weight recent speed counts to be more important
     * and older speed counts to be "forgotten" fast.
     * @return
     */
    public double getSpeed () {
        return speed;
    }

    public String toString () {
        if (stopDate == null) {
            return "Speed: " + speed + ", MeanSpeed: " + meanSpeed + ", Updates: " + numUpdates + ", Started: " + getStartDateString();
        } else {
            return "Speed: " + speed + ", MeanSpeed: " + meanSpeed + ", Updates: " + numUpdates + ", Started: " + getStartDateString() + ", Stopped: " + getStopDateString();
        }
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getStartDateString() {
        return  Profiler.dateFormatter.format (startDate);
    }

    public Date getStopDate() {
        return stopDate;
    }

    public String getStopDateString() {
        if (stopDate == null) {
            return "Profiler not stopped";
        }
        return Profiler.dateFormatter.format (stopDate);
    }
}




