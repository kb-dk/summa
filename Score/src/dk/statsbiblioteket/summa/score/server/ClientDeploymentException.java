package dk.statsbiblioteket.summa.score.server;

/**
 * Thrown when there is an error deploying or starting a client
 */
public class ClientDeploymentException extends RuntimeException {

    public ClientDeploymentException (Throwable cause) {
        super(cause);
    }

    public ClientDeploymentException (String msg, Throwable cause) {
        super(msg, cause);
    }

}
