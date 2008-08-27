package dk.statsbiblioteket.summa.control.service;

import java.io.File;
import java.io.IOException;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageConnectionFactory;
import dk.statsbiblioteket.summa.storage.api.RecordIterator;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.RMIConnectionFactory;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * StorageService Tester.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StorageServiceTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(StorageServiceTest.class);

    public StorageServiceTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        if (location.exists()) {
            try {
                Files.delete(location);
            } catch (IOException e) {
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

    File location =
            new File(System.getProperty("java.io.tmpdir"), "gooeykabloey");

    public void testConstruction() throws Exception {
        Configuration conf = createconfiguration();
//        System.setProperty("summa.control.client.persistent.dir", "/tmp/t");
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
        new StorageService(conf);

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

        // Connect to the Storage remotely
        ConnectionFactory<Storage> cf = new StorageConnectionFactory(conf);
        ConnectionManager<Storage> cm = new ConnectionManager<Storage>(cf);

        // Do this for each connection
        ConnectionContext<Storage> ctx =
                cm.get("//localhost:27000/TestStorage");
        assertNotNull("The ConnectionManager should return an Storage"
                      + " ConnectionContext", ctx);
        Storage remoteStorage = ctx.getConnection();
        remoteStorage.flush(new Record("foo", "bar", new byte[0]));
        RecordIterator recordIterator =
                remoteStorage.getRecordsModifiedAfter(0, "bar");
        assertTrue("The iterator should have at least one element",
                   recordIterator.hasNext());
        Record extracted = recordIterator.next();
        assertEquals("The extracted Record should match the flushed",
                     "foo", extracted.getId());

        log.debug("Releasing remoteStorage connection context");
        cm.release(ctx);
        log.debug("Stopping remote service");
        serviceRemote.stop();
        log.debug("Releasing service connection context");
        serviceCM.release(serviceContext);
        log.debug("Finished testRemote unit test");
    }

    private Configuration createconfiguration() {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(DatabaseStorage.PROP_LOCATION, location.toString());
        conf.set(Service.SERVICE_PORT, 27003);
        conf.set(Service.REGISTRY_PORT, 27000);
        conf.set(Service.SERVICE_ID, "TestStorage");
        System.setProperty(Service.SERVICE_ID, "TestStorage");
        return conf;
    }

}
