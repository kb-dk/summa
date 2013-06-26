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
package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Convenience iterator implementation masking the manual storage interactions
 * of the key-based iteration of the {@link ReadableStorage#next} methods.
 * <p/>
 * This class takes care to optimize the network overhead by only requesting
 * batches of records via {@link ReadableStorage#next(long, int)} instead of
 * fetching them one by one.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "mke")
public class StorageIterator implements Iterator<Record>, Serializable {
    private static final long serialVersionUID = 186584L;
    // TODO: Consider making this adjustable
    public static final int DEFAULT_MAX_QUEUE_SIZE = 100;

    private int maxQueueSize;
    private final ReadableStorage iteratorHolder;
    private final long key;
    private final Queue<Record> records;
    private boolean next;
    private long totalReceived = 0;

    private Log log;

    /**
     * Create an iterator on a given storage and iteration key (as returned
     * by one of the getters on the {@link ReadableStorage} interface)
     *
     * @param iteratorHolder the storage holding the iterator key {@code key}
     * @param key the iteration key as returned from the {@link ReadableStorage}
     */
    public StorageIterator(ReadableStorage iteratorHolder, long key) {
        this(iteratorHolder, key, DEFAULT_MAX_QUEUE_SIZE);
    }

    /**
     * Create an iterator on a given storage and iteration key (as returned
     * by one of the getters on the {@link ReadableStorage} interface)
     *
     * @param iteratorHolder the storage holding the iterator key {@code key}
     * @param key the iteration key as returned from the {@link ReadableStorage}
     * @param maxBufferSize maximum number of records to prefetch
     */
    public StorageIterator(ReadableStorage iteratorHolder, long key, int maxBufferSize) {
        log = LogFactory.getLog (this.getClass().getName());
        this.iteratorHolder = iteratorHolder;
        this.key = key;
        this.next = true;
        records = new LinkedBlockingQueue<Record>(maxBufferSize);
        maxQueueSize = maxBufferSize;
        if(log.isTraceEnabled()) {
            log.trace("Created StorageIterator(" + iteratorHolder + ", " + key + ", " + maxBufferSize + ")");
        }
    }

    @Override
    public boolean hasNext() {
        try {
            checkRecords();
        } catch (IOException e) {
            log.warn ("Failed to retrieve records: " + e.getMessage(), e);
            next = false;
        }

        return next || !records.isEmpty();
    }

    @Override
    public Record next() {
        if (!hasNext()) {
            throw new NoSuchElementException ("Depleted");
        }
        return records.poll();
    }

    /**
     * Not supported
     * @throws UnsupportedOperationException the remove operation is not supported by this Iterator
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Download next batch of records if applicable and set the 'next' state appropriately
     *
     * @throws IOException if something went wrong when checking records.
     */
    private void checkRecords () throws IOException {
        if (records.isEmpty() && next) {
            try {
                long startTime = System.currentTimeMillis();
                List<Record> recs = iteratorHolder.next(key, maxQueueSize);
                totalReceived += recs.size();
                log.debug("Received " + recs.size() + " Records (" + maxQueueSize + " requested, "+ totalReceived
                          + " received in total) in " + (System.currentTimeMillis()-startTime) + "ms");
                if (recs.size() < maxQueueSize) {
                    next = false;
                }
                records.addAll(recs);
                if (records.isEmpty()) {
                    log.info("Received 0 records from iteratorHolder, but no NoSuchElementException");
                }
            } catch (Exception e) { // Often this is a java.rmi.ServerException indirectly wrapping NoSuchElementEx...
                try {
                    Throwable sub = e;
                    while (sub != null) {
                        if (sub instanceof NoSuchElementException) {
                            log.info("Got NoSuchElementException, which signals no more Records. "
                                     + "Received a total of " + totalReceived + " Records");
                            next = false;
                            return;
                        }
                        sub = sub.getCause();
                    }
                    throw new RemoteException("Received Exception that did not have a NoSuchElementException "
                                              + "in the causes chain", e);
                } catch (Exception e2) {
                    throw new RemoteException("Exception triggered while searching for NoSuchElementException "
                                              + "cause in " + e, e2);
                }

            }
        }
    }
}
