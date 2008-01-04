package dk.statsbiblioteket.summa.common.rpc;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;

/**
 * A {@link ConnectionFactory} creating a backend {@link ConnectionFactory}
 * dynamically from a {@link Configuration}. 
 */
public class GenericConnectionFactory<E> extends SummaConnectionFactory<E> {

    /**
     * Number of seconds in between retrying broken connections.
     * Default is 5 seconds.
     */
    public static final String GRACE_TIME = "summa.rpc.connections.graceTime";

    /**
     * Number of times to retry establishing broken connections.
     * Default is 5 times.
     */
    public static final String RETRIES = "summa.rpc.connections.retries";

    /**
     * Name of class to as backend {@link dk.statsbiblioteket.util.rpc.ConnectionFactory}. This class
     * must be a {@link dk.statsbiblioteket.summa.common.configuration.Configurable}. The default is
     * {@link dk.statsbiblioteket.summa.common.rpc.SummaRMIConnectionFactory}.
     */
    public static final String FACTORY = "summa.rpc.connections.factoryClass";

    private Log log;

    @SuppressWarnings("unchecked")
    public GenericConnectionFactory (Configuration conf) {
        super (conf);
        log = LogFactory.getLog (SummaConnectionFactory.class);

        graceTime = conf.getInt(GRACE_TIME, 5);
        connectionRetries = conf.getInt(RETRIES, 5);
        log.debug ("Configuration: gracetime=" + graceTime
                 + ", and retries=" + connectionRetries);

        Class<? extends SummaConnectionFactory> backendClass =
                            conf.getClass(FACTORY, SummaConnectionFactory.class,
                                          SummaRMIConnectionFactory.class);

        log.debug ("Found backend class " + backendClass.getName());

        // Suppressed unchecked assignment here
        backend = conf.create (backendClass);

        log.trace ("Applying configuration on backend");
        backend.setGraceTime(graceTime);
        backend.setNumRetries(connectionRetries);
    }
}
