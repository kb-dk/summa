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
package dk.statsbiblioteket.summa.common.rpc;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;

/**
 * A {@link ConnectionFactory} creating a backend {@link ConnectionFactory}
 * dynamically from a {@link Configuration}. 
 */
public class GenericConnectionFactory<E> extends ConnectionFactory<E> {

    /**
     * Number of seconds in between retrying broken connections.
     * Default is 5 seconds.
     */
    public static final String CONF_GRACE_TIME = "summa.rpc.connections.gracetime";

    /**
     * Number of times to retry establishing broken connections.
     * Default is 5 times.
     */
    public static final String CONF_RETRIES = "summa.rpc.connections.retries";

    /**
     * Name of class to as backend {@link dk.statsbiblioteket.util.rpc.ConnectionFactory}. This class
     * must be a {@link dk.statsbiblioteket.summa.common.configuration.Configurable}. The default is
     * {@link dk.statsbiblioteket.summa.common.rpc.SummaRMIConnectionFactory}.
     */
    public static final String CONF_FACTORY = "summa.rpc.connections.factoryclass";

    private Log log;
    private ConnectionFactory<? extends E> backend;

    @SuppressWarnings("unchecked")
    public GenericConnectionFactory (Configuration conf) {
        super ();
        log = LogFactory.getLog (GenericConnectionFactory.class);

        Class<? extends ConnectionFactory> backendClass =
                            conf.getClass(CONF_FACTORY, ConnectionFactory.class,
                                          SummaRMIConnectionFactory.class);

        log.debug ("Found backend class " + backendClass.getName());

        // Suppressed unchecked assignment here
        backend = Configuration.create(backendClass, conf);

        log.trace ("Applying configuration on backend");
        setGraceTime(conf.getInt(CONF_GRACE_TIME, 5));
        setNumRetries(conf.getInt(CONF_RETRIES, 5));
        log.debug ("Configuration: gracetime=" + getGraceTime()
                 + ", and retries=" + getNumRetries());
    }

    public E createConnection(String s) {
        return backend.createConnection(s);
    }

    public int getGraceTime () {
        return backend.getGraceTime();
    }

    public void setGraceTime (int seconds) {
        backend.setGraceTime(seconds);
    }

    public int getNumRetries () {
        return backend.getNumRetries();
    }

    public void setNumRetries (int retries) {
        backend.setNumRetries(retries);
    }
}




