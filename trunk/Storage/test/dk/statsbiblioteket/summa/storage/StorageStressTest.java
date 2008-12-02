package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.database.derby.DerbyStorage;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.util.Profiler;

import java.io.File;

/**
 * A small test tool to assess the performance of a Storage impl.
 */
public class StorageStressTest {

    public static final Class<? extends Storage> DEFAULT_STORAGE =
                                                             DerbyStorage.class;

    public static final String CONF_NUM_RECORDS =
                                                "summa.storage.test.numrecords";

    public static final long DEFAULT_NUM_RECORDS = 10000;

    public static final String DEFAULT_DB_LOCATION =
                                               "/tmp/summa-storage-stress-test";

    public static void main (String[] args) throws Exception {
        Configuration conf = Configuration.getSystemConfiguration(true);

        if (!conf.valueExists(DatabaseStorage.CONF_LOCATION)) {
            conf.set(DatabaseStorage.CONF_LOCATION, DEFAULT_DB_LOCATION);
        }

        if (new File(conf.getString(DatabaseStorage.CONF_LOCATION)).exists()) {
            System.err.println("Database '"
                               +conf.getString(DatabaseStorage.CONF_LOCATION)
                               +"' already exist. Please delete it.");
            System.exit(1);
        }

        if (!conf.valueExists(Storage.CONF_CLASS)) {
            conf.set (Storage.CONF_CLASS, DEFAULT_STORAGE);
        }

        System.out.println("Using storage class: "
                           + conf.getClass(Storage.CONF_CLASS));
        Storage storage = StorageFactory.createStorage(conf);

        long numRecs = conf.getLong(CONF_NUM_RECORDS, DEFAULT_NUM_RECORDS);

        System.out.println("Flushing " + numRecs + " records");
        Profiler p = new Profiler();
        p.setExpectedTotal(numRecs);

        for (long i = 0; i < numRecs; i++) {
            storage.flush(TestRecordFactory.next());
            p.beat();
        }
        double bps = p.getBps();
        long runningTime = p.getSpendMilliseconds();

        System.out.println("Done flushing " + numRecs + " records");
        System.out.println("Total running time: "
                           + Profiler.millisecondsToString(runningTime));
        System.out.println("Average: " + bps + " records/s");

        storage.close();
    }
}
