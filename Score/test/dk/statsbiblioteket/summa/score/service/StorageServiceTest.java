package dk.statsbiblioteket.summa.score.service;

import java.io.File;
import java.security.AllPermission;
import java.security.Permission;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.score.api.Service;
import dk.statsbiblioteket.summa.score.api.Status;
import dk.statsbiblioteket.summa.storage.database.DatabaseControl;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(StorageServiceTest.class);
    }


    private String EXIT_MESSAGE = "Thou shall not exit!";
    public void testConstruction() throws Exception {
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
        File location =
                new File(System.getProperty("java.io.tmpdir"), "kabloey");
        Configuration conf = Configuration.newMemoryBased();
        conf.set(DatabaseControl.PROP_LOCATION, location.toString());
        conf.set(Service.SERVICE_PORT, 27003);
        conf.set(Service.REGISTRY_PORT, 27000);
        conf.set(Service.SERVICE_ID, "TestStorage");
        System.setProperty(Service.SERVICE_ID, "TestStorage");
        
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
}
