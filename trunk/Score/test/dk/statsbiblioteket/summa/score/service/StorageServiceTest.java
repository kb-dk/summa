package dk.statsbiblioteket.summa.score.service;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.score.api.Service;
import dk.statsbiblioteket.summa.score.api.Status;
import dk.statsbiblioteket.summa.storage.database.DatabaseControl;
import dk.statsbiblioteket.summa.storage.io.Access;
import dk.statsbiblioteket.summa.storage.io.RecordIterator;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.RMIConnectionFactory;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.security.Permission;

/**
 * StorageService Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StorageServiceTest extends TestCase {
    public StorageServiceTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        checkSecurityManager();
        if (location.exists()) {
            try {
                Files.delete(location);
            } catch (Exception e) {
                System.out.println("Could not remove previous database at '"
                                   + location + "'");
            }
        }
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(StorageServiceTest.class);
    }

    private String EXIT_MESSAGE = "Thou shall not exit!";
    File location =
            new File(System.getProperty("java.io.tmpdir"), "gooeykabloey");

    public void testConstruction() throws Exception {
        Configuration conf = createconfiguration();
        StorageService storage = new StorageService(conf);
        assertEquals("The state of the service should be "
                     + Status.CODE.constructed,
                     Status.CODE.constructed, storage.getStatus().getCode());

        storage.start();
        long endTime = System.currentTimeMillis() + 50 * 1000; // Not forever
        while (Status.CODE.startingUp.equals(storage.getStatus().getCode())
               && System.currentTimeMillis() < endTime) {
            Thread.sleep(50);
        }
        assertEquals("The state of the service should be "
                     + Status.CODE.running,
                     Status.CODE.running, storage.getStatus().getCode());
        try {
            storage.stop();
            endTime = System.currentTimeMillis() + (10 * 1000 + 500);
            while (Status.CODE.stopping.equals(storage.getStatus().getCode())
                   && System.currentTimeMillis() < endTime) {
                Thread.sleep(50);
            }
        } catch (SecurityException e) {
            assertEquals("The SecurityException should be about an attempted "
                         + "System.exit", EXIT_MESSAGE, e.getMessage());
        }
        assertEquals("The state of the service should be "
                     + Status.CODE.stopped,
                     Status.CODE.stopped, storage.getStatus().getCode());
    }

    public void testRemote() throws Exception {
        Configuration conf = createconfiguration();
        StorageService storage = new StorageService(conf);

        // Start the service remotely
        ConnectionFactory<Service> serviceCF =
                new RMIConnectionFactory<Service>();
        ConnectionManager<Service> serviceCM =
                new ConnectionManager<Service>(serviceCF);
        ConnectionContext<Service> serviceContext =
                serviceCM.get("//localhost:27000/TestStorage");
        assertNotNull("The ConnectionManager should return a Service"
                      + " ConnectionContext", serviceContext);
        Service serviceRemote = serviceContext.getConnection();
        serviceRemote.start();
        System.out.println("Sleeping 10 seconds (hack until threading is removed in the StorageService)");
        Thread.sleep(10*1000);
        System.out.println("Finished sleeping, commencing");

        // Connect to the Storage remotely
        ConnectionFactory<Access> cf = new RMIConnectionFactory<Access>();
        ConnectionManager<Access> cm = new ConnectionManager<Access>(cf);

        // Do this for each connection
        ConnectionContext<Access> ctx =
                cm.get("//localhost:27000/TestStorage");
        assertNotNull("The ConnectionManager should return an Access"
                      + " ConnectionContext", ctx);
        Access remoteStorage = ctx.getConnection();
        remoteStorage.flush(new Record("foo", "bar", new byte[0]));
        RecordIterator recordIterator =
                remoteStorage.getRecordsModifiedAfter(0, "bar");
        assertTrue("The iterator should have at least one element",
                   recordIterator.hasNext());
        Record extracted = recordIterator.next();
        assertEquals("The extracted Record should match the flushed",
                     "foo", extracted.getId());

        cm.release(ctx);
        serviceRemote.stop();
        serviceCM.release(serviceContext);


//        ConnectionManager manager 

        // TODO: Create StorageService, connect, try Control-interface
    }

    private Configuration createconfiguration() {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
        conf.set(Service.SERVICE_PORT, 27003);
        conf.set(Service.REGISTRY_PORT, 27000);
        conf.set(Service.SERVICE_ID, "TestStorage");
        System.setProperty(Service.SERVICE_ID, "TestStorage");
        return conf;
    }

    private void checkSecurityManager() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager() {
                public void checkPermission(Permission perm) {
                    if (perm.getName().startsWith("exitVM")) {
                        throw new SecurityException(EXIT_MESSAGE);
                    }
                }
                public void checkPermission(Permission perm, Object context) {
                    if (perm.getName().startsWith("exitVM")) {
                        throw new SecurityException(EXIT_MESSAGE);
                    }
                }
            });
        }
    }
}
