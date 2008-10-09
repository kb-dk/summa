package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.storage.database.derby.DerbyStorage;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.util.Files;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;

import junit.framework.TestCase;

/**
 *
 */
public class StorageTest extends TestCase {

    Storage storage;

    static String testDBLocation = "test_db";
    static String testBase1 = "foobar";
    static String testId1 = "testId1";
    static String testId2 = "testId2";
    static String testId3 = "testId3";
    static String testId4 = "testId4";
    static int storageCounter = 0;
    static byte[] testContent1 = new byte[] {'s', 'u', 'm', 'm', 'a'};

    public static Configuration createConf () throws Exception {

        Configuration conf = Configuration.newMemoryBased(
                StorageFactory.CONF_STORAGE,
                DerbyStorage.class,
                DatabaseStorage.CONF_LOCATION,
                testDBLocation + (storageCounter++),
                DatabaseStorage.CONF_FORCENEW,
                true
        );

        return conf;
    }

    public void setUp () throws Exception {
        if (new File(testDBLocation + storageCounter).exists()) {
            Files.delete (testDBLocation + storageCounter);
        }

        storage = StorageFactory.createStorage(createConf());

        /* We get spurious errors where the connection to the db isn't ready
         * when running the unit tests in batch mode */
        Thread.sleep(200);
    }

    public void tearDown () throws Exception {
        storage.close();
        /* We get spurious errors where the connection to the db isn't ready
         * when running the unit tests in batch mode */
        Thread.sleep(200);
    }

    public void testGetEmpty () throws Exception {
        List<Record> recs = storage.getRecords(new ArrayList<String>(), 0);
        assertEquals(0, recs.size());
    }

    public void testGetNonExisting () throws Exception {
        List<Record> recs = storage.getRecords(Arrays.asList(testId1), 0);
        assertEquals(0, recs.size());
    }

    public void testClearEmptyBase () throws Exception {
        storage.clearBase (testBase1);
        assertBaseEmpty (testBase1);
    }

    public void assertBaseEmpty (String base) throws Exception {
        Iterator<Record> iter = storage.getRecordsFromBase(base);
        long counter = 0;
        while (iter.hasNext()) {
            Record r = iter.next();
            if (!r.isDeleted()) {
                counter++;
            }
        }

        if (counter != 0) {
            fail ("Base '" + base + "' should be empty, but found " + counter
                  + " records");
        }
    }

    public void testIteration() throws Exception {
        storage.clearBase (testBase1);
        storage.flush(new Record(testId1, testBase1, testContent1));
        storage.flush(new Record(testId2, testBase1, testContent1));
        assertBaseCount(testBase1, 2);
    }

    public void assertBaseCount (String base, long expected) throws Exception {
        Iterator<Record> iter = storage.getRecordsFromBase(base);
        long actual = 0;
        while (iter.hasNext()) {
            iter.next();
            actual++;
        }

        if (actual != expected) {
            fail("Base '" + base + "' should contain " + expected
                 + " records, but found " + actual);
        }
    }

    public void testAddOne () throws Exception {
        Record rec = new Record (testId1, testBase1, testContent1);
        storage.flush (rec);

        List<Record> recs = storage.getRecords(Arrays.asList(testId1), 0);

        assertEquals(1, recs.size());
        assertEquals(rec, recs.get(0));

        assertEquals(null, recs.get(0).getChildren());
        assertEquals(null, recs.get(0).getChildIds());
        assertEquals(null, recs.get(0).getParents());
        assertEquals(null, recs.get(0).getParentIds());
    }

    public void testClearOne() throws Exception {
        testAddOne();
        storage.clearBase(testBase1);
        assertBaseEmpty(testBase1);
    }

    public void testAddOneWithOneChildId() throws Exception {
        Record rec = new Record (testId1, testBase1, testContent1);
        rec.setChildIds(Arrays.asList(testId2));
        storage.flush (rec);

        List<Record> recs = storage.getRecords(Arrays.asList(testId1), 0);

        assertEquals(1, recs.size());
        assertEquals(rec, recs.get(0));

        assertEquals(null, recs.get(0).getChildren());
        assertEquals(Arrays.asList(testId2), recs.get(0).getChildIds());
        assertEquals(null, recs.get(0).getParents());
        assertEquals(null, recs.get(0).getParentIds());
    }

    public void testAddOneWithTwoChildIds() throws Exception {
        Record rec = new Record (testId1, testBase1, testContent1);
        rec.setChildIds(Arrays.asList(testId2, testId3));
        storage.flush (rec);

        List<Record> recs = storage.getRecords(Arrays.asList(testId1), 0);

        assertEquals(1, recs.size());
        assertEquals(rec, recs.get(0));

        assertEquals(null, recs.get(0).getChildren());
        assertEquals(Arrays.asList(testId2, testId3), recs.get(0).getChildIds());
        assertEquals(null, recs.get(0).getParents());
        assertEquals(null, recs.get(0).getParentIds());
    }

    public void testAddOneWithOneParentId() throws Exception {
        Record rec = new Record (testId1, testBase1, testContent1);
        rec.setParentIds(Arrays.asList(testId2));
        storage.flush (rec);

        List<Record> recs = storage.getRecords(Arrays.asList(testId1), 0);

        assertEquals(1, recs.size());
        assertEquals(rec, recs.get(0));

        assertEquals(null, recs.get(0).getChildren());
        assertEquals(null, recs.get(0).getChildIds());
        assertEquals(null, recs.get(0).getParents());
        assertEquals(Arrays.asList(testId2), recs.get(0).getParentIds());
    }

    public void testAddOneWithTwoParentIds() throws Exception {
        Record rec = new Record (testId1, testBase1, testContent1);
        rec.setParentIds(Arrays.asList(testId2, testId3));
        storage.flush (rec);

        List<Record> recs = storage.getRecords(Arrays.asList(testId1), 0);

        assertEquals(1, recs.size());
        assertEquals(rec, recs.get(0));

        assertEquals(null, recs.get(0).getChildren());
        assertEquals(null, recs.get(0).getChildIds());
        assertEquals(null, recs.get(0).getParents());
        assertEquals(Arrays.asList(testId2, testId3), recs.get(0).getParentIds());
    }

    public void testAddTwo () throws Exception {
        Record rec1 = new Record(testId1, testBase1, testContent1);
        Record rec2 = new Record(testId2, testBase1, testContent1);

        storage.flushAll (Arrays.asList(rec1, rec2));

        List<Record> recs = storage.getRecords(Arrays.asList(testId1, testId2),
                                               0);

        assertEquals(2, recs.size());

        assertEquals(rec1, recs.get(0));
        assertEquals(null, recs.get(0).getChildIds());
        assertEquals(null, recs.get(0).getChildren());
        assertEquals(null, recs.get(0).getParentIds());
        assertEquals(null, recs.get(0).getParents());

        assertEquals(rec2, recs.get(1));
        assertEquals(null, recs.get(1).getChildIds());
        assertEquals(null, recs.get(1).getChildren());
        assertEquals(null, recs.get(1).getParentIds());
        assertEquals(null, recs.get(1).getParents());
    }

    public void testClearTwo () throws Exception {
        testAddTwo();
        storage.clearBase(testBase1);
        assertBaseEmpty(testBase1);
    }

    public void testExpandShallowRecord () throws Exception {
        testAddOne();

        List<Record> recs = storage.getRecords(Arrays.asList(testId1),
                                               1);
        assertEquals(1, recs.size());

        Record rec = recs.get(0);
        assertEquals(rec.getContentAsUTF8(), rec.getContentAsUTF8());
        assertEquals(rec.getId(), rec.getId());
    }

    public void testFullExpandShallowRecord () throws Exception {
        testAddOne();

        List<Record> recs = storage.getRecords(Arrays.asList(testId1),
                                               -1);
        assertEquals(1, recs.size());

        Record rec = recs.get(0);
        assertEquals(rec.getContentAsUTF8(), rec.getContentAsUTF8());
        assertEquals(rec.getId(), rec.getId());
    }

    public void testAddLinkedRecords () throws Exception {
        Record recP = new Record (testId1, testBase1, testContent1);
        Record recC1 = new Record (testId2, testBase1, testContent1);
        Record recC2 = new Record (testId3, testBase1, testContent1);

        recP.setChildIds(Arrays.asList(recC1.getId(), recC2.getId()));

        recC1.setChildIds(Arrays.asList(recC2.getId()));
        recC1.setParentIds(Arrays.asList(recP.getId()));

        recC2.setParentIds(Arrays.asList(recC1.getId()));

        storage.flushAll (Arrays.asList(recP, recC1, recC2));

        /* Fetch without expansion, we test that elewhere */
        List<Record> recs = storage.getRecords(Arrays.asList(testId1, testId2),
                                               0);

        assertEquals(2, recs.size());

        System.out.println ("ORIG:\n" + recP.toString(true));
        System.out.println ("GOT :\n" + recs.get(0).toString(true));

        /* We can't compare the records directly because recP has the child
         * records nested, while the retrieved records only has the ids */
        assertEquals(recP.getId(), recs.get(0).getId());
        assertEquals(recP.getBase(), recs.get(0).getBase());
        assertEquals(recP.getContentAsUTF8(), recs.get(0).getContentAsUTF8());

        /* We should have the ids of the children, but they should not be
         * expanded */
        assertEquals(recP.getChildIds(), recs.get(0).getChildIds());
        assertEquals(null, recs.get(0).getChildren());


        assertEquals(recC1.getContentAsUTF8(), recs.get(1).getContentAsUTF8());
        assertEquals(recC1.getId(), recs.get(1).getId());
        assertEquals(recC1.getBase(), recs.get(1).getBase());
        assertEquals(recC1.getParentIds(), recs.get(1).getParentIds());
    }

    public void testExpandLinkedRecord () throws Exception {
        testAddLinkedRecords();

        /* Fetch records expanding immediate children only */
        List<Record> recs = storage.getRecords(Arrays.asList(testId1, testId2),
                                               1);

        assertEquals(2, recs.size());

        /* Check that the first record holds a child relation to the next */
        assertEquals(2, recs.get(0).getChildren().size());
        assertEquals(testId2, recs.get(0).getChildren().get(0).getId());

        /* testId3 is a child of testId2 and should be expanded */
        assertEquals(1, recs.get(1).getChildren().size());
        assertEquals(1, recs.get(1).getChildIds().size());
        assertEquals(testId3, recs.get(1).getChildIds().get(0));
        assertEquals(testId3, recs.get(1).getChildren().get(0).getId());

    }

    public void testRecursiveExpandLinkedRecord () throws Exception {
        testAddLinkedRecords();

        /* Fetch records expanding immediate children only */
        List<Record> recs = storage.getRecords(Arrays.asList(testId1, testId2, testId3),
                                               -1);

        assertEquals(3, recs.size());

        /* Check that the first record holds child relations to recC1, and recC2 */
        assertEquals(recs.get(0).getChildren(),
                     Arrays.asList(recs.get(1), recs.get(2)));

        /* Check that recC1 has child recC2 */
        assertEquals(recs.get(1).getChildren(),
                     Arrays.asList(recs.get(2)));
    }

    /**
     * Test that ingesting one record also ingests any child records on it
     */
    public void testAddNestedRecords () throws Exception {
        Record recP = new Record (testId1, testBase1, testContent1);
        Record recC1 = new Record (testId2, testBase1, testContent1);
        Record recC2 = new Record (testId3, testBase1, testContent1);
        Record recCC1 = new Record (testId4, testBase1, testContent1);

        /* We need to explicitely set all relations here to make the assertions
         * work*/

        recP.setChildren(Arrays.asList(recC1, recC2));

        recC1.setParents (Arrays.asList(recP));
        recC1.setChildren(Arrays.asList(recCC1));
        recC1.setIndexable(false);

        recC2.setParents(Arrays.asList(recP));
        recC2.setIndexable(false);

        recCC1.setParents(Arrays.asList(recC1));
        recCC1.setIndexable(false);

        /* The child records should be implicitly flushed as well */
        storage.flushAll (Arrays.asList(recP));

        List<Record> recs = storage.getRecords(Arrays.asList(testId1, testId2,
                                                             testId3, testId4),
                                               -1);

        assertEquals(4, recs.size());

        assertEquals(recP, recs.get(0));
        assertEquals(recC1, recs.get(1));
        assertEquals(recC2, recs.get(2));
        assertEquals(recCC1, recs.get(3));

        assertNotNull(recs.get(0).getChildren());
        assertNotNull(recs.get(0).getChildIds());
        assertEquals(2, recs.get(0).getChildren().size());
        assertEquals(2, recs.get(0).getChildIds().size());

        assertEquals(recs.get(0).getChildren().get(0), recC1);
        assertEquals(recs.get(0).getChildren().get(1), recC2);

        assertNotNull(recs.get(1).getChildren());
        assertNotNull(recs.get(1).getChildIds());
        assertEquals(1, recs.get(1).getChildren().size());
        assertEquals(1, recs.get(1).getChildIds().size());
        assertEquals(recCC1, recs.get(1).getChildren().get(0));

        assertNull(recs.get(2).getChildren());
        assertNull(recs.get(2).getChildIds());

        assertNull(recs.get(3).getChildren());
        assertNull(recs.get(3).getChildIds());
    }

    /*
    TODO
     - test mtime/ctime and repeated adding of same rec
     */


}
