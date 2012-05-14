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

import java.util.TimerTask;
import java.util.Date;
import java.text.DateFormat;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The Schedule is resposible for calling a {@link Schedulable}.
 * A schedule is instanciated by the {@link Scheduler}
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
class Schedule extends TimerTask{

    /**
    * reference to the component called by this component.
    */
    private Schedulable scheduleClass;

    private Log log = LogFactory.getLog(this.getClass().getPackage().getName());


    /**
     * Default constructor.
     * @param object the object to be scheduled.
     */
    Schedule(final Schedulable object){
         scheduleClass = object;
    }

    /**
     * implementing {@link Runnable}.
     */
    public void run() {
        log.info("Scheduled method call on:" + scheduleClass.getClass().getName() + "runtime:" + DateFormat.getInstance().format(new Date()));
        scheduleClass.perform();
    }

}




