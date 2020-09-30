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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import junit.framework.TestCase;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import sun.security.krb5.Config;

import java.io.IOException;

/**
 * Not a unit test. Used for debugging performance problems with H2 and relies on a local copy of a production
 * base being present.
 */
public class H2StorageDebugTest extends TestCase {
    private static Log log = LogFactory.getLog(H2StorageDebugTest.class);

    public static final String storageFolder = "/home/te/projects/summa/doms/";

    public void testPeek() throws IOException {
        H2Storage storage = getStorage();
        if (storage == null) {
            log.info("Unable to run debugging \"unit test\" as the expected H2 folder is not present: " + storageFolder);
            return;
        }

        try {
            System.out.println("******************" + storage.getStats());
        } finally {
            storage.close();
        }
    }

    private H2Storage getStorage() throws IOException {
        Configuration conf = Configuration.newMemoryBased(
                Storage.CONF_CLASS, H2Storage.class,
                DatabaseStorage.CONF_LOCATION, storageFolder,
                H2Storage.CONF_H2_SERVER_PORT, 9317,
                DatabaseStorage.CONF_FORCENEW, false,
                DatabaseStorage.CONF_CREATENEW, false
        );
        return new H2Storage(conf);
    }


}
