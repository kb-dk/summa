package dk.statsbiblioteket.summa.score.server;

/**
 * Thrown when you try to establish a connection to an unknown
 * {@link dk.statsbiblioteket.summa.score.client.Client}.
 */
public class NoSuchClientException extends RuntimeException {

    public NoSuchClientException (String msg) {
        super (msg);
    }

}
