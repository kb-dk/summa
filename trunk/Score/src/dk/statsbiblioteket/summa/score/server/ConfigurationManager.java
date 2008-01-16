package dk.statsbiblioteket.summa.score.server;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.RemoteStorage;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public class ConfigurationManager implements Runnable, Configurable {

    /**
     * Property defining which class to use for {@link ConfigurationStorage}.
     * Default is
     * {@link dk.statsbiblioteket.summa.common.configuration.storage.XStorage}.
     */
    public static final String PROP_STORAGE =
                                    "summa.score.configurationManager.storage";

    private Log log;
    private ConfigurationStorage storage;

    public ConfigurationManager (Configuration conf) {
        log = LogFactory.getLog (ConfigurationManager.class);

        setupStorage(conf);
    }

    private void setupStorage (Configuration conf) {
        Class<? extends ConfigurationStorage> storageClass =
                                       conf.getClass(PROP_STORAGE,
                                                     ConfigurationStorage.class,
                                                     XStorage.class);
        log.debug ("Using configuration storage class: "
                   + storageClass.getName());
        storage = conf.create (storageClass);
    }

    private void exportRemoteStorage (Configuration conf) {
        log.debug("Setting up remote interface");
        RemoteStorage remote = conf.create (RemoteStorage.class);
        log.trace("Overriding default storage for RemoteStorage");
        remote.setStorage(storage);
    }

    public void run() {

    }

    public static void main (String[] args) {
        Configuration conf = Configuration.getSystemConfiguration();
        ConfigurationManager cm = new ConfigurationManager(conf);

        cm.run();
    }
}
