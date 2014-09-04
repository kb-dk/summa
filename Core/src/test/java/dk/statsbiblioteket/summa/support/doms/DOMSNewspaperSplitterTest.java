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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.ingest.split.StreamController;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.File;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DOMSNewspaperSplitterTest extends TestCase {
    private static Log log = LogFactory.getLog(DOMSNewspaperSplitterTest.class);

    public void testProcessPayload() throws Exception {
        final File DOMS_ALTO = new File("/home/te/tmp/sumfresh/sites/aviser/avis_4f23.xml");
        if (!DOMS_ALTO.exists()) {
            log.info("Cannon run test as '" + DOMS_ALTO + "' does not exist");
        }

        ObjectFilter source = new PayloadFeederHelper(0, DOMS_ALTO.toString());
        ObjectFilter splitter = new StreamController(Configuration.newMemoryBased(
                StreamController.CONF_PARSER, DOMSNewspaperParser.class.getCanonicalName()
        ));
        splitter.setSource(source);
        assertTrue("There should be a Record available", splitter.hasNext());
        while (splitter.hasNext()) {
            Payload payload = splitter.next();
            log.info("Extracted " + payload.getId());
            System.out.println(RecordUtil.getString(payload));
        }
    }
}
