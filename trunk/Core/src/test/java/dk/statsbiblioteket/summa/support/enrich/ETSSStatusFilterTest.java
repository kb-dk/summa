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
package dk.statsbiblioteket.summa.support.enrich;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.marc.MARCObject;
import dk.statsbiblioteket.summa.common.marc.MARCObjectFactory;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ETSSStatusFilterTest extends TestCase {
    private final String HYPERION =
            "http://hyperion:8642/genericDerby/services/GenericDBWS?method=getFromDB&arg0=access_etss_$ID_AND_PROVIDER";

    public ETSSStatusFilterTest(String name) {
        super(name);
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
        return new TestSuite(ETSSStatusFilterTest.class);
    }

    public void testExistingFromID() throws IOException, XMLStreamException, ParseException {
        String[] existing = new String[] {
            "0040-5671_theologischeliteraturzeitung",
            "1902-3545_stateanduniversitylibraryejournalholdings"
        };
        for (String e: existing) {
            assertStatusFromID(e, true);
        }
    }

    public void testExistingFromMarc() throws IOException, XMLStreamException, ParseException {
        String[] existing = new String[] {
            "common/marc/existing_022a.xml",
            "common/marc/existing_022x.xml",
            "common/marc/existing_022l.xml"
        };
        for (String e: existing) {
            assertStatus(e, true);
        }
    }

    public void testFlow() throws IOException, XMLStreamException, ParseException {
        String[] existing = new String[] {
            "common/marc/existing_022a.xml",
            "common/marc/existing_022x.xml",
            "common/marc/existing_022l.xml"
        };
        PayloadFeederHelper feeder = new PayloadFeederHelper(existing);
        ETSSStatusFilter etss = new ETSSStatusFilter(Configuration.newMemoryBased(
                ETSSStatusFilter.CONF_REST, HYPERION
        ));
        etss.setSource(feeder);
        List<Payload> pumped = pump(etss);
        assertEquals("There should be " + existing.length + " enriched Payloads",
                     existing.length, pumped.size());

        // Re-process no discard
        feeder = new PayloadFeederHelper(pumped);
        etss = new ETSSStatusFilter(Configuration.newMemoryBased(
                ETSSStatusFilter.CONF_REST, HYPERION,
                ETSSStatusFilter.CONF_CLEAN_PREVIOUS_STATUS, true,
                ETSSStatusFilter.CONF_DISCARD_UNCHANGED, false
        ));
        etss.setSource(feeder);
        pumped = pump(etss);
        assertEquals("There should be " + existing.length + " re-enriched Payloads without discarding",
                     existing.length, pumped.size());

        // Re-process
        feeder = new PayloadFeederHelper(pumped);
        etss = new ETSSStatusFilter(Configuration.newMemoryBased(
                ETSSStatusFilter.CONF_REST, HYPERION,
                ETSSStatusFilter.CONF_CLEAN_PREVIOUS_STATUS, true,
                ETSSStatusFilter.CONF_DISCARD_UNCHANGED, true
        ));
        etss.setSource(feeder);
        pumped = pump(etss);
        assertEquals("There should be 0 re-enriched Payloads with discarding",
                     0, pumped.size());
    }

    public void testUpdateClean() throws IOException, XMLStreamException, ParseException {
        String[] existing = new String[] {
            "common/marc/existing_022a.xml",
            "common/marc/existing_022x.xml",
            "common/marc/existing_022l.xml"
        };
        PayloadFeederHelper feeder = new PayloadFeederHelper(existing);
        ETSSStatusFilter etss = new ETSSStatusFilter(Configuration.newMemoryBased(
                ETSSStatusFilter.CONF_REST, HYPERION,
                ETSSStatusFilter.CONF_CLEAN_PREVIOUS_STATUS, true,
                ETSSStatusFilter.CONF_DISCARD_UNCHANGED, true
        ));
        etss.setSource(feeder);
        List<Payload> pumped = pump(etss);
        assertEquals("There should be " + existing.length + " enriched Payloads",
                     existing.length, pumped.size());
    }

    private List<Payload> pump(ObjectFilter filter) {
        List<Payload> pumped = new ArrayList<Payload>();
        while (filter.hasNext()) {
            pumped.add(filter.next());
        }
        filter.close(true);
        return pumped;
    }

    public void testNonExisting() throws IOException, XMLStreamException, ParseException {
        assertStatus("common/marc/single_marc.xml", false);
        assertStatus("common/marc/existing_245a.xml", false); // Due to collapse of '-' in 245*a
    }

    public void testIDAddition() throws IOException, XMLStreamException, ParseException {
        String EXPECTED = "0040-5671_theologischeliteraturzeitung";
        String SOURCE = "common/marc/existing_marc.xml";
        PayloadFeederHelper feeder = new PayloadFeederHelper(Arrays.asList(
            new Payload(new FileInputStream(Resolver.getFile(SOURCE)), SOURCE)
        ));
        ETSSStatusFilter statusFilter = new ETSSStatusFilter(Configuration.newMemoryBased(
            ETSSStatusFilter.CONF_REST, HYPERION
        ));
        statusFilter.setSource(feeder);
        Payload processed = statusFilter.next();
        MARCObject marc = MARCObjectFactory.generate(RecordUtil.getStream(processed)).get(0);
        assertEquals("The generated ID should be correct", EXPECTED, marc.getFirstSubField("856", "w").getContent());
        statusFilter.close(true);
    }

    public void testNormaliseID() throws IOException, XMLStreamException, ParseException {
        testNormaliseID("common/marc/normalisetest_245a.xml", "sometitle_theologischeliteraturzeitung");
    }

    public void testExtractID() throws IOException, XMLStreamException, ParseException {
        testNormaliseID("common/marc/existing_022a.xml", "0040-5671_theologischeliteraturzeitung");
    }

    public void testNormaliseID(String marcFile, String expected)
        throws IOException, XMLStreamException, ParseException {
        PayloadFeederHelper feeder = new PayloadFeederHelper(Arrays.asList(
            new Payload(new FileInputStream(Resolver.getFile(marcFile)), marcFile)
        ));
        ETSSStatusFilter statusFilter = new ETSSStatusFilter(Configuration.newMemoryBased(
            ETSSStatusFilter.CONF_REST, HYPERION
        ));
        statusFilter.setSource(feeder);
        Payload processed = statusFilter.next();
        MARCObject marc = MARCObjectFactory.generate(RecordUtil.getStream(processed)).get(0);
        assertEquals("The generated ID should be correct", expected, marc.getFirstSubField("856", "w").getContent());
        statusFilter.close(true);
    }

    public void testProviderNormaliser() {
        ETSSStatusFilter statusFilter = new ETSSStatusFilter(Configuration.newMemoryBased(
            ETSSStatusFilter.CONF_REST, "dummy"
        ));
        assertEquals("foobar", statusFilter.normaliseProvider("Foo Bar"));
        assertEquals("foobar", statusFilter.normaliseProvider("Foo Bæar"));
        assertEquals("foobar87", statusFilter.normaliseProvider("Foo Bæar87"));
        assertEquals("annlasejatr", statusFilter.normaliseProvider("annl`ase=jat_r"));
    }

    private void assertStatusFromID(String id, boolean hasPassword) throws IOException {
        ETSSStatusFilter statusFilter = new ETSSStatusFilter(Configuration.newMemoryBased(
            ETSSStatusFilter.CONF_REST, HYPERION
        ));
        assertEquals("The password status for " + id + " should be correct",
                     hasPassword,
                     statusFilter.needsPassword(statusFilter.lookup(id, statusFilter.getLookupURI(id, id))));
    }

    public void testCommentID() throws IOException, XMLStreamException, ParseException {
        String[][] TESTS = new String[][] {
            // Hand held by Hans, now disabled
            //{"1397-3290_freeaarhusuniversitylibrariesejournalholdings", "Dette er en kommentar"},
            // æ is entity-escaped
            {"1602-7930_aarhusuniversitylibrariesejournalholdings", "Klik på gæstelogin under den enkelte artikel"}
        };
        for (String[] test: TESTS) {
            assertCommentID(test[0], test[1]);
        }
    }

    private void assertCommentID(String id, String comment) throws IOException {
        ETSSStatusFilter statusFilter = new ETSSStatusFilter(Configuration.newMemoryBased(
            ETSSStatusFilter.CONF_REST,
            HYPERION
        ));
        assertEquals("The comment for " + id + " should be correct",
                     comment, statusFilter.getComment(statusFilter.lookup(id, statusFilter.getLookupURI(id, id))));
    }

    public void assertStatus(String marcFile, boolean hasPassword)
        throws IOException, XMLStreamException, ParseException {
        PayloadFeederHelper feeder = new PayloadFeederHelper(Arrays.asList(
            new Payload(new FileInputStream(Resolver.getFile(marcFile)), marcFile)
        ));
        ETSSStatusFilter statusFilter = new ETSSStatusFilter(Configuration.newMemoryBased(
            ETSSStatusFilter.CONF_REST, HYPERION
        ));
        statusFilter.setSource(feeder);
        assertTrue("There should be at least one Payload", statusFilter.hasNext());
        Payload processed = statusFilter.next();
        assertFalse("There should be no more Payloads", statusFilter.hasNext());
        MARCObject marc = MARCObjectFactory.generate(RecordUtil.getStream(processed)).get(0);
        assertNotNull("The ID should be present in field 856*k from input " + marcFile,
                      marc.getFirstSubField("856", "w"));
        if (hasPassword) {
            assertNotNull("MARC object '" + marcFile + "' should contain a password marker",
                         marc.getFirstSubField("856", ETSSStatusFilter.PASSWORD_SUBFIELD));
            assertEquals("MARC object '" + marcFile + "' should be marked as requiring password",
                         ETSSStatusFilter.PASSWORD_CONTENT,
                         marc.getFirstSubField("856", ETSSStatusFilter.PASSWORD_SUBFIELD).getContent());
        } else if (marc.getFirstSubField("856", ETSSStatusFilter.PASSWORD_SUBFIELD) != null
                   && ETSSStatusFilter.PASSWORD_CONTENT.equals(
            marc.getFirstSubField("856", ETSSStatusFilter.PASSWORD_SUBFIELD).getContent())) {
            fail("MARC object '" + marcFile + "' should not be marked as requiring password but was");
        }
        statusFilter.close(true);
    }
}
