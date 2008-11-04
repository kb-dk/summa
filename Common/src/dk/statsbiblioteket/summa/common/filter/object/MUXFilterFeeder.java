/* $Id$
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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

/**
 * Helper class for {@link MUXFilter} that handles feeding of the right Payloads
 * into Filters.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MUXFilterFeeder implements ObjectFilter {
    private static Log log = LogFactory.getLog(MUXFilterFeeder.class);

    /**
     * The length of the queue. This can be used to create a FilterProxy.
     * </p><p>
     * This property is optional. Default is 1.
     */
    public static final String CONF_QUEUE_LENGTH =
            "summa.muxfilter.feeder.queuelength";
    public static final int DEFAULT_QUEUE_LENGTH = 1;

    private static final Payload STOP =
            new Payload(new Record("EOF", "Dummy", new byte[0]));

    private boolean eofReached = false;
    private ArrayBlockingQueue<Payload> queue;

    public MUXFilterFeeder(Configuration conf) {
        log.trace("Constructing MUXFilterFeeder");
        queue = new ArrayBlockingQueue<Payload>(
                conf.getInt(CONF_QUEUE_LENGTH, DEFAULT_QUEUE_LENGTH), true);

    }

    private Filter createFilter(Configuration configuration) {
        Class<? extends Filter> filter = configuration.getClass(
                MUXFilter.CONF_FILTER_CLASS, Filter.class);
        log.debug("Got filter class " + filter + ". Commencing creation");
        return Configuration.create(filter, configuration);
    }

    /**
     * Add a Payload to the queue, blocking until the queue accepts it.
     * @param payload the Payload to add.
     * @throws InterruptedException if an interruption was received while
     *                              offering payload to the queue.
     */
    public void queuePayload(Payload payload) throws InterruptedException {
        if (log.isTraceEnabled()) {
            log.trace("Queueing " + payload);
        }
        queue.offer(payload, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    /**
     * Signal that no more Payloads will be added.
     */
    public void signalEOF() {
        log.debug("signalEOF() entered");
        eofReached = true;
        while (true) {
            try {
                queue.offer(STOP, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
                log.trace("signalEOF() completed");
                return;
            } catch (InterruptedException e) {
                log.warn("Interrupted while adding EOF-signal to queue. "
                         + "Retrying", e);
            }
        }
    }

    /**
     * @return the number of Payloads currently in the queue.
     */
    public int getQueueSize() {
        return queue.size();
    }

    public boolean hasNext() {
        while (true) {
            try {
                return !eofReached &&
                       queue.poll(Integer.MAX_VALUE, TimeUnit.MILLISECONDS) != 
                       STOP;
                // TODO: Not poll, but busy peek?
            } catch (InterruptedException e) {
                log.warn("Interrupted while polling for next Payload.. "
                         + "Retrying", e);
            }
        }
    }

    public void setSource(Filter filter) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean pump() throws IOException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close(boolean success) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Payload next() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void remove() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
