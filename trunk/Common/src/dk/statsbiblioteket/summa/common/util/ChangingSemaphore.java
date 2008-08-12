/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.common.util;

import java.util.concurrent.Semaphore;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Semaphore that keeps track of the overall number of permits and allows
 * this number to be changed. This means that the number of available permits
 * can be negative.
 * */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ChangingSemaphore extends Semaphore {
    private static Log log = LogFactory.getLog(ChangingSemaphore.class);

    private int overallPermits;

    public ChangingSemaphore(int permits) {
        super(permits);
        overallPermits = permits;
    }

    public ChangingSemaphore(int permits, boolean fair) {
        super(permits, fair);
        overallPermits = permits;
    }

    /**
     * Change the number of overall permits.
     * @param permits the new number of overall permits for this Semaphore.
     */
    public synchronized void setOverallPermits(int permits) {
        if (permits == overallPermits) {
            return;
        }
        log.trace("Changing permits from " + overallPermits + " to " + permits);
        if (permits > overallPermits) {
            release(permits - overallPermits);
        } else {
            reducePermits(overallPermits - permits);
        }
    }

    /**
     * @return the number of overall permits.
     */
    public int getOverallPermits() {
        return overallPermits;
    }
}
