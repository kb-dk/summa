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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ETSSStatusFilterTest extends TestCase {
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

    public void testExisting() throws IOException, XMLStreamException, ParseException {
        assertStatus("common/marc/existing_marc.xml", true);
    }

    public void testNonExisting() throws IOException, XMLStreamException, ParseException {
        assertStatus("common/marc/single_marc.xml", false);
    }

    public void testIDAddition() throws IOException, XMLStreamException, ParseException {
        String EXPECTED = "0040-5671_theologischeliteraturzeitung";
        PayloadFeederHelper feeder = new PayloadFeederHelper(Arrays.asList(
            new Payload(new FileInputStream(Resolver.getFile("common/marc/existing_marc.xml")))
        ));
        ETSSStatusFilter statusFilter = new ETSSStatusFilter(Configuration.newMemoryBased(
            ETSSStatusFilter.CONF_REST,
            "http://hyperion:8642/genericDerby/services/GenericDBWS?method=getFromDB&arg0=access_etss_$ID_AND_PROVIDER"
        ));
        statusFilter.setSource(feeder);
        Payload processed = statusFilter.next();
        MARCObject marc = MARCObjectFactory.generate(RecordUtil.getStream(processed)).get(0);
        assertEquals("The generated ID should be correct", EXPECTED, marc.getFirstSubField("856", "w").getContent());
        statusFilter.close(true);
    }

    public void testIDNormaliser() {
        ETSSStatusFilter statusFilter = new ETSSStatusFilter(Configuration.newMemoryBased(
            ETSSStatusFilter.CONF_REST,
            "http://hyperion:8642/genericDerby/services/GenericDBWS?method=getFromDB&arg0=access_etss_$ID_$PROVIDER"
        ));
        String CORRECT = "etss12345678";
        String CORRECT_N = "1234-5678";

        String INCORRECT1 = "ets12345678";
        String INCORRECT2 = "etss1234a678";
        assertEquals("The proper ID " + CORRECT + " should be parsed ", CORRECT_N, statusFilter.normaliseID(CORRECT));
        assertEquals("The faulty ID " + INCORRECT1 + " should be returned verbatim ",
                     INCORRECT1, statusFilter.normaliseID(INCORRECT1));
        assertEquals("The faulty ID " + INCORRECT2 + " should be returned verbatim ",
                     INCORRECT2, statusFilter.normaliseID(INCORRECT2));
    }

    public void testProviderNormaliser() {
        ETSSStatusFilter statusFilter = new ETSSStatusFilter(Configuration.newMemoryBased(
            ETSSStatusFilter.CONF_REST,
            "http://hyperion:8642/genericDerby/services/GenericDBWS?method=getFromDB&arg0=access_etss_$ID_$PROVIDER"
        ));
        assertEquals("foobar", statusFilter.normaliseProvider("Foo Bar"));
        assertEquals("foobar", statusFilter.normaliseProvider("Foo Bæar"));
        assertEquals("foobar87", statusFilter.normaliseProvider("Foo Bæar87"));
    }

    public void assertStatus(String input, boolean hasPassword) throws IOException, XMLStreamException, ParseException {
        PayloadFeederHelper feeder = new PayloadFeederHelper(Arrays.asList(
            new Payload(new FileInputStream(Resolver.getFile(input)))
        ));
        ETSSStatusFilter statusFilter = new ETSSStatusFilter(Configuration.newMemoryBased(
            ETSSStatusFilter.CONF_REST,
            "http://hyperion:8642/genericDerby/services/GenericDBWS?method=getFromDB&arg0=access_etss_$ID_AND_PROVIDER"
        ));
        statusFilter.setSource(feeder);
        assertTrue("There should be at least one Payload", statusFilter.hasNext());
        Payload processed = statusFilter.next();
        assertFalse("There should be no more Payloads", statusFilter.hasNext());
        MARCObject marc = MARCObjectFactory.generate(RecordUtil.getStream(processed)).get(0);
        if (hasPassword) {
            assertEquals("MARC object '" + input + "' should be marked as requiring password",
                         ETSSStatusFilter.PASSWORD_CONTENT,
                         marc.getFirstSubField("856", ETSSStatusFilter.PASSWORD_SUBFIELD).getContent());
        } else if (marc.getFirstSubField("856", ETSSStatusFilter.PASSWORD_SUBFIELD) != null
                   && ETSSStatusFilter.PASSWORD_CONTENT.equals(
            marc.getFirstSubField("856", ETSSStatusFilter.PASSWORD_SUBFIELD).getContent())) {
            fail("MARC object '" + input + "' should not be marked as requiring password but was");
        }
        statusFilter.close(true);
    }

}
