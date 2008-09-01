package dk.statsbiblioteket.summa.common.rpc;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

/**
 * A generic base class for application wanting to consume a single connection
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
 *         } finally {
 *             releaseConnection ();
 *         }
 *     }
 *
 * }
 * </pre>
 */
public class ConnectionConsumer<E> implements Configurable {

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
    public static final String PROP_RPC_TARGET = "summa.rpc.vendor";

    private ConnectionManager<E> connMan;
    private ConnectionContext<E> conn;
    private String connId;

    /**
     * Instantiate a new {@code ConnectionConsumer} based on {@code conf}.
     * <p></p>
     * Note that {@code conf} <i>must</i> define the
     * {@link #PROP_RPC_TARGET} property.
     * <p></p>
     * The {@code ConnectionConsumer} utilizes a {@link GenericConnectionFactory}
     * underneath so the configuration may override any of the standard
     * properties for this class to customize the behavior of the connection
     * consumer. These include
     * {@link GenericConnectionFactory#RETRIES},
     * {@link GenericConnectionFactory#GRACE_TIME},
     * and {@link GenericConnectionFactory#FACTORY}.
     *
     * @param conf configuration used to instantiate the connection consumer
     */
    public ConnectionConsumer (Configuration conf) {
        ConnectionFactory<E> connFact = new GenericConnectionFactory<E> (conf);
        connMan = new ConnectionManager<E>(connFact);
        try {
            connId = conf.getString(PROP_RPC_TARGET);
        } catch (NullPointerException e) {
            throw new ConfigurationException(PROP_RPC_TARGET + " not set. No"
                                             + "RPC vendor");
        }
        conn = null;
    }

    /**
     * Look up a connection. When done using the connection it is the callers
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
        if (conn == null) {
            conn = connMan.get (connId);
        }

        if (conn == null) {
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
     * {@link #PROP_RPC_TARGET} property.
     * @return the value of the {@link #PROP_RPC_TARGET} from the configuration
     *         passed to the constructor
     */
    public String getVendorId () {
        return connId;
    }
}
