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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;

/* Not really a test, more of a debug tool */

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StorageWSTest extends TestCase {
    private static Log log = LogFactory.getLog(StorageWSTest.class);

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

    public void testInnerRMIWrappedH2() throws InterruptedException, IOException {
        // Create a Storage inside of the StorageWS
        {
            File storageConf = Resolver.getFile("storagews_rmi_h2.xml");
            assertNotNull("There should be a storage configuration file", storageConf);
            assertTrue("The storage configuration file '" + storageConf + "' should exist", storageConf.exists());
            StorageWS.conf = Configuration.load(storageConf.getAbsolutePath());
            StorageWS storageWS = new StorageWS();
            log.info("Created StorageWS. Priming inner storage by requesting dummy record");
            storageWS.getRecord("nonexisting");
        }

        log.info("Attempting to connect from the outside using StorageReaderClient...");
        Thread.sleep(100);

        {
            // Connect to the storage from the outside
            StorageReaderClient storageReader = new StorageReaderClient(Configuration.newMemoryBased(
                    ConnectionConsumer.CONF_RPC_TARGET, "//localhost:51700/aviser-storage"
            ));
            log.info("StorageReaderClient connected successfully");

            Record outRecord = storageReader.getRecord("foo", null);
            assertNull("No record should be returned", outRecord);
        }
    }

    public void testGetRecord() {
//        final String ID = "oai:open-archive.highwire.org:vetrec:174/18/456";
//        final String ID = "oai:doaj-articles:3a087332fac5eed3a0e5a4c1bba3c8ac";
        final String ID = "sb_pure_ddfmxd:5357";
        StorageWS.conf =  Configuration.newMemoryBased(
            //ConnectionConsumer.CONF_RPC_TARGET, "//mars:56600/aulhub",
            ConnectionConsumer.CONF_RPC_TARGET, "//localhost:57000/sb-storage",
            RecordUtil.CONF_ESCAPE_CONTENT, false
        );
        StorageWS storage = new StorageWS();
        String recXML = storage.getRecord(ID);
        assertTrue("A record should be returned", recXML.contains("<record"));
        System.out.println(recXML.replace(">", ">\n"));
    }

    public void disabledtestGetRecordsSB() {
//        final String ID = "oai:open-archive.highwire.org:vetrec:174/18/456";
//        final String ID = "oai:doaj-articles:3a087332fac5eed3a0e5a4c1bba3c8ac";
        final String ID = "sb_pure_ddfmxd:5357";
        StorageWS.conf =  Configuration.newMemoryBased(
            //ConnectionConsumer.CONF_RPC_TARGET, "//mars:56600/aulhub",
            ConnectionConsumer.CONF_RPC_TARGET, "//localhost:57000/sb-storage",
            RecordUtil.CONF_ESCAPE_CONTENT, false
        );
        StorageWS storage = new StorageWS();
        String recXML = storage.getRecords(new String[]{ID});
        assertTrue("A record should be returned", recXML.contains("<record"));
        System.out.println(recXML.replace(">", ">\n"));
    }

    // Requires a local 
    public void testGetRecordsDOMS() {
        final String ID = "sb_pure_ddfmxd:5357";
        StorageWS.conf =  Configuration.newMemoryBased(
            //ConnectionConsumer.CONF_RPC_TARGET, "//mars:56600/aulhub",
            ConnectionConsumer.CONF_RPC_TARGET, "//localhost:57000/sb-storage",
            RecordUtil.CONF_ESCAPE_CONTENT, false
        );
        StorageWS storage = new StorageWS();
        String recXML = storage.getRecords(new String[]{ID});
        assertTrue("A record should be returned", recXML.contains("<record"));
        System.out.println(recXML.replace(">", ">\n"));
    }

    public void testSpecificLocal() {
        final String CONF = "/home/te/tmp/records/storagews_aulhub_conf.xml";
        final String[] IDS = new String[]{
                "etss_ssj0000401761",
                "doms_radioTVCollection:uuid:4fc889eb-8b5d-4832-95d1-4fe5825c424d"
        };

        if (!new File(CONF).exists()) {
            return;
        }

        System.setProperty(StorageWS.CONFIGURATION_LOCATION, CONF);
        StorageWS storage = new StorageWS();

        for (String id: IDS) {
            String response = storage.getRecords(new String[]{id});
            assertNotNull("There should be a response for Record ID '" + id + "'", response);
            assertTrue("The response should contain a Record with ID '" + id + "'\n" + response,
                       response.contains(id));
        }
    }
}
