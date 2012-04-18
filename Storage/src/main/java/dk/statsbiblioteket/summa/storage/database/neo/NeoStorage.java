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
package dk.statsbiblioteket.summa.storage.database.neo;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.UniqueTimestampGenerator;
import dk.statsbiblioteket.summa.storage.StorageUtils;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.util.Zips;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.index.lucene.QueryContext;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.management.RuntimeErrorException;

/**
 * NoSQL with focus on graph capabilities (parent-child relations).
 * </p><p>
 * Important:; This class is not fully implemented and should not be used in
 * production. Preliminary testing indicates that the memory overhead during
 * extraction of a large number of records is excessive (>1GB / 1M records).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class NeoStorage implements Storage {
    private static Log log = LogFactory.getLog(NeoStorage.class);

    private static final String ID_KEY = "ID";
    /** Key for base in record. */
    private static final String BASE_KEY = "BASE";
    /** Delete flag key in record. */
    private static final String DELETED_FLAG_KEY = "DELETED";
    /** Indexable flag key in record. */
    private static final String INDEXABLE_FLAG_KEY = "INDEXABLE";
    /** Has relations flag key in record. */
    private static final String HAS_RELATIONS_FLAG_KEY = "HAS_RELATIONS";
    /** Gzipped content flag key in record. */
    private static final String GZIPPED_CONTENT_FLAG_KEY = "GZIPPED";
    /** Created time key in record. */
    private static final String CTIME_KEY = "CTIME";
    /** Modified time key in record. */
    private static final String MTIME_KEY = "MTIME";
    /** Meta-data key in record. */
    private static final String META_KEY = "META";
    /** Parents IDs key in record. */
    private static final String PARENT_IDS_KEY = "PARENTS";
    /** Chils IDs key in record. */
    private static final String CHILD_IDS_KEY = "CHILDREN";

    private static final String CONTENT_KEY = "CONTENT";

    private enum RELATIONS implements RelationshipType {
        PARENT, CHILD
    }

    private File location;
    private GraphDatabaseService neo;
    private IndexManager indexManager;
    private Index<Node> ids;
    private Index<Node> mtime;
    private UniqueTimestampGenerator timestampGenerator =
        new UniqueTimestampGenerator();

    private long hitsKey = 0;
    private final Map<Long, IndexHits<Node>> hits =
        new HashMap<Long, IndexHits<Node>>(10);
    private final Map<Long, QueryOptions> options =
        new HashMap<Long, QueryOptions>(10);

    public NeoStorage(Configuration conf) {
        // Database file location
        if (conf.valueExists(DatabaseStorage.CONF_LOCATION)) {
            log.debug("Property '" + DatabaseStorage.CONF_LOCATION
                      + "' exists, using value '"
                      + conf.getString(DatabaseStorage.CONF_LOCATION)
                      + "' as location");
            location = new File(conf.getString(DatabaseStorage.CONF_LOCATION));
        } else {
            location = new File (StorageUtils.getGlobalPersistentDir(conf),
                                 "storage" + File.separator + "h2");
            log.debug("Using default location '" + location + "'");
        }

        if (!location.equals(location.getAbsoluteFile())) {
            log.debug(String.format("Transforming relative location '%s' to "
                                    + "absolute location'%s'",
                                    location, location.getAbsoluteFile()));
            location = location.getAbsoluteFile();
        }

        if (location.isFile()) {
            throw new ConfigurationException("Database path contains a regular"
                                             + " file");
        }

        // Create new DB?
        boolean createNew =
            conf.getBoolean(DatabaseStorage.CONF_CREATENEW, true);

        // Force new DB?
        boolean forceNew =
            conf.getBoolean(DatabaseStorage.CONF_FORCENEW, false);

        log.info("NeoStorage location: '" + location
                 + "', createNew: " + createNew + ", forceNew: " + forceNew);
        init(location, createNew, forceNew);
        log.trace("Construction completed");

    }

    private void init(File location, boolean createNew, boolean forceNew) {
        neo = new EmbeddedGraphDatabase(location.getAbsolutePath());
        indexManager =  neo.index();
        ids = indexManager.forNodes(ID_KEY);
        mtime = indexManager.forNodes(MTIME_KEY);
    }

    @Override
    public long getModificationTime(String base) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<Record> getRecords(List<String> ids, QueryOptions options) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Record getRecord(String id, QueryOptions options) throws IOException {
        IndexHits<Node> hits = ids.get(ID_KEY, id);
        Node node = hits.getSingle();
        hits.close();
        if (node == null) {
            return null;
        }
        return nodeToRecord(node, options);
    }

    private Record nodeToRecord(Node node, QueryOptions options) {
        Record record = new Record((String)node.getProperty(ID_KEY),
                                   (String)node.getProperty(BASE_KEY),
                                   new byte[0]);
        record.setCreationTime(timestampGenerator.systemTime(
            (Long) node.getProperty(CTIME_KEY)));
        record.setModificationTime(timestampGenerator.systemTime(
            (Long) node.getProperty(MTIME_KEY)));
        // TODO: Handle compression
        record.setContent((byte[])node.getProperty(CONTENT_KEY), false);
        // TODO: Finish this (remember options)
        List<Record> children = new ArrayList<Record>();
        for (Relationship rel: node.getRelationships(RELATIONS.CHILD)) {
            Node child = rel.getEndNode();
            if (!node.equals(child)) { // Why do we need this check?
                children.add(nodeToRecord(rel.getEndNode(), options));
            }
        }
        record.setChildren(children);
        return record;
    }

    @Override
    public long getRecordsModifiedAfter(
        long time, String base, QueryOptions options) throws IOException {
        log.debug(
            "getRecordsModifiedAfter(" + time + ", " + base + ", ...) called");
        long dMtime = timestampGenerator.baseTimestamp(time);

        String QUERY = base == null ?
                       String.format(
                           "%s:[%s TO 999]",
                           MTIME_KEY, Long.toString(dMtime)) :
                       String.format(
                           "%s:%s AND %s:[%s TO 999]",
                           BASE_KEY, base, MTIME_KEY, Long.toString(dMtime));
        IndexHits<Node> iHits = mtime.query(
            QUERY, new QueryContext(QUERY).sort(MTIME_KEY));
        hits.put(hitsKey, iHits);
        this.options.put(hitsKey, options);
        return hitsKey++;
    }


    @Override
    public Record next(long iteratorKey) throws IOException {
        IndexHits<Node> iHits = hits.get(iteratorKey);
        if (iHits == null || !iHits.hasNext()) {
            throw new NoSuchElementException(
                "No (more) Records for key " + iteratorKey);
        }
        Record current = nodeToRecord(iHits.next(), options.get(iteratorKey));
        if (!iHits.hasNext()) {
            iHits.close();
            hits.remove(iteratorKey);
            options.remove(iteratorKey);
        }
        return current;
    }

    @Override
    public List<Record> next(long iteratorKey, int maxRecords) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void flush(Record record, QueryOptions options) throws IOException {
        log.trace("Flushing single record " + record);
        long flushTime = -System.nanoTime();
        Transaction tx = neo.beginTx();
        try {
            addNodeWithConnection(record);
            tx.success();
        } finally {
            tx.finish();
        }
        flushTime += System.nanoTime();
        if (log.isDebugEnabled()) {
            log.debug("Flushed single record " + record + " in "
                      + flushTime / 1000000.0 + "ms");
        }
        // TODO Respect queryOptions
    }

    private Node addNodeWithConnection(Record record) {
        Node node = neo.createNode();
        
        long nowStamp = timestampGenerator.next();
        record.setModificationTime(nowStamp);
        boolean hasRelations = record.hasParents() || record.hasChildren();

        node.setProperty(ID_KEY, record.getId());
        node.setProperty(BASE_KEY, record.getBase());
        node.setProperty(DELETED_FLAG_KEY, record.isDeleted());
        node.setProperty(INDEXABLE_FLAG_KEY, record.isIndexable());
        node.setProperty(HAS_RELATIONS_FLAG_KEY, hasRelations);
        // TODO: Leading zeroes
        node.setProperty(CTIME_KEY, timestampGenerator.baseTimestamp(
            record.getCreationTime()));
        node.setProperty(MTIME_KEY, timestampGenerator.baseTimestamp(
            record.getModificationTime()));
        node.setProperty(GZIPPED_CONTENT_FLAG_KEY,
                         Zips.gzipBuffer(record.getContent()));
        node.setProperty(META_KEY,
                         record.hasMeta() ?
                         record.getMeta().toFormalBytes() : new byte[0]);
        // TODO: Add compression
        node.setProperty(CONTENT_KEY, record.getContent());

        // TODO: Match with previous children

        // Update index
        ids.add(node, ID_KEY, record.getId());
        mtime.add(node, BASE_KEY, record.getBase());
        mtime.add(node, MTIME_KEY, record.getModificationTime());

        if (record.getChildren() != null) {
            for (Record child: record.getChildren()) {
                Node childNode = addNodeWithConnection(child);
                node.createRelationshipTo(childNode, RELATIONS.CHILD);
                //childNode.createRelationshipTo(node, RELATIONS.PARENT);
            }
        }
        return node;
    }

    @Override
    public void flush(Record record) throws IOException {
        flush(record, null);
    }

    @Override
    public void flushAll(List<Record> records, QueryOptions options) throws IOException {
        log.trace("Flushing " + records.size() + " records");
        long flushTime = -System.nanoTime();
        Transaction tx = neo.beginTx();
        try {
            for (Record record: records) {
                addNodeWithConnection(record);
            }
            tx.success();
        } finally {
            tx.finish();
        }
        flushTime += System.nanoTime();
        if (log.isDebugEnabled()) {
            log.trace("Flushed " + records.size() + " in "
                      + flushTime / 1000000.0 + "ms ("
                      + records.size() * 1000 * 1000000 / flushTime
                      + "records/sec");
        }
        // TODO Respect queryOptions
    }

    @Override
    public void flushAll(List<Record> records) throws IOException {
        flushAll(records, null);
    }

    @Override
    public synchronized void close() throws IOException {
        for (Map.Entry<Long, IndexHits<Node>> hit: hits.entrySet()) {
            hit.getValue().close();
        }
        hits.clear();
        neo.shutdown();
    }

    @Override
    public void clearBase(String base) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String batchJob(String jobName, String base, long minMtime, long maxMtime, QueryOptions options) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
