/**
 * Created: te 09-04-2008 20:45:43
 * CVS:     $Id:$
 */
package dk.statsbiblioteket.summa.score.rmiapi;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.score.api.Service;
import dk.statsbiblioteket.summa.score.service.ServiceBase;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.RMIConnectionFactory;
import junit.framework.TestCase;

import java.security.Permission;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests the api for establishing RMI connections and communicating.
 */
public class RemoteTest extends TestCase {
    protected void setUp() throws Exception {
        super.setUp();
        checkSecurityManager();
    }

    private String EXIT_MESSAGE = "No no";
    private void checkSecurityManager() { // Allow all
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager() {
                public void checkPermission(Permission perm) { }
                public void checkPermission(Permission perm, Object context) { }
            });
        }
    }
    public void testRemote() throws Exception {
        Configuration someConf = Configuration.newMemoryBased();
        someConf.set(ServiceBase.REGISTRY_PORT, 27000);
        someConf.set(ServiceBase.SERVICE_BASEPATH, "Whereever");
        someConf.set(ServiceBase.SERVICE_ID, "some");
        someConf.set(ServiceBase.SERVICE_PORT, 27003);
        System.setProperty(ServiceBase.SERVICE_ID, "some");
        SomeService service = new SomeService(someConf);

        // Connect so that we can start
        ConnectionFactory<Service> serviceCF =
                new RMIConnectionFactory<Service>();
        ConnectionManager<Service> serviceCM =
                new ConnectionManager<Service>(serviceCF);

        ConnectionContext<Service> serviceContext =
                serviceCM.get("//localhost:27000/some");
        assertNotNull("The ConnectionManager should return a Service"
                      + " ConnectionContext", serviceContext);
        Service serviceRemote = serviceContext.getConnection();

        serviceRemote.start();
        assertEquals("First call should return 0", 0, service.getNext());


        ConnectionFactory<SomeInterface> cf =
                new RMIConnectionFactory<SomeInterface>();
        ConnectionManager<SomeInterface> cm = 
                new ConnectionManager<SomeInterface>(cf);

        // Do this for each connection
        ConnectionContext<SomeInterface> ctx =
                cm.get("//localhost:27000/some");
        assertNotNull("The ConnectionManager should return a SomeInterface"
                      + " ConnectionContext", ctx);
        SomeInterface server = ctx.getConnection();
        assertEquals("Second call should give 1", 1, server.getNext());
        cm.release(ctx);

        serviceRemote.stop();
        serviceCM.release(serviceContext);
    }

}
