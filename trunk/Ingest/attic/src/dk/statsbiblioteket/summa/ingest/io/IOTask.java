/* $Id: IOTask.java,v 1.8 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.8 $
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
package dk.statsbiblioteket.summa.ingest.io;

import java.io.IOException;

import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides functionality for performing io tasks.<br>
 *
 * The functionality includes standard error handling for
 * {@link dk.statsbiblioteket.summa.storage.api.Storage} resource exhaustion.
 * An IOTask will wait until the exhaustion is handled by the subsystem.
 * @deprecated
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class IOTask implements Runnable {
    Log log = LogFactory.getLog(IOTask.class);

    // TODO: Make this configurable
    private static final int MAX_RETRIES = Integer.MAX_VALUE;
    /**
     * Sleep this amount of milliseconds between each retry, in case of
     * "Address already in use" exceptions. Note that this is increased
     * {@link #sleepDelta} for each wait.
     */
    private long sleepTime = 50;
    /**
     * Increate sleepTime with this amount of milliseconds for each retry.
     */
    private int sleepDelta = 100;

    Storage _io;
    Record r;



    public IOTask (Storage io, Record r){
         this._io = io;
         this.r = r;
    }


    public void run() {
        int retryCount = 0;
        int max_retries = MAX_RETRIES;
        long sleepTime = this.sleepTime;
        while (retryCount < max_retries) {
            try {
                _io.flush(r);
                return;
            } catch (IOException e) {
                if (e.getMessage().contains("java.net.BindException: "
                                             + "Address already in use")) { // Wait a little while before retrying
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e1) {
                        log.error("Error sleeping for " + sleepTime
                                  + " milliseconds: " + e.getMessage(), e);
                    }
                    sleepTime += sleepDelta;
                retryCount++;
            }
        }
            log.error("Failed executing io_task on recordID" + r.getId() + " "+r.toString());

        }
    }

}



