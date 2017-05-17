/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.storage.database.postgresql;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.util.RecordStatsCollector;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/*
  This test requires a running PostgreSQL. The current version relies on an inhouse test-PostgreSQL at
  the Royal Danish Library - Aarhus.
 */
public class PostGreSQLStorageTest {
    // Contains credentials, so not committed. Anyway it is for the non-critical test-installation
    // The file should mimick the storagews-configuration for the SummaRise/aviser project:
    // <properties>
    //    <entry key="database.driver.url">jdbc:postgresql://MACHINE:PORT/summa_aviser_devel</entry>
    //    <entry key="summa.storage.database.username">XXX</entry>
    //    <entry key="summa.storage.database.password">XXX</entry>
    //    <entry key="storage.relations.touch">child</entry>
    //    <entry key="storage.relations.clear">parent</entry>
    //    <entry key="databasestorage.relatives.expandidlist">false</entry>
    //    <entry key="queryoptions.filter.deleted">null</entry>
    //    <entry key="queryoptions.filter.indexable">null</entry>
    //    <entry key="queryoptions.child.depth">0</entry>
    //    <entry key="queryoptions.parent.depth">3</entry>
    //    <entry key="queryoptions.attributes">ID, BASE, DELETED, INDEXABLE, HAS_RELATIONS, CONTENT, CREATIONTIME, MODIFICATIONTIME, PARENTS</entry>
    // </properties>
    //public static final String MARS = "/home/te/projects/summa/Core/src/test/resources/storage/postgresql_mars.xml";
    public static final String MARS = "/storage/postgresql_mars.xml";
    //public static final String PS3 = "/home/te/projects/summa/Core/src/test/resources/storage/postgresql_ps3.xml";
    public static final String PS3 = "storage/postgresql_ps3.xml";


    @Test
    public void TEGAviserTestPS3() throws Exception {
        
      final String BASE = "aviser";
      long mTime  = 1494469519068L-60*60*24*5;
      PostGreSQLStorage storage = getDeveloperTestStorage(PS3, false);
      List<Record> records = storage.getRecordsModifiedAfterOptimized(
                0L, null, null, DatabaseStorage.OPTIMIZATION.singleParent).getKey();

      for (Record r : records){
        String recordId=r.getId();
        String parentid = r.getParentIds().get(0);
        System.out.println(recordId+" : "+ parentid);
      }
      
    }
    
    
    @Test
    public void testConnectionMarsOptimized() throws IOException {
        testConnection(MARS, 0L, 2000, true);
    }

    @Test
    public void testConnectionPS3_ZeroOptimized() throws IOException { // Fast (760 records/sec, average=72KB)
        testConnection(PS3, 0L, 1000, true);
    }
    @Test
    public void testConnectionPS3_ZeroNonOptimized() throws IOException { // Fast (760 records/sec, average=72KB)
        testConnection(PS3, 0L, 1000, false);
    }

    @Test
    public void testConnectionPS3_first4000Optimized() throws IOException { // Fast (760 records/sec, average=43KB)
        testConnection(PS3, 1450342478857L, 1000, true);
    }

    @Test
    public void testConnectionPS3LateOptimized() throws IOException {
        // 20170511-042519.068
        testConnection(PS3, 1494469519068L-60*60*24*5, 1000, true);
    }
    @Test
    public void testConnectionPS3LateNonOptimized() throws IOException { // Slow (14 records/sec, average=83KB)
        // 20170511-042519.068
        testConnection(PS3, 1494469519068L-60*60*24*5, 1000, false);
    }


    // Unwarmed: Retrieved 1000 records in 196.7 seconds at 5.1 records/sec with last timestamp 1494472231463 and stats read(records=1000(compressed=1000), average=75KB, smallest=(size=3KB, ID=doms_newspaperCollection:uuid:b64f049a-b6b7-4a94-b1b4-f32940c749c6), largest=(size=281KB, ID=doms_newspaperCollection:uuid:b87a9d5c-087c-4d8f-bb2e-bba8ab92963f), last=(size=37KB, ID=doms_newspaperCollection:uuid:a0f816cc-e763-4a78-a453-4899fe0a9de4))
    // Warmed_ Retrieved 1000 records in 165.6 seconds at 6.0 records/sec with last timestamp 1494472231463 and stats read(records=1000(compressed=1000), average=75KB, smallest=(size=3KB, ID=doms_newspaperCollection:uuid:b64f049a-b6b7-4a94-b1b4-f32940c749c6), largest=(size=281KB, ID=doms_newspaperCollection:uuid:b87a9d5c-087c-4d8f-bb2e-bba8ab92963f), last=(size=37KB, ID=doms_newspaperCollection:uuid:a0f816cc-e763-4a78-a453-4899fe0a9de4))
    @Test
    public void testConnectionPS3YearBackOptimized() throws IOException {
        testConnection(PS3, 1494469519068L-60*60*24*365, 1000, true);
    }
    // Warmed: Retrieved 1000 records in 155.1 seconds at 6.4 records/sec with last timestamp 1494472231463 and stats read(records=1000(compressed=1000), average=75KB, smallest=(size=3KB, ID=doms_newspaperCollection:uuid:b64f049a-b6b7-4a94-b1b4-f32940c749c6), largest=(size=281KB, ID=doms_newspaperCollection:uuid:b87a9d5c-087c-4d8f-bb2e-bba8ab92963f), last=(size=37KB, ID=doms_newspaperCollection:uuid:a0f816cc-e763-4a78-a453-4899fe0a9de4))
    @Test
    public void testConnectionPS3YearBackNonOptimized() throws IOException {
        testConnection(PS3, 1494469519068L-60*60*24*365, 1000, false);
    }

    @Test
    public void testConnectionPS3One() throws IOException { // Is it the specifying of a timestamp that is the problem?
        testConnection(PS3, 1L, 1000, true);
    }

    private double testConnection(String postgreSQLSetup, long firstTimestamp, int maxRecords, boolean useOptimizations)
            throws IOException {
        final String BASE = "aviser";

        PostGreSQLStorage storage = getDeveloperTestStorage(postgreSQLSetup, useOptimizations);
        if (storage == null) {
            return 0D;
        }
//        StringWriter sw = new StringWriter();
//        BaseStats.toXML(storage.getStats(), sw);
//        System.out.println(sw.toString());

        final long iteratorKey = storage.getRecordsModifiedAfter(firstTimestamp, BASE, getQueryOptions());
        Iterator<Record> recordIterator = new StorageIterator(storage, iteratorKey, 100, false);
        assertTrue("There should be at least 1 result",
                   recordIterator.hasNext());

        Profiler profiler = new Profiler();
        profiler.setBpsSpan(1000);
        RecordStatsCollector stats = new RecordStatsCollector("read", 0, false);
        Record last = null;
        while (recordIterator.hasNext()) {
            last = recordIterator.next();
            assertTrue("There should be content in Record " + last.getId(),
                       last.getContent(false).length > 0);
            profiler.beat();
            stats.process(last);
            if (profiler.getBeats() % 1000 == 0) {
                System.out.println(String.format("************* Current speed: %.1f records/second. Stats: %s",
                                                 profiler.getBps(true), stats));
            }
            if (profiler.getBeats() == maxRecords) {
                break;
            }
        }
        storage.close();
        System.out.println(String.format(
                "Retrieved %d records in %.1f seconds at %.1f records/sec with last timestamp %d and stats %s",
                profiler.getBeats(), profiler.getSpendMilliseconds()/1000.0, profiler.getBps(false),
                last == null ? 0 : last.getModificationTime(), stats));
        return profiler.getBps(false);
    }

    private QueryOptions getQueryOptions() {
        QueryOptions opts = new QueryOptions(null, null,
                                             0, 1,
                                             null, QueryOptions.ATTRIBUTES_ALL);
        opts.removeAttribute(QueryOptions.ATTRIBUTES.CHILDREN);
        return opts;
    }

    private PostGreSQLStorage getDeveloperTestStorage(String postgreSQLSetup, boolean useOptimizations)
            throws IOException {
        File CF = Resolver.getFile(postgreSQLSetup);
        if (CF == null || !CF.exists()) {
            System.out.println("Unable to run test 'PostGreSQLStorageTest.testConnection' as the file '" +
                               postgreSQLSetup + "' is not available");
            return null;
        }
        File CF2 = new File(CF.getAbsoluteFile() + ".copy");
        Files.copy(CF, CF2, true);

        Configuration conf = Configuration.load(CF2.getAbsolutePath());
        conf.set(DatabaseStorage.CONF_USE_OPTIMIZATIONS, useOptimizations);
        return new PostGreSQLStorage(conf);
    }
}