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
package dk.statsbiblioteket.summa.storage.database;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import junit.framework.TestCase;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Not a unit test. Used for debugging performance problems with H2 and relies on a local copy of a production
 * base being present. IDs are valid for medio September 2020.
 */
public class H2StorageDebugTest extends TestCase {
    private static final Log log = LogFactory.getLog(H2StorageDebugTest.class);

    public static final String storageFolder = "/home/te/projects/summa/doms/";
    public static final List<String> PVICA_PARENT = Arrays.asList(
            "pvica_radioTV:du:81b2f407-9656-46f6-a681-e2d1b520150a", // 24 s with QueryOptions(null, null, 10, 0, null, QueryOptions.ATTRIBUTES_ALL)
            "pvica_radioTV:du:bd3092e8-983d-42ba-a545-f53e48eaf7e7",
            "pvica_radioTV:du:ba55a082-55b8-4bf3-b393-3d0b09e874d1"
    );
    public static final List<String> PVICA_CHILDREN = Arrays.asList(
            "pvica_radioTV:man:ebe6ea7f-fd74-402b-858c-471cd44838ea",
            "pvica_radioTV:man:4fc92d64-0175-4bbc-9c16-87d9fe1c6833",
            "pvica_radioTV:man:4b8fc9df-0627-49fd-83d7-4fa0e4d2e68d"
    );

    public void testGetRecord() throws IOException {
        final QueryOptions qo = new QueryOptions(null, null, -1, -1, null);
        execute(wrap(storage -> {
            for (String id : PVICA_PARENT) {
                long startTime = System.nanoTime();
                Record record = storage.getRecord(id, qo);
                assertNotNull("A record should be returned for id '" + id + "'", record);
                System.out.printf(Locale.ENGLISH, "Got '%s' in %.1f ms%n",
                                  id, (System.nanoTime() - startTime) / 1000000.0);
            }
        }));
    }

    public void testPeek() throws IOException {
        final QueryOptions qo = new QueryOptions(null, null, -1, -1, null);
        execute(wrap(storage -> {
            long startTime = System.nanoTime();
            final long iteratorToken = storage.getRecordsModifiedAfter(0, "pvica_radioTV", qo);
            log.info(String.format(Locale.ENGLISH, "Constructed iterator %d in %.1f ms%n",
                              iteratorToken, (System.nanoTime() - startTime) / 1000000.0));
        }));
    }

    protected void execute(Consumer<H2Storage> consumer) {
        H2Storage storage = null;
        try {
            storage = getStorage();
            if (storage == null) {
                return;
            }
            consumer.accept(storage);
        } catch (IOException e) {
            throw new RuntimeException("IOException during access to Storage at '" + storage + "'", e);
        } finally {
            try {
                if (storage != null) {
                    storage.close();
                }
            } catch (IOException e) {
                log.warn("Unable to close Storage properly", e);
            }
        }
    }

    private H2Storage getStorage() throws IOException {
        if (!Files.exists(Paths.get(storageFolder))) {
            log.info("Unable to run debugging \"unit test\" as the expected H2 folder is not present: " + storageFolder);
            return null;
        }
        Configuration conf = Configuration.newMemoryBased(
                Storage.CONF_CLASS, H2Storage.class,
                DatabaseStorage.CONF_LOCATION, storageFolder,
                //DatabaseStorage.CONF_PAGE_SIZE, 5,
                H2Storage.CONF_H2_SERVER_PORT, 9317,
                DatabaseStorage.CONF_FORCENEW, false,
                DatabaseStorage.CONF_CREATENEW, false
        );
        try {
            return new H2Storage(conf);
        } catch (Exception e) {
            log.warn("Exception creating H2Storage from database files in '" + storageFolder + "'");
            return null;
        }
    }

    /*
           DEBUG [ajp-nio-127.0.0.1-9382-exec-276] [2020-10-02 10:15:41,035] [dk.statsbiblioteket.mediestream.services.RecordResource] Timing:
           getRecord(subj='pvica_radioTV:du:f2cb8337-5537-4b5a-b159-561044eca3a7', 10094ms, 0upd, 0ms/upd, 0upd/s, min=0ms, max=0ms, util=100.0%,
             [getFallbackRecord(5087ms, 1upd, 5087ms/upd, 0upd/s, min=5087ms, max=5087ms, util=50.4%,
               [getXSLT(0ms, 1upd, 0ms/upd, 9208upd/s, min=0ms, max=0ms, util=0.0%),
                getLegacyRecord(5042ms, 1upd, 5042ms/upd, 0upd/s, min=5042ms, max=5042ms, util=50.0%),
                downloadInfo(44ms, 1upd, 44ms/upd, 22upd/s, min=44ms, max=44ms, util=0.9%)
               ]),
               LARMWait(0ms, 1upd, 0ms/upd, 394632upd/s, min=0ms, max=0ms, util=0.0%),
               accessInfo(5007ms, 1upd, 5007ms/upd, 0upd/s, min=5007ms, max=5007ms, util=100.0%,
                 [contentInfo(5007ms, 0upd, 0ms/upd, 0upd/s, min=0ms, max=0ms, util=100.0%,
                   [contentResolve(5007ms, 1upd, 5007ms/upd, 0upd/s, min=5007ms, max=5007ms, util=100.0%)])])])

     */

    // https://stackoverflow.com/questions/27644361/how-can-i-throw-checked-exceptions-from-inside-java-8-streams
    @FunctionalInterface
    public interface ConsumerWithExceptions<T, E extends Exception> {
        void accept(T t) throws E;
    }

    public static <T, E extends Exception> Consumer<T> wrap(ConsumerWithExceptions<T, E> consumer) throws E {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

}
