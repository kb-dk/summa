package dk.statsbiblioteket.summa.common.configuration;

import dk.statsbiblioteket.summa.common.configuration.storage.JStorage;

/**
 *
 */
public class JStorageTest extends ConfigurationStorageTestCase {

    public JStorageTest() {
        super(new JStorage());
    }
}
