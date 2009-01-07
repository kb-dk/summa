package dk.statsbiblioteket.summa.common.filter;

import java.util.Arrays;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.filter.object.DummyStreamToRecords;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.filter.stream.DummyReader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class FilterPumpTest extends TestCase {
    public FilterPumpTest(String name) {
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

    public void testGetChainName() throws Exception {
        //TODO: Test goes here...
    }

    public static Test suite() {
        return new TestSuite(FilterPumpTest.class);
    }

    public void testBasics() throws Exception {
        XStorage pumpStorage = new XStorage();

        ConfigurationStorage streamSub =
                pumpStorage.createSubStorage("Streamer");
        streamSub.put(FilterSequence.CONF_FILTER_CLASS,
                       DummyReader.class.getName());
        streamSub.put(DummyReader.CONF_BODY_COUNT, 3);
        streamSub.put(DummyReader.CONF_BODY_SIZE, 100);

        ConfigurationStorage convertSub =
                pumpStorage.createSubStorage("Converter");
        convertSub.put(FilterSequence.CONF_FILTER_CLASS,
                       DummyStreamToRecords.class.getName());
        convertSub.put(DummyStreamToRecords.CONF_DATA_SIZE, 99);


        Configuration pumpConf = new Configuration(pumpStorage);
        pumpConf.set(Filter.CONF_FILTER_NAME, "FilterPumpTest");
        pumpConf.setStrings(FilterSequence.CONF_FILTERS,
                            Arrays.asList("Streamer", "Converter"));

        FilterPump pump = new FilterPump(pumpConf);

        pump.start();
        pump.waitForFinish();
        assertTrue("The number of processed records should be > 0",
                   DummyStreamToRecords.getIdCount() > 0);
    }
}



