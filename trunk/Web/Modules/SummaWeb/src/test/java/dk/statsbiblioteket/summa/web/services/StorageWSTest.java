/**  Licensed under the Apache License, Version 2.0 (the "License");
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
package dk.statsbiblioteket.summa.web.services;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

/* Not really a test, more of a debug tool */

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StorageWSTest extends TestCase {

    public StorageWSTest(String name) {
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

    public void testGetRecord() {
        final String ID = "doms:dda_paper_vol2_paper14-8";
//        final String ID = "oai:doaj-articles:3a087332fac5eed3a0e5a4c1bba3c8ac";
        StorageWS.conf =  Configuration.newMemoryBased(
            ConnectionConsumer.CONF_RPC_TARGET, "//localhost:57000/sb-storage",
            RecordUtil.CONF_ESCAPE_CONTENT, false
        );
        StorageWS storage = new StorageWS();
        System.out.println(storage.getRecord(ID).replace(">", ">\n"));
    }
}
