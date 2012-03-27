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
package dk.statsbiblioteket.summa.common.filter;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.object.DummyStreamToRecords;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.filter.stream.DummyReader;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.List;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class FilterControlTest extends TestCase {
    private static Log log = LogFactory.getLog(FilterControlTest.class);

    public FilterControlTest(String name) {
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

    public void testGetVerboseStatus() throws Exception {
        //TODO: Test goes here...
    }

    public static Test suite() {
        return new TestSuite(FilterControlTest.class);
    }

    /* Deprecated as it uses Dummyreader */
    private void makeSimple(Configuration conf) throws Exception {
//        List<Configuration> chainConfs =
                conf.createSubConfigurations(FilterControl.CONF_CHAINS, 1);
//        chainConfs.get(0).set(Filter.CONF_FILTER_NAME, "FilterPumptest");
        List<Configuration> filterConfs =
                conf.createSubConfigurations(
                        FilterSequence.CONF_FILTERS,  2);

        Configuration streamSub = filterConfs.get(0);
        streamSub.set(Filter.CONF_FILTER_NAME, "Streamer");
        streamSub.set(FilterSequence.CONF_FILTER_CLASS,
                       DummyReader.class.getName());
        streamSub.set(DummyReader.CONF_BODY_COUNT, 3);
        streamSub.set(DummyReader.CONF_BODY_SIZE, 100);

        Configuration convertSub = filterConfs.get(1);
        streamSub.set(Filter.CONF_FILTER_NAME, "Converter");
        convertSub.set(FilterSequence.CONF_FILTER_CLASS,
                       DummyStreamToRecords.class.getName());
        convertSub.set(DummyStreamToRecords.CONF_DATA_SIZE, 99);
    }

    public void testDumpConfig() throws Exception {
        Configuration ingesterStorage = Configuration.newMemoryBased();
        Configuration pumpStorage =
                ingesterStorage.createSubConfiguration("TestPump");
        makeSimple(pumpStorage);
    }

    /*
     * Creates a simple chain and returns the number of records processed by
     * all executed DummyStreamToRecords.
     * Note: Multiple calls will increase this number.
     */
    private int simpleResult() throws Exception {
        Configuration ingestConf = Configuration.newMemoryBased();
        Configuration pumpStorage = ingestConf.createSubConfigurations(
                FilterControl.CONF_CHAINS, 1).get(0);
        pumpStorage.set(Filter.CONF_FILTER_NAME, "TestPump");
        makeSimple(pumpStorage);
//        ingestConf.setStrings(FilterControl.CONF_CHAINS,
//                              Arrays.asList("TestPump"));
        ingestConf.set(FilterControl.CONF_SEQUENTIAL, true);
        FilterControl ingester = new FilterControl(
                new Configuration(ingestConf));
        ingester.start();
        ingester.waitForFinish();
        return DummyStreamToRecords.getIdCount();
    }


    /* Deprecated as it uses Dummyreader */
    public void invalidtestSimple() throws Exception {
        int beginCount = DummyStreamToRecords.getIdCount();
        assertTrue("The number of processed records after run 1 should be > 0",
                   simpleResult() > beginCount);
        beginCount = DummyStreamToRecords.getIdCount();
        assertTrue("The number of processed records after run 1 should be > 0, "
                   + "counted by the Dummy filter",
                   simpleResult() > beginCount);
    }

    /* Deprecated as it uses Dummyreader */
    public void invalidtestMultipleSequential() throws Exception {
        testMultiple(true);
    }

    /* Deprecated as it uses Dummyreader */
    public void invalidtestMultipleParallel() throws Exception {
        testMultiple(false);
    }

    public void testMultiple(boolean sequential) throws Exception {
        DummyStreamToRecords.clearIdCount();
        int singleRun = simpleResult();
        DummyStreamToRecords.clearIdCount();

        Configuration ingestConf = Configuration.newMemoryBased();
        List<Configuration> chains = ingestConf.createSubConfigurations(
                FilterControl.CONF_CHAINS, 2);

        Configuration pumpStorage1 = chains.get(0);
        pumpStorage1.set(Filter.CONF_FILTER_NAME, "TestPump1");
        makeSimple(pumpStorage1);
        Configuration pumpStorage2 = chains.get(1);
        pumpStorage1.set(Filter.CONF_FILTER_NAME, "TestPump2");
        makeSimple(pumpStorage2);

//        ingestConf.setStrings(FilterControl.CONF_CHAINS,
//                              Arrays.asList("TestPump1", "TestPump2"));
        ingestConf.set(FilterControl.CONF_SEQUENTIAL, true);

        FilterControl ingester =
                new FilterControl(new Configuration(ingestConf));
        ingester.start();
        ingester.waitForFinish();
        assertEquals("The number of records processed (sequential: "
                     + sequential + ") should be double of single run",
                     singleRun*2, DummyStreamToRecords.getIdCount());
    }

    /* Deprecated as it uses Dummyreader */
    public void invalidtestMultipleXMLSetup() throws Exception {
        DummyStreamToRecords.clearIdCount();
        int singleRun = simpleResult();
        DummyStreamToRecords.clearIdCount();

        File confLocation =
                new File("Common/test/dk/statsbiblioteket/summa/"
                         + "common/filter/filter_setup.xml").getAbsoluteFile();
        log.debug("Loading configuration from " + confLocation);
        Configuration conf = Configuration.load(confLocation.getPath());

        assertNotNull("Configuration should contain "
                      + FilterControl.CONF_CHAINS,
                      conf.getSubConfigurations(FilterControl.CONF_CHAINS));

        FilterControl ingester = new FilterControl(conf);
        ingester.start();
        ingester.waitForFinish();
        assertEquals("The number of records processed  should be double of "
                     + "a single run",
                     singleRun * 2, DummyStreamToRecords.getIdCount());
    }
}

