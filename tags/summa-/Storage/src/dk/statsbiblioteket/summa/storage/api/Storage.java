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
package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.summa.storage.api.WritableStorage;

/**
 * Storage is a unified interface for {@link ReadableStorage} and
 * {@link WritableStorage}.
 *
 * Client wishing to connect to remote Storage services should look
 * at {@link StorageConnectionFactory}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public interface Storage extends ReadableStorage, WritableStorage {

    /**
     * Configuration property specifying the class to use as storage
     * implementation. The default is
     * {@link dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxy}
     */
    public static final String CONF_CLASS = "summa.storage.class";

    /**
     * Configuration property specifying which port the Storage service
     * should communicate on. Default is 27027.
     */
    public static final String CONF_SERVICE_PORT = "summa.storage.service.port";        

    /**
     * Configuration property specifying where the storage should store
     * persistent data. Default is {@code ~/summa-control/persistent}.
     */
    public static final String CONF_DATA_DIR = "summa.storage.data.dir";
}




