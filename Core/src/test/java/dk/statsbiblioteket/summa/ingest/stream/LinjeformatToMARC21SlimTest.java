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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.ingest.split.StreamController;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class LinjeformatToMARC21SlimTest extends TestCase {
    private static Log log = LogFactory.getLog(LinjeformatToMARC21SlimTest.class);

    public LinjeformatToMARC21SlimTest(String name) {
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
        return new TestSuite(LinjeformatToMARC21SlimTest.class);
    }

    public static final String CONFORMING =
            "001 000 *a9789221077442\n"
            + "004 000 *ru*ae\n"
            + "008 000 *tm*a1991*bch*da*dy*leng*v4*&2\n"
            + "009 000 *aa*gxx\n"
            + "021 000 *a9221077446*d45.00F\n"
            + "021 000 *e9789221077442\n"
            + "088 000 *a331.6*a(470)*a(47)\n"
            + "096 000 *b331.6 In*cb\n"
            + "245 000 *aIn @* hiding of narrowness*cthe new Danish labour salespoint*eedited by Standup Guy\n"
            + "$\n"
            + "001 000 *a9789221077443\n"
            + "004 000 *ru*ae\n"
            + "008 000 *tm*a1991*bch*da*dy*leng*v4*&2\n"
            + "009 000 *aa*gxx\n"
            + "021 000 *a9221077446*d45.00F\n"
            + "021 000 *e9789221077442\n"
            + "088 000 *a331.6*a(470)*a(47)\n"
            + "096 000 *b331.6 In*cb\n"
            + "245 000 *aIn hiding of @002B narrowness*c\n"
            + "    the new Danish labour salespoint*eedited by Standup Guy\n"
            + "$\n";

    public static final String NON_CONFORMING =
            "001 00/0 »a9789221077442«\n"
            + "004 00/0 »ru»ae«\n"
            + "008 00/0 »tm»a1991»bch»da»dy»leng»v4»&2«\n"
            + "009 00/0 »aa»gxx«\n"
            + "021 00/0 »a9221077446»d45.00F«\n"
            + "021 00/0 »e9789221077442«\n"
            + "088 00/0 »a331.6»a(470)»a(47)«\n"
            + "096 00/0 »b331.6 In»cb«\n"
            + "245 00/0 »aIn hiding of narrowness»cthe new Danish labour salespoint»eedited by Standup Guy«\n"
            + "\n"
            + "001 00/0 »a9789221077443«\n"
            + "004 00/0 »ru»ae«\n"
            + "008 00/0 »tm»a1991»bch»da»dy»leng»v4»&2«\n"
            + "009 00/0 »aa»gxx«\n"
            + "021 00/0 »a9221077446»d45.00F«\n"
            + "021 00/0 »e9789221077442«\n"
            + "088 00/0 »a331.6»a(470)»a(47)«\n"
            + "096 00/0 »b331.6@ In»cb«\n" // Invalid @
            + "245 00/0 »aIn hiding of narrowness»c"
            + "the new Danish labour salespoint»eedited by Standup Guy«";

    public void testConforming() throws Exception {
        Payload sourcePayload = new Payload(
                new ByteArrayInputStream(CONFORMING.getBytes(StandardCharsets.UTF_8)));
        PayloadFeederHelper feeder = new PayloadFeederHelper(
                Arrays.asList(sourcePayload));
        Configuration conf = Configuration.newMemoryBased(
                StreamController.CONF_PARSER,
                LinjeformatToMARC21Slim.class.getCanonicalName(),
                LinjeformatToMARC21Slim.CONF_INPUT_CHARSET, "utf-8");
        StreamController converter = new StreamController(conf);
        converter.setSource(feeder);
        Payload processed = converter.next();
        assertTrue("The processed Payload should contain a Stream",
                   processed.getStream() != null);
        String content = Strings.flushLocal(processed.getStream());
        assertFalse("There should be only a single payload",
                    converter.hasNext());
        log.info(content);
        // todo assert
    }

    public void testNonConforming() throws Exception {
        Payload sourcePayload = new Payload(
                new ByteArrayInputStream(NON_CONFORMING.getBytes(StandardCharsets.UTF_8)));
        PayloadFeederHelper feeder = new PayloadFeederHelper(
                Arrays.asList(sourcePayload));
        Configuration conf = Configuration.newMemoryBased(
                StreamController.CONF_PARSER,
                LinjeformatToMARC21Slim.class.getCanonicalName(),
                LinjeformatToMARC21Slim.CONF_INPUT_CHARSET, "utf-8",
                LinjeformatToMARC21Slim.CONF_DIVIDER, "»",
                LinjeformatToMARC21Slim.CONF_EOL, "«",
                LinjeformatToMARC21Slim.CONF_EOR, "");
        StreamController converter = new StreamController(conf);
        converter.setSource(feeder);
        Payload processed = converter.next();
        assertTrue("The processed Payload should contain a Stream",
                   processed.getStream() != null);
        String content = Strings.flushLocal(processed.getStream());
        assertFalse("There should be only a single payload",
                    converter.hasNext());
        log.info(content);
        // tODO
    }
}