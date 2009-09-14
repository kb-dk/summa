package dk.statsbiblioteket.summa.control.api;

/**
 * Thrown when connecting to a client that is either not running
 * or has a broken connection.
 */
public class InvalidClientStateException extends RuntimeException {

    private String clientId;

    public InvalidClientStateException (String clientId, String msg) {
        super (msg);
        this.clientId = clientId;
    }

    public InvalidClientStateException (String clientId, String msg,
                                        Throwable cause) {
        super (msg, cause);
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

}



