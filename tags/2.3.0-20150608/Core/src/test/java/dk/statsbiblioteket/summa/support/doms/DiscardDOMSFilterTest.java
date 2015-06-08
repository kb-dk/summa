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
package dk.statsbiblioteket.summa.support.doms;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DiscardDOMSFilterTest extends TestCase {
    private static Log log = LogFactory.getLog(DiscardDOMSFilterTest.class);

    // TODO: We do not have license to publish these files. Generate obfuscated test files from them
    public static final String TEST_FILE = "/home/te/tmp/1sec.xml";
    public static final String TEST_FILE_HOLE = "/home/te/tmp/hole.xml";

    public DiscardDOMSFilterTest(String name) {
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
        return new TestSuite(DiscardDOMSFilterTest.class);
    }


    public void testDefaultNoDiscard() throws IOException {
        testDeleteStatus(Configuration.newMemoryBased(), false);
    }

    public void testDateDiscard() throws IOException {
        testDeleteStatus(Configuration.newMemoryBased(
                DiscardDOMSFilter.CONF_DATE_LASTVALID, "2012-04-01"
        ), true);
    }

    public void testHolePass() throws IOException {
        testGeneric(Configuration.newMemoryBased(
                DiscardDOMSFilter.CONF_GAP_MIDDLE_MAX, 60
        ), TEST_FILE_HOLE, false);
    }

    public void testHolediscard() throws IOException {
        testGeneric(Configuration.newMemoryBased(
                DiscardDOMSFilter.CONF_GAP_MIDDLE_MAX, 59
        ), TEST_FILE_HOLE, true);
    }


    private void testDeleteStatus(Configuration conf, boolean isDeleted) throws IOException {
        testGeneric(conf, TEST_FILE, isDeleted);
    }

    private void testGeneric(Configuration conf, String input, boolean isDeleted) throws IOException {
        PayloadFeederHelper feeder = new PayloadFeederHelper(Arrays.asList(
                new Payload(new Record("doms_radioTVCollection_dummy_0", "doms_radiotv",
                                       Files.loadString(new File(input)).getBytes()))));
        DiscardDOMSFilter domsDiscarder = new DiscardDOMSFilter(conf);
        domsDiscarder.setSource(feeder);
        long startTime = System.currentTimeMillis();
        if (isDeleted) {
            assertFalse("There should not be a record available", domsDiscarder.hasNext());
            log.info("Processing time: " + (System.currentTimeMillis()-startTime) + "ms");
            return;
        }
        assertTrue("There should be a record available", domsDiscarder.hasNext());
        log.info("Processing time: " + (System.currentTimeMillis() - startTime) + "ms");
        Payload processed = domsDiscarder.next();
        assertEquals("The processed Payload's delete status should be correct", processed.getRecord().isDeleted(),
                     isDeleted);
    }
}
