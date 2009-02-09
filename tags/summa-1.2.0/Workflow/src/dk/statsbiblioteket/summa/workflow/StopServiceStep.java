package dk.statsbiblioteket.summa.workflow;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.control.api.Service;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;

/**
 * A {@link WorkflowStep} that will stop a {@link Service} by calling
 * {@link Service#stop}.
 *
 * @see StartServiceStep
 */
public class StopServiceStep extends ConnectionConsumer<Service>
                             implements WorkflowStep {

    private Log log;

    public StopServiceStep (Configuration conf) {
        super(conf);
        log = LogFactory.getLog(this.getClass().getName());

        log.debug("Configured to stop service: '" + getVendorId() + "'");
    }

    public void run() {
        Service service = getConnection();
        try {
            log.debug("Stopping " + getVendorId());
            service.stop();
        } catch (RemoteException e) {
            throw new RuntimeException("Error stopping service "
                                       + getVendorId() + ": " + e.getMessage(),
                                       e);
        } finally {
            releaseConnection();
        }
    }
}
