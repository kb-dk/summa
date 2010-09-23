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
package dk.statsbiblioteket.summa.control.bundle;

import dk.statsbiblioteket.util.Files;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests for {@link BundleSpecBuilder}.
 */
public class BundleSpecBuilderTest extends TestCase {
    /** Private logger. */
    private static Log log = LogFactory.getLog(BundleSpecBuilderTest.class);
    /** Bundle spec builder. */
    private BundleSpecBuilder b;
    /** Sample spec. */
    private static String sampleSpec = "<bundle>\n"
        + "  <instanceId>myInstanceId</instanceId>\n"
        + "  <bundleId>myBundleId</bundleId>\n"
        + "  <mainClass>myMainClass</mainClass>\n"
        + "  <mainJar>myMainJar</mainJar>\n"
        + "  <autoStart>true</autoStart>"
        + "  <description>myDescription</description>\n"
        + "  <property name=\"myProp1\" value=\"myProp1Value\"/>\n"
        + "  <property name=\"myProp2\" value=\"myProp2Value\"/>\n"
        + "  <jvmArg>-Xmx64m</jvmArg>\n"
        + "  <jvmArg>-Xms32m</jvmArg>\n"
        + "  <publicApi>\n"
        + "    <file>myLib-1.1.jar</file>\n"
        + "  </publicApi>\n"
        + "  <fileList>\n"
        + "    <file>myFile1</file>\n"
        + "    <file>myFile2</file>\n"
        + "  </fileList>\n"
        + "</bundle>";

    @Override
    public final void setUp() {
        b = new BundleSpecBuilder();
    }

    public void testGetSetMainJar() {
        assertNull(b.getMainJar());
        b.setMainJar("foo");
        assertEquals("foo", b.getMainJar());
        b.setMainJar("bar");
        assertEquals("bar", b.getMainJar());
    }

    public void testGetSetMainClass() {
        assertNull(b.getMainClass());
        b.setMainClass("foo");
        assertEquals("foo", b.getMainClass());
        b.setMainClass("bar");
        assertEquals("bar", b.getMainClass());
    }

    public void testGetSetBundleId() {
        assertNull(b.getBundleId());
        b.setBundleId("foo");
        assertEquals("foo", b.getBundleId());
        b.setBundleId("bar");
        assertEquals("bar", b.getBundleId());
    }

    public void testGetSetInstanceId() {
        assertNull(b.getInstanceId());
        b.setInstanceId("foo");
        assertEquals("foo", b.getInstanceId());
        b.setInstanceId("bar");
        assertEquals("bar", b.getInstanceId());
    }

    public void testGetSetBundleType() {
        assertNotNull(b.getBundleType());
        b.setBundleType(Bundle.Type.CLIENT);
        assertEquals(Bundle.Type.CLIENT, b.getBundleType());
        b.setBundleType(Bundle.Type.SERVICE);
        assertEquals(Bundle.Type.SERVICE, b.getBundleType());
    }

    public void testGetSetAutoStart() {
        assertFalse(b.isAutoStart());
        b.setAutoStart(true);
        assertTrue(b.isAutoStart());
        b.setAutoStart(false);
        assertFalse(b.isAutoStart());
    }

    public void testGetSetDescription() {
        assertNull(b.getDescription());
        b.setDescription("foo");
        assertEquals("foo", b.getDescription());
        b.setDescription("bar");
        assertEquals("bar", b.getDescription());
    }

    public void testGetSetProperty() {
        try {
            b.getProperty("foo");
            fail("NPE should be thrown on non existing property");
        } catch (NullPointerException e) {
            // expected
        }
        b.setProperty("foo", "bar");
        assertEquals("bar", b.getProperty("foo"));
        b.setProperty("baz", "boo");
        assertEquals("boo", b.getProperty("baz"));
        b.clearProperty("boo");
        try {
            b.getProperty("boo");
            fail("NPE should be thrown on non existing property");
        } catch (NullPointerException e) {
            // expected
        }
        b.clearProperty("foo");
        try {
            b.getProperty("foo");
            fail("NPE should be thrown on non existing property");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testAddRemoveFile() {
        assertFalse(b.hasFile("foo"));
        b.addFile("foo");
        assertTrue(b.hasFile("foo"));
        b.removeFile("foo");
        assertFalse(b.hasFile("foo"));
    }

    public void testAddRemoveApi() {
        assertFalse(b.hasApi("foo"));
        b.addApi("foo");
        assertTrue(b.hasApi("foo"));
        b.removeApi("foo");
        assertFalse(b.hasApi("foo"));
    }

    /**
     * b should point at a fully loaded spec, matching {@link #sampleSpec}.
     */
    public void doTestSampleSpec() {
        assertEquals("myMainJar", b.getMainJar());
        assertEquals("myMainClass", b.getMainClass());
        assertEquals("myInstanceId", b.getInstanceId());
        assertEquals("myBundleId", b.getBundleId());
        assertEquals(true, b.isAutoStart());
        assertEquals("myDescription", b.getDescription());

        assertEquals("myProp1Value", b.getProperty("myProp1"));
        assertEquals("myProp2Value", b.getProperty("myProp2"));

        assertEquals(Arrays.asList("-Xmx64m", "-Xms32m"),
                     b.getJvmArgs());
        try {
            b.getProperty("myPropNonExistant");
            fail("Retrieving a non-existing property should raise a NPE");
        } catch (NullPointerException e) {
            assertTrue(true);
        }

        assertTrue(b.hasApi("myLib-1.1.jar"));
        assertFalse(b.hasApi("myLibNonExistant-0.1-alpha1.jar"));

        assertTrue(b.hasFile("myFile1"));
        assertTrue(b.hasFile("myFile2"));
        assertFalse(b.hasFile("myFileNonExistant"));
    }

    public void testRead() {
        b.read(new ByteArrayInputStream(sampleSpec.getBytes()));
        doTestSampleSpec();
    }

    public void testOpen() {
        b = BundleSpecBuilder.open(
                               new ByteArrayInputStream(sampleSpec.getBytes()));
        doTestSampleSpec();

    }

    public void testWrite() {
        b.read(new ByteArrayInputStream(sampleSpec.getBytes()));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            b.write(out);
        } catch (Exception e) {
            fail("No exception expected here");
        }

        b = BundleSpecBuilder.open(new ByteArrayInputStream(out.toByteArray()));
        doTestSampleSpec();
    }

    /**
     * b should should be a fully loaded spec.
     */
    public void doTestWriteFile() {
        try {
            File dir = new File(System.getProperty("user.dir"), "tmp");
            File spec = b.writeToDir(dir);

            assertTrue(spec.isFile());

            if (b.getBundleType() == Bundle.Type.CLIENT) {
                assertEquals("client.xml", spec.getName());
            } else if (b.getBundleType() == Bundle.Type.SERVICE) {
                assertEquals("service.xml", spec.getName());
            } else {
                fail("Unknown bundle type '" + b.getBundleType().name() + "'");
            }

            b = BundleSpecBuilder.open(new FileInputStream(spec));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            b.write(out);
            log.info("SPEC:\n" + new String(out.toByteArray()));
            doTestSampleSpec();
            // TODO assert
        } catch (Exception e) {
            fail("No exception expected in helper method");
        }
    }

    public void testClientSpecWrite () {
        b.read(new ByteArrayInputStream(sampleSpec.getBytes()));
        b.setBundleType(Bundle.Type.CLIENT);
        doTestWriteFile();
    }

    public void testServiceSpecWrite() {
        b.read(new ByteArrayInputStream(sampleSpec.getBytes()));
        b.setBundleType(Bundle.Type.SERVICE);
        doTestWriteFile();
    }

    public void testBuildFileList() {
        File bundleRoot = new File("test/test-search-1");
        try {
            b.buildFileList(bundleRoot);
        } catch (Exception e) {
            fail("No exception expected in helper method");
        }
        assertTrue(b.hasFile("service.xml"));
        assertTrue(b.hasFile("config/configuration.xml"));
        assertTrue(b.hasFile("config/jmx.access"));
        assertTrue(b.hasFile("config/jmx.password"));
        assertTrue(b.hasFile("config/policy"));
        assertEquals(5, b.getFiles().size());
    }

    public void testGetFilename() {
        b.setBundleType(Bundle.Type.CLIENT);
        assertEquals("client.xml", b.getFilename());
        b.setBundleType(Bundle.Type.SERVICE);
        assertEquals("service.xml", b.getFilename());
    }

    public void testBuildBundle() {
        b.setBundleType(Bundle.Type.SERVICE);
        b.addFile("config/configuration.xml");
        b.setBundleId("unit-test");
        File bundleFile = null;
        try {
            bundleFile = b.buildBundle(new File("test/test-search-1"),
                                       new File("tmp/delete_me"));
            log.info("Wrote: " + bundleFile);
            assertTrue(bundleFile.exists());
            Files.delete("tmp/delete_me");
        } catch (Exception e) {
            fail("No exception expected in helper method");
        }
    }
}
