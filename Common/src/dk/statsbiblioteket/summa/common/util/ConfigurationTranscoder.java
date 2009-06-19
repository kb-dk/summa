package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.JStorage;

/**
 * Helper class to read the default system configuration, as obtained by
 * {@link Configuration#getSystemConfiguration()}, and dump it as a JStorage.
 * <p/>
 * This tool should be expanded to include arbitrary configuration formats,
 * but there is not canonical way to serialize configurations as of writing.
 */
public class ConfigurationTranscoder {

    public static void main(String[] args) {
        Configuration from = Configuration.getSystemConfiguration(false);
        Configuration to = new Configuration(new JStorage());
        to.importConfiguration(from);
        System.out.println(to.getStorage().toString());
    }
}
