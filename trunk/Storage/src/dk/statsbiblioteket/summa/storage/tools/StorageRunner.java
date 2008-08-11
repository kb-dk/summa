package dk.statsbiblioteket.summa.storage.tools;

import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.StorageFactory;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.rmi.RMISecurityManager;

/**
 * A small debug utility to launch a Storage instance
 */
public class StorageRunner {

    public static void main (String[] args) throws Exception {
        Configuration conf = Configuration.getSystemConfiguration(true);        

        System.out.println ("Creating Storage instance");
        Storage storage = StorageFactory.createStorage(conf);        
    }

}
