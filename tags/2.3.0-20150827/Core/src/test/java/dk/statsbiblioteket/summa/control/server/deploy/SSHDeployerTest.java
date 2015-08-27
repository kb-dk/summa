/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.control.server.deploy;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.control.api.feedback.ConsoleFeedback;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;

/**
 * SSHDeployer Tester.
 *
 * This unit-test is somewhat special, as it requires manuel activation.
 * This is because it was too large a task to make a fake SSH-server.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class SSHDeployerTest extends TestCase {
    /** Login property. */
    public static final String PROPERTY_LOGIN = "te@pc990.sb";
    /** Source property. */
    public static final String PROPERTY_SOURCE =
            "ClientManager/test/dk/statsbiblioteket/summa/control/"
            + "server/deploy/FakeZIP.zip";
    /** Destination property. */
    public static final String PROPERTY_DESTINATION = "/tmp/fakeClient";
    /** Configuration server start property. */
    public static final String PROPERTY_START_CONFSERVER = "NA";
    /** Fake client instance ID. */
    public static final String PROPERTY_CLIENT_INSTANCEID = "fake client-01";
    /** FakeZIP.zip destination. */
    private static final String DEST = PROPERTY_DESTINATION + "/FakeZIP.zip";
    /** FakeJAr.jar destination. */
    private static final String JAR = PROPERTY_DESTINATION + "/FakeJAR.jar";
    /** output.txt destination. */
    private static final String OUTPUT = PROPERTY_DESTINATION + "/output.txt";

    /**
     * Constructs a SSH deployer test with a name.
     * @param name The name.
     */
    public SSHDeployerTest(String name) {
        super(name);
    }

    @Override
    public final void setUp() throws Exception {
        /*if (!new File(PROPERTY_SOURCE).exists()) {
            throw new IOException("The test-package " + PROPERTY_SOURCE
                                  + " should be at "
                                  + new File(PROPERTY_SOURCE).getAbsoluteFile().
                                    getParent() + ". It can be build with "
                                  + "the script maketestpackage.sh");
        }*/
        super.setUp();
    }

    @Override
    public final void tearDown() throws Exception {
        String[] deletables = new String[]{DEST, JAR, OUTPUT};
        for (String deletable : deletables) {
            if (new File(deletable).exists()) {
                new File(deletable).delete();
            }
        }
        super.tearDown();
    }

    /**
     * @return The test suite.
     */
    public static Test suite() {
        return new TestSuite(SSHDeployerTest.class);
    }

    /**
     * Test instantiation.
     */
    public void testInstantiation() {
        SSHDeployer deployer = null;
        try {
            deployer = new SSHDeployer(new Configuration(new MemoryStorage()));
            fail("The deployer should throw an exception when it could not "
                 + "find the right properties");
        } catch (Exception e) {
            // Expected behavior
            assertNull(deployer);
        }

        // It should work fine with a good configuration however
        Configuration conf = makeConfiguration();
        SSHDeployer depl = new SSHDeployer(conf);
        assertNotNull(depl);

        // Try with a malformed port definition
        try {
            conf.set(SSHDeployer.CONF_DEPLOYER_TARGET, "localhost:asdmfhg");
            depl = new SSHDeployer(conf);
            fail("The deployer should throw an exception when given a malformed"
                 + " target def");
        } catch (Exception e) {
            System.out.println("Got exception as expected: " + e.getMessage());
        }
    }

    /**
     * @return A usable configuration.
     */
    private Configuration makeConfiguration() {
        MemoryStorage storage = new MemoryStorage();
        storage.put(SSHDeployer.CONF_DEPLOYER_BUNDLE, PROPERTY_SOURCE);
        storage.put(SSHDeployer.CONF_BASEPATH, PROPERTY_DESTINATION);
        storage.put(SSHDeployer.CONF_DEPLOYER_TARGET, PROPERTY_LOGIN);
        storage.put(SSHDeployer.CONF_CLIENT_CONF, PROPERTY_START_CONFSERVER);
        storage.put(SSHDeployer.CONF_INSTANCE_ID, "test-client-1");
        storage.put(SSHDeployer.CONF_DEPLOYER_BUNDLE_FILE,
                         "${user.dir}/Control/test/data/dummy-repo/foo.bundle");
        new FakeThinClient();
        return new Configuration(storage);
    }

    /**
     * do deployment.
     */
    public void doDeploy() {
        assertTrue("The source " + new File(PROPERTY_SOURCE).getAbsoluteFile()
                   + " should exist", new File(PROPERTY_SOURCE).exists());
        assertFalse("The file " + DEST
                   + " should not exist before deploy",
                   new File(DEST).exists());
        Configuration configuration = makeConfiguration();
        SSHDeployer deployer = new SSHDeployer(configuration);
        try {
            deployer.deploy(new ConsoleFeedback());
        } catch (Exception e) {
            fail("Should not happen");
        }
        assertTrue("The file " + DEST
                   + " should exist after deploy",
                   new File(DEST).exists());
        assertTrue("The file " + JAR
                   + " should exist after deploy",
                   new File(JAR).exists());
    }

    /**
     * Tests a start up.
     */
    public void doStart() {
        assertTrue("The source " + new File(PROPERTY_SOURCE).getAbsoluteFile()
                   + " should exist", new File(PROPERTY_SOURCE).exists());
        Configuration configuration = makeConfiguration();
        SSHDeployer deployer = new SSHDeployer(configuration);
        try {
            deployer.deploy(new ConsoleFeedback());
        } catch (Exception e) {
            fail("Should not happen");
        }
        assertTrue("The file " + JAR
                   + " should exist after deploy",
                   new File(JAR).exists());

        assertFalse("The file " + OUTPUT
                   + " should not exist before start",
                   new File(OUTPUT).exists());
        try {
            deployer.start(new ConsoleFeedback());
        } catch (Exception e) {
            fail("Should not happen");
        }
        assertTrue("The file " + OUTPUT
                   + " should exist after start",
                   new File(OUTPUT).exists());
    }

    /**
     * Test get Port method.
     */
    public void testPort() {
        final int specialPort = 222;
        Configuration conf = makeConfiguration();
        conf.set(SSHDeployer.CONF_DEPLOYER_BUNDLE_FILE, "Somefile");
        conf.set(SSHDeployer.CONF_DEPLOYER_TARGET, "host");
        assertEquals("Host only should work",
                     "host", new SSHDeployer(conf).getLogin());
        conf.set(SSHDeployer.CONF_DEPLOYER_TARGET, "user@host");
        assertEquals("User + host should work",
                     "user@host", new SSHDeployer(conf).getLogin());
        conf.set(SSHDeployer.CONF_DEPLOYER_TARGET, "user@host:222");
        assertEquals("User + host + port should work for login",
                     "user@host", new SSHDeployer(conf).getLogin());
        assertEquals("User + host + port should work for port",
                     specialPort, new SSHDeployer(conf).getPort());
    }

    /**
     * Test getHostName with colon in target.
     */
    // Disabled as deployer is deprecated
/*    public void testGetHostNameWithColon() {
        Configuration conf = makeConfiguration();

        conf.set(SSHDeployer.CONF_DEPLOYER_TARGET, "user@host:~/dir");
        SSHDeployer deployer = new SSHDeployer(conf);
        assertEquals("host", deployer.getTargetHost());


        conf.set(SSHDeployer.CONF_DEPLOYER_TARGET, "host:~/dir");
        deployer = new SSHDeployer(conf);
        assertEquals("host", deployer.getTargetHost());
    }
  * /
    /**
     * Test get host name method.
     */
    public void testGetHostname() {
        Configuration conf = makeConfiguration();

        conf.set(SSHDeployer.CONF_DEPLOYER_TARGET, "host");
        SSHDeployer deployer = new SSHDeployer(conf);
        assertEquals("host", deployer.getTargetHost());

        conf.set(SSHDeployer.CONF_DEPLOYER_TARGET, "user@host");
        deployer = new SSHDeployer(conf);
        assertEquals("host", deployer.getTargetHost());
    }
}
