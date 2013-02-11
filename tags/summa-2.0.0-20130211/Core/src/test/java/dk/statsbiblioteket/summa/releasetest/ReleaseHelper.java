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
package dk.statsbiblioteket.summa.releasetest;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.ingest.split.XMLSplitterFilter;
import dk.statsbiblioteket.summa.ingest.stream.ArchiveReader;
import dk.statsbiblioteket.summa.storage.api.*;
import dk.statsbiblioteket.summa.storage.api.filter.RecordWriter;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxy;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * General helpers for controlling Storage, ingest, index and Searchers.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ReleaseHelper {
    private static Log log = LogFactory.getLog(ReleaseHelper.class);

    public static final String STORAGE_RMI_PREFIX = "//localhost:28000/";
    public static final String INDEX_ROOT = System.getProperty("java.io.tmpdir") + File.separator + "testindex";

    static int storageCounter = 0;
    public static final File storageRoot =
            new File(new File(System.getProperty("java.io.tmpdir")), "test_storages");

    public static File getStorageLocation(String name) {
        return new File(IngestTest.sourceRoot, "storage_" + name);
    }

    /**
     * @param source where to look for data.
     * @return a configuration meant for an
     * {@link dk.statsbiblioteket.summa.ingest.stream.ArchiveReader}, delivering
     * all .xml-files from the given source.
     */
    public static Configuration getArchiveReaderConfiguration(String source) {
        return Configuration.newMemoryBased(
            ArchiveReader.CONF_ROOT_FOLDER, source,
            ArchiveReader.CONF_RECURSIVE, true,
            ArchiveReader.CONF_COMPLETED_POSTFIX, ""
        );
    }

    /**
     * The given name will be used for the RMI-setup so that the Storage can be
     * accessed with
     * {@link dk.statsbiblioteket.summa.storage.api.StorageReaderClient} or
     * {@link dk.statsbiblioteket.summa.storage.api.StorageWriterClient}
     * at the address {@code //localhost:28000/name}.
     * @param name the name for the RMI exposed Storage.
     * @return configuration for a h2 storage at an unique temporary location,
     *         wrapped in a RMIStorage.
     */
    public static Configuration getStorageConfiguration(String name) {
        return Configuration.newMemoryBased(
            Storage.CONF_CLASS,
            "dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxy", // Wrapper
            RMIStorageProxy.CONF_REGISTRY_PORT, 28000,
            RMIStorageProxy.CONF_SERVICE_NAME, name,
            RMIStorageProxy.CONF_SERVICE_PORT, 28001, // Not used directly

            RMIStorageProxy.CONF_BACKEND,
            "dk.statsbiblioteket.summa.storage.database.h2.H2Storage", // Impl.
            DatabaseStorage.CONF_LOCATION, getNewStorageLocation().toString(),
            DatabaseStorage.CONF_FORCENEW, true,
            DatabaseStorage.CONF_FORCENEW, true
        );

/*        conf.set(Service.CONF_SERVICE_PORT, 27003);
        conf.set(Service.CONF_REGISTRY_PORT, 27000);  // Why is this not done?
        conf.set(Service.CONF_SERVICE_ID, "TestStorage");
        System.setProperty(Service.CONF_SERVICE_ID, "TestStorage");*/
    }

    /**
     * @param storageID the name of the remote Storage.
     * @return a ReadableStorage for the Storage exposed at //localhost:28000/storageID.
     */
    public static StorageReaderClient getReader(String storageID) {
        return new StorageReaderClient(getStorageClientConfiguration(storageID));
    }
    
    /**
     * @param storageID the name of the remote Storage.
     * @return a ReadableStorage for the Storage exposed at //localhost:28000/storageID.
     */
    public static StorageWriterClient getWriter(String storageID) {
        return new StorageWriterClient(getStorageClientConfiguration(storageID));
    }
    
    /**
     * @param name for the Storage listening at {@code //localhost:28000/name}.
     * @return setup for a StorageWriterClient connecting to the named Storage.
     */
    public static Configuration getStorageClientConfiguration(String name) {
        return Configuration.newMemoryBased(
            ConnectionConsumer.CONF_RPC_TARGET, STORAGE_RMI_PREFIX + name
        );
    }

    /**
     * Reads all XML-files from source as streams, splits them on recordTag and
     * assigns the given recordBase and ids from idTag, prefixed with idPrefix.
     * The generated Records are ingested into the Storage listening at the RMI
     * address {@code //localhost:28000/storage}.
     * </p><p>
     * This is standard behaviour for most SummaRise ingesters.
     * @param storage   the name of the Storage to ingest into.
     * @param source    where to get the data for ingesting.
     * @param recordBase created Records will have this base assigned.
     * @param idPrefix   the ids for created Records will have this prefix.
     * @param recordTag  the start- and end-tag for a Record in the XML stream.
     * @param idTag      the tag containing the id of the Record in the XML.
     * @return the number of ingested records.
     */
    public static int ingest(String storage, String source, String recordBase, String idPrefix,
                             String recordTag, String idTag) {
        return ingest(storage, source, new XMLSplitterFilter(
            Configuration.newMemoryBased(
                XMLSplitterFilter.CONF_BASE, recordBase,
                XMLSplitterFilter.CONF_COLLAPSE_PREFIX, "true",
                XMLSplitterFilter.CONF_ID_ELEMENT, idTag,
                XMLSplitterFilter.CONF_ID_PREFIX, idPrefix,
                XMLSplitterFilter.CONF_PRESERVE_NAMESPACES, "true",
                XMLSplitterFilter.CONF_RECORD_ELEMENT, recordTag,
                XMLSplitterFilter.CONF_REQUIRE_VALID, "false")
        ));
    }

    /**
     * Ingests the Records from the given source into the Storage.
     * @param storage destination for the Records.
     * @param source  source for the Records.
     * @return the number of ingested Records.
     */
    public static int ingest(String storage, ObjectFilter source) {
        StorageWriterClient writableStorage = new StorageWriterClient(getStorageClientConfiguration(storage));
        RecordWriter writer = new RecordWriter(writableStorage, 10, 10000);
        writer.setSource(source);
        int count = 0;
        Record last = null;
        while (writer.hasNext()) {
            last = writer.next().getRecord();
            log.debug("Pushed " + last.getId() + " to " + storage);
            count++;
        }
        log.debug("Finished ingesting " + count + " records from " + source);
        writer.close(true);

        if (last == null) {
            return count;
        }

        // We want to guarantee flush
        StorageReaderClient readableStorage = new StorageReaderClient(getStorageClientConfiguration(storage));
        try {
            while (readableStorage.getRecord(last.getId(), null) == null) {
                Thread.sleep(100);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to request '" + last.getId() + "' from storage at " + storage);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting to ensure flush", e);
        }
        return count;
    }

    public static int ingest(String storage, String source, ObjectFilter processor) {
        log.debug("Ingesting from " + source);
        Filter reader = new ArchiveReader(getArchiveReaderConfiguration(source));
        processor.setSource(reader);
        return ingest(storage, processor);
    }

    /**
     * Connects to the Storage at RMI address {@code //localhost:28000/name} and
     * extracts all Records.
     * @param storage the remote Storage.
     * @return all Records in the Storage.
     * @throws java.io.IOException if the Storage could not be contacted.
     */
    public static List<Record> getRecords(String storage) throws IOException {
        StorageReaderClient remote = new StorageReaderClient(getStorageClientConfiguration(storage));
        try {
            long iterKey = remote.getRecordsModifiedAfter(0, null, null);
            Iterator<Record> iterator = new StorageIterator(remote, iterKey);
            List<Record> extracted = new ArrayList<Record>();
            while (iterator.hasNext()) {
                extracted.add(iterator.next());
            }
            return extracted;
        } finally {
            remote.releaseConnection();
        }
    }

    public static Record getRecord(String storage, String recordId) throws IOException {
        StorageReaderClient remote = new StorageReaderClient(getStorageClientConfiguration(storage));
        try {
            return remote.getRecord(recordId, null);
        } finally {
            remote.releaseConnection();
        }
    }

    /**
     * Starts a clean Storage that responds to RMI connections to
     * {@code //localhost:28000/name}.
     * @param name the name for the wanted Storage.
     * @return a Storage ready for use.
     * @throws java.io.IOException if the Storage could not ne started.
     */
    public static Storage startStorage(String name) throws IOException {
        log.debug("Starting Storage at " + STORAGE_RMI_PREFIX + name);
        return StorageFactory.createStorage(getStorageConfiguration(name));
    }

    /**
     * Removes all generates storage files and index data.
     * @throws Exception if some files could not be deleted.
     */
    public static void cleanup() throws Exception {
        if (storageRoot.exists()) {
            Files.delete(storageRoot);
        }
    }

    /**
     * @return a location for storage files that is unique within this session.
     */
    public static File getNewStorageLocation() {
        return new File(storageRoot, "storage" + storageCounter++);
    }

    public static Configuration loadGeneralConfiguration(String storage, String location) {
        System.setProperty("index_location", INDEX_ROOT);
        System.setProperty("index_storage", ReleaseHelper.STORAGE_RMI_PREFIX + storage);
        return Configuration.load(location);
    }
}
