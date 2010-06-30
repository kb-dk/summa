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

import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.RMIConnectionFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.SummaRMIConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Helper class to create connections to remote services exposing a
 * {@link Storage} interface.</p>
 *
 * <p>The right way to use this class is to pass an instance of it
 * to a {@link ConnectionManager} and use that to manage the connections.
 * For example:
 * <pre>
 * ConnectionFactory&lt;Storage&gt; connFact = new StorageConnectionFactory (conf);
 * ConnectionManager&lt;Storage&gt; connMgr = new ConnectionManager (connFact);
 *
 * ConnectionContext<Storage> connCtx = connMgr.get ("//localhost:27000/summa-storage");
 * Storage storage = connCtx.getConnection();
 *
 * // do stuff with storage
 *
 * connMgr.release (connCtx);
 * </pre>
 * </p>
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public class StorageConnectionFactory extends ConnectionFactory<Storage>
                                      implements Configurable {

    /**
     * Configuration property specifying which {@link ConnectionFactory} class
     * to use for storage connections. Default is {@link RMIConnectionFactory}.
     */
    public static final String CONN_FACT_CLASS = "summa.storage.connectionfactory.class";

    private ConnectionFactory<? extends Storage> backend;
    //private ConnectionManager<Storage> connMgr;
    private Log log;

    /**
     * Instantiate a new factory based on the supplied configuration.
     * @param conf the configuration for the storage connection. See
     *             {@link #CONN_FACT_CLASS}.
     */
    @SuppressWarnings({"unchecked"})
    public StorageConnectionFactory(Configuration conf) {
        log = LogFactory.getLog(StorageConnectionFactory.class);

        /* Lookup which connection factory to use.
         * Default to an RMI factory */
        Class<? extends ConnectionFactory> backendClass =
                                conf.getClass(CONN_FACT_CLASS,
                                              ConnectionFactory.class,
                                              SummaRMIConnectionFactory.class);

        log.debug("Using backend connection factory '" + backendClass + "'");

        backend = Configuration.create(backendClass, conf);
        log.trace("Backend instantiated");
    }

    /**
     * Create a new factory instance with an empty configuration.
     * Ie. using the default configuration values throughout.
     */
    public StorageConnectionFactory () {
        this (Configuration.newMemoryBased());
    }

    public Storage createConnection(String connectionId) {
        log.trace("Creating connection to '" + connectionId + "' via backend "
                   + "'" + backend.getClass().getName() + "'");
        Object o = backend.createConnection(connectionId);
        log.trace("Got storage proxy class: " + o);
        return (Storage) o;
    }
}




