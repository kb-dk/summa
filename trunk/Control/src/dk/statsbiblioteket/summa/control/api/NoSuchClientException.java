package dk.statsbiblioteket.summa.control.api;

/**
 * Thrown when you try to establish a connection to an unknown
 * {@link dk.statsbiblioteket.summa.control.client.Client}.
 */
public class NoSuchClientException extends RuntimeException {

    public NoSuchClientException (String msg) {
        super (msg);
    }

}



