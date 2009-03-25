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
package dk.statsbiblioteket.summa.common.filter;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.Collection;

/**
 * A queue tailored for Payloads, where the maximum queue size can be defined
 * by either count of (estimated) size of the Payloads.
 * </p><p>
 * The queue is blocking, thread-safe and fair.
 * </p><p>
 * Note: remainingCapacity is a maximum, as the sizes of Payloads are not known
 * before they are added.
 */
@QAInfo(level = QAInfo.Level.PEDANTIC,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment = "The hard part about this is to ensure that totalSize is"
                  + "true under all conditions")
public class PayloadQueue extends ArrayBlockingQueue<Payload> {
    private static Log log = LogFactory.getLog(PayloadQueue.class);

    private long totalSize = 0;
    private long maxSize;
    /**
     * The flag is notified when elements are removed from the queue.
     */
    private final Object flag = new Object();

    /**
     * @param maxCount the maximum number of Payloads in the queue.
     * @param maxSize the maximum number of bytes in the queue.
     */
    public PayloadQueue(int maxCount, long maxSize) {
        super(maxCount, true);
        this.maxSize = maxSize;
    }

    @Override
    public synchronized boolean offer(Payload payload) {
        long payloadSize = calculateSize(payload);
        if (payloadSize + totalSize > maxSize) {
            return false;
        }
        if (super.offer(payload)) {
            totalSize += payloadSize;
            return true;
        }
        return false;
    }

    @Override
    public synchronized void put(Payload payload) throws InterruptedException {
        long payloadSize = waitForRoom(payload);
        super.put(payload);
        totalSize += payloadSize;
    }

    // TODO: Change implementation of waitforRoom to support timeouts
    @Override
    public synchronized boolean offer(Payload payload, long timeout,
                                      TimeUnit unit)
                                                   throws InterruptedException {
        long payloadSize = waitForRoom(payload);
        if (super.offer(payload, timeout, unit)) {
            totalSize += payloadSize;
            return true;
        }
        return false;
    }

    @Override
    public boolean add(Payload payload) {
        // Add is a wrapper for offer, so don't update totalSize
        return super.add(payload);
    }

    @Override
    public synchronized Payload poll() {
        Payload result = super.poll();
        if (result != null) {
            totalSize -= calculateSize(result);
            flag.notifyAll();
        }
        return result;
    }

    @Override
    public synchronized Payload take() throws InterruptedException {
        Payload result = super.take();
        totalSize -= calculateSize(result);
        synchronized (flag) {
             flag.notifyAll();
         }
        return result;
    }

    @Override
    public boolean remove(Object o) {
        boolean success = super.remove(o);
        if (success && o instanceof Payload) {
            totalSize -= calculateSize((Payload)o);
            synchronized (flag) {
                 flag.notifyAll();
             }
        }
        return success;
    }

    @Override
    public synchronized void clear() {
        super.clear();
        totalSize = 0;
        synchronized (flag) {
             flag.notifyAll();
         }
    }

    @Override
    public synchronized Payload poll(long timeout, TimeUnit unit)
                                                   throws InterruptedException {
        Payload result = super.poll(timeout, unit);
        if (result != null) {
            totalSize -= calculateSize(result);
            synchronized (flag) {
                flag.notifyAll();
            }
        }
        return result;
    }

    @Override
    public synchronized int drainTo(Collection<? super Payload> c) {
        int count = super.drainTo(c);
        totalSize = 0;
        synchronized (flag) {
             flag.notifyAll();
         }
        return count;
    }

    @Override
    public int drainTo(Collection<? super Payload> c, int maxElements) {
        throw new UnsupportedOperationException(
                "drainTo with max not supported yet");
/*        int count = super.drainTo(c, maxElements);    // TODO: Implement this
        flag.notifyAll();
        return count;*/
    }

    /**
     * Blocks until there is room in the queue for the payload.
     * @param payload estimated byte size if extracted from this payload.
     * @return the extimated size in bytes of the Payload to insert.
     */
    public synchronized long waitForRoom(Payload payload) {
        long size = calculateSize(payload);
        synchronized(flag) {
            while (size() != 0
                   && (remainingCapacity() == 0
                       || size + totalSize > maxSize)) {
                try {
                    flag.wait();
                } catch (InterruptedException e) {
                    //noinspection DuplicateStringLiteralInspection
                    log.debug("Was interrupted while waiting for flag."
                              + " Retrying", e);
                }
            }
        }
        return size;
    }

    // TODO: Make proper estimations, not just loose guesses
    private long calculateSize(Payload payload) {
        long BASE = 50;
        return BASE +
               (!payload.hasData() ? 0 : 1000) +
               (payload.getRecord() == null ? 0 :
                payload.getRecord().getContent().length)
               + (!payload.getRecord().hasMeta() ? 0 : 1000);
    }
}

