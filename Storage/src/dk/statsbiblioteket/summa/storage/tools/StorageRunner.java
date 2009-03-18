package dk.statsbiblioteket.summa.storage.tools;

import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A small utility to launch a Storage instance
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public class StorageRunner {

    public static void main (String[] args) throws Exception {
        Log log = LogFactory.getLog(StorageRunner.class);
        Configuration conf = Configuration.getSystemConfiguration(true);        

        log.info("Creating storage instance");
        Storage storage = StorageFactory.createStorage(conf);

        log.info("Storage is running");

        // Block indefinitely (non-busy)
        while(true) {
            synchronized (conf) {
                conf.wait();
            }
        }
    }

}



