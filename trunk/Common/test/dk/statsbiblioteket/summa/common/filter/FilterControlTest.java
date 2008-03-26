package dk.statsbiblioteket.summa.common.filter;

import java.util.Arrays;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.filter.object.DummyStreamToRecords;
import dk.statsbiblioteket.summa.common.filter.stream.DummyReader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * FilterControl Tester.
 *
 * @author <Authors name>
 * @since <pre>03/26/2008</pre>
 * @version 1.0
 */
public class FilterControlTest extends TestCase {
    public FilterControlTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

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
        ConfigurationStorage streamSub =
                storage.createSubStorage("Streamer");
        streamSub.put(FilterPump.CONF_FILTER_CLASS,
                       DummyReader.class.getName());
        streamSub.put(DummyReader.CONF_BODY_COUNT, 3);
        streamSub.put(DummyReader.CONF_BODY_SIZE, 100);

        ConfigurationStorage convertSub =
                storage.createSubStorage("Converter");
        convertSub.put(FilterPump.CONF_FILTER_CLASS,
                       DummyStreamToRecords.class.getName());
        convertSub.put(DummyStreamToRecords.CONF_DATA_SIZE, 99);

        Configuration pumpConf = new Configuration(storage);
        pumpConf.set(FilterPump.CONF_CHAIN_NAME, "FilterPumpTest");
        pumpConf.setStrings(FilterPump.CONF_FILTERS,
                            Arrays.asList("Streamer", "Converter"));
    }

    /*
     * Creates a simple chain and returns the number of records processed by
     * all executed DummyStreamToRecords.
     * Note: Multiple calls will increase this number.
     */
    private int simpleResult() throws Exception {
        XStorage ingesterStorage = new XStorage();
        ConfigurationStorage pumpStorage =
                ingesterStorage.createSubStorage("TestPump");
        makeSimple(pumpStorage);
        Configuration ingestConf = new Configuration(ingesterStorage);
        ingestConf.setStrings(FilterControl.CONF_CHAINS,
                              Arrays.asList("TestPump"));
        ingestConf.set(FilterControl.CONF_SEQUENTIAL, true);
        FilterControl ingester =
                new FilterControl(new Configuration(ingesterStorage));
        ingester.start();
        ingester.waitForFinish();
        return DummyStreamToRecords.getIdCount();
    }


    public void testSimple() throws Exception {
        int beginCount = DummyStreamToRecords.getIdCount();
        assertTrue("The number of processed records after run 1 should be > 0",
                   simpleResult() > beginCount);
        beginCount = DummyStreamToRecords.getIdCount();
        assertTrue("The number of processed records after run 1 should be > 0",
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
        ConfigurationStorage pumpStorage1 =
                ingesterStorage.createSubStorage("TestPump1");
        makeSimple(pumpStorage1);
        ConfigurationStorage pumpStorage2 =
                ingesterStorage.createSubStorage("TestPump2");
        makeSimple(pumpStorage2);

        Configuration ingestConf = new Configuration(ingesterStorage);
        ingestConf.setStrings(FilterControl.CONF_CHAINS,
                              Arrays.asList("TestPump1", "TestPump2"));
        ingestConf.set(FilterControl.CONF_SEQUENTIAL, true);

        FilterControl ingester =
                new FilterControl(new Configuration(ingesterStorage));
        ingester.start();
        ingester.waitForFinish();
        assertEquals("The number of records processed (sequential: "
                     + sequential + ") should be double of single run",
                     singleRun*2, DummyStreamToRecords.getIdCount());
    }
}
