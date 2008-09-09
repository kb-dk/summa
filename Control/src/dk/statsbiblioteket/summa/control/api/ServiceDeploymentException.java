package dk.statsbiblioteket.summa.control.api;

/**
 * Thrown when there is an error deploying a service into a running
 * client instance.
 */
public class ServiceDeploymentException extends RuntimeException {

    public ServiceDeploymentException (String msg) {
        super(msg);
    }

    public ServiceDeploymentException (Throwable cause) {
        super(cause);
    }

    public ServiceDeploymentException (String msg, Throwable cause) {
        super(msg, cause);
    }

}
