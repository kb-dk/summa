package dk.statsbiblioteket.summa.score.client;

import dk.statsbiblioteket.summa.score.api.Service;
import dk.statsbiblioteket.summa.score.api.ClientConnection;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.util.HashMap;
import java.util.Collection;
import java.io.File;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.net.MalformedURLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Helper class to maintain a collection of a {@link Service} connections.
 * Connections are created on request and are kept around for a predefined
 * amount of time settable vi {@link #setLingerTime}.</p>
 *
 * <p>One of the core benefits of using this class is that it allows
 * the Client to use stateless connections - ie not fail if the
 * service crashes and and comes back up by "magical" means. Failure
 * handling is provided free of charge.</p>
 *
 * </p>Intended for use inside a {@link Client}. Consumer of this class
 * are primarly only interested in the {@link #get} and {@link #release}
 * methods.</p>
 */
public class ServiceConnectionManager implements Configurable {

    private int lingerTime;
    private HashMap<String,ServiceContext> services;
    private String serviceDir;
    private String clientName;
    private int registryPort;
    private int connectionRetries;
    private int graceTime;
    private Log log;

    /**
     * Property defining how long unused connections should be kept around,
     * measured in seconds. Default is 10. 
     */
    public static final String LINGER_TIME =
                                        "summa.score.client.connections.linger";

    /**
     * Property defining how seconds we should wait in between retrying
     * establishing connections to the services. Default 5.
     */
    public static final String GRACE_TIME =
                                        "summa.score.client.connections.graceTime";

    /**
     * Property defining how many retries we should try on each service
     * connection before giving up. Default 5. 
     */
    public static final String CONNECTION_RETRIES =
                                        "summa.score.client.connections.retries";

    /**
     * A thread safe reference counted object wrapping a Service.
     */
    public class ServiceContext {

        private Service service;
        private int refCount;
        private long lastUse;
        private String instanceId;

        public ServiceContext (Service service, String instanceId) {
            this.service = service;
            this.refCount = 0;
            this.lastUse = System.currentTimeMillis();
            this.instanceId = instanceId;
        }

        public synchronized void ref () {
            refCount++;
            lastUse = System.currentTimeMillis();
        }

        public synchronized void unref () {
            refCount--;
            lastUse = System.currentTimeMillis();
        }

        public String getInstanceId () {
            return instanceId;
        }

        public synchronized int getRefCount () {
            return refCount;
        }

        public synchronized long getLastUse () {
            return lastUse;
        }

        public synchronized Service getService () {
            lastUse = System.currentTimeMillis();
            return service;
        }

    }

    private class ConnectionMonitor implements Runnable {

        private ServiceConnectionManager owner;
        private Log log;
        private boolean mayRun;

        public ConnectionMonitor (ServiceConnectionManager owner) {
            this.owner = owner;
            this.log = LogFactory.getLog(ConnectionMonitor.class);
            this.mayRun = true;
        }

        public synchronized void stop () {
            mayRun = false;
            this.notify();
        }

        public void run() {
            while (mayRun) {
                try {
                    Thread.sleep (owner.getLingerTime());
                } catch (InterruptedException e) {
                    log.warn ("Interrupted. Forcing connection scan.");
                }

                long now = System.currentTimeMillis();

                for (ServiceContext ctx : owner.getConnections()) {
                    if (now - ctx.getLastUse() > owner.getLingerTime() &&
                        ctx.getRefCount() == 0) {
                        owner.purge (ctx.getInstanceId());
                    }
                }

            }

        }
    }

    /**
     * Create a new ServiceConnectionManager. You should pass the
     * {@link Configuration} used for the {@link Client} to this
     * constructor.
     * @param conf Same configuration object as used by the client owning this
     *             connection manager
     */
    public ServiceConnectionManager (Configuration conf) {
        log = LogFactory.getLog (ServiceConnectionManager.class);

        clientName = conf.getString(ClientConnection.CLIENT_ID);
        registryPort = conf.getInt(ClientConnection.REGISTRY_PORT);
        serviceDir = System.getProperty("user.home") + File.separator
                                     + conf.getString(ClientConnection.CLIENT_BASEPATH)
                                     + File.separator + clientName;

        setLingerTime(conf.getInt(LINGER_TIME, 10));
        graceTime = conf.getInt(GRACE_TIME, 5);
        connectionRetries = conf.getInt(LINGER_TIME, 5);
    }

    private String getServiceAddress (String instanceId) {
        // FIXME: We should really use a BundleLoader to get the correct port for the service's registry - current impl assumes that client and service share the same registry
        return "//localhost:" + registryPort + "/" + instanceId;
    }

    private Service createConnection (String instanceId) {
        String address = getServiceAddress(instanceId);
        int retries = 0;
        Exception lastError = null;

        for (retries = 0; retries < connectionRetries; retries++) {
            log.debug ("Looking up '" + address + "'");
            try {
                Thread.sleep(graceTime*1000);
                return (Service) Naming.lookup(address);
            } catch (MalformedURLException e) {
                lastError = e;
            } catch (NotBoundException e) {
                lastError = e;
            } catch (RemoteException e) {
                lastError = e;
            } catch (InterruptedException e) {
                log.error ("Interrupted. Aborting connection creation.");
                break;
            }
        }
        log.error("Failed to look up service on '" + address + "'. "
                 + "Last error was:", lastError);
        return null;
    }

    public void setLingerTime (int seconds) {
        lingerTime = seconds*1000;
    }

    public int getLingerTime () {
        return lingerTime/1000;
    }

    public Collection<ServiceContext> getConnections () {
        return services.values();
    }

    public synchronized void purge (String instanceId) {
        ServiceContext ctx = services.get (instanceId);

        if (ctx == null) {
            log.warn ("Cannot purge unknown service '" + instanceId + "'");
            return;
        } else if (ctx.getRefCount() > 0) {
            log.warn("Ignoring request to purge '" + instanceId + "'"
                     + " with positive refCount " + ctx.getRefCount());
            return;
        }

        log.debug ("Purging service connection '" + instanceId + "'");
        services.remove (instanceId);
    }

    /**
     * Use this method to obtain a connection to the service with id
     * {@code instanceId}. Make sure you call {@link #release} on the
     * instance id when you are done using the connection.
     * @param instanceId instance id of the service to get a connection for
     * @return a connection to a service
     */
    public synchronized Service get (String instanceId) {
        ServiceContext ctx = services.get (instanceId);

        if (ctx == null) {
            log.debug ("No connection to '" + instanceId + "' in cache");
            Service service = createConnection(instanceId);
            ctx = new ServiceContext(service, instanceId);
            log.trace ("Adding new context for '" + instanceId + "' to cache");
            services.put (instanceId, ctx);
        } else {
            log.debug ("Found connection to '" + instanceId + "' in cache");
        }
        ctx.ref();
        return ctx.getService();
    }

    /**
     * Any call to {@link #get} should be followed by a matching call to
     * this method. It is equivalent to remembering closing your file
     * descriptors.
     * @param instanceId instance id of the service you wish to release your
     *                  connection for
     */
    public synchronized void release (String instanceId) {
        ServiceContext ctx = services.get (instanceId);

        if (ctx == null) {
            log.warn ("Can't release unknown service '" + instanceId + "'");
            return;
        }

        ctx.unref();
    }

}
