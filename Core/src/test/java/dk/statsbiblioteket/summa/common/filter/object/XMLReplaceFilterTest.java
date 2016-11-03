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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import junit.framework.TestCase;

import java.io.IOException;

public class XMLReplaceFilterTest extends TestCase {

    public void testSpaceRemoval() throws IOException {
        PayloadFeederHelper feeder = new PayloadFeederHelper("common/marc/marc_replacement_sample.xml");
        XMLReplaceFilter replacer = new XMLReplaceFilter(Configuration.newMemoryBased(
                XMLReplaceFilter.CONF_ID_FIELDS, "001*a,002*a,002*b"
        ));
        replacer.setSource(feeder);
        assertEquals("The replaced XML should be as expected",
                     Resolver.getUTF8Content("common/marc/marc_replacement_sansspace.xml"),
                     RecordUtil.getString(replacer.next()) + "\n");
    }
}