package dk.statsbiblioteket.summa.storage.tools;

import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A small debug utility to launch a Storage instance
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public class StorageRunner {

    public static void main (String[] args) throws Exception {
        Configuration conf = Configuration.getSystemConfiguration(true);        

        System.out.println ("Creating Storage instance");
        Storage storage = StorageFactory.createStorage(conf);
    }

}



