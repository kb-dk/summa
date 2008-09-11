package dk.statsbiblioteket.summa.control.api;

/**
 * Thrown when connecting to a client that is either not running
 * or has a broken connection.
 */
public class InvalidClientStateException extends RuntimeException {

    public InvalidClientStateException (String clientId, String msg) {
        super ("Error connecting to '" + clientId + "': " + msg);
    }

    public InvalidClientStateException (String clientId, String msg,
                                        Throwable cause) {
        super ("Error connecting to '" + clientId + "': " + msg, cause);
    }

}



