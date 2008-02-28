package dk.statsbiblioteket.summa.ingest;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.ingest.records.DummyStreamToRecords;
import dk.statsbiblioteket.summa.ingest.stream.DummyReader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;

/**
 * FilterPump Tester.
 *
 * @author <Authors name>
 * @since <pre>02/20/2008</pre>
 * @version 1.0
 */
public class FilterPumpTest extends TestCase {
    public FilterPumpTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(FilterPumpTest.class);
    }

    public void testBasics() throws Exception {
        XStorage pumpStorage = new XStorage();

        ConfigurationStorage streamSub =
                pumpStorage.createSubStorage("Streamer");
        streamSub.put(FilterPump.CONF_FILTER_CLASS,
                       DummyReader.class.getName());
        streamSub.put(DummyReader.CONF_BODY_COUNT, 3);
        streamSub.put(DummyReader.CONF_BODY_SIZE, 100);

        ConfigurationStorage convertSub =
                pumpStorage.createSubStorage("Converter");
        convertSub.put(FilterPump.CONF_FILTER_CLASS,
                       DummyStreamToRecords.class.getName());
        convertSub.put(DummyStreamToRecords.CONF_DATA_SIZE, 99);

        
        Configuration pumpConf = new Configuration(pumpStorage);
        pumpConf.set(FilterPump.CONF_CHAIN_NAME, "FilterPumpTest");
        pumpConf.setStrings(FilterPump.CONF_STREAM_FILTERS,
                            Arrays.asList("Streamer"));
        pumpConf.setStrings(FilterPump.CONF_RECORD_FILTERS,
                            Arrays.asList("Converter"));

        FilterPump pump = new FilterPump(pumpConf);

        pump.run();
    }
}
