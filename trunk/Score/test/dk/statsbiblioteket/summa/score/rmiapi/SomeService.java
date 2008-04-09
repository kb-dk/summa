/**
 * Created: te 09-04-2008 20:24:38
 * CVS:     $Id:$
 */
package dk.statsbiblioteket.summa.score.rmiapi;

import dk.statsbiblioteket.summa.score.service.ServiceBase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A dummy service with a state and a simple call.
 */
public class SomeService extends ServiceBase implements SomeInterface {
    private Log log = LogFactory.getLog(SomeService.class);

    private SomeClass someClass = new SomeClass();

    public SomeService(Configuration conf) throws RemoteException {
        super(conf);
        exportRemoteInterfaces();
    }

    public int getNext() throws RemoteException {
        try {
            return someClass.getNext();
        } catch (Exception e) {
            throw new RemoteException("Could not get next", e);
        }
    }

    public void start() throws RemoteException {
        log.info("Starting");
        someClass = new SomeClass();
    }

    public void stop() throws RemoteException {
        log.info("Stopping");
        someClass = null;
        unexportRemoteInterfaces();
    }

    public String getId() throws RemoteException {
        return "SomeService";
    }
}
