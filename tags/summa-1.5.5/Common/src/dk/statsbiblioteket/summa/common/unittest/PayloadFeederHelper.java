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
package dk.statsbiblioteket.summa.common.unittest;

import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Takes a list of Payloads in the constructor and delivers the Payloads when
 * requested. Used for testing.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class PayloadFeederHelper implements ObjectFilter {
    private static Log log = LogFactory.getLog(PayloadFeederHelper.class);

    private List<Payload> payloads;
    private int delay = 0;
    private Long lastDeliveryTime = 0L;
    
    public PayloadFeederHelper(List<Payload> payloads) {
        //noinspection DuplicateStringLiteralInspection
        log.debug("Creating feeder with " + payloads.size() + " Payloads");
        this.payloads = new ArrayList<Payload>(payloads.size());
        this.payloads.addAll(payloads);
    }

    public PayloadFeederHelper(
            List<Payload> payloads, int delayBetweenPayloads) {
        //noinspection DuplicateStringLiteralInspection
        log.debug("Creating feeder with " + payloads.size() + " Payloads");
        this.payloads = new ArrayList<Payload>(payloads.size());
        this.payloads.addAll(payloads);
        delay = delayBetweenPayloads;
    }

    public boolean hasNext() {
        return payloads.size() > 0;
    }

    public void setSource(Filter filter) {
        throw new UnsupportedOperationException("setSource not supported");
    }

    public boolean pump() throws IOException {
        return next() != null && hasNext();
    }

    public void close(boolean success) {
        payloads.clear();
    }

    public Payload next() {
        long sleepTime = lastDeliveryTime + delay - System.currentTimeMillis();
        if (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                log.debug("Interrupted while sleeping " + sleepTime + "ms");
            }
        }
        Payload payload = !hasNext() ? null : payloads.remove(0);
        if (log.isTraceEnabled()) {
            log.trace("Delivering " + payload);
        }
        lastDeliveryTime = System.currentTimeMillis();
        return payload;
    }

    public void remove() {
        //noinspection DuplicateStringLiteralInspection
        throw new UnsupportedOperationException("Remove not supported");
    }
}

