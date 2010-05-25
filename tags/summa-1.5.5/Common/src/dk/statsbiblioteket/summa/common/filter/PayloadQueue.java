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
package dk.statsbiblioteket.summa.common.filter;

import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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

    private AtomicLong totalSize = new AtomicLong(0);
    private long maxSize;
    /**
     * The flag is notified when elements are added or removed from the queue.
     */
    private final Object flag = new Object();

    /**
     * @param maxCount the maximum number of Payloads in the queue.
     * @param maxSize the maximum number of bytes in the queue.
     */
    public PayloadQueue(int maxCount, long maxSize) {
        super(maxCount, true);
        this.maxSize = maxSize;
        log.debug("Constructed PayloadQueue with max Payloads " + maxCount
                  + " and max bytes " + maxSize);
    }

    @Override
    public boolean offer(Payload payload) {
        long payloadSize = calculateSize(payload);
        if (payloadSize + totalSize.get() > maxSize) {
            return false;
        }
        if (super.offer(payload)) {
            totalSize.addAndGet(payloadSize);
            synchronized (flag) {
                flag.notifyAll();
                return true;
             }
        }
        return false;
    }

    /**
     * A put the queue cannot refuse.
     * @param payload the Payload to offer.
     */
    public void uninterruptablePut(Payload payload) {
        while (true) {
            try {
                put(payload);
                return;
            } catch (InterruptedException e) {
                log.warn(String.format(
                        "Interrupted while calling put(%s) from "
                        + "uninterruptiblePut. Retrying", payload), e);
            }
        }
    }



    @Override
    public void put(Payload payload) throws InterruptedException {
        long payloadSize = waitForRoom(payload);
        super.put(payload);
        totalSize.addAndGet(payloadSize);
        synchronized (flag) {
             flag.notifyAll();
         }
    }

    // TODO: Change implementation of waitforRoom to support timeouts
    @Override
    public boolean offer(Payload payload, long timeout, TimeUnit unit)
                                                   throws InterruptedException {
        long payloadSize = waitForRoom(payload);
        if (super.offer(payload, timeout, unit)) {
            totalSize.addAndGet(payloadSize);
            synchronized (flag) {
                 flag.notifyAll();
                return true;
             }
        }
        return false;
    }

    @Override
    public boolean add(Payload payload) {
        // Add is a wrapper for offer, so don't update totalSize or flag
        return super.add(payload);
    }

    @Override
    public Payload poll() {
        Payload result = super.poll();
        if (result != null) {
            totalSize.addAndGet(-1 * calculateSize(result));
            synchronized (flag) {
                 flag.notifyAll();
             }
        }
        return result;
    }

    @Override
    public Payload take() throws InterruptedException {
        Payload result = super.take();
        totalSize.addAndGet(-1 * calculateSize(result));
        synchronized (flag) {
            flag.notifyAll();
            return result;
        }
    }

    /**
     * Block until a Payload is available on the queue, then take it and return. 
     * @return the next element in the queue.
     */
    public Payload uninterruptibleTake() {
        while (true) {
            try {
                Payload result = take();
                synchronized (flag) {
                     flag.notifyAll();
                    return result;
                 }
            } catch (InterruptedException e) {
                log.warn("Got InterruptedException while taking in "
                         + "uninterruptibleTake. Retrying", e);
            }
        }
    }

    @Override
    public boolean remove(Object o) {
        boolean success = super.remove(o);
        if (success && o instanceof Payload) {
            totalSize.addAndGet(-1 * calculateSize((Payload)o));
            synchronized (flag) {
                 flag.notifyAll();
             }
        }
        return success;
    }

    @Override
    public void clear() {
        super.clear();
        totalSize.set(0);
        synchronized (flag) {
             flag.notifyAll();
         }
    }

    @Override
    public Payload poll(long timeout, TimeUnit unit)
                                                   throws InterruptedException {
        Payload result = super.poll(timeout, unit);
        if (result != null) {
            totalSize.addAndGet(-1 * calculateSize(result));
            synchronized (flag) {
                flag.notifyAll();
            }
        }
        return result;
    }

    @Override
    public int drainTo(Collection<? super Payload> c) {
        int count = super.drainTo(c);
        totalSize.set(0);
        synchronized (flag) {
            flag.notifyAll();
            return count;
         }
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
    public long waitForRoom(Payload payload) {
        long size = calculateSize(payload);
        synchronized(flag) {
            while (size() != 0
                   && (remainingCapacity() == 0
                       || size + totalSize.get() > maxSize)) {
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

    /**
     * Waits until theres is at least one entry or an interrupt is called.
     * @throws InterruptedException if an interrupt is called.
     */
    public void waitForEntry() throws InterruptedException {
        synchronized(flag) {
            while (size() == 0) {
                flag.wait();
            }
        }
    }

    /**
     * Waits until there is at least one entry.
     */
    public void uninterruptibleWaitForEntry() {
        synchronized(flag) {
            while (size() == 0) {
                try {
                    flag.wait();
                } catch (InterruptedException e) {
                    log.trace("uninterruptibleWaitForEntry caught interrupt. "
                              + "Continuing");
                }
            }
        }
    }

    private long calculateSize(Payload payload) {
        return payload.getRecord() == null ? 200
               : RecordUtil.calculateRecordSize(payload.getRecord(), true);
    }
}

