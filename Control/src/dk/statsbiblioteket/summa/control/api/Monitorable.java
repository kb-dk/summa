package dk.statsbiblioteket.summa.control.api;

import java.io.IOException;

/**
 * An interface for objects that have a state that can be monitored.
 * This is mostly targetted at remote services.
 */
public interface Monitorable {

    /**
     * Get the status of the object.
     * 
     * @return the status of the object. In case the object can not be contacted,
     *         fx. when it is not running, {@code null} should be returned
     * @throws IOException if there are errors commnunicating with the object
     */
    public Status getStatus () throws IOException;
}
