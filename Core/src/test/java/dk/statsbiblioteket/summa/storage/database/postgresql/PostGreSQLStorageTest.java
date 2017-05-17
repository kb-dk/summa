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
    public static final String MARS = "/home/te/projects/summa/Core/src/test/resources/storage/postgresql_mars.xml";
    public static final String PS3 = "/home/teg/workspace/summa/Core/postgresql_ps3.xml";


    @Test
    public void TEGAviserTestPS3() throws Exception {
        
      final String BASE = "aviser";
      long mTime  = 1494469519068L-60*60*24*5;
      PostGreSQLStorage storage = getDeveloperTestStorage(PS3);
      List<Record> records = storage.aviserLoadFromMTime(mTime, 500);
      
      for (Record r : records){
        String recordId=r.getId();
        String parentid = r.getParentIds().get(0);
        System.out.println(recordId+" : "+ parentid);
      }
      
    }
    
    
    @Test
    public void testConnectionMars() throws IOException {
        testConnection(MARS, 0L, 2000);
    }

    @Test
    public void testConnectionPS3_Zero() throws IOException { // Fast (760 records/sec, average=72KB)
        testConnection(PS3, 0L, 500);
    }

    @Test
    public void testConnectionPS3_first4000() throws IOException { // Fast (760 records/sec, average=43KB)
        testConnection(PS3, 1450342478857L, 500);
    }

    @Test
    public void testConnectionPS3Late() throws IOException { // Slow (14 records/sec, average=83KB)
        // 20170511-042519.068
        testConnection(PS3, 1494469519068L-60*60*24*5, 500);
    }
    @Test
    public void testConnectionPS3YearBack() throws IOException { // Slow (14 records/sec)
        testConnection(PS3, 1494469519068L-60*60*24*365, 500);
    }

    @Test
    public void testConnectionPS3One() throws IOException { // Is it the specifying of a timestamp that is the problem?
        testConnection(PS3, 1L, 500);
    }

    private double testConnection(String postgreSQLSetup, long firstTimestamp, int maxRecords) throws IOException {
        final String BASE = "aviser";

        PostGreSQLStorage storage = getDeveloperTestStorage(postgreSQLSetup);
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

    private PostGreSQLStorage getDeveloperTestStorage(String postgreSQLSetup) throws IOException {
        File CF = Resolver.getFile(postgreSQLSetup);
        if (CF == null) {
            System.out.println("Unable to run test 'PostGreSQLStorageTest.testConnection' as the file '" + MARS +
                               "' is not available");
            return null;
        }
        File CF2 = new File(CF.getAbsoluteFile() + ".copy");
        Files.copy(CF, CF2, true);

        Configuration conf = Configuration.load(CF2.getAbsolutePath());
        return new PostGreSQLStorage(conf);
    }
}