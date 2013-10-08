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
import dk.statsbiblioteket.summa.support.alto.as2.AS2AltoAnalyzerTest;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoStatFilterTest extends TestCase {
    private static Log log = LogFactory.getLog(AltoStatFilterTest.class);

    public void testBasicStats() throws Exception {
        ObjectFilter feeder = new PayloadFeederHelper(AS2AltoAnalyzerTest.s1795_1, AS2AltoAnalyzerTest.s1846_1);
        Configuration conf = Configuration.newMemoryBased(
                AltoStatFilter.CONF_REGEXPS, "a[a-z]+"
        );
        AltoStatFilter stats = new AltoStatFilter(conf);
        stats.setSource(feeder);

        //noinspection StatementWithEmptyBody
        while (stats.pump());

        System.out.println(stats.getStats());
    }
}
