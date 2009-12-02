/* $Id: Schedule.java,v 1.3 2007/10/04 13:28:21 te Exp $
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



