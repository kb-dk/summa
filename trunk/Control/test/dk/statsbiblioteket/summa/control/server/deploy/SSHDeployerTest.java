/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.control.server.deploy;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.control.api.feedback.ConsoleFeedback;
import dk.statsbiblioteket.util.qa.QAInfo;

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
    public static final String PROPERTY_LOGIN =
            "te@pc990.sb";

    public static final String PROPERTY_SOURCE =
            "ClientManager/test/dk/statsbiblioteket/summa/control/server/deploy/FakeZIP.zip";
    public static final String PROPERTY_DESTINATION =
            "/tmp/fakeClient";
    public static final String PROPERTY_START_CONFSERVER =
            "NA";
    public static final String PROPERTY_CLIENT_INSTANCEID =
            "fake client-01";

    private static final String dest = PROPERTY_DESTINATION + "/FakeZIP.zip";
    private static final String jar = PROPERTY_DESTINATION + "/FakeJAR.jar";
    private static final String output = PROPERTY_DESTINATION + "/output.txt";

    public SSHDeployerTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        /*if (!new File(PROPERTY_SOURCE).exists()) {
            throw new IOException("The test-package " + PROPERTY_SOURCE
                                  + " should be at "
                                  + new File(PROPERTY_SOURCE).getAbsoluteFile().
                                    getParent() + ". It can be build with "
                                  + "the script maketestpackage.sh");
        }*/
        super.setUp();
    }

    public void tearDown() throws Exception {
        String[] deletables = new String[]{dest, jar, output};
        for (String deletable: deletables) {
            if (new File(deletable).exists()) {
                new File(deletable).delete();
            }
        }
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(SSHDeployerTest.class);
    }

    public void testInstantiation() throws Exception {
        try {
            SSHDeployer deployer =
                new SSHDeployer(new Configuration(new MemoryStorage()));
            fail("The deployer should throw an exception when it could not "
                 + "find the right properties");
        } catch (Exception e) {
            // Expected behaviour
        }

        // It should work fine with a good config however
        Configuration conf = makeConfiguration();
        SSHDeployer depl = new SSHDeployer(conf);
        assertNotNull(depl);

        // Try with a malformed port def
        try {
            conf.set(SSHDeployer.CONF_DEPLOYER_TARGET, "localhost:asdmfhg");
            depl = new SSHDeployer(conf);
            fail("The deployer should throw an exception when given a malformed"
                 + " target def");
        } catch (Exception e) {
            System.out.println("Got exception as expected: " + e.getMessage());
        }
    }

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

    public void doDeploy() throws Exception {
        assertTrue("The source " + new File(PROPERTY_SOURCE).getAbsoluteFile()
                   + " should exist", new File(PROPERTY_SOURCE).exists());
        assertFalse("The file " + dest
                   + " should not exist before deploy",
                   new File(dest).exists());
        Configuration configuration = makeConfiguration();
        SSHDeployer deployer = new SSHDeployer(configuration);
        deployer.deploy(new ConsoleFeedback());
        assertTrue("The file " + dest
                   + " should exist after deploy",
                   new File(dest).exists());
        assertTrue("The file " + jar
                   + " should exist after deploy",
                   new File(jar).exists());
    }

    public void doStart() throws Exception {
        assertTrue("The source " + new File(PROPERTY_SOURCE).getAbsoluteFile()
                   + " should exist", new File(PROPERTY_SOURCE).exists());
        Configuration configuration = makeConfiguration();
        SSHDeployer deployer = new SSHDeployer(configuration);
        deployer.deploy(new ConsoleFeedback());
        assertTrue("The file " + jar
                   + " should exist after deploy",
                   new File(jar).exists());

        assertFalse("The file " + output
                   + " should not exist before start",
                   new File(output).exists());
        deployer.start(new ConsoleFeedback());
        assertTrue("The file " + output
                   + " should exist after start",
                   new File(output).exists());
    }

    public void testPort() throws Exception {
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
                     222, new SSHDeployer(conf).getPort());
    }

    public void testGetHostname () throws Exception {
        Configuration conf = makeConfiguration();

        conf.set (SSHDeployer.CONF_DEPLOYER_TARGET, "user@host:~/dir");
        SSHDeployer deployer = new SSHDeployer(conf);
        assertEquals("host", deployer.getTargetHost());


        conf.set (SSHDeployer.CONF_DEPLOYER_TARGET, "host:~/dir");
        deployer = new SSHDeployer(conf);
        assertEquals("host", deployer.getTargetHost());

        conf.set (SSHDeployer.CONF_DEPLOYER_TARGET, "host");
        deployer = new SSHDeployer(conf);
        assertEquals("host", deployer.getTargetHost());

        conf.set (SSHDeployer.CONF_DEPLOYER_TARGET, "user@host");
        deployer = new SSHDeployer(conf);
        assertEquals("host", deployer.getTargetHost());

    }
}



