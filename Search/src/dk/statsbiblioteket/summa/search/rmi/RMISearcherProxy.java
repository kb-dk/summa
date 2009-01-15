package dk.statsbiblioteket.summa.search.rmi;

import dk.statsbiblioteket.summa.search.SummaSearcherFactory;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.*;
import dk.statsbiblioteket.summa.search.api.rmi.RemoteSearcher;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link SummaSearcher} implementation exposing an RMI interface,
 * proxying all method calls to a backend {@code SummaSearcher}.
 */
public class RMISearcherProxy extends UnicastRemoteObject
                              implements RemoteSearcher {

    private static final Log log = LogFactory.getLog (RMISearcherProxy.class);

    /**
     * The class used for the searcher backend. If this is set it will be
     * written to {@link SummaSearcher#CONF_CLASS} before submitting
     * the configuration to {@link SummaSearcherFactory#createSearcher}.
     */
    public static final String CONF_BACKEND = "summa.searcher.rmi.backend";

    /**
     * Configuration property defining on which port the RMI registry can be
     * found or may be started. Default is 28000
     */
    public static final String CONF_REGISTRY_PORT =
                                             "summa.searcher.rmi.registry.port";

    /**
     * Configuration property defining the name under which the searcher should
     * run. Default is 'summa-searcher'.
     */
    public static final String CONF_SERVICE_NAME = "summa.searcher.rmi.name";

    public static final Class<? extends SummaSearcher> DEFAULT_BACKEND =
                                                        SummaSearcherImpl.class;

    private SummaSearcher backend;
    private String serviceName;
    private int registryPort;

    /**
     * Create a new searcher proxy. The configuration passed in must specify
     * {@link SummaSearcher#CONF_SERVICE_PORT} for the RMI service port to use,
     * as well as either {@link SummaSearcher#CONF_CLASS} or
     * {@link RMISearcherProxy#CONF_BACKEND} to define what backend searcher to
     * use.
     * <p></p>
     * The whole configuration will be copied before submission to the backend
     * implementation. Furthermore the value {@link #CONF_BACKEND} property will
     * be written into the {@link #CONF_CLASS} property of this new
     * configuration, before passing it to a {@link SummaSearcherFactory}.
     * <p></p>
     * If the value of {@link #CONF_CLASS} is
     * {@code dk.statsbiblioteket.summa.control.rmi.RMISearcherProxy} then this
     * class will avoid infinite recursion by forcing this property into
     * a {@link SummaSearcherImpl}.
     * @param conf
     * @throws RemoteException
     */
    public RMISearcherProxy (Configuration conf) throws IOException {
        super (getServicePort(conf));

        /* Create configuration for the backend, based on our own,
         * rewriting the class property if necessary */
        // FIXME: The below config should really be kept entirely in memory,
        //        but we can't use a memorybased config because of bug:
        //        https://gforge.statsbiblioteket.dk/tracker/index.php?func=detail&aid=1453&group_id=8&atid=109
        Configuration backendConf = new Configuration (new XStorage ());
        backendConf.importConfiguration (conf);
        if (conf.valueExists (CONF_BACKEND)) {
            backendConf.set (CONF_CLASS, conf.getString (CONF_BACKEND));
        } else {
            log.info (CONF_BACKEND + " not set, using " + DEFAULT_BACKEND + " for "
                      + "backend");
            backendConf.set (CONF_CLASS, DEFAULT_BACKEND);
        }

        /* If the backend is set to be another RMISeacherProxy then avoid
         * infinite recursion by forcing it into a SummaSearcherImpl */
        if (backendConf.valueExists (CONF_CLASS)) {
            if (this.getClass().getName().equals(
                                          backendConf.getString (CONF_CLASS))) {
                log.warn ("Backend set to RMISearcherProxy. Forcing backend " +
                          "class to " + DEFAULT_BACKEND.getName()
                          + " to avoid infinite recursion");
                backendConf.set (CONF_CLASS, DEFAULT_BACKEND.getName());
            }
        }

        if (log.isTraceEnabled ()) {
            log.trace ("Backend conf:\n" + backendConf.dumpString ());
        }

        log.trace ("Creating searcher backend");
        backend = SummaSearcherFactory.createSearcher (backendConf);
        log.trace ("Created searcher: " + backend.getClass().getName());

        serviceName = conf.getString (CONF_SERVICE_NAME, "summa-searcher");
        registryPort = conf.getInt(CONF_REGISTRY_PORT, 28000);
        
        RemoteHelper.exportRemoteInterface (this, registryPort, serviceName);

        try {
            RemoteHelper.exportMBean (this);
        } catch (Exception e) {
            String msg = "Error exporting MBean of '" + this
                         + "'. Going on without it: " + e.getMessage ();
            if (log.isTraceEnabled()) {
                log.warn (msg, e);
            } else {
                log.warn(msg);
            }
        }
    }

    private static int getServicePort (Configuration conf) {
        try {
            return conf.getInt(CONF_SERVICE_PORT);
        } catch (NullPointerException e) {
            log.warn ("Service port property " + CONF_SERVICE_PORT + " not "
                     + "defined in configuration. Falling back to anonymous "
                     + "port");
            return 0;
        }
    }

    @Override
    public ResponseCollection search(Request request) throws RemoteException {
        try {
            ResponseCollection resp = backend.search (request); 
            return resp;
        } catch (IOException e) {
            throw new RemoteException ("Search request failed: "
                                       + e.getMessage (), e);
        }
    }

    @Override
    public void close() throws RemoteException {
        try {
            RemoteHelper.unExportRemoteInterface (serviceName, registryPort);
        } catch (IOException e) {
            throw new RemoteException ("Failed to unexport RMI interface: "
                                       + e.getMessage (), e);
        }

        try {
            RemoteHelper.unExportMBean (this);
        } catch (IOException e) {
            // Don't bail out because of this...
            log.warn ("Failed to unexport MBean: " + e.getMessage (), e);
        }

        try {
            backend.close ();
        } catch (IOException e) {
            throw new RemoteException ("Close request failed: "
                                       + e.getMessage (), e);
        }
    }
    
}



