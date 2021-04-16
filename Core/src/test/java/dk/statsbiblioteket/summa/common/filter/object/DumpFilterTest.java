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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * DumpFilter Tester.
 *
 * @author  tokerefind
 * @since <pre>03/31/2009</pre>
 * @version 1.0
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class DumpFilterTest extends TestCase {
    private static Log log = LogFactory.getLog(DumpFilterTest.class);

    @SuppressWarnings({"DuplicateStringLiteralInspection"})

    private static final File OUT = new File("target/tmp/dumpTest");
            /*new File(new File(System.getProperty(
            "java.io.tmpdir")), "dumpTest");*/

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
        try {
                Files.delete(OUT);
        } catch (IOException e) {
                fail("Unable to delete the folder " + OUT);
        }
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
                        "<myxml>My content</myxml>".getBytes(StandardCharsets.UTF_8)))
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
        final String noIDPrefix = String.format(Locale.ROOT, "%1$tF_", calendar);
        File[] noIDFiles = OUT.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(noIDPrefix);
            }
        });
        assertEquals("the number of files with no ID should be correct",
                     2, noIDFiles.length);
        boolean foundStream = false;
        for (File noIDFile: noIDFiles) {
            assertTrue(String.format(Locale.ROOT,
                    "The no ID file '%s' should have length > 0", noIDFile),
                       noIDFile.length() > 0);
            foundStream |= noIDFile.toString().endsWith(".stream");
        }
        assertTrue("One of the noID-files should end with .stream",
                   foundStream);
    }

    public void testParentChild() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                DumpFilter.CONF_OUTPUTFOLDER, OUT.getAbsolutePath(),
                DumpFilter.CONF_DUMP_STREAMS, true,
                DumpFilter.CONF_DUMP_XML, true,
                DumpFilter.CONF_DUMP_XML, true);
        Record origin = new Record("¤%&&:id-flam87:", "mybase", "<myxml>Origin</myxml>".getBytes(StandardCharsets.UTF_8));
        Record parent = new Record("parentID", "mybase", "<myxml>Parent</myxml>".getBytes(StandardCharsets.UTF_8));
        origin.setChildren(Arrays.asList(parent));
        List<Payload> payloads = Arrays.asList(new Payload(origin));
        ObjectFilter feeder = new PayloadFeederHelper(payloads);
        ObjectFilter dumpFilter = new DumpFilter(conf);
        dumpFilter.setSource(feeder);

        int count = 0;
        while (dumpFilter.hasNext()) {
            log.debug("Pumped " + dumpFilter.pump());
            count++;
        }
        assertEquals("The number of processed Payloads should match", 1, count);
        assertEquals("The number of created files should match",
                     3, OUT.listFiles().length);
        File ec = new File(OUT, "_____id-flam87_.content");
        File em = new File(OUT, "_____id-flam87_.meta");
        File ex = new File(OUT, "_____id-flam87_.xml");
        assertTrue("The expected content-file '" + ec + "' should be present",
                   ec.exists());
        assertTrue("The expected meta-file '" + em + "' should be present",
                   em.exists());
        assertTrue("The expected XML-file '" + ex + "' should be present",
                   ex.exists());
        final String xml = Files.loadString(ex);
        assertTrue("The XML dump should contain '<content'\n" + xml, xml.contains("<content"));
        assertTrue("The XML dump should contain '<children>'\n" + xml, xml.contains("<children>"));
        log.info("Content of XML-dump:\n" + xml);
    }
}

