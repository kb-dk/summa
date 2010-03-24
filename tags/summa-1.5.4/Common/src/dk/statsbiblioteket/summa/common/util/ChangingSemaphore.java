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




