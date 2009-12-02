package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.util.Files;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * DumpFilter Tester.
 *
 * @author <Authors name>
 * @since <pre>03/31/2009</pre>
 * @version 1.0
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class DumpFilterTest extends TestCase {
    private static Log log = LogFactory.getLog(DumpFilterTest.class);

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static final File OUT = new File(new File(System.getProperty(
            "java.io.tmpdir")), "dumpTest");

    public DumpFilterTest(String name) {
        super(name);
        if (OUT.exists()) {
            try {
                Files.delete(OUT);
            } catch (IOException e) {
                fail("Unable to delete the folder " + OUT);
            }
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(DumpFilterTest.class);
    }

    public void testDumper() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                DumpFilter.CONF_OUTPUTFOLDER, OUT.getAbsolutePath(),
                DumpFilter.CONF_DUMP_STREAMS, true,
                DumpFilter.CONF_DUMP_XML, true);
        List<Payload> payloads = Arrays.asList(
                new Payload(new ByteArrayInputStream(new byte[10])),
                new Payload(new Record(
                        "¤%&&:id-flam87:", "mybase",
                        "<myxml>My content</myxml>".getBytes("utf-8")))
        );
        ObjectFilter feeder = new PayloadFeederHelper(payloads);
        ObjectFilter dumpFilter = new DumpFilter(conf);
        dumpFilter.setSource(feeder);

        int count = 0;
        while (dumpFilter.hasNext()) {
            log.debug("Pumped " + dumpFilter.pump());
            count++;
        }
        assertEquals("The number of processed Payloads should match", 2, count);
        assertEquals("The number of created files should match",
                     5, OUT.listFiles().length);
        File ec = new File(OUT, "_____id-flam87_.content");
        File em = new File(OUT, "_____id-flam87_.meta");
        File ex = new File(OUT, "_____id-flam87_.xml");
        assertTrue("The expected content-file '" + ec + "' should be present",
                   ec.exists());
        assertTrue("The expected meta-file '" + em + "' should be present",
                   em.exists());
        assertTrue("The expected XML-file '" + ex + "' should be present",
                   ex.exists());
        log.debug("Content of XML-dump:\n" + Files.loadString(ex));

        Calendar calendar = Calendar.getInstance();
        final String noIDPrefix = String.format("%1$tF_", calendar);
        File[] noIDFiles = OUT.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(noIDPrefix);
            }
        });
        assertEquals("the number of files with no ID should be correct",
                     2, noIDFiles.length);
        boolean foundStream = false;
        for (File noIDFile: noIDFiles) {
            assertTrue(String.format(
                    "The no ID file '%s' should have length > 0", noIDFile),
                       noIDFile.length() > 0);
            foundStream |= noIDFile.toString().endsWith(".stream");
        }
        assertTrue("One of the noID-files should end with .stream",
                   foundStream);
    }

}
