package dk.statsbiblioteket.summa.control.bundle;
/**
 *
 */

import junit.framework.*;
import dk.statsbiblioteket.summa.control.bundle.BundleSpecBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

public class BundleSpecBuilderTest extends TestCase {

    BundleSpecBuilder b;

    static String sampleSpec =
      "<bundle>\n"
    + "  <instanceId>myInstanceId</instanceId>\n"
    + "  <bundleId>myBundleId</bundleId>\n"
    + "  <mainClass>myMainClass</mainClass>\n"
    + "  <mainJar>myMainJar</mainJar>\n"
    + "  <description>myDescription</description>\n"
    + "  <property name=\"myProp1\" value=\"myProp1Value\"/>\n"
    + "  <property name=\"myProp2\" value=\"myProp2Value\"/>\n"
    + "  <publicApi>\n"
    + "    <file>myLib-1.1.jar</file>\n"
    + "  </publicApi>\n"
    + "  <fileList>\n"
    + "    <file>myFile1</file>\n"
    + "    <file>myFile2</file>\n"
    + "  </fileList>\n"
    + "</bundle>";

    public void setUp () throws Exception {
        b = new BundleSpecBuilder();
    }

    public void testGetSetMainJar() throws Exception {
        assertNull(b.getMainJar());
        b.setMainJar("foo");
        assertEquals("foo", b.getMainJar());
        b.setMainJar("bar");
        assertEquals("bar", b.getMainJar());
    }

    public void testGetSetMainClass() throws Exception {
        assertNull(b.getMainClass());
        b.setMainClass("foo");
        assertEquals("foo", b.getMainClass());
        b.setMainClass("bar");
        assertEquals("bar", b.getMainClass());
    }

    public void testGetSetBundleId() throws Exception {
        assertNull(b.getBundleId());
        b.setBundleId("foo");
        assertEquals("foo", b.getBundleId());
        b.setBundleId("bar");
        assertEquals("bar", b.getBundleId());
    }

    public void testGetSetInstanceId() throws Exception {
        assertNull(b.getInstanceId());
        b.setInstanceId("foo");
        assertEquals("foo", b.getInstanceId());
        b.setInstanceId("bar");
        assertEquals("bar", b.getInstanceId());
    }

    public void testGetSetBundleType() throws Exception {
        assertNotNull(b.getBundleType());
        b.setBundleType(Bundle.Type.CLIENT);
        assertEquals(Bundle.Type.CLIENT, b.getBundleType());
        b.setBundleType(Bundle.Type.SERVICE);
        assertEquals(Bundle.Type.SERVICE, b.getBundleType());
    }

    public void testGetSetDescription() throws Exception {
        assertNull(b.getDescription());
        b.setDescription("foo");
        assertEquals("foo", b.getDescription());
        b.setDescription("bar");
        assertEquals("bar", b.getDescription());
    }

    public void testGetSetProperty() throws Exception {
        try {
            b.getProperty("foo");
            fail ("NPE should be thrown on non existing property");
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
            fail ("NPE should be thrown on non existing property");
        } catch (NullPointerException e) {
            // expected
        }
        b.clearProperty("foo");
        try {
            b.getProperty("foo");
            fail ("NPE should be thrown on non existing property");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testAddRemoveFile () throws Exception {
        assertFalse(b.hasFile("foo"));
        b.addFile("foo");
        assertTrue(b.hasFile("foo"));
        b.removeFile("foo");
        assertFalse(b.hasFile("foo"));
    }

    public void testAddRemoveApi () throws Exception {
        assertFalse(b.hasApi("foo"));
        b.addApi("foo");
        assertTrue(b.hasApi("foo"));
        b.removeApi("foo");
        assertFalse(b.hasApi("foo"));
    }

    /**
     * b should point at a fully loaded spec, matching {@link #sampleSpec}
     */
    public void doTestSampleSpec () throws Exception {
        assertEquals("myMainJar", b.getMainJar());
        assertEquals("myMainClass", b.getMainClass());
        assertEquals("myInstanceId", b.getInstanceId());
        assertEquals("myBundleId", b.getBundleId());
        assertEquals("myDescription", b.getDescription());

        assertEquals("myProp1Value", b.getProperty("myProp1"));
        assertEquals("myProp2Value", b.getProperty("myProp2"));
        try {
            b.getProperty("myPropNonExistant");
            fail("Retrieving a non-existing property should raise a NPE");
        } catch (NullPointerException e) {
            //expected
        }

        assertTrue(b.hasApi("myLib-1.1.jar"));
        assertFalse(b.hasApi("myLibNonExistant-0.1-alpha1.jar"));

        assertTrue(b.hasFile("myFile1"));
        assertTrue(b.hasFile("myFile2"));
        assertFalse(b.hasFile("myFileNonExistant"));
    }

    public void testRead () throws Exception {
        b.read(new ByteArrayInputStream(sampleSpec.getBytes()));
        doTestSampleSpec();
    }

    public void testOpen () throws Exception {
        b = BundleSpecBuilder.open(
                            new ByteArrayInputStream(sampleSpec.getBytes()));
        doTestSampleSpec();

    }

    public void testWrite() throws Exception {
        b.read(new ByteArrayInputStream(sampleSpec.getBytes()));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        b.write (out);

        BundleSpecBuilder bb = BundleSpecBuilder.open (
                                   new ByteArrayInputStream(out.toByteArray()));
        doTestSampleSpec();
    }

    /**
     * b should should be a fully loaded spec
     * @throws Exception on error
     */
    public void doTestWriteFile() throws Exception {

        File dir = new File(System.getProperty("user.dir"), "tmp");
        File spec = b.writeToDir(dir);

        assertTrue(spec.isFile());

        if (b.getBundleType() == Bundle.Type.CLIENT) {
            assertEquals("client.xml", spec.getName());
        } else if (b.getBundleType() == Bundle.Type.SERVICE) {
            assertEquals("service.xml", spec.getName());
        } else {
            fail ("Unknown bundle type '" + b.getBundleType().name() + "'");
        }

        b = BundleSpecBuilder.open (new FileInputStream(spec));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        b.write (out);
        System.out.println ("SPEC:\n" + new String(out.toByteArray()));
        doTestSampleSpec();
    }

    public void testClientSpecWrite () throws Exception {
        b.read(new ByteArrayInputStream(sampleSpec.getBytes()));
        b.setBundleType(Bundle.Type.CLIENT);
        doTestWriteFile();
    }

    public void testServiceSpecWrite () throws Exception {
        b.read(new ByteArrayInputStream(sampleSpec.getBytes()));
        b.setBundleType(Bundle.Type.SERVICE);
        doTestWriteFile();
    }

    public void testBuildFileList () throws Exception {
        File bundleRoot = new File ("Control/test/test-search-1");
        b.buildFileList(bundleRoot);

        assertTrue(b.hasFile("service.xml"));
        assertTrue(b.hasFile("config/configuration.xml"));
        assertTrue(b.hasFile("config/jmx.access"));
        assertTrue(b.hasFile("config/jmx.password"));
        assertTrue(b.hasFile("config/policy"));
        assertEquals(5, b.getFiles().size());
    }

    public void testGetFilename () throws Exception {
        b.setBundleType(Bundle.Type.CLIENT);
        assertEquals("client.xml", b.getFilename());
        b.setBundleType(Bundle.Type.SERVICE);
        assertEquals("service.xml", b.getFilename());
    }

    public void testBuildBundle () throws Exception {
        b.setBundleType(Bundle.Type.SERVICE);
        b.addFile("config/configuration.xml");
        b.setBundleId("unit-test");
        File bundleFile = b.buildBundle(new File("Control/test/test-search-1"),
                                        new File("tmp/delete_me"));

        System.out.println ("Wrote: " + bundleFile);

        //Files.delete ("tmp/delete_me");
    }

}





