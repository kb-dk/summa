package dk.statsbiblioteket.summa.search.rmi;

import dk.statsbiblioteket.summa.search.*;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
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
     * written to {@link SummaSearcher#PROP_CLASS} before submitting
     * the configuration to {@link SummaSearcherFactory#createSearcher}.
     */
    public static final String PROP_BACKEND = "summa.searcher.rmi.backend";

    /**
     * Configuration property defining on which port the RMI registry can be
     * found or may be started. Default is 28000
     */
    public static final String PROP_REGISTRY_PORT =
                                             "summa.searcher.rmi.registry.port";

    /**
     * Configuration property defining the name under which the searcher should
     * run. Default is 'summa-searcher'.
     */
    public static final String PROP_SERVICE_NAME = "summa.searcher.rmi.name";

    public static final Class<? extends SummaSearcher> DEFAULT_BACKEND_CLASS =
                                                        SummaSearcherImpl.class;

    private SummaSearcher backend;
    private String serviceName;
    private int registryPort;

    /**
     * Create a new searcher proxy. The configuration passed in must specify
     * {@link SummaSearcher#PROP_SERVICE_PORT} for the RMI service port to use,
     * as well as either {@link SummaSearcher#PROP_CLASS} or
     * {@link RMISearcherProxy#PROP_BACKEND} to define what backend searcher to
     * use.
     * <p></p>
     * The whole configuration will be copied before submission to the backend
     * implementation. Furthermore the value {@link #PROP_BACKEND} property will
     * be written into the {@link #PROP_CLASS} property of this new
     * configuration, before passing it to a {@link SummaSearcherFactory}.
     * <p></p>
     * If the value of {@link #PROP_CLASS} is
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
        Configuration backendConf = Configuration.newMemoryBased ();
        backendConf.importConfiguration (conf);
        if (conf.valueExists (PROP_BACKEND)) {
            backendConf.set (PROP_CLASS, conf.getString (PROP_BACKEND));
        } else {
            log.info (PROP_BACKEND + " not set, using " + PROP_CLASS + " for "
                      + "backend");
        }

        /* If the backend is set to be another RMISeacherProxy then avoid
         * infinite recursion by forcing it into a SummaSearcherImpl */
        if (backendConf.valueExists (PROP_CLASS)) {
            if (this.getClass().getName().equals(
                                          backendConf.getString (PROP_CLASS))) {
                log.warn ("Backend set to RMISearcherProxy. Forcing backend " +
                          "class to " + DEFAULT_BACKEND_CLASS.getName()
                          + " to avoid infinite recursion");
                backendConf.set (PROP_CLASS, DEFAULT_BACKEND_CLASS.getName());
            }
        }

        if (log.isTraceEnabled ()) {
            log.trace ("Backend conf:\n" + backendConf.dumpString ());
        }

        log.trace ("Creating searcher backend");
        backend = SummaSearcherFactory.createSearcher (backendConf);
        log.trace ("Created searcher: " + backend.getClass().getName());

        serviceName = conf.getString (PROP_SERVICE_NAME, "summa-searcher");
        registryPort = conf.getInt(PROP_REGISTRY_PORT, 28000);
        
        RemoteHelper.exportRemoteInterface (this, registryPort, serviceName);

        try {
            RemoteHelper.exportMBean (this);
        } catch (Exception e) {
            log.warn ("Error exporting MBean of '" + this
                      + "'. Going on without it: " + e.getMessage (), e);
        }
    }

    private static int getServicePort (Configuration conf) {
        return (conf.getInt(PROP_SERVICE_PORT, 28020));
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
