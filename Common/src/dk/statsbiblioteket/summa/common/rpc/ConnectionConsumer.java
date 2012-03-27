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

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A generic base class for applications wanting to consume a single connection
 * managed by a {@link ConnectionManager}.
 * <p></p>
 * Example illustrating a class to fetch the message of the day as broadcasted
 * by some message-of-the-day server<br/>
 * <pre>
 * class MessageClient extends ConnectionConsumer&lt;MessageConnection&gt;
 *                     implements Configurable {
 *
 *     public MessageClient (Configuration conf) {
 *         super (conf);
 *     }
 *
 *     public String getMessageOfTheDay () {
 *         MessageConnection msg = getConnection();
 *
 *         if (msg == null) {
 *             throw new ConnectException ("Failed to connect to "
 *                                         + getVendorId());
 *         }
 *
 *         try {
 *             return msg.getMessageOfTheDay ();
 *         } catch (Throwable t) {
 *             connectionError (t);
 *             throw new IOException ("Failed to read MOTD: " + t.getMessage(),
 *                                    t);
 *         } finally {
 *             releaseConnection ();
 *         }
 *     }
 *
 * }
 * </pre>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public class ConnectionConsumer<E> implements Configurable {
    private static Log log = LogFactory.getLog(ConnectionConsumer.class);

    /**
     * Configuration property defining the address the host providing the
     * RPC service resides on.
     * <p></p>
     * For RMI connections this will be on the form
     * {@code //myhost:2768/myService}. Other RPC transports will use different
     * vendor schemes.
     * <p></p>
     * This property <i>must</i> must be set in the {@link Configuration}
     * passed to the constructor {@link #ConnectionConsumer(Configuration)}.
     */
    public static final String CONF_RPC_TARGET = "summa.rpc.vendor";

    private ConnectionManager<E> connMan;
    private ConnectionContext<E> conn;
    private String connId;

    /**
     * Instantiate a new {@code ConnectionConsumer} based on {@code conf}.
     * <p></p>
     * Note that {@code conf} <i>must</i> define the
     * {@link #CONF_RPC_TARGET} property.
     * <p></p>
     * The {@code ConnectionConsumer} utilizes a {@link GenericConnectionFactory}
     * underneath so the configuration may override any of the standard
     * properties for this class to customize the behavior of the connection
     * consumer. These include
     * {@link GenericConnectionFactory#CONF_RETRIES},
     * {@link GenericConnectionFactory#CONF_GRACE_TIME},
     * and {@link GenericConnectionFactory#CONF_FACTORY}.
     *
     * @param conf configuration used to instantiate the connection consumer
     */
    public ConnectionConsumer (Configuration conf) {
        ConnectionFactory<E> connFact = new GenericConnectionFactory<E> (conf);
        connMan = new ConnectionManager<E>(connFact);
        try {
            connId = conf.getString(CONF_RPC_TARGET);
        } catch (NullPointerException e) {
            throw new ConfigurationException(String.format(
                    "%s not set. No RPC vendor", CONF_RPC_TARGET));
        }
        conn = null;
        //noinspection DuplicateStringLiteralInspection
        log.debug(String.format("Created ConnectionConsumer '%s' for %s",
                                this, connId));
    }

    /**
     * Like {@link #ConnectionConsumer(Configuration)}, but use
     * {@code defaultVendor} if {@link #CONF_RPC_TARGET} is not set in
     * {@code conf}.
     * <p></p>
     * The {@code ConnectionConsumer} utilizes a {@link GenericConnectionFactory}
     * underneath so the configuration may override any of the standard
     * properties for this class to customize the behavior of the connection
     * consumer. These include
     * {@link GenericConnectionFactory#CONF_RETRIES},
     * {@link GenericConnectionFactory#CONF_GRACE_TIME},
     * and {@link GenericConnectionFactory#CONF_FACTORY}.
     *
     * @param conf configuration used to instantiate the connection consumer
     * @param defaultVendor the RPC vendor to use as fallback
     */
    public ConnectionConsumer (Configuration conf, String defaultVendor) {
        ConnectionFactory<E> connFact = new GenericConnectionFactory<E> (conf);
        connMan = new ConnectionManager<E>(connFact);
        connId = conf.getString(CONF_RPC_TARGET, defaultVendor);
        conn = null;
        //noinspection DuplicateStringLiteralInspection
        log.debug(String.format("Created ConnectionConsumer '%s' for %s",
                                this, connId));
    }

    /**
     * Look up a connection. When done using the connection it is the caller's
     * duty to release it again using {@link #releaseConnection()}. This is
     * typically done in a {@code finally} clause.
     * <p></p>
     * Note that in case of errors it is advised to call {@link #connectionError}
     * to notify the underlying connection caches that the connection should be
     * purged and re-established. 
     *
     * @return an interface proxy for a {@code E} or {@code null} if the
     *         connection can not be established
     */
    public synchronized E getConnection () {
        if (log.isTraceEnabled()) {
            log.trace("getConnection called for " + connId);
        }
        if (conn == null) {
            conn = connMan.get(connId);
        }

        if (conn == null) {
            log.debug("getConnection: ConnectionContext is null for id "
                      + connId);
            return null;
        }

        return conn.getConnection();
    }

    /**
     * Used when done using a connection obtained from {@link #getConnection()}.
     * Failing to call this method may cause leaks of connection handles.
     * <p></p>
     * Note that in case of errors it is advised to call {@link #connectionError}
     * to notify the underlying connection caches that the connection should be
     * purged and re-established.
     */
    public synchronized void releaseConnection () {
        if (conn == null) {
            return;
        }

        connMan.release(conn);
        conn = null;
    }

    /**
     * Report to the underlying connection manager that the connection is likely
     * to be broken and should be purged from the cache. The typical usage of
     * this method is have a very broad {@code catch} block around your calls
     * into the {@code E} proxy and call this method in case of an exception.
     *
     * @param cause the throwable causing the problem
     * @see #connectionError(String)
     */
    public synchronized void connectionError (Throwable cause) {
        if (conn == null) {
            return;
        }

        connMan.reportError(conn, cause);

        conn = null;
    }

    /**
     * Report to the underlying connection manager that the connection is likely
     * to be broken and should be purged from the cache. The typical usage of
     * this method is have a very broad {@code catch} block around your calls
     * into the {@code E} proxy and call this method in case of an exception.
     *
     * @param cause the throwable causing the problem
     * @see #connectionError(Throwable)
     */
    public synchronized void connectionError (String cause) {
        if (conn == null) {
            return;
        }

        connMan.reportError(conn, cause);

        conn = null;
    }

    /**
     * Return the vendor id as defined by the configuration's
     * {@link #CONF_RPC_TARGET} property.
     * @return the value of the {@link #CONF_RPC_TARGET} from the configuration
     *         passed to the constructor
     */
    public String getVendorId () {
        return connId;
    }
}

