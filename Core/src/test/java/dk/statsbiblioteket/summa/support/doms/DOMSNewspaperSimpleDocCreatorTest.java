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
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DOMSNewspaperSimpleDocCreatorTest extends TestCase {
    private static Log log = LogFactory.getLog(DOMSNewspaperSimpleDocCreatorTest.class);

    public void testProcessPayload() throws Exception {
        final File DOMS_ALTO = new File("/home/te/tmp/sumfresh/sites/aviser/avis_4f23.xml");
        if (!DOMS_ALTO.exists()) {
            log.info("Cannon run test as '" + DOMS_ALTO + "' does not exist");
        }

        ObjectFilter source = new PayloadFeederHelper(DOMS_ALTO.toString());
        ObjectFilter simpleCreator = new DOMSNewspaperSimpleDocCreator(Configuration.newMemoryBased(

        ));
        simpleCreator.setSource(source);
        assertTrue("There should be a Record available", simpleCreator.hasNext());
        Payload payload = simpleCreator.next();
        System.out.println(payload.getRecord().getContentAsUTF8().replace("<", "\n<"));
    }
}
