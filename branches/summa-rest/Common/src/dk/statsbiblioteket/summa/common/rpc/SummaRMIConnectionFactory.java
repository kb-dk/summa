package dk.statsbiblioteket.summa.common.rpc;

import dk.statsbiblioteket.util.rpc.RMIConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.rmi.Remote;

/**
 * <p>A thin {@link Configurable} wrapper around a
 * {@link RMIConnectionFactory}.</p>
 * 
 */
public class SummaRMIConnectionFactory<E extends Remote>
                                               extends RMIConnectionFactory<E>
                                               implements Configurable {
    
    public SummaRMIConnectionFactory (Configuration conf) {
        super ();
    }
}



