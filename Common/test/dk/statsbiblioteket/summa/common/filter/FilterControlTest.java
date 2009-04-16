package dk.statsbiblioteket.summa.common.filter;

import java.util.List;
import java.io.File;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.filter.object.DummyStreamToRecords;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.filter.stream.DummyReader;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private void makeSimple(ConfigurationStorage storage) throws Exception {
        List<ConfigurationStorage> chainConfs =
                storage.createSubStorages(FilterControl.CONF_CHAINS, 1);
        chainConfs.get(0).put(Filter.CONF_FILTER_NAME, "FilterPumptest");
        List<ConfigurationStorage> filterConfs =
                chainConfs.get(0).createSubStorages(
                        FilterSequence.CONF_FILTERS,  2);

        ConfigurationStorage streamSub = filterConfs.get(0);
        streamSub.put(Filter.CONF_FILTER_NAME, "Streamer");
        streamSub.put(FilterSequence.CONF_FILTER_CLASS,
                       DummyReader.class.getName());
        streamSub.put(DummyReader.CONF_BODY_COUNT, 3);
        streamSub.put(DummyReader.CONF_BODY_SIZE, 100);

        ConfigurationStorage convertSub = filterConfs.get(1);
        streamSub.put(Filter.CONF_FILTER_NAME, "Converter");
        convertSub.put(FilterSequence.CONF_FILTER_CLASS,
                       DummyStreamToRecords.class.getName());
        convertSub.put(DummyStreamToRecords.CONF_DATA_SIZE, 99);
    }

    public void testDumpConfig() throws Exception {
        XStorage ingesterStorage = new XStorage();
        ConfigurationStorage pumpStorage =
                ingesterStorage.createSubStorage("TestPump");
        makeSimple(pumpStorage);
        new Configuration(ingesterStorage);
    }

    /*
     * Creates a simple chain and returns the number of records processed by
     * all executed DummyStreamToRecords.
     * Note: Multiple calls will increase this number.
     */
    private int simpleResult() throws Exception {
        XStorage ingesterStorage = new XStorage();
        ConfigurationStorage pumpStorage = ingesterStorage.createSubStorages(
                FilterControl.CONF_CHAINS, 1).get(0);
        pumpStorage.put(Filter.CONF_FILTER_NAME, "TestPump");
        makeSimple(pumpStorage);
        Configuration ingestConf = new Configuration(ingesterStorage);
//        ingestConf.setStrings(FilterControl.CONF_CHAINS,
//                              Arrays.asList("TestPump"));
        ingestConf.set(FilterControl.CONF_SEQUENTIAL, true);
        FilterControl ingester = new FilterControl(
                new Configuration(ingesterStorage));
        ingester.start();
        ingester.waitForFinish();
        return DummyStreamToRecords.getIdCount();
    }


    public void testSimple() throws Exception {
        int beginCount = DummyStreamToRecords.getIdCount();
        assertTrue("The number of processed records after run 1 should be > 0",
                   simpleResult() > beginCount);
        beginCount = DummyStreamToRecords.getIdCount();
        assertTrue("The number of processed records after run 1 should be > 0, "
                   + "counted by the Dummy filter",
                   simpleResult() > beginCount);
    }

    public void testMultipleSequential() throws Exception {
        testMultiple(true);
    }

    public void testMultipleParallel() throws Exception {
        testMultiple(false);
    }

    public void testMultiple(boolean sequential) throws Exception {
        DummyStreamToRecords.clearIdCount();
        int singleRun = simpleResult();
        DummyStreamToRecords.clearIdCount();

        XStorage ingesterStorage = new XStorage();
        List<ConfigurationStorage> chains =
                ingesterStorage.createSubStorages(FilterControl.CONF_CHAINS, 2);

        ConfigurationStorage pumpStorage1 = chains.get(0);
        pumpStorage1.put(Filter.CONF_FILTER_NAME, "TestPump1");
        makeSimple(pumpStorage1);
        ConfigurationStorage pumpStorage2 = chains.get(1);
        pumpStorage1.put(Filter.CONF_FILTER_NAME, "TestPump2");
        makeSimple(pumpStorage2);

        Configuration ingestConf = new Configuration(ingesterStorage);
//        ingestConf.setStrings(FilterControl.CONF_CHAINS,
//                              Arrays.asList("TestPump1", "TestPump2"));
        ingestConf.set(FilterControl.CONF_SEQUENTIAL, true);

        FilterControl ingester =
                new FilterControl(new Configuration(ingesterStorage));
        ingester.start();
        ingester.waitForFinish();
        assertEquals("The number of records processed (sequential: "
                     + sequential + ") should be double of single run",
                     singleRun*2, DummyStreamToRecords.getIdCount());
    }

    public void testMultipleXMLSetup() throws Exception {
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
