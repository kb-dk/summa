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

import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.rmi.*;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A helper class utilizing a stateless connection to a storage service exposing
 * a {@link ReadableStorage} interface. Unless your needs are very advanced
 * or you must do manual connection management, this is by far the
 * easiest way to use a remote {@link ReadableStorage}.
 * <p></p>
 * This class is modelled as a {@link ConnectionConsumer} meaning that you can
 * tweak its behavior by changing the configuration parameters
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory#CONF_RETRIES},
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory#CONF_GRACE_TIME},
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory#CONF_FACTORY}, and
 * {@link ConnectionConsumer#CONF_RPC_TARGET}.
 * <p/>
 * The property {@link ConnectionConsumer#CONF_RPC_TARGET} <i>must</i> be
 * defined while all others are optional.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke, hbk")
public class StorageReaderClient extends ConnectionConsumer<ReadableStorage>
                                 implements Configurable, ReadableStorage {

    /**
     * Create a new storage reader. If no RPC vendor is defined in the
     * {@link #CONF_RPC_TARGET} property of {@code conf} then the reader will
     * default to {@code //localhost:28000/summa-storage}.
     *
     * @param conf configuration used to instantiate the underlying
     *             {@link ConnectionConsumer}.
     */
    public StorageReaderClient(Configuration conf) {
        super (conf, "//localhost:28000/summa-storage");
    }

    @Override
    public long getRecordsModifiedAfter(long time, String base,
                                        QueryOptions options)
                                                            throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.getRecordsModifiedAfter(time, base, options);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("getRecordsModifiedAfter("+time+", "+base
                                  +") failed: " + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    @Override
    public long getModificationTime (String base) throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.getModificationTime (base);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("getModificationTime("+base+") failed: "
                                  + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    @Override
    public List<Record> getRecords(List<String> ids, QueryOptions options)
                                                            throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.getRecords(ids, options);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("getRecords("+ Logs.expand(ids, 10)
                                  +", options="+ options + ") failed: "
                                  + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    @Override
    public Record getRecord(String id, QueryOptions options) throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.getRecord(id, options);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("getRecord("+id+", options="
                                  + options + ") failed: "
                                  + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    @Override
    public Record next(long iteratorKey) throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.next(iteratorKey);
        } catch (NoSuchElementException e) {
            // iterator depleted
            throw new NoSuchElementException();
        } catch (Throwable t) {
            // TODO: Consider should this be called if NoSuchElement on other site?
            connectionError(t);
            checkForNoSuchElementException(t);
            throw new IOException("next("+iteratorKey+") failed: "
                                  + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    /**
     * Helper method for recursively checking a nested exception stack, this is
     * because our RMI helper methods wraps a possible
     * {@link NoSuchElementException} into a {@link RemoteException} when
     * talking to a remote storage.
     * Note: this is a result of an API which throws
     * {@link NoSuchElementException} when a {@link Iterable} is depleted.
     *  
     * @param e An exception possible nested, which posibly contains a
     * {@link NoSuchElementException}.
     */
    private void checkForNoSuchElementException(Throwable e) {
        if(e instanceof RemoteException && e.getCause() != null) {
            if(e.getCause() instanceof NoSuchElementException) {
                throw (NoSuchElementException)e.getCause();
            }
            checkForNoSuchElementException(e.getCause());
        }
    }

    @Override
    public List<Record> next(long iteratorKey, int maxRecords)
                                                            throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.next(iteratorKey, maxRecords);
        } catch (NoSuchElementException e) {
            // iterator depleted
            throw new NoSuchElementException();
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("next("+iteratorKey+", "+maxRecords
                                  +") failed: " + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

}




