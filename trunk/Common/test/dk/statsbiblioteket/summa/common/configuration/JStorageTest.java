package dk.statsbiblioteket.summa.common.configuration;

import dk.statsbiblioteket.summa.common.configuration.storage.JStorage;

/**
 *
 */
public class JStorageTest extends ConfigurationStorageTestCase {

    public JStorageTest() {
        super(new JStorage());
    }

    public void testLoadFromResource() throws Exception {
        JStorage js = new JStorage("configuration.js");
        Configuration conf = new Configuration(js);
        checkSampleConfig(conf);
    }

    public void testLoadFromSysProp() throws Exception {
        System.setProperty(Configuration.CONF_CONFIGURATION_PROPERTY,
                           "configuration.js");
        try {
            Configuration conf = Configuration.getSystemConfiguration(false);
            checkSampleConfig(conf);
        } finally {
            System.clearProperty(Configuration.CONF_CONFIGURATION_PROPERTY);
        }

    }

    public void checkSampleConfig(Configuration conf) throws Exception {
        assertFalse(conf.valueExists("summa.nonexisting"));

        assertEquals(27, conf.getInt("summa.test.int"));
        assertEquals(27, conf.getLong("summa.test.int"));

        assertTrue(conf.getBoolean("summa.test.boolean"));

        assertEquals(1, conf.getInt("summa.test.closure"));
        assertEquals(2, conf.getInt("summa.test.closure"));
        assertEquals(3, conf.getInt("summa.test.closure"));
    }

    public void testNested() throws Exception {
        JStorage sto = new JStorage();
        assertEquals(0, sto.size());

        JStorage sub = sto.createSubStorage("summa.sub");

        assertEquals(0, sub.size());
    }
}
