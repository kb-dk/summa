package dk.statsbiblioteket.summa.common.rpc;

import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

/**
 * Abstract {@link Configurable} wrapper around a {@link ConnectionFactory}.
 * It allows arbitrary {@link ConnectionFactory} backends.
 *
 * @see SummaRMIConnectionFactory
 * @see GenericConnectionFactory
 */
public abstract class SummaConnectionFactory<E> extends ConnectionFactory<E>
                                                implements Configurable {

    protected ConnectionFactory<? extends E> backend;

    public SummaConnectionFactory (Configuration conf) {
        super();
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
