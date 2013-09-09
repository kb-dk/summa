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
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.support.alto.as2.AS2AltoAnalyzerTest;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoGeneratorFilterTest extends TestCase {
    private static Log log = LogFactory.getLog(AltoGeneratorFilterTest.class);

    public void testBasicGeneration() throws Exception {
        final int RECORDS = 5;

        AltoGeneratorFilter generator = new AltoGeneratorFilter(Configuration.newMemoryBased(
                AltoGeneratorFilter.CONF_RANDOM_SEED, 87,
                AltoGeneratorFilter.CONF_RECORDS, RECORDS
        ));
        generator.setSource(getFeeder());
        assertTrue("There should be a Payload", generator.hasNext());
        int received = 0;
        while (generator.hasNext()) {
            generator.next();
            received++;
        }
        assertEquals("The correct number of ALTO records should be produced", RECORDS, received);
    }



    private ObjectFilter getFeeder() throws IOException {
        return new PayloadFeederHelper(AS2AltoAnalyzerTest.s1795_1, AS2AltoAnalyzerTest.s1846_1);
    }

}
