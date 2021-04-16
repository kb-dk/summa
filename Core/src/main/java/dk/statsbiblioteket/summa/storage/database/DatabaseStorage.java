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

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.SummaConstants;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.util.SimplePair;
import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.summa.common.util.UniqueTimestampGenerator;
import dk.statsbiblioteket.summa.storage.BaseStats;
import dk.statsbiblioteket.summa.storage.BatchJob;
import dk.statsbiblioteket.summa.storage.StorageBase;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.database.MiniConnectionPoolManager.StatementHandle;
import dk.statsbiblioteket.summa.storage.database.cursors.*;
import dk.statsbiblioteket.util.*;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.script.ScriptException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An abstract implementation of a SQL database driven extension of {@link StorageBase}.
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, teg, mke")
public abstract class DatabaseStorage extends StorageBase {
    private static final Log recordlog = LogFactory.getLog("storagequeries");
    private static final Log log = LogFactory.getLog(DatabaseStorage.class);

    /**
     * Internal batch jobs are basically a hack. If {@link #batchJob} is called with INTERNAL_BATCH_JOB as job name,
     * the query options must contain the parameter {@link #INTERNAL_JOB_NAME} and the method
     * {@link #handleInternalBatchJob} will be called.
     */
    public static final String INTERNAL_BATCH_JOB = "_internal_";

    /**
     * The name of the internal job to execute.
     */
    public static final String INTERNAL_JOB_NAME = "jobname";

    /**
     * Key for ID in a record.
     */
    public static final int ID_KEY = 1;
    /**
     * Key for base in record.
     */
    public static final int BASE_KEY = 2;
    /**
     * Delete flag key in record.
     */
    public static final int DELETED_FLAG_KEY = 3;
    /**
     * Indexable flag key in record.
     */
    public static final int INDEXABLE_FLAG_KEY = 4;
    /**
     * Has relations flag key in record.
     */
    public static final int HAS_RELATIONS_FLAG_KEY = 5;
    /**
     * Gzipped content flag key in record.
     */
    public static final int GZIPPED_CONTENT_FLAG_KEY = 6;
    /**
     * Created time key in record.
     */
    public static final int CTIME_KEY = 7;
    /**
     * Modified time key in record.
     */
    public static final int MTIME_KEY = 8;
    /**
     * Meta-data key in record.
     */
    public static final int META_KEY = 9;
    /**
     * Parents IDs key in record.
     */
    public static final int PARENT_IDS_KEY = 10;
    /**
     * Chils IDs key in record.
     */
    public static final int CHILD_IDS_KEY = 11;

    /**
     * The number of minutes iterators opened with
     * {@link #getRecordsModifiedAfter} are valid if they are unused. After the
     * specified amount of inactivity they may be cleaned up by the storage.
     * The default value is {@link #DEFAULT_ITERATOR_TIMEOUT}.
     */
    public static final String CONF_ITERATOR_TIMEOUT = "summa.storage.database.iteratortimeout";

    /**
     * Default value for the {@link #CONF_ITERATOR_TIMEOUT} property.
     */
    public static final long DEFAULT_ITERATOR_TIMEOUT = 120;

    /**
     * The property-key for the username for the underlying database, if needed.
     * The default value is {@link #DEFAULT_USERNAME}.
     */
    public static final String CONF_USERNAME = "summa.storage.database.username";
    /**
     * Default value for {@link #CONF_USERNAME}.
     */
    public static final String DEFAULT_USERNAME = "summa";

    /**
     * The property-key for the the password for the underlying database, if
     * needed.
     */
    public static final String CONF_PASSWORD = "summa.storage.database.password";

    /**
     * The name of the database to connect to. Default value is
     * {@link #DEFAULT_DATABASE}.
     */
    public static final String CONF_DATABASE = "summa.storage.database.name";

    /**
     * Default value for {@link #CONF_DATABASE}.
     */
    public static final String DEFAULT_DATABASE = "summa";

    /**
     * Maximum number of concurrent connections to keep to the database.
     * Default value is {@link #DEFAULT_MAX_CONNECTIONS}.
     */
    public static final String CONF_MAX_CONNECTIONS = "summa.storage.database.maxconnections";

    /**
     * Default value for {@link #CONF_MAX_CONNECTIONS}.
     */
    public static final int DEFAULT_MAX_CONNECTIONS = 10;

    /**
     * The port to connect to the database on.
     */
    public static final String CONF_PORT = "summa.storage.database.port";

    /**
     * The hostname to connect to the database on. Default is
     * {@link #DEFAULT_HOST}.
     */
    public static final String CONF_HOST = "summa.storage.database.host";

    /**
     * Default value for {@link #CONF_HOST}.
     */
    public static final String DEFAULT_HOST = "localhost";

    /**
     * A list of strings with base names that explicitly should have relations
     * tracking disabled. That is, records from a base listed here will not have
     * their parent/child relations tracked.
     * <p/>
     * Excepting big bases which are known a-priori to not use relations from
     * relations tracking may be a big optimization on certain Storage
     * implementations.
     * <p/>
     * Even though relation ship tracking is disabled records may still have
     * relationships, the storage just wont make sure they are consistent.
     * <p/>
     * The default value for this property is the empty list, meaning that
     * all bases will have relationship tracking enabled.
     */
    public static final String CONF_DISABLE_RELATIONS_TRACKING = "summa.storage.database.disablerelationstracking";

    /**
     * The property-key for the boolean value determining if a new database
     * should be created is there is no existing database. If createnew is
     * true and a database exists and forcenew is true, the existing database
     * is deleted and a new one created. If createnew is true and a database
     * exists and forcenew is false, the existing database is reused.
     */
    public static final String CONF_CREATENEW = "summa.storage.database.createnew";

    /**
     * The property-key for the boolean determining if a new database should
     * be created, no matter is a database already exists.
     */
    public static final String CONF_FORCENEW = "summa.storage.database.forcenew";

    /**
     * The location of the database to use/create. If the location is not an
     * absolute path, it will be appended to the System property "
     * summa.control.client.persistent.dir". If that system property does not
     * exist, the location will be relative to the current dir.
     */
    public static final String CONF_LOCATION = "summa.storage.database.location";

    /**
     * Number of hits to return per-page. This configuration option is only
     * used if the actual storage implementation request that a paging model
     * be used to scan large result sets.
     * </p><p>
     * Note: This page size is used for read-operations as SQL addition 'LIMIT xxx'.
     * See {@link #CONF_PAGE_SIZE_UPDATE}.
     * </p><p>
     * Optional. The default value for this property is 500.
     */
    public static final String CONF_PAGE_SIZE = "summa.storage.database.pagesize";
    public static final int DEFAULT_PAGE_SIZE = 500;

    /**
     * Number of rows to lock when performing updates.  This configuration option is only
     * used if the actual storage implementation request that a paging model be used to scan large sets.
     * </p><p>
     * Note: This page size is used for update-operations as SQL addition 'LIMIT xxx'.
     * See {@link #CONF_PAGE_SIZE}.
     * </p><p>
     * Optional. The default value for this property is {@link #CONF_PAGE_SIZE} if defines, else 100.
     */
    public static final String CONF_PAGE_SIZE_UPDATE = "summa.storage.database.pagesize.update";
    public static final int DEFAULT_PAGE_SIZE_UPDATE = 100;

    /**
     * The name of the main table in the database in which record metadata
     * is stored. Parent/child relations are stored in {@link #RELATIONS}
     */
    public static final String RECORDS = "summa_records";

    /**
     * The name of the table holding the parent/child relations of the records
     * from {@link #RECORDS}.
     */
    public static final String RELATIONS = "summa_relations";

    /**
     * The of the table holding statistic on a base level.
     */
    public static final String BASE_STATISTICS = "summa_basestats";

    /**
     * Number of records in deleted stated and indexable.
     */
    public static final String DELETE_INDEXABLES_COLUMN = "deletedindexables";

    /**
     * Number of records not in deleted stated but indexable.
     */
    public static final String NON_DELETED_INDEXABLES_COLUMN = "nondeletedindexables";

    /**
     * Number of records in deleted state and not indexable.
     */
    public static final String DELETED_NON_INDEXABLES_COLUMN = "deletednonindexables";

    /**
     * Number of records not deleted and not indexable.
     */
    public static final String NON_DELETED_NON_INDEXABLES_COLUMN = "nonDeletedNonIndexables";
    /**
     * The {@code id} column contains the unique identifier for a given record
     * in the database. The value that is mapped directly to the {@code id}
     * value of the constructed {@link Record} objects.
     */
    public static final String ID_COLUMN = "id";

    /**
     * The {@code base} column contains the value that is mapped directly to
     * the {@code base} of the {@link Record} objects retrieved from this
     * storage.
     * <p/>
     * Generally the record base is used to track provenance and decide which
     * filters/XSLTs to apply to the contents and so forth.
     */
    public static final String BASE_COLUMN = "base";

    /**
     * The {@code deleted} column signifies whether the record should be treated
     * as non-existing. If deleted records should be retrieved or not can
     * be specified by setting the relevant flags in the {@link QueryOptions}
     * passed to the storage
     */
    public static final String DELETED_COLUMN = "deleted";

    /**
     * The {@code indexable} column signifies whether the indexer should index
     * the given record. Whether or not to retrieve non-indexable records
     * can be handled by setting the appropriate {@link QueryOptions} in
     * {#getRecordsModifiedAfter}
     */
    public static final String INDEXABLE_COLUMN = "indexable";

    /**
     * The {@code hasRelations} column contains a flag that indicates whether
     * the record is a part of any parent/child relationship. This flag
     * is used as an optimization in the lazy relation lookups strategy
     */
    public static final String HAS_RELATIONS_COLUMN = "hasRelations";

    /**
     * The {@code data} column contains the raw record-data as ingested.
     */
    public static final String DATA_COLUMN = "data";

    /**
     * The {@code ctime} column signifies the time of record creation in the
     * database.
     * <p/>
     * The value stored here is a unique timestamp generated with a
     * {@link UniqueTimestampGenerator} which means it does not map directly
     * to the system time. On record construction time the timestamp is
     * mapped back to a system time value and can be inspected via
     * {@link Record#getCreationTime()} as usual.
     */
    public static final String CTIME_COLUMN = "ctime";

    /**
     * The {@code mtime} column signifies the time of record modification in the
     * database. This timestamp is used when {@link #getRecordsModifiedAfter} is
     * called.
     * <p/>
     * The value stored here is a unique timestamp generated with a
     * {@link UniqueTimestampGenerator} which means it does not map directly
     * to the system time. On record construction time the timestamp is
     * mapped back to a system time value and can be inspected via
     * {@link Record#getModificationTime()} as usual.
     */
    public static final String MTIME_COLUMN = "mtime";

    /**
     * The {@code meta} column contains meta-data for the Record in the form of
     * key-value pairs of Strings. See {@link StringMap#toFormal()} for format.
     * These values are mapped to the {@link Record} meta contents which can
     * be handled with {@link Record#getMeta()} and friends.
     */
    public static final String META_COLUMN = "meta";

    /**
     * First column in the {@link #RECORDS} table. Contains the record id of the
     * parent in a parent/child relationship. The referred parent does
     * not need to be present in the database.
     */
    public static final String PARENT_ID_COLUMN = "parentId";

    /**
     * Second row in the {@link #RELATIONS} table. Contains the record
     * id of the child record. The referred child record does not need to
     * exist in the database.
     */
    public static final String CHILD_ID_COLUMN = "childId";

    /**
     * The valid column id, for the {@link #BASE_STATISTICS} table. The column
     * holds the state of the indexable, deleted count and ctime.
     */
    public static final String VALID_COLUMN = "valid";

    /* Constants for database-setup */
    /**
     * The ID limit.
     */
    public static final int ID_LIMIT = 255;
    /**
     * The base limit.
     */
    public static final int BASE_LIMIT = 31;
    //public static final int DATA_LIMIT =     50*1024*1024;
    /**
     * The fetch size.
     */
    public static final int FETCH_SIZE = 100;

    /**
     * An empty iterator key.
     */
    private static final long EMPTY_ITERATOR_KEY = -1;

    /**
     * If true, the parentIDs and childIDs of requested Records are enriched with all relations from the
     * relations table. If false, only the explicitely in-Record stored IDs are returned.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_EXPAND_RELATIVES_ID_LIST = "databasestorage.relatives.expandidlist";
    public static final boolean DEFAULT_EXPAND_RELATIVES_ID_LIST = false;

    /**
     * If true, expanded Record trees are pruned of relatives not it any ID-list before delivery.
     * </p><p>
     * Optional. Default is true;
     */
    private static final String CONF_PRUNE_RELATIVES_ON_GET = "databasestorage.relatives.pruneonget";
    private static final boolean DEFAULT_PRUNE_RELATIVES_ON_GET = true;

    /**
     * The timestamp contract states that all mtime timestamps are unique. This ensures consistent ordering and
     * allows for single-Record granularity for {@link #getRecordsModifiedAfter(long, String, QueryOptions)}.
     * </p><p>
     * This option must be changed for an existing index without re-creating the M-index with the proper modifier.
     * See {@link #doCreateSummaRecordsTable(Connection)}.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_OBEY_TIMESTAMP_CONTRACT = "databasestorage.obeytimestampcontract";
    public static final boolean DEFAULT_OBEY_TIMESTAMP_CONTRACT = true;

    /**
     * Whether or not to use special-casing of certain requests, notably ones with parent expansion and no children
     * {@link #getRecordsModifiedAfterSingleParent(long, String)}.
     * </p><p>
     * Optional. Default is false (which will likely be changed to true in the future).
     */
    public static final String CONF_USE_OPTIMIZATIONS = "databasestorage.useoptimizations";
    public static final boolean DEFAULT_USE_OPTIMIZATIONS = false;

    private static final int IS_CONNECTION_VALID_TIMEOUT_SECONDS = 2;

    /**
     * A list of strings with base names that explicitly should have relations
     * stored as meta data on Record level, as opposed to only stored in the
     * relations table.
     * </p><p>
     * Enable this for bases where invalidation of relations is a problem and
     * where the sizes of relation-trees is manageable (< 100 Records).
     * <p/>
     * The default value for this property is the empty list, meaning that
     * zero bases will have relationship storing enabled.
     */
    public static final String CONF_BASES_WITH_STORED_RELATIONS = "summa.storage.database.basesWithStoredRelations";
    // Used for holding the original stored IDs as Record metadata
    private static final String STORED_PARENT_IDS = "_StoredParentIDs_";
    private static final String STORED_CHILD_IDS = "_StoredChildIDs_";
    private static final String STORED_ID_DELIMITER = "__DELIM__";

    /*
    private StatementHandle stmtGetModifiedAfter;
    private StatementHandle stmtGetModifiedAfterAll;
    private StatementHandle stmtGetModifiedAfterNoData;
    private StatementHandle stmtGetModifiedAfterAllNoData;
    private StatementHandle stmtGetRecordTemp;
    private StatementHandle stmtGetRecord;
    private StatementHandle stmtDeleteRecord;
    private StatementHandle stmtCreateRecord;
    private StatementHandle stmtUpdateRecord;
    private StatementHandle stmtTouchRecord;
    private StatementHandle stmtGetParentIDQuery;
    private StatementHandle stmtTouchParents;
    private StatementHandle stmtGetChildren;
    private StatementHandle stmtGetParents;
    private StatementHandle stmtGetRelatedIds;
    private StatementHandle stmtMarkHasRelations;
    private StatementHandle stmtCreateRelation;
     */
    /* Update statement for modification time of a base. */
    //    private StatementHandle stmtUpdateMtimForBase;
    /* Insert statement for base statistic. */
    //    private StatementHandle stmtInsertBaseStats;
    /* Sets a base statistic row in invalid. */
    //    private StatementHandle stmtSetBaseStatsInvalid;
    /* Retrieves the last modification time for a base. */
    //    private StatementHandle stmtGetLastModificationTime;
    /* Insert full set of data into base statistic row. */
    //    private StatementHandle stmtInsertFullBaseStats;
    /* Update full set of base statistic. */
    //    private StatementHandle stmtUpdateFullBaseStats;
    /* String for all columns. */
    //    private String allColumns;
    //    private String allColumnsButDataAndMeta;

    /**
     * Iterator keys.
     */
    private Map<Long, Cursor> iterators = new HashMap<>(10);

    private CursorReaper iteratorReaper;
    /**
     * Unique time stamp generator.
     */
    private UniqueTimestampGenerator timestampGenerator;

    /**
     * List of base names for which we don't track relations.
     */
    private Set<String> disabledRelationsTracking;
    /**
     * List of base names which has relations stored on Record level.
     */
    private final TreeSet<String> basesWithStoredRelations;

    /**
     * True if lazy relation should be used.
     */
    private boolean useLazyRelations;
    /**
     * True if paging model should be used.
     */
    private boolean usePagingModel;

    private final int pageSize;
    private int pageSizeUpdate;
    private final boolean expandRelativesLists;
    private final boolean pruneRelativesOnGet;
    private boolean useOptimizations;

    protected StatementHandler statementHandler;
    private final Profiler profiler = new Profiler(Integer.MAX_VALUE, 100);
    /**
     * Used by {@link #getRecord(String, QueryOptions)} and {@link #getRecords(List, QueryOptions)}.
     */
    private final QueryOptions defaultGetOptions;

    private final boolean obeyTimestampContract;

    // Timestamp for service start. Used for logging
    private final String START_TIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").format(new Date());

    protected static final Timing timing = new Timing("all");

    protected static final Timing timingGetRecordsModifiedAfter = timing.getChild("recordsModifiedAfter");
    protected static final Timing timingDoGetRecordsModifiedAfterCursor = timing.getChild("doGetRecordsModifiedAfterCursor");
    protected static final Timing timingNext = timing.getChild("next");
    protected static final Timing timingRecordExists = timing.getChild("recordExists");

    protected static final Timing timingGetRecord = timing.getChild("getRecord");
    protected static final Timing timingGetRecordWithConnection = timing.getChild("getRecordWithConnection");
    protected static final Timing timingGetRecords = timing.getChild("getRecords");
    protected static final Timing timingGetRecordsWithFullObjectTree = timing.getChild("getRecordsWithFullObjectTree");
    protected static final Timing timingGetRecordsWithConnection = timing.getChild("getRecordsWithConnection");
    protected static final Timing timingGetParents = timing.getChild("getParents");
    protected static final Timing timingGetParentsIDsOnly = timing.getChild("getParentsIDsOnly");
    protected static final Timing timingGetChildren = timing.getChild("getChildren");
    protected static final Timing timingGetChildIDsOnly = timing.getChild("getChildIDsOnly");
    protected static final Timing timingResolveRelatedIDs = timing.getChild("resolveRelatedIDs");

    protected static final Timing timingFlush = timing.getChild("flush");
    protected static final Timing timingFlushWithConnection = timing.getChild("flushWithConnection");
    protected static final Timing timingCreateNewRecordWithConnection = timing.getChild("CreateNewRecordWithConnection");
    protected static final Timing timingFlushAll = timing.getChild("flushAll");
    protected static final Timing timingLoadAndSetChildRelations = timing.getChild("loadAndSetChildRelations");
    protected static final Timing timingUpdateRecord = timing.getChild("updateRecord");
    protected static final Timing timingUpdateRecordWithConnection = timing.getChild("updateRecordWithConnection");
    protected static final Timing timingTouchRecord = timing.getChild("touchRecord");

    protected static final Timing timingTouchOldParentChildRelations = timing.getChild("touchOldParentChildRelations");
    protected static final Timing timingRelationsExists = timing.getChild("CheckRelations");
    protected static final Timing timingCreateRelations = timing.getChild("CreateRelations");
    protected static final Timing timingCheckHasRelations = timing.getChild("CheckHasRelations");
    protected static final Timing timingExpandRelationsWithConnection = timing.getChild("expandRelationsWithConnection");

    protected static final Timing timingClearBase = timing.getChild("ClearBase");
    protected static final Timing timingBatchJob = timing.getChild("BatchJob");
    protected static final Timing timingApplyJobToRecord = timing.getChild("ApplyJobToRecord");
    protected static final Timing timingUpdateLastModficationTimeForBase = timing.getChild("UpdateLastModficationTimeForBase");
    protected static final Timing timingGetModificationTime = timing.getChild("GetModificationTime");
    protected static final Timing timingGetStats = timing.getChild("GetStats");
    protected static final Timing timingInvalidateStats = timing.getChild("InvalidateStats");
    /**
     * A variation of {@link QueryOptions} used to keep track of recursion
     * depths for expanding children and parents.
     */
    private static class RecursionQueryOptions extends QueryOptions {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 16841L;

        /**
         * Recursion depth for children.
         */
        private int childRecursionDepth;
        /**
         * Parent recursion height.
         */
        private int parentRecursionHeight;

        /**
         * Recursion query options constructor.
         *
         * @param original The original query options.
         */
        public RecursionQueryOptions(QueryOptions original) {
            super(original);
            resetRecursionLevels();
        }

        /**
         * Return the child recursion depth.
         *
         * @return The child recursion depth.
         */
        public int childRecursionDepth() {
            return childRecursionDepth;
        }

        /**
         * Return the parent recursion height.
         *
         * @return The parent recursion height.
         */
        public int parentRecursionHeight() {
            return parentRecursionHeight;
        }

        /**
         * Do a child recursion, count down the child recursion depth.
         *
         * @return The recursion query options equals this object.
         */
        public RecursionQueryOptions decChildRecursionDepth() {
            childRecursionDepth--;
            return this;
        }

        /**
         * Count the parent recursion height down with one.
         *
         * @return This object.
         */
        public RecursionQueryOptions decParentRecursionHeight() {
            parentRecursionHeight--;
            return this;
        }

        /**
         * Reset the child depth and parent height.
         *
         * @return This object.
         */
        public RecursionQueryOptions resetRecursionLevels() {
            childRecursionDepth = childDepth();
            parentRecursionHeight = parentHeight();
            return this;
        }

        /**
         * Ensure that {@code options} is a RecursionQueryOptions. If it already
         * is, just reset it and return it.
         *
         * @param options the QueryOptions to convert to a RecursionQueryOptions
         * @return the input wrapped as a RecursionQueryOptions, or just the
         *         input if it is already recursive
         */
        @SuppressWarnings("unused")
        public static RecursionQueryOptions wrap(QueryOptions options) {
            if (options == null) {
                return null;
            }

            return options instanceof RecursionQueryOptions ?
                    ((RecursionQueryOptions) options).resetRecursionLevels() :
                    new RecursionQueryOptions(options);
        }

        /**
         * Wrap {@code options} in a RecursionQueryOptions which will be
         * set to not extract parent records.
         *
         * @param options the query options to wrap
         * @return a new query options that will not extract parent records
         */
        public static RecursionQueryOptions asChildOnlyOptions(QueryOptions options) {
            if (options.parentHeight() == 0 && options instanceof RecursionQueryOptions) {
                return (RecursionQueryOptions) options;
            }

            RecursionQueryOptions o = new RecursionQueryOptions(options);
            o.parentHeight = 0;
            o.parentRecursionHeight = 0;
            return o;
        }

        /**
         * Wrap {@code options} in a RecursionQueryOptions which will be
         * set to not extract child records.
         *
         * @param options the query options to wrap
         * @return a new query options that will not extract child records
         */
        public static RecursionQueryOptions asParentsOnlyOptions(QueryOptions options) {
            if (options.childDepth() == 0 && options instanceof RecursionQueryOptions) {
                return (RecursionQueryOptions) options;
            }

            RecursionQueryOptions o = new RecursionQueryOptions(options);
            o.childDepth = 0;
            o.childRecursionDepth = 0;
            return o;
        }
    }

    /**
     * Constructions a database storage with a given configuration. This means
     * that the parent is also constructed with the configuration.
     *
     * @param conf The configuration.
     * @throws IOException If error occur while starting the storage.
     */
    public DatabaseStorage(Configuration conf) throws IOException {
        super(updateConfiguration(conf));
        obeyTimestampContract = conf.getBoolean(CONF_OBEY_TIMESTAMP_CONTRACT, DEFAULT_OBEY_TIMESTAMP_CONTRACT);
        final DatabaseStorage myself = this;
        statementHandler = new StatementHandler(useLazyRelationLookups()) {
            @Override
            public String getPagingStatement(String sql, boolean readOnly) {
                return usePagingResultSets() ? myself.getPagingStatement(sql, readOnly) : sql;
            }

            @Override
            public StatementHandle prepareStatement(String sql) throws SQLException {
                return myself.prepareStatement(sql);
            }
        };
        expandRelativesLists = conf.getBoolean(CONF_EXPAND_RELATIVES_ID_LIST, DEFAULT_EXPAND_RELATIVES_ID_LIST);
        timestampGenerator = new UniqueTimestampGenerator();
        iteratorReaper = new CursorReaper(iterators, conf.getLong(CONF_ITERATOR_TIMEOUT, DEFAULT_ITERATOR_TIMEOUT));
        pruneRelativesOnGet = conf.getBoolean(CONF_PRUNE_RELATIVES_ON_GET, DEFAULT_PRUNE_RELATIVES_ON_GET);

        usePagingModel = usePagingResultSets();
        useLazyRelations = useLazyRelationLookups();

        if (usePagingModel) {
            pageSize = conf.getInt(CONF_PAGE_SIZE, DEFAULT_PAGE_SIZE);
            pageSizeUpdate = conf.getInt(CONF_PAGE_SIZE_UPDATE,
                                         conf.containsKey(CONF_PAGE_SIZE) ? pageSize : DEFAULT_PAGE_SIZE_UPDATE);
            log.debug("Using paging model for large result sets. Page size: " + pageSize);
        } else {
            log.debug("Using default model for large result sets");
            pageSize = -1;
            pageSizeUpdate = -1;
        }

        if (useLazyRelations) {
            log.debug("Using lazy relation resolution");
        } else {
            log.debug("Using direct relation resolution");
        }

        disabledRelationsTracking = new TreeSet<>();
        disabledRelationsTracking.addAll(conf.getStrings(CONF_DISABLE_RELATIONS_TRACKING, new ArrayList<String>()));
        if (disabledRelationsTracking.isEmpty()) {
            log.debug("Tracking relations on all bases");
        } else {
            log.info("Disabling relationships tracking on: " + Strings.join(disabledRelationsTracking, ", "));
        }
        basesWithStoredRelations = new TreeSet<String>();
        basesWithStoredRelations.addAll(conf.getStrings(CONF_BASES_WITH_STORED_RELATIONS, new ArrayList<String>()));

        defaultGetOptions = QueryOptions.getOptions(conf);
        useOptimizations = conf.getBoolean(CONF_USE_OPTIMIZATIONS, DEFAULT_USE_OPTIMIZATIONS);
        log.info("Constructed " + this);
    }

    /**
     * Update the configurations.
     *
     * @param configuration The new configurations.
     * @return A new configurations object.
     */
    private static Configuration updateConfiguration(Configuration configuration) {
        String location;
        try {
            location = configuration.getString(CONF_LOCATION);
        } catch (NullPointerException e) {
            log.debug("Could not locate key " + CONF_LOCATION + ". Skipping updating of location");
            return configuration;
        }
        try {
            File locationFile = new File(location);
            File newLocationFile = Resolver.getPersistentFile(locationFile);
            log.trace("locationFile: " + locationFile + ", persistent location file: " + newLocationFile);
            if (!locationFile.equals(newLocationFile)) {
                log.debug("Storing new location '" + newLocationFile + "' to property key " + CONF_LOCATION);
                configuration.set(CONF_LOCATION, newLocationFile.getPath());
            } else {
                log.debug(CONF_LOCATION + " is an absolute path (" + locationFile + "). No changes will be done");
            }
        } catch (Exception e) {
            log.debug("Could not transmute key '" + CONF_LOCATION + "' in configuration", e);
        }
        return configuration;
    }

    /**
     * The initializer connects to the database and prepares SQL statements.
     * You <i>must</i> call this in all constructors of sub classes of
     * DatabaseStorage.
     *
     * @param conf The setup for the database.
     * @throws ConfigurationException if the initialization could not finish.
     * @throws IOException            on failing to connect to the database
     */
    protected void init(Configuration conf) throws IOException {
        log.trace("init called");
        connectToDatabase(conf);

        iteratorReaper.runInThread();

        log.debug("Initialization finished");
    }

    /**
     * Connect to the relevant database and establish a connection, so that
     * calls to {@link #getConnection()} can be performed. Depending on the
     * configuration, this might involve creating a table in the database and
     * initializing that to Summa-use.
     *
     * @param configuration setup for the database.
     * @throws IOException if a connection could not be established to the
     *                     database.
     */
    protected abstract void connectToDatabase(Configuration configuration) throws IOException;

    /**
     * Look up a connection from a connection pool. The returned connection
     * <i>must</i> be closed by the caller. This should typically be done in a
     * <code>finally</code> clause.
     * <p/>
     * Unclosed connections will lead to connection leaking which can end up
     * locking up the entire storage.
     *
     * @return a connection to a SQL-compatible database. The returned
     *         connection <i>must</i> be closed by the caller to avoid
     *         connection leaking.
     */
    protected abstract Connection getConnection();

    /**
     * Get an auto committing, write enabled connection.
     *
     * @return a pooled connection.
     * @throws SQLException if unable to set write access or auto commit.
     */
    public Connection getDefaultConnection() throws SQLException {
        Connection conn = getConnection();
        return conn;
    }

    /**
     * Get a new pooled connection via {@link #getConnection()} and optimize it
     * for transactional mode, meaning that auto commit is off and the
     * transaction isolation level is set to the fastet one making sense.
     *
     * @return database connection from connection pool.
     */
    public Connection getTransactionalConnection()   {

        try{
            Connection conn = getConnection();

            return conn;
        }
        catch(Exception e){
            System.out.println(e);
            throw new RuntimeException("Error with database connection properties", e);
        }

    }

    /**
     * Set up a prepared statement and return an opaque handle to it.
     * <p/>
     * A copy of the {@link PreparedStatement} can be retrieved by calling
     * {@link #getManagedStatement}. PreparedStatements <i>must</i> be
     * closed by invoking the {@link PreparedStatement#close} method when the
     * client is done using them. This should typically be done in a
     * <code>finally</code> clause to make sure statements
     * (and thus connections) are not leaked.
     * <p/>
     * Connection leaking can end up locking up the entire storage process.
     *
     * @param sql SQL statement for the prepared statement.
     * @return a handle that can be used to retrieve a PreparedStatement.
     * @throws SQLException from implementation.
     */
    protected abstract StatementHandle prepareStatement(String sql) throws SQLException;

    /**
     * Look up a prepared statement given a handle as constructed by
     * {@link #prepareStatement(String)}.
     * <p/>
     * Note that PreparedStatements <i>must</i> be
     * closed by invoking the {@link PreparedStatement#close} method when the
     * client is done using them. This should typically be done in a
     * <code>finally</code> clause to make sure statements
     * (and thus connections) are not leaked.
     * <p/>
     * Connection leaking can end up locking up the entire storage process.
     *
     * @param handle statement handle as obtained when calling
     *               {@link #prepareStatement(String)}.
     * @return a prepared statement that <i>must</i> be closed by the caller.
     * @throws SQLException from implementation.
     */
    protected abstract PreparedStatement getManagedStatement(StatementHandle handle) throws SQLException;

    /**
     * Not all database backends return real
     * {@link SQLIntegrityConstraintViolationException}s when they should, but
     * use custom vendor-specific error codes that can be retrieved by calling
     * {@link java.sql.SQLException#getErrorCode()}.
     * <p/>
     * The default implementation of this method simply checks if {@code e}
     * is {@code instanceof SQLIntegrityConstraintViolationException}.
     *
     * @param e the sql exception to inspect the real cause for
     * @return whether or not {@code e} was due to an integrity constraint
     *         violation
     */
    protected boolean isIntegrityConstraintViolation(SQLException e) {
        return e instanceof SQLIntegrityConstraintViolationException;
    }

    /**
     * Return whether the database must do manual paging over the result sets
     * or the cursoring supplied by the database is sufficient. DatabaseStorage
     * will invoke this method one once during its initialization and never
     * again.
     * <p/>
     * If this method returns {@code true} the
     * {@link #getPagingStatement(String, boolean)} will be called with the SQL
     * statements the DatabaseStorage wants paging versions of.
     * <p/>
     * The default implementation of this method returns {@code false}
     *
     * @return whether or not to use a manual client side paging model for
     *         retrieval of large result sets
     */
    protected boolean usePagingResultSets() {
        return true;
    }

    /**
     * Return whether or not Record relations should be resolved lazily
     * (in other words "on-the-fly") or if they should be resolved "directly"
     * using an SQL JOIN.
     * <p/>
     * Some databases perform badly when doing JOINs on tables with a lot of
     * rows. This appear especially to be the case for Java embedded databases
     * like Derby and H2.
     * <p/>
     * The default implementation of this method returns {@code false}, meaning
     * that JOINs should be used.
     *
     * @return {@code false} if JOINs should be used to fetch records and their
     *         relation in one go, or {@code true} if relations should be looked
     *         up in a separate SQL call
     */
    protected boolean useLazyRelationLookups() {
        return false;
    }

    /**
     * If {@link #usePagingModel} is true, ' LIMIT xxx' will be appended to the given sql. The value of xxx is
     * {@link #pageSize} if readOnly is true, else {@link #pageSizeUpdate}.
     * @param sql      SQL expression.
     * @param readOnly if true, the expression is expected to be read only.
     * @return the statement modified with LIMIT.
     */
    protected String getPagingStatement(String sql, boolean readOnly) {
        return usePagingModel ? getPagingStatement(sql, readOnly ? pageSize : pageSizeUpdate) : sql;
    }

    /**
     * Return an altered version of the input {@code sql} which adds one extra
     * parameter, <i>to the end of the SQL statement</i>, which is used
     * to limit the number of rows returned.
     * <p/>
     * For many databases a legal implementation of this method would simply
     * return:<br/>
     * <pre>
     *   sql + " LIMIT " + getPageSize()
     * </pre>
     *
     * @param sql Input SQL statement on which to append another parameter that
     *            limits the number of rows returned
     * @return A modified version of {@code sql} that adds a parameter
     *         to the statement that will limit the number of rows returned
     */
    protected String getPagingStatement(String sql, int limit) {
        return sql + " LIMIT " + limit;
    }

    /**
     * Return the unique time stamp generator used by this instance.
     *
     * @return The unique time stamp generator.
     */
    public UniqueTimestampGenerator getTimestampGenerator() {
        return timestampGenerator;
    }

    /**
     * Return the used page size.
     *
     * @return The used page size.
     */
    public int getPageSize() {
        return pageSize;
    }

    public int getPageSizeUpdate() {
        return pageSizeUpdate;
    }

    /**
     * Prepare relevant SQL statements for later use.
     *
     * @throws SQLException if the syntax of a statement was wrong or the
     *                      connection to the database was faulty.
     */
    private void prepareStatements() throws SQLException {
        log.debug("Preparing SQL statements");

        /*        String allCellsRelationsCols;
        if (useLazyRelations) {
            // Always select empty cells for relations and look them up later
            allCellsRelationsCols = "'', ''";
        } else {
            allCellsRelationsCols = RELATIONS + "." + PARENT_ID_COLUMN + "," + RELATIONS + "." + CHILD_ID_COLUMN;
        }
         */
        /*
        allColumns = RECORDS + "." + ID_COLUMN + ","
                     + RECORDS + "." + BASE_COLUMN + ","
                     + RECORDS + "." + DELETED_COLUMN + ","
                     + RECORDS + "." + INDEXABLE_COLUMN + ","
                     + RECORDS + "." + HAS_RELATIONS_COLUMN + ", "
                     + RECORDS + "." + DATA_COLUMN + ","
                     + RECORDS + "." + CTIME_COLUMN + ","
                     + RECORDS + "." + MTIME_COLUMN + ","
                     + RECORDS + "." + META_COLUMN + ","
                     + allCellsRelationsCols;

      //used for ingest. (recordsModifiedAfter). All fields must still be selected because of scanrecord method is
      general but no need here.. sigh
        allColumnsButDataAndMeta = RECORDS + "." + ID_COLUMN + ","  
        	      + RECORDS + "." + BASE_COLUMN + ","
                  + RECORDS + "." + DELETED_COLUMN + ","
                  + RECORDS + "." + INDEXABLE_COLUMN + ","
                  + RECORDS + "." + HAS_RELATIONS_COLUMN + ", "
                  + "'' AS "+  DATA_COLUMN + ","
                  + RECORDS + "." + CTIME_COLUMN + ","
                  + RECORDS + "." + MTIME_COLUMN + ","
                  + "'' AS "+  META_COLUMN +" ," 
                  + allCellsRelationsCols;        

        String relationsClause = RECORDS + "." + ID_COLUMN + "="
                                + RELATIONS + "." + PARENT_ID_COLUMN
                                + " OR " + RECORDS + "." + ID_COLUMN + "="
                                + RELATIONS + "." + CHILD_ID_COLUMN;
         */
        /* modifiedAfter */
        // We can order by mtime only because the generated mtimes are unique
        /*        String modifiedAfterQuery;
        String modifiedAfterQueryNoData;
        if (useLazyRelations) {
            modifiedAfterQuery = "SELECT " + allColumns
                                 + " FROM " + RECORDS
                                 + " WHERE " + BASE_COLUMN + "=?"
                                 + " AND " + MTIME_COLUMN + ">?"
                                 + " ORDER BY " + MTIME_COLUMN;

            modifiedAfterQueryNoData = "SELECT " + allColumnsButDataAndMeta
                    + " FROM " + RECORDS
                    + " WHERE " + BASE_COLUMN + "=?"
                    + " AND " + MTIME_COLUMN + ">?"
                    + " ORDER BY " + MTIME_COLUMN;


        } else {
            modifiedAfterQuery = "SELECT " + allColumns
                                 + " FROM " + RECORDS
                                 + " LEFT JOIN " + RELATIONS
                                 + " ON " + relationsClause
                                 + " WHERE " + BASE_COLUMN + "=?"
                                 + " AND " + MTIME_COLUMN + ">?"
                                 + " ORDER BY " + MTIME_COLUMN;

            modifiedAfterQueryNoData = "SELECT " + allColumnsButDataAndMeta
                    + " FROM " + RECORDS
                    + " LEFT JOIN " + RELATIONS
                    + " ON " + relationsClause
                    + " WHERE " + BASE_COLUMN + "=?"
                    + " AND " + MTIME_COLUMN + ">?"
                    + " ORDER BY " + MTIME_COLUMN;            

        }*/
        //        if (usePagingModel) {
        //            modifiedAfterQuery = getPagingStatement(modifiedAfterQuery);
        //            modifiedAfterQueryNoData = getPagingStatement(modifiedAfterQueryNoData,5000);
        //        }
        /*        log.debug("Preparing query getModifiedAfter with '"  + modifiedAfterQuery + "'");
        stmtGetModifiedAfter = prepareStatement(modifiedAfterQuery);
        stmtGetModifiedAfterNoData = prepareStatement(modifiedAfterQueryNoData);

        log.debug("getModifiedAfter handle: " + stmtGetModifiedAfter);
        log.debug("getModifiedAfterNoData handle: " + stmtGetModifiedAfterNoData);
         */
        /* modifiedAfterAll */
        // We can order by mtime only because the generated mtimes are unique
        /*        String modifiedAfterAllQuery;
        String modifiedAfterAllQueryNoData;
        if (useLazyRelations){
            modifiedAfterAllQuery = "SELECT " + allColumns
                                    + " FROM " + RECORDS
                                    + " WHERE " + MTIME_COLUMN + ">?"
                                    + " ORDER BY " + MTIME_COLUMN;

            modifiedAfterAllQueryNoData = "SELECT " + allColumnsButDataAndMeta
                    + " FROM " + RECORDS
                    + " WHERE " + MTIME_COLUMN + ">?"
                    + " ORDER BY " + MTIME_COLUMN;

        } else {
            modifiedAfterAllQuery = "SELECT " + allColumns
                                    + " FROM " + RECORDS
                                    + " LEFT JOIN " + RELATIONS
                                    + " ON " + relationsClause
                                    + " WHERE " + MTIME_COLUMN + ">?"
                                    + " ORDER BY " + MTIME_COLUMN;

            modifiedAfterAllQueryNoData = "SELECT " + allColumnsButDataAndMeta
                    + " FROM " + RECORDS
                    + " LEFT JOIN " + RELATIONS
                    + " ON " + relationsClause
                    + " WHERE " + MTIME_COLUMN + ">?"
                    + " ORDER BY " + MTIME_COLUMN;

        }
        if (usePagingModel) {
            modifiedAfterAllQuery = getPagingStatement(modifiedAfterAllQuery);
            modifiedAfterAllQueryNoData = getPagingStatement(modifiedAfterAllQueryNoData,5000);
        }

        log.debug("Preparing query getModifiedAfterAll with '"  + modifiedAfterAllQuery + "'");
        log.debug("Preparing query getModifiedAfterAllNoData with '"  + modifiedAfterAllQueryNoData + "'");
        stmtGetModifiedAfterAll = prepareStatement(modifiedAfterAllQuery);
        stmtGetModifiedAfterAllNoData = prepareStatement(modifiedAfterAllQueryNoData);
        log.debug("getModifiedAfterAll handle: " + stmtGetModifiedAfterAll);
        log.debug("getModifiedAfterAllNoData handle: " + stmtGetModifiedAfterAllNoData);
         */
        /* getRecord */
        // getRecordsQuery uses JOINs no matter if useLazyRelations is set.
        // Fetching single records using a LEFT JOIN is generally not a problem
        /*        String getRecordQuery = "SELECT " + allColumns
                                + " FROM " + RECORDS
                                + " LEFT JOIN " + RELATIONS
                                + " ON " + relationsClause
                                + " WHERE " + RECORDS + "." + ID_COLUMN + "=?";
        log.debug("Preparing query getRecord with '" + getRecordQuery + "'");
        stmtGetRecord = prepareStatement(getRecordQuery);
        log.debug("getRecord handle: " + stmtGetRecord);

         */
        /*        String  getRecordQueryTemp = "SELECT " + allColumns
                + " FROM " + RECORDS
                + " WHERE " + RECORDS + "." + ID_COLUMN + "=?";
                  log.debug("Preparing query getRecordTemp with '" + getRecordQueryTemp + "'");
        stmtGetRecordTemp = prepareStatement(getRecordQueryTemp);
             log.debug("getRecord handle: " + stmtGetRecordTemp);
         */

        /*         String getParentIDQuery=
        		  "SELECT  SUMMA_RECORDS.id, SUMMA_RELATIONS.parentID from" +
                  " SUMMA_RECORDS join SUMMA_RELATIONS on SUMMA_RECORDS.id = SUMMA_RELATIONS.childid" +
           		  " where SUMMA_RECORDS.id = ?";
         stmtGetParentIDQuery=prepareStatement(getParentIDQuery);
         */

        /*
         FIXME: We might want a prepared statement to fetch multiple records in
                one go. However it seems to be inefficient to use prepared
                statements with IN clauses in them . See fx:
                http://forum.springframework.org/archive/index.php/t-16001.html

        String getRecordsQuery = "SELECT " + allColumns
                                + " FROM " + RECORDS
                                + " WHERE " + ID_COLUMN + " IN ?";
        log.debug("Preparing query recordsQuery with '"
                  + getRecordsQuery + "'");
        stmtGetRecords = prepareStatement(getRecordsQuery);
         */

        /* deleteRecord */
        /*        String deleteRecordQuery = "UPDATE " + RECORDS
                                   + " SET " + MTIME_COLUMN + "=?, "
                                   + DELETED_COLUMN + "=?"
                                   + " WHERE " + ID_COLUMN + "=?";
        log.debug("Preparing query deleteRecord with '"
                  + deleteRecordQuery + "'");
        stmtDeleteRecord = prepareStatement(deleteRecordQuery);
        log.debug("deleteRecord handle: " + stmtDeleteRecord);
         */
        /* createRecord */
        /*        String createRecordQuery = "INSERT INTO " + RECORDS
                                + " (" + ID_COLUMN + ", "
                                       + BASE_COLUMN + ", "
                                       + DELETED_COLUMN + ", "
                                       + INDEXABLE_COLUMN + ", "
                                       + HAS_RELATIONS_COLUMN + ", "
                                       + CTIME_COLUMN + ", "
                                       + MTIME_COLUMN + ", "
                                       + DATA_COLUMN + ", "
                                       + META_COLUMN
                                       + ") VALUES (?,?,?,?,?,?,?,?,?)";
        log.debug("Preparing query createRecord with '" + createRecordQuery
                  + "'");
        stmtCreateRecord = prepareStatement(createRecordQuery);
        log.debug("createRecord handle: " + stmtCreateRecord);
         */
        /* updateRecord */
        /*        String updateRecordQuery = "UPDATE " + RECORDS + " SET "
                                   + BASE_COLUMN + "=?, "
                                   + DELETED_COLUMN + "=?, "
                                   + INDEXABLE_COLUMN + "=?, "
                                   + HAS_RELATIONS_COLUMN + "=?, "
                                   + MTIME_COLUMN + "=?, "
                                   + DATA_COLUMN + "=?, "
                                   + META_COLUMN + "=? "
                                   + "WHERE " + ID_COLUMN + "=?";
        log.debug("Preparing query updateRecord with '" + updateRecordQuery
                  + "'");
        stmtUpdateRecord = prepareStatement(updateRecordQuery);
        log.debug("updateRecord handle: " + stmtUpdateRecord);
         */
        /* touchRecord */
        /*    String touchRecordQuery = "UPDATE " + RECORDS + " SET "
                                   + MTIME_COLUMN + "=? "
                                   + "WHERE " + ID_COLUMN + "=?";
        log.debug("Preparing query touchRecord with '" + touchRecordQuery
                  + "'");
        stmtTouchRecord = prepareStatement(touchRecordQuery);
         */
        /* touchParents */
        /*        String touchParentsQuery = "UPDATE " + RECORDS
                                + " SET " + MTIME_COLUMN + "=? "
                                + " WHERE " + ID_COLUMN + " IN ("
                                + " SELECT " + PARENT_ID_COLUMN
                                + " FROM " + RELATIONS
                                + " WHERE " + CHILD_ID_COLUMN + "=? )";
        log.debug("Preparing query touchParents with '" + touchParentsQuery
                  + "'");
        stmtTouchParents = prepareStatement(touchParentsQuery);
        log.debug("touchParents handle: " + stmtTouchParents);
         */
        /* getChildren */
        /*        String getChildrenQuery = "SELECT " + allColumns
                                + " FROM " + RELATIONS
                                + " JOIN " + RECORDS
                                + " ON " + RECORDS + "." + ID_COLUMN + "="
                                          + RELATIONS + "." + CHILD_ID_COLUMN
                                + " WHERE " + RELATIONS + "."
                                            + PARENT_ID_COLUMN + "=?"
                                + " ORDER BY " + RECORDS + "." + ID_COLUMN;
        log.debug("Preparing query getChildren with '" + getChildrenQuery
                  + "'");
        stmtGetChildren = prepareStatement(getChildrenQuery);
        log.debug("getChildren handle: " + stmtGetChildren);
         */
        /* getParents */
        /*        String getParentsQuery = "SELECT " + allColumns
                                + " FROM " + RELATIONS
                                + " JOIN " + RECORDS
                                + " ON " + RECORDS + "." + ID_COLUMN + "="
                                          + RELATIONS + "." + PARENT_ID_COLUMN
                                + " WHERE " + RELATIONS + "."
                                            + CHILD_ID_COLUMN + "=?"
                                + " ORDER BY " + RECORDS + "." + ID_COLUMN;
        log.debug("Preparing query getParents with '" + getParentsQuery
                  + "'");
        stmtGetParents = prepareStatement(getParentsQuery);
        log.debug("getParents handle: " + stmtGetParents);
         */
        /* getRelatedIds */
        // The obvious thing to do here was to use an OR instead of the UNION,
        // however some query optimizers have problems using the right indexes
        // when ORing (H2 for instance). Using a UNION is easier for the
        // optimizer
        /*      String getRelatedIdsQuery = "SELECT " + PARENT_ID_COLUMN
                                    + ", " + CHILD_ID_COLUMN
                                    + " FROM " + RELATIONS
                                    + " WHERE " + PARENT_ID_COLUMN + "=?"
                                    + " UNION "
                                    + "SELECT " + PARENT_ID_COLUMN
                                    + ", " + CHILD_ID_COLUMN
                                    + " FROM " + RELATIONS
                                    + " WHERE " + CHILD_ID_COLUMN + "=?";

        log.debug("Preparing getRelatedIds with '" + getRelatedIdsQuery + "'");
        stmtGetRelatedIds = prepareStatement(getRelatedIdsQuery);
        log.debug("getRelatedIds handle: " + stmtGetRelatedIds);
         */
        /* markHasRelations */
        /*        String markHasRelationsQuery = "UPDATE " + RECORDS
                                    + " SET " + HAS_RELATIONS_COLUMN + "=1 "
                                    + " WHERE " + ID_COLUMN + "=?";
        log.debug("Preparing markHasRelations with '"
                  + markHasRelationsQuery + "'");
        stmtMarkHasRelations = prepareStatement(markHasRelationsQuery);
        log.debug("markHasRelations handle: " + stmtMarkHasRelations);
         */
        /* createRelation */
        /*      String createRelation = "INSERT INTO " + RELATIONS
                                + " (" + PARENT_ID_COLUMN + ","
                                       + CHILD_ID_COLUMN
                                       + ") VALUES (?,?)";
        log.debug("Preparing query createRelation with '"
                + createRelation + "'");
        stmtCreateRelation = prepareStatement(createRelation);
        log.debug("createRelation handle: " + stmtCreateRelation);

        // Updated mtime per base
        String updateMtimeForBase = "UPDATE " + BASE_STATISTICS
            + " SET " + MTIME_COLUMN + " = ? WHERE " + BASE_COLUMN
            + " = ?"; 
        log.debug("Preparing query updateMtimeforBase with '"
                  + updateMtimeForBase + "'");
        stmtUpdateMtimForBase = prepareStatement(updateMtimeForBase);
        log.debug("updateMtimeForBase handle: " + stmtUpdateMtimForBase);

        // Insert new base statistic row
        String insertBaseStats = "INSERT INTO " + BASE_STATISTICS
            + " (" + BASE_COLUMN + ", " + MTIME_COLUMN + ", "
            + DELETE_INDEXABLES_COLUMN + ", " + NON_DELETED_INDEXABLES_COLUMN
            + ", " + DELETED_NON_INDEXABLES_COLUMN + ", "
            + NON_DELETED_NON_INDEXABLES_COLUMN + ", " + VALID_COLUMN
            + ") VALUES (?, ?, 0, 0, 0, 0, 0)";
        log.debug("Preparing query insertBaseStats with '" + insertBaseStats
                  + "'");
        stmtInsertBaseStats = prepareStatement(insertBaseStats);
        log.debug("insertBaseStats handle: " + stmtInsertBaseStats);

        // Set base statistic row invalid
        String setBaseStatsInvalid = "UPDATE " + BASE_STATISTICS + " SET "
            + VALID_COLUMN + " = 0 WHERE " + BASE_COLUMN + " = ?";
        log.debug("Preparing query setBaseStatsInvalid with '"
                + setBaseStatsInvalid + "'");
        stmtSetBaseStatsInvalid = prepareStatement(setBaseStatsInvalid);
        log.debug("setBaseStatsInvalid handle: " + stmtSetBaseStatsInvalid);

        // SQL for getting last modification time
        String getLastModificationTime = "SELECT " + MTIME_COLUMN + " FROM "
            + BASE_STATISTICS + " WHERE " + BASE_COLUMN + " = ?";
        log.debug("Preparing query getLastModificationTime with '"
                + getLastModificationTime + "'");
        stmtGetLastModificationTime = prepareStatement(getLastModificationTime);
        log.debug("getLastModificationTime handle: "
                  + stmtGetLastModificationTime);

        // SQL for updating full base statistic
        String updateFullBaseStats = "UPDATE " + BASE_STATISTICS + " SET "
            + DELETE_INDEXABLES_COLUMN + " = ?, "
            + NON_DELETED_INDEXABLES_COLUMN + " = ?, "
            + DELETED_NON_INDEXABLES_COLUMN + " = ?, "
            + NON_DELETED_NON_INDEXABLES_COLUMN + " = ?, "
            + VALID_COLUMN + " = 1 "
            +" WHERE " + BASE_COLUMN + " = ?";
        log.debug("Preparing query getLastModificationTime with '"
                + updateFullBaseStats + "'");
        stmtUpdateFullBaseStats = prepareStatement(updateFullBaseStats);
        log.debug("updateFullBaseStats handle: " + stmtUpdateFullBaseStats);

        // SQL for updating full base statistic
        String insertFullBaseStats = "INSERT INTO " + BASE_STATISTICS
        + " (" + BASE_COLUMN + ", " + MTIME_COLUMN + ", "
        + DELETE_INDEXABLES_COLUMN + ", " + NON_DELETED_INDEXABLES_COLUMN
        + ", " + DELETED_NON_INDEXABLES_COLUMN + ", "
        + NON_DELETED_NON_INDEXABLES_COLUMN + ", " + VALID_COLUMN
        + ") VALUES (?, ?, ?, ?, ?, ?, 1)";
        log.debug("Preparing query getLastModificationTime with '"
                  + insertFullBaseStats + "'");
        stmtInsertFullBaseStats = prepareStatement(insertFullBaseStats);
        log.debug("insertFullBaseStats handle: " + stmtInsertFullBaseStats);
         */
        log.debug("Finished preparing SQL statements");
    }

    /**
     * Close a statement and log any errors in the process. If this method
     * is passed {@code null} it will return silently.
     *
     * @param stmt the statement to close
     */
    private void closeStatement(Statement stmt) {
        if (stmt == null) {
            return;
        }

        try {
            stmt.close();
        } catch (SQLException e) {
            log.warn("Failed to close statement " + stmt, e);
        }
    }

    /**
     * Close a connection and log any errors in the process. If this method
     * is passed {@code null} it will return silently.
     *
     * @param conn the connection to close
     */
    public void closeConnection(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.close();
        } catch (SQLException e) {
            log.warn("Failed to close connection " + conn, e);
        }
    }

    /**
     * Data column is NOT loaded
     * <p/>
     * LIMIT BY 5000 on resultset
     * <p/>
     * Get a {@link ResultSetCursor} over all records with an {@code mtime}
     * timestamp strictly bigger than {@code mtimeTimestamp}. The provided
     * timestamp must be in the format as returned by a
     * {@link dk.statsbiblioteket.summa.common.util.UniqueTimestampGenerator},
     * ie. it is <i>not</i> a normal system time in milliseconds.
     * <p/>
     * The returned iteration key, can be used for later iteration over records.
     *
     * @param mtime   a timestamp as returned by a {@link UniqueTimestampGenerator}.
     * @param base    the base which the retrieved records must belong to.
     *                if the base is null, all bases matches.
     * @param options any {@link QueryOptions} the query should match.
     * @return a iteration key, usable for calls to {@link #next}.
     * @throws IOException if prepared SQL statement is invalid.
     */
    @Override
    public synchronized long getRecordsModifiedAfter(long mtime, String base, QueryOptions options) throws IOException {
        log.debug("getRecordsModifiedAfter(" + mtime + ", '" + base + "', " +
                  (options != null ? options : "defaultOptions: " + defaultGetOptions) +
                  ").");
        if (options == null) {
            options = defaultGetOptions;
        }
        timingGetRecordsModifiedAfter.start();
        if (!hasMTime(options)) {
            throw new IllegalArgumentException(
                    "MTIME must be part of QueryOptions-ATTRIBUTES when requesting a MTIME-based iterator. "
                    + "Defined attributes were " + Strings.join(options.getAttributes(), ", "));
        }

        // Convert time to the internal binary format used by DatabaseStorage
        long mtimeTimestamp = timestampGenerator.baseTimestamp(mtime);

        Cursor cursor;
        if (useOptimizations && options != null && !options.hasDeletedFilter() && !options.hasIndexableFilter() &&
            options.childDepth() == 0 && options.parentHeight() == 1) {
            log.info(String.format(Locale.ROOT,
                    "getRecordsModifiedAfter(mtime=%d, base=%s, options=%s): " +
                    "Using optimized record iterator with optimization=%s",
                    mtime, base, options, OPTIMIZATION.singleParent));
            try {
                cursor = new ChunkedCursor(this, OPTIMIZATION.singleParent, base, mtimeTimestamp, options);
                if (!cursor.hasNext()) {
                    timingGetRecordsModifiedAfter.stop();
                    return EMPTY_ITERATOR_KEY;
                }

            } catch (Exception e) {
                throw new IOException("Unable to construct optimized cursor for base " + base, e);
            }
        } else {
            log.info(String.format(Locale.ROOT,
                    "getRecordsModifiedAfter(mtime=%d, base=%s, options=%s): " +
                    "No optimization available (useOptimizations=%b), creating standard Record iterator",
                    mtime, base, options, useOptimizations));
            cursor = getRecordsModifiedAfterCursor(mtimeTimestamp, base, options);
            //        Cursor iter = getRecordsModifiedAfterCursor(mtimeTimestamp, base, options);

            if (cursor == null || !cursor.hasNext()) {
                timingGetRecordsModifiedAfter.stop();
                return EMPTY_ITERATOR_KEY;
            }

            if (usePagingModel) {
                cursor = new PagingCursor(this, (ResultSetCursor) cursor);
            }
        }
        timingGetRecordsModifiedAfter.stop();
        return registerCursor(cursor);
    }

    /**
     * Same as getRecordsModifiedAfter, but does load data column.
     * LIMIT BY 500 on resultset
     *
     *
     * Get a {@link ResultSetCursor} over all records with an {@code mtime}
     * timestamp strictly bigger than {@code mtimeTimestamp}. The provided
     * timestamp must be in the format as returned by a
     * {@link dk.statsbiblioteket.summa.common.util.UniqueTimestampGenerator},
     * ie. it is <i>not</i> a normal system time in milliseconds.
     * <p/>
     * The returned iteration key, can be used for later iteration over records.
     *
     * @param mtime a timestamp as returned by a
     *                       {@link UniqueTimestampGenerator}.
     * @param base the base which the retrieved records must belong to.
     * @param options any {@link QueryOptions} the query should match.
     * @return a iteration key.
     * @throws IOException if prepared SQL statement is invalid.
     */
    /*
    public synchronized long getRecordsModifiedAfterLoadData(
             long mtime, String base, QueryOptions options) throws IOException {

       log.debug("DatabaseStorage.getRecordsModifiedAfterLoadData(" + mtime + ", '" + base + "', " + options + ").");
        // Convert time to the internal binary format used by DatabaseStorage
        long mtimeTimestamp = timestampGenerator.baseTimestamp(mtime);

        Cursor iter = getRecordsModifiedAfterCursorLoadData(mtimeTimestamp, base, options);

        if (iter == null) {
            return EMPTY_ITERATOR_KEY;
        }

        if (usePagingModel) {
            iter = new PagingCursor(this, (ResultSetCursor) iter,true);
        }
        return registerCursor(iter);
    }
     */

    /**
     * Get a {@link ResultSetCursor} over all records with an {@code mtime}
     * timestamp strictly bigger than {@code mtimeTimestamp}. The provided
     * timestamp must be in the format as returned by a
     * {@link dk.statsbiblioteket.summa.common.util.UniqueTimestampGenerator},
     * ie. it is <i>not</i> a normal system time in milliseconds.
     * <p/>
     * The returned ResultSetCursor <i>must</i> be closed by the caller to
     * avoid leaking connections and locking up the storage.
     *
     * @param mtime   a timestamp as returned by a
     *                {@link UniqueTimestampGenerator}.
     * @param base    the base which the retrieved records must belong to.
     *                if the base is null, the results from all bases is retrieved.
     * @param options any {@link QueryOptions} the query should match.
     * @return a {@link ResultSetCursor} that <i>must</i> be closed by the
     *         caller to avoid leaking connections and locking up the storage.
     * @throws IOException if prepared SQL statement is invalid.
     */
    public Cursor getRecordsModifiedAfterCursor(long mtime, String base, QueryOptions options) throws IOException {

        StatementHandle handle;
        PreparedStatement stmt;
        log.debug("getRecordsModifiedAfterCursor(" + mtime + ", '" + base + "', " +
                  (options != null ? options : "defaultGetOptions: " + defaultGetOptions) +
                  ").");
        if (options == null) {
            options = defaultGetOptions;
        }

        if (!hasMTime(options)) {
            throw new IllegalArgumentException(
                    "MTIME must be part of QueryOptions-ATTRIBUTES when requesting a MTIME-based iterator. "
                    + "Defined attributes were " + Strings.join(options.getAttributes(), ", "));
        }

        try {
            handle = base == null ?
                    statementHandler.getGetModifiedAfterAll(options) :
                    statementHandler.getGetModifiedAfter(options);
                    /*            if (base == null) {
                statement = stmtGetModifiedAfterAllNoData.getSql();
                stmt = getManagedStatement(stmtGetModifiedAfterAllNoData);
            } else {
                statement = stmtGetModifiedAfterNoData.getSql();
                stmt = getManagedStatement(stmtGetModifiedAfterNoData);
            }*/

            log.debug("getRecordsModifiedAfterCursor statement: " + handle.getSql());
            stmt = getManagedStatement(handle);
        } catch (SQLException e) {
            throw new IOException("Failed to manage prepared statement for base '" + base + "'. SQL: 'N/A'", e);
        }
        // doGetRecordsModifiedAfter creates and iterator and 'stmt' will
        // be closed together with that iterator
        return doGetRecordsModifiedAfterCursor(mtime, base, options, stmt);
    }

    protected boolean hasMTime(QueryOptions options) {
        if (options == null || options.getAttributes() == null) {
            return true; // null means everything
        }
        for (QueryOptions.ATTRIBUTES attribute : options.getAttributes()) {
            if (attribute == QueryOptions.ATTRIBUTES.MODIFICATIONTIME) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasAttribute(QueryOptions options, QueryOptions.ATTRIBUTES wanted) {
        if (options == null || options.getAttributes() == null) {
            return true; // null means everything
        }
        for (QueryOptions.ATTRIBUTES attribute : options.getAttributes()) {
            if (attribute == wanted) {
                return true;
            }
        }
        return false;
    }


    /**
     * Same as getRecordsModifiedAfterCursor, except data column is  loaded.
     *  LIMIT BY 500 on resultset 
     *
     *
     * Get a {@link ResultSetCursor} over all records with an {@code mtime}
     * timestamp strictly bigger than {@code mtimeTimestamp}. The provided
     * timestamp must be in the format as returned by a
     * {@link dk.statsbiblioteket.summa.common.util.UniqueTimestampGenerator},
     * ie. it is <i>not</i> a normal system time in milliseconds.
     * <p/>
     * The returned ResultSetCursor <i>must</i> be closed by the caller to
     * avoid leaking connections and locking up the storage.
     *
     * @param mtime a timestamp as returned by a
     *                       {@link UniqueTimestampGenerator}.
     * @param base the base which the retrieved records must belong to.
     * @param options any {@link QueryOptions} the query should match.
     * @return a {@link ResultSetCursor} that <i>must</i> be closed by the
     *         caller to avoid leaking connections and locking up the storage.
     * @throws IOException if prepared SQL statement is invalid.
     */
    /*    public ResultSetCursor getRecordsModifiedAfterCursorLoadData(long mtime,
                         String base, QueryOptions options) throws IOException {
        PreparedStatement stmt;
        String statement = "";
        log.debug("DatabaseStorage.getRecordsModifiedAfterCursorLoadData(" + mtime + ", '" + base + "', "
                  + options + ").");
        return getRecordsModifiedAfterCursor(mtime, base, options);*/
    /*        try {
            if (base == null) {
                statement = stmtGetModifiedAfterAll.getSql();
                stmt = getManagedStatement(stmtGetModifiedAfterAll);
            } else {
                statement = stmtGetModifiedAfter.getSql();
                stmt = getManagedStatement(stmtGetModifiedAfter);
            }

        log.debug("getRecordsModifiedAfterCursorLoadData statement:"+statement );

        } catch (SQLException e) {
            throw new IOException("Failed to get prepared statement "
                                  + statement + ": "
                                  + e.getMessage(), e);
        }
        // doGetRecordsModifiedAfter creates and iterator and 'stmt' will
        // be closed together with that iterator
        return doGetRecordsModifiedAfterCursor(mtime, base, options, stmt);
     */
    //}


    /**
     * Helper method dispatched by {@link #getRecordsModifiedAfterCursor}
     * <p/>
     * This method is responsible for closing 'stmt'. This is handled implicitly
     * since 'stmt' is added to a ResultIterator and it will be closed when
     * the iterator is closed.
     *
     * @param mtimeTimestamp a timestamp as returned by a
     *                       {@link UniqueTimestampGenerator}.
     * @param base           the base which the retrieved records must belong to.
     * @param options        any {@link QueryOptions} the query should match.
     * @param stmt           prepared statement.
     * @return {@code null} if there are no records updated in {@code base}
     *         after {@code time}. Otherwise a ResultIterator ready for fetching
     *         records.
     * @throws IOException if error experienced when preparing connection for
     *                     cursoring.
     */
    private synchronized ResultSetCursor doGetRecordsModifiedAfterCursor(
            long mtimeTimestamp, String base, QueryOptions options, PreparedStatement stmt) throws IOException {
        log.debug("doGetRecordsModifiedAfterCursor('" + mtimeTimestamp + "', " + base + ") entered");
        timingDoGetRecordsModifiedAfterCursor.start();

        if (!hasMTime(options)) {
            throw new IllegalArgumentException(
                    "MTIME must be part of QueryOptions-ATTRIBUTES when requesting a MTIME-based iterator. "
                    + "Defined attributes were " + Strings.join(options.getAttributes(), ", "));
        }

        // Set the statement up for fetching of large result sets, see fx.
        // http://jdbc.postgresql.org/documentation/83/query.html#query-with-cursor
        // This prevents an OOM for backends like Postgres
        try {
            stmt.setFetchDirection(ResultSet.FETCH_FORWARD);

            assignFetchSize(stmt, true);
        } catch (SQLException e) {
            timingDoGetRecordsModifiedAfterCursor.stop();
            log.warn("Error preparering fetchDirection and size");
            throw new IOException("Error preparing connection for cursoring", e);
        }


/*      Removed because the check was unreliable
        if (timestampGenerator.systemTime(mtimeTimestamp) > getModificationTime(base)) {
            log.info("Storage not flushed after " + mtimeTimestamp + " for base '" + base
                     + ". Returning empty iterator");
            try {
                stmt.close();
            } catch (SQLException e) {
                log.warn("Failed to close statement", e);
            }
            return null;
        }
  */
        // Prepared stmt for all bases
        if (base == null) {
            try {
                stmt.setLong(1, mtimeTimestamp);
            } catch (SQLException e) {
                throw new IOException(String.format(Locale.ROOT, "Could not prepare stmtGetModifiedAfterAll with time %d",
                                                    mtimeTimestamp), e);
            }

            // stmt will be closed when the iterator is closed
            ResultSetCursor cursor = startIterator(stmt, null, options);
            timingDoGetRecordsModifiedAfterCursor.stop();
            return cursor;
        }

        // Prepared stmt for a specific base
        try {
            stmt.setString(1, base);
            stmt.setLong(2, mtimeTimestamp);
        } catch (SQLException e) {
            throw new IOException(
                    "Could not prepare stmtGetModifiedAfter with base '" + base + "' and time " + mtimeTimestamp, e);
        }

        ResultSetCursor cursor = startIterator(stmt, base, options);
        timingDoGetRecordsModifiedAfterCursor.stop();
        return cursor;
    }

    @Override
    public List<Record> getRecords(List<String> ids, QueryOptions options) throws IOException {
        long startNS = System.nanoTime();
        if (options == null) {
            options = defaultGetOptions;
        }
        List<Record> result = new  ArrayList<>();

        profiler.beat();

        for (String currentID : ids){
            Record record = getRecord(currentID, options);
            result.add(record);
        }
        final long spendNS = System.nanoTime() - startNS;
        if (log.isDebugEnabled()) {
            String message = "Finished getRecords(" + ids.size() + " ids (" + Strings.join(ids, 10) + ")) with "
                             + result.size() + " results (" + Strings.join(result, 10) + ") in "
                             + spendNS/1000000 + "ms. " + getRequestStats();
            log.debug(message);
        }
        //            recordlog.info(message); // Already logged in getRecord
        timingGetRecords.addNS(spendNS);
        return result;

    }

    /**
     * Return a list of records filtered with the given query options.
     *
     * @param ids     The list of ID's.
     * @param options The query options to filter record by.
     * @param conn    The database connection.
     * @return A list of records.
     * @throws IOException If error occur while fetching records.
     */
    @Deprecated
    public List<Record> getRecordsWithConnection(
            List<String> ids, QueryOptions options, Connection conn) throws IOException {
        final long startNS = System.nanoTime();
        ArrayList<Record> result = new ArrayList<>(ids.size());

        for (String id : ids) {
            try {
                Record r = getRecordWithConnection(id, options, conn);
                if (r != null) {
                    result.add(r);
                }
            } catch (SQLException e) {
                log.error("Failed to get record '" + id + "' for batch request", e);
//                result.add(null);
            }
        }
        timingGetRecordsWithConnection.addNS(System.nanoTime()-startNS);
        return result;
    }

    /**
     * Return the full object tree for the given recordId
     * This is an optimized method. Takes no QueryOptions.
     * 10-100 times faster than getRecord(id,options) for larger object trees
     *
     * @param recordId an id for an existing Record.
     * @return the record with the given ID.
     * @throws IOException If error occur while fetching records.
     */
    public Record getRecordWithFullObjectTree(String recordId) throws IOException {
        final long startNS = System.nanoTime();
        String orgRecordId= recordId;
        log.trace("getRecordWithFullObjectTree(" + recordId + ") called");

        long connectTime = -System.nanoTime();
        Connection conn = getTransactionalConnection();
        connectTime += System.nanoTime();

        ResultSet resultSet = null;
        try {
            long parentIDTime = -System.nanoTime();

            Boolean mayHaveParent = true;
            String parentId = recordId; // For each parent found, this value will be overwritten

            HashSet<String> parents = new HashSet<>();//cycle detection
            while (mayHaveParent) {
                StatementHandle handle = statementHandler.getParentIdsOnly();
                // TODO: Use handle directly
                PreparedStatement pstmtParentIdOnly = conn.prepareStatement(handle.getSql());
                pstmtParentIdOnly.setString(1, parentId);
                ResultSet parentRS = pstmtParentIdOnly.executeQuery();
                mayHaveParent = parentRS.next();
                if (mayHaveParent) {
                    parentId = parentRS.getString("PARENTID"); //Set new Parent
                    if (parents.contains(parentId)){
                        Logging.logProcess(this.getClass().getName(), "Parent-child cycle detected for id",
                                           Logging.LogLevel.WARN, parentId);
                        mayHaveParent=false; //stop resolving parents
                    }
                    parents.add(parentId);

                }
                parentRS.close();
            }
            parentIDTime += System.nanoTime();

            //Simple loading strategy. Load parent (full). Then load all children(full) in 1 SQL. Do this recurssive.

            // TODO: Use handle directly

            long statementTime = -System.nanoTime();
            StatementHandle handle = statementHandler.getGetRecordFullObjectTree();

            PreparedStatement stmt = conn.prepareStatement(handle.getSql());
            stmt.setString(1, parentId);//This is the top node in the tree
            statementTime += System.nanoTime();

            long executeTime = -System.nanoTime();
            resultSet = stmt.executeQuery();
            executeTime += System.nanoTime();

            long iterateTime = -System.nanoTime();
            if (!resultSet.next() && parentId != null && !Objects.equals(parentId, recordId)) {
                String  msg= "Parent/child relation error, record:" + recordId + " can not load an ancestor with id:"
                             + parentId + " .Only children are loaded. Original recordId in query:" + orgRecordId;
                log.warn(msg);
                //    throw new RuntimeException(msg);
                parentId=orgRecordId; //Use this as parent
            }
            if (parentId != null && Objects.equals(parentId, recordId)){ // No parent found. Using recordid as top-parent
                stmt.setString(1, recordId);//using the record requested as top-parent
                resultSet = stmt.executeQuery();
                boolean next = resultSet.next();

                if(!next){
                    log.warn(String.format(Locale.ROOT, "RecordId '%s' not found" , recordId));
                    return null;
                }
            }
            iterateTime += System.nanoTime();

            long childTime = -System.nanoTime();

            Record topParentRecord = constructRecordFromRSNoRelationsSet(resultSet);
            if (!topParentRecord.isHasRelations()) {
                //Sanity check. Can probably be removed after some time if this never happens.
                if (!topParentRecord.getId().equals(recordId)) {
                    throw new RuntimeException(
                            "Database inconsistency hasRelations for id " + topParentRecord.getId() + " and "
                            + recordId +" Original recordId in query:"+orgRecordId);
                }
                if (log.isDebugEnabled()) {
                    log.debug(String.format(Locale.ROOT,
                            "Finished getRecordWithFullObjectTree(%s) expanded with %s in %dms total (%.1f connect, " +
                            "%.1f parentID, %.1f prepareStatement, %.1f execute, %.1f iterate)",
                            recordId, parentChildStats(topParentRecord), (System.nanoTime() - startNS)/1000000,
                            connectTime/M, parentIDTime/M, statementTime/M, executeTime/M, iterateTime/M));
                }
                return topParentRecord; //Return recordId (no relations found). will happen 90% of all requests
            }

            loadAndSetChildRelations(topParentRecord, conn,null);
            childTime += System.nanoTime();

            long postOrderTime = -System.nanoTime();
            //Find the recordId in the object tree and return this. Minimal performance overhead here.
            //Post-order transversal algorithm

            Record recordNode = findRecordIdPostOrderTransversal(recordId, topParentRecord);
            postOrderTime += System.nanoTime();
            if (log.isDebugEnabled()) {
                log.debug(String.format(Locale.ROOT,
                        "Finished getRecordWithFullObjectTree(%s) expanded with %s in %dms total (%.1f connect, " +
                        "%.1f parentID, %.1f prepareStatement, %.1f execute, %.1f iterate, %.1f child, %.1f postOrder)",
                        recordId, parentChildStats(recordNode), (System.nanoTime() - startNS)/1000000,
                        connectTime/M, parentIDTime/M, statementTime/M, executeTime/M, iterateTime/M, childTime/M,
                        postOrderTime/M));
            }
            return recordNode;

        } catch (SQLException e) {
            log.warn(String.format(Locale.ROOT, "Failed to load record '%s' for original recordId '%s'", recordId, orgRecordId), e);
            return null;
        } finally {
            try {
                resultSet.close();
            } catch (SQLException e) {
                log.error(String.format(Locale.ROOT, "Failed to close result set after Record '%s'", recordId), e);
                return null;
            }
            closeConnection(conn);
            timingGetRecordsWithFullObjectTree.addNS(System.nanoTime()-startNS);
        }
    }

    private Object parentChildStats(Record record) {
        if (record == null) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "%d parents and %d children",
                             record.getParents() == null ? 0 : record.getParents().size(),
                             record.getChildren() == null ? 0 : record.getChildren().size());
    }

    public static final double M = 1000000.0;
    public static final long MI = 1000000;

    //Post-order traversal is most obvious here as this traverses nodes in same order they appear in childrenlist.
    //Traverses the tree left to right and return the record matching recordId.
    private Record findRecordIdPostOrderTransversal(String recordId, Record currentRecord) {
        if (currentRecord.getId().equals(recordId)) {
            return currentRecord; //found it!
        }
        List<Record> children = currentRecord.getChildren();
        if (children != null) {
            for (Record child : currentRecord.getChildren()) {
                Record result = findRecordIdPostOrderTransversal(recordId, child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;

    }

    //This method will call itself recursively
    private void loadAndSetChildRelations(Record parentRecord, Connection conn, HashSet<String> previousIdsForCycleDetection) throws SQLException {
        final long startNS = System.nanoTime();
        if (previousIdsForCycleDetection== null){
            previousIdsForCycleDetection = new HashSet<>();
        }
        // TODO: Use handle directly
        StatementHandle handle = statementHandler.getGetChildren(null);
        PreparedStatement pstmtSelectChildren = conn.prepareStatement(handle.getSql());
        pstmtSelectChildren.setString(1, parentRecord.getId());
        ResultSet childrenRS = pstmtSelectChildren.executeQuery();

        List<Record> children = new ArrayList<>();
        while (childrenRS.next()) {
            Record child = constructRecordFromRSNoRelationsSet(childrenRS);
            child.setParents(Arrays.asList(parentRecord));
            children.add(child);
            if(previousIdsForCycleDetection.contains(child.getId())){
                Logging.logProcess(this.getClass().getName(), "Parent-child cycle detected for id(stopped loading rest of hierachy)",
                                   Logging.LogLevel.WARN, child.getId());
                break;
            }
            previousIdsForCycleDetection.add(child.getId());
            loadAndSetChildRelations(child, conn, previousIdsForCycleDetection); //This is the recursive call
        }
        childrenRS.close();
        if (!children.isEmpty()) { //Keep current API where it is null if none exist instead of empty list
            parentRecord.setChildren(children);
        }
        timingLoadAndSetChildRelations.addNS(System.nanoTime()-startNS);
    }

    /**
     * Populate a record from a resultset
     *
     * @return if record has relations (these are not set  in this method)
     */

    private Record constructRecordFromRSNoRelationsSet(ResultSet resultSet) throws SQLException {
        String id = resultSet.getString(ID_KEY);
        String base = resultSet.getString(BASE_KEY);
        boolean deleted = getIntBool(resultSet, DELETED_FLAG_KEY, false);
        boolean indexable = getIntBool(resultSet, INDEXABLE_FLAG_KEY, true);
        boolean hasRelations = getIntBool(resultSet, HAS_RELATIONS_FLAG_KEY, false);
        byte[] gzippedContent = resultSet.getBytes(GZIPPED_CONTENT_FLAG_KEY);
        long ctime = getLong(resultSet, CTIME_KEY, 0);
        long mtime = getLong(resultSet, MTIME_KEY, 0);
        byte[] meta = resultSet.getBytes(META_KEY);

        ctime = timestampGenerator.systemTime(ctime);
        mtime = timestampGenerator.systemTime(mtime);

        /* The result set cursor will now be on the start of the next record */

        /* Create a record with gzipped content. The content will be unzipped
         * lazily by the Record class upon access */
        Record record = new Record(id, base, deleted, indexable, gzippedContent, ctime, mtime, null, null,
                                   meta.length == 0 ? null : StringMap.fromFormal(meta), gzippedContent.length != 0);
        record.setHasRelations(hasRelations);

        return record;
    }


    @Override
    public Record getRecord(String id, QueryOptions options) throws IOException {
        final long startNS = System.nanoTime();
        if (options == null) {
            options = defaultGetOptions;
        }
        //Call new optimized DB method to extract complete object tree
        if (options == null) {
            return pruneRelatives(getRecordWithFullObjectTree(id));
        }

        if (options.parentHeight() == -1 && options.childDepth() == -1 &&
            (options.meta() == null || options.meta().isEmpty())) {
            return pruneRelatives(getRecordWithFullObjectTree(id));
        }

        long startTime = System.currentTimeMillis();
        Connection conn = getTransactionalConnection();

        try {
            if (log.isTraceEnabled()) {
                log.trace("Calling getRecord(" + id + ", ...) with meta "
                          + (options.meta() == null ? "N/A" : options.meta().toFormal()));
            }
            Record record = getRecordWithConnection(id, options, conn);
            profiler.beat();
            String m = "Finished getRecord(" + id + ", " + options + ") " + (record == null ? "without" : "with")
                       + " result in " + (System.currentTimeMillis() - startTime) + "ms. " + getRequestStats();
            log.debug(m);
            recordlog.info(m);
            return pruneRelatives(record);
        } catch (SQLException e) {
            String m = "Failed getRecord(" + id + ", ...) in " + (System.currentTimeMillis() - startTime)
                       + "ms. " + getRequestStats();
            log.error(m, e);
            recordlog.error(m);
            return null;
        } finally {
            closeConnection(conn);
            timingGetRecord.addNS(System.nanoTime()-startNS);
        }
    }

    /**
     * Traverses a tree of Records, starting from the given record, pruning parents or children where there is no
     * ID-match in either the parent's childIDs or the childrens parentID.
     * Note: This deadlocks if there are cycles.
     * Note: This is only active if {@link #CONF_PRUNE_RELATIVES_ON_GET} is specified to true.
     * @param record a tree of Records (can be just a single Record).
     * @return the pruned Record tree.
     */
    public Record pruneRelatives(Record record) {
        if (!pruneRelativesOnGet) {
            return record;
        }
        final long startNS = System.nanoTime();
        if (record == null) {
            log.trace("pruneRelatives(null) called. Returning immediately");
            return null;
        }
        log.trace("Pruning deceased relatives from " + record.getId());
        // Hack: As parent & child-IDs are updated from the relations-base, we need to reset them to the original values
        resetRelatives(record);

        pruneRelatives(null, record);
        timingGetRecord.addNS(System.nanoTime()-startNS);
        return record;
    }

    private void resetRelatives(Record record) {
        if (!basesWithStoredRelations.contains(record.getBase())) {
            return;
        }
        visitRelatives(record, new RecordCallback() {
            @Override
            public void process(Record record) {
                if (record.getMeta(STORED_PARENT_IDS) == null) {
                    Logging.logProcess(
                            "DatabaseStorage.resetRelatives",
                            "Expected " + STORED_PARENT_IDS + " to be present for all records in base '" +
                            record.getBase() + "'. It is likely that the base needs a full re-ingest",
                            Logging.LogLevel.WARN, record.getId());
                } else {
                    record.setParentIds(splitStoredIDs(record.getMeta(STORED_PARENT_IDS)), false);
                }
                if (record.getMeta(STORED_CHILD_IDS) == null) {
                    // TODO: Is there a process log for persistent storage?
                    Logging.logProcess(
                            "DatabaseStorage.resetRelatives",
                            "Expected " + STORED_CHILD_IDS + " to be present for all records in base '" +
                            record.getBase() + "'. It is likely that the base needs a full re-ingest",
                            Logging.LogLevel.WARN, record.getId());
                } else {
                    record.setChildIds(splitStoredIDs(record.getMeta(STORED_CHILD_IDS)), false);
                }

                log.debug("Resetting parent & child IDs to stored values for " + record);
            }

            private List<String> splitStoredIDs(String storedIDs) {
                return storedIDs == null || storedIDs.isEmpty() ? Collections.<String>emptyList() :
                        Arrays.asList(storedIDs.split(STORED_ID_DELIMITER));
            }
        });
    }

    private void pruneRelatives(Record previous, Record origo) {
        if (origo == null) {
            log.trace("pruneRelatives(previous, origo(null)) called. Returning immediately");
            return;
        }
        if (origo.getParents() != null && !origo.getParents().isEmpty()) {
            ArrayList<Record> parents = new ArrayList<>(origo.getParents());
            for (int i = parents.size()-1 ; i >= 0; i--) {
                Record parent = parents.get(i);
                if ((origo.getParentIds() == null || !origo.getParentIds().contains(parent.getId())) &&
                    (parent.getChildIds() == null || !parent.getChildIds().contains(origo.getId()))) {
                    parents.remove(i);
                } else if (previous != parent) {
                    pruneRelatives(origo, parent);
                }
            }
            origo.setParents(parents);
        }

        if (origo.getChildren() != null && !origo.getChildren().isEmpty()) {
            ArrayList<Record> children = new ArrayList<>(origo.getChildren());
            for (int i = children.size()-1 ; i >= 0; i--) {
                Record child = children.get(i);
                if ((origo.getChildIds() == null || !origo.getChildIds().contains(child.getId())) &&
                    (child.getParentIds() == null || !child.getParentIds().contains(origo.getId()))) {
                    children.remove(i);
                } else if (previous != child) {
                    pruneRelatives(origo, child);
                }
            }
            origo.setChildren(children);
        }
    }

    private String getRequestStats() {
        return "Stats(#getRecords=" + profiler.getBeats()
               + ", q/s(last " + profiler.getBpsSpan() + ")=" + profiler.getBps(true);
    }

    private Record getRecordWithConnection(
            String id, QueryOptions options, Connection conn) throws IOException, SQLException {
        log.trace("getRecord('" + id + "', " + options + ")");
        final long startNS = System.nanoTime();

        if (isPrivateId(id)) {
            if (!allowsPrivate(options)) {
                log.debug(String.format(Locale.ROOT, "Request for private record '%s' denied", id));
                throw new IllegalArgumentException(
                        "Private record requested, but ALLOW_PRIVATE flag not set in query options");
            }
            return getPrivateRecord(conn, id);
        }
        long fullStart = System.nanoTime();
        //long statement = System.nanoTime();
        // TODO: Use the handle directly
        StatementHandle handle = statementHandler.getGetRecord(options);
        PreparedStatement stmt = conn.prepareStatement(handle.getSql());
        //log.debug("getRecordWithConnection***: Prepared statement in " + (System.nanoTime()-statement)/M + "ms");
        Record record = null;
        try {
//            long exe = System.nanoTime();
            stmt.setString(1, id);
            ResultSet resultSet = stmt.executeQuery();
//            log.debug("getRecordWithConnection***: Executed query for " + id + " in " + (System.nanoTime()-exe)/M + "ms");

            if (!resultSet.next()) {
                log.debug("No such record '" + id + "'");
                //throw new RuntimeException("Just for debug tracing");
                return null;
            }

            try {
//                long scan = System.nanoTime();
                record = scanRecord(resultSet, options);
//                log.debug("getRecordWithConnection***: ScanRecord in " + (System.nanoTime()-scan)/M + "ms");

                /* Sanity checks if we log on debug */
                if (log.isDebugEnabled()) {
                    if (!id.equals(record.getId())) {
                        log.error("Record id '" + record.getId() + "' does not match requested id: " + id);
                    }

                    while (resultSet.next()) {
                        Record tmpRec = scanRecord(resultSet, options);
                        log.warn("Bogus record in result set: " + tmpRec);
                    }
                }

                if (options != null && !options.allowsRecord(record)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Record '" + id + "' not allowed by query options. Returning null");
                    }
                    return null;
                }
                final long expandNS = System.nanoTime();
                expandRelationsWithConnection(record, options, conn);
                timingExpandRelationsWithConnection.addNS(System.nanoTime()-expandNS);
                timingExpandRelationsWithConnection.update();

//                log.debug("getRecordWithConnection***: Expand in " + (System.nanoTime()-expand)/M + "ms");

            } finally {
//                long close = System.nanoTime();
                resultSet.close();
//                log.debug("getRecordWithConnection***: Close in " + (System.nanoTime()-close)/M + "ms");

            }

        } catch (SQLException e) {
            throw new IOException("Error getting record " + id, e);
        } finally {
//            long closeS = System.nanoTime();
            closeStatement(stmt);
//            log.debug("getRecordWithConnection***: CloseStatement in " + (System.nanoTime()-closeS)/M + "ms");
//            log.debug("Full getRecordWithConnection***: " + (System.nanoTime()-fullStart)/MI + "ms");
            timingGetRecordWithConnection.addNS(System.nanoTime()-startNS);
        }

        timingGetRecordWithConnection.addNS(System.nanoTime()-startNS);
        return record;
    }

    /**
     * Return a private record, such as __holdings__ or __statistics__.
     *
     * @param id the id of the private record to retrieve.
     * @return the matching record or {@code null} in case of an unknown id.
     * @throws IOException if error when reading private record.
     */
    private Record getPrivateRecord(Connection conn, String id) throws IOException {
        log.debug(String.format(Locale.ROOT, "Fetching private record '%s'", id));
        if ("__holdings__".equals(id)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(bytes);
            BaseStats.toXML(getStatsWithConnection(conn), writer);
            return new Record("__holdings__", "__private__", bytes.toByteArray());
        } else if ("__version__".equals(id)) {
            return new Record("__version__", "__private__", SummaConstants.getVersion().getBytes(StandardCharsets.UTF_8));
        } else if ("__statistics__".equals(id)) {
            return new Record("__statistics__", "__private__", getHumanStats().getBytes(StandardCharsets.UTF_8));
        } else if ("__relation_stats__".equals(id)) {
            return new Record("__relation_stats__", "__private__", getRelationStats(conn, false).getBytes(StandardCharsets.UTF_8));
        } else if ("__relation_stats_extended__".equals(id)) {
            return new Record("__relation_stats__", "__private__", getRelationStats(conn, true).getBytes(StandardCharsets.UTF_8));
        } else if (id != null && id.startsWith("__relation_cleanup_")) {
            String rrS = id.substring("__relation_cleanup_".length(), id.length() - 2);
            try {
                REMOVAL_REQUIREMENT rr = REMOVAL_REQUIREMENT.valueOf(rrS);
                return new Record(id, "__private__",
                                  ("Cleared " + clearRelations(rr) + " relations").getBytes(StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                String message = "Unknown clean up requirement '" + rrS + "'";
                log.warn(message, e);
                return new Record(id, "__private__", (message).getBytes(StandardCharsets.UTF_8));
            }
        } else if (id != null && id.startsWith("__dump_to_file_")) {
            Matcher matcher = DTF_PATTERN.matcher(id);
            if (!matcher.matches()) {
                return new Record(id, "__private__",
                                  ("Error: Unable to parse dump to file command '" + id + "'").
                                          getBytes(StandardCharsets.UTF_8));
            }
            boolean dumpDeleted = Boolean.parseBoolean(matcher.group(1));
            String dest = matcher.group(2);
            return new Record(id, "__private__",
                              dumpToFilesystem(dest, dumpDeleted).getBytes(StandardCharsets.UTF_8));
        } else {
            log.debug(String.format(Locale.ROOT, "No such private record '%s'", id));
            return null;
        }
    }
    private final Pattern DTF_PATTERN = Pattern.compile("__dump_to_file_([a-z]+)_(.*)_");

    /**
     * Extract statistics on relations.
     * @param extended if true, extended statistics is extracted. This is a lot more heavy than non-extended.
     * @return human readable statistics on relations.
     * @throws IOException if the statistics gathering failed.
     */
    public String getRelationStats(boolean extended) throws IOException {
        try (Connection conn = getTransactionalConnection()) {
            return getRelationStats(conn, extended);
        } catch (SQLException e) {
            String message = "getRelationStats(" + extended + "): SQLException requesting simple connection stats";
            log.warn(message, e);
            throw new IOException(message, e);
        }
    }

    /**
     * Extract statistics on relations.
     * @param conn     an established connection to the Storage.
     * @param extended if true, extended statistics is extracted. This is a lot more heavy than non-extended.
     * @return human readable statistics on relations.
     * @throws IOException if the statistics gathering failed.
     */
    public String getRelationStats(Connection conn, boolean extended) throws IOException {
        try {
            return extended ?
                    getExtendedRelationStatsWithConnection(conn) :
                    getSimpleRelationStatsWithConnection(conn);
        } catch (SQLException e) {
            String message = "SQLException requesting simple connection stats";
            log.warn(message, e);
            throw new IOException(message, e);
        }
    }

    /**
     * Return light relation statistics for a given connection.
     *
     * @param conn The connection.
     * @return a human readable report of relations.
     * @throws SQLException If error getting the SQL exception.
     * @throws IOException  If error handling RMI.
     */
    private String getSimpleRelationStatsWithConnection(Connection conn) throws SQLException {
        long startTime = System.currentTimeMillis();
        String report = "Total relation rows: " + getRowCount(conn, RELATIONS);
        log.debug(String.format("Extracted simple relations stats in %sms", System.currentTimeMillis() - startTime));
        return report;
    }

    /**
     * @param conn an existing connection to the database.
     * @param table a table in the database.
     * @return the number of rows in the given table (fast).
     */
    public long getRowCount(Connection conn, String table) throws SQLException {
        final String query = "SELECT count(*) FROM " + table;
        return getSingleLong(conn, query);
    }

    /**
     * @param conn an existing connection to the database.
     * @param table a table in the database.
     * @param field the field to count uniques for.
     * @return the number of unique values for a given field in the given table.
     */
    public long getDistinctCount(Connection conn, String table, String field) throws SQLException {
        final String query = "SELECT COUNT(DISTINCT " + field + ") FROM " + table;
        return getSingleLong(conn, query);
    }

    /**
     * Takes a query, executes it and return a long extracted from the first result in the result set, at position 1.
     * @param conn an existing connection to the database.
     * @param query a query such as "SELECT COUNT(*) FROM footable".
     * @return the first field in the first result.
     * @throws SQLException if the query failed or there was no result.
     */
    private long getSingleLong(Connection conn, String query) throws SQLException {
        Statement stmt = conn.createStatement();
        log.debug("Getting resultset for '" + query + "'");
        long st = System.currentTimeMillis();

        try (ResultSet result = stmt.executeQuery(query)) {
            log.debug("Got ResultSet for '" + query + "' in " + (System.currentTimeMillis() - st) + "ms");
            if (result.next() && !result.isAfterLast()) {
                return result.getLong(1);
            }
            String message = "No result trying to extract first field in first result for query '" + query + "'";
            log.warn(message);
            throw new SQLException(message);
        }
    }

    /**
     * Takes a SQL statement and executes it, ignoring output.
     * @param conn an existing connection to the database.
     * @param statement the SQL statement to execute.
     * @throws SQLException if the query failed or there was no result.
     */
    private void executeStatement(Connection conn, String statement) throws SQLException {
        Statement stmt = conn.createStatement();
        log.debug("Executing SQL statement '" + statement + "'");
        long st = System.currentTimeMillis();

        stmt.executeUpdate(statement);
        log.debug("Executed statement '" + statement + "' in " + (System.currentTimeMillis() - st) + "ms");
    }

    /**
     * Return heavy relation statistics for a given connection.
     *
     * @param conn The connection.
     * @return a human readable report of relations.
     * @throws SQLException If error getting the SQL exception.
     * @throws IOException  If error handling RMI.
     */
    private String getExtendedRelationStatsWithConnection(Connection conn) throws SQLException {
        long startTime = System.currentTimeMillis();

        String NONDEL_PARENTS =
                "SELECT COUNT(DISTINCT rel." + DatabaseStorage.PARENT_ID_COLUMN +
                ") FROM " + DatabaseStorage.RELATIONS + " rel " +
                " LEFT JOIN " + DatabaseStorage.RECORDS + " rec " +
                " WHERE rel." + DatabaseStorage.PARENT_ID_COLUMN + "=" + "rec." + DatabaseStorage.ID_COLUMN +
                " AND rec." + DatabaseStorage.DELETED_COLUMN + "=false";
        String NONDEL_CHILDREN =
                "SELECT COUNT(DISTINCT rel." + DatabaseStorage.CHILD_ID_COLUMN +
                ") FROM " + DatabaseStorage.RELATIONS + " rel " +
                " LEFT JOIN " + DatabaseStorage.RECORDS + " rec " +
                " WHERE rel." + DatabaseStorage.CHILD_ID_COLUMN + "=" + "rec." + DatabaseStorage.ID_COLUMN +
                " AND rec." + DatabaseStorage.DELETED_COLUMN + "=false";

        List<String> report = new ArrayList<>();
        try {
            report.add("records (all): " + getRowCount(conn, RECORDS));
            report.add("records (non-deleted): " + getSingleLong(
                    conn, "SELECT COUNT(*) FROM " + RECORDS + " where " + DELETED_COLUMN + "=false"));
            report.add("relations (all): " + getRowCount(conn, RELATIONS));
            report.add("relations (either parent or child present): " + getValidRelationCount(conn));
            report.add("relations (distinct parentIDs): " + getDistinctCount(conn, RELATIONS, PARENT_ID_COLUMN));
            report.add("relations (distinct non-deleted parentIDs): " + getSingleLong(conn, NONDEL_PARENTS));
            report.add("relations (distinct childIDs): " + getDistinctCount(conn, RELATIONS, CHILD_ID_COLUMN));
            report.add("relations (distinct non-deleted childIDs): " + getSingleLong(conn, NONDEL_CHILDREN));
        } catch (Exception e) {
            String message = "Exception generating extended relations report: " + e.getMessage();
            log.warn(message, e);
            report.add("Exception generating extended relations report: " + e.getMessage());
        }
        log.debug("Extended relations report generated in " + (System.currentTimeMillis()-startTime) + "ms");
        return Strings.join(report, "\n");
    }

    public enum REMOVAL_REQUIREMENT {
        only_parent_valid, only_child_valid, only_one_valid, none_valid
    }

    /**
     * Delete the record with recordID from the {@link #RECORDS} database.
     * Relations are not updated.
     * @param recordID the ID for the record to delete.
     * @throws IOException wrapper for any SQLExceptions thrown during delete.
     */
    public void deleteRecord(String recordID) throws IOException {
        try (Connection conn = getTransactionalConnection()) {
            PreparedStatement delete = conn.prepareStatement("DELETE FROM " + RECORDS + " WHERE " + ID_COLUMN + "=?");
            delete.setString(1, recordID);
            delete.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            String message = "deleteRecord(" + recordID + ") encountered a SQLException";
            log.warn(message, e);
            throw new IOException(message, e);
        }
    }
    /**
     * Clear all relations where the removalRequirement is satisfied.
      * @param removalRequirement thr grounds for removal of a relation.
     * @return the number of relations that were removed.
     */
    public long clearRelations(REMOVAL_REQUIREMENT removalRequirement) throws IOException {
        try (Connection conn = getTransactionalConnection()) {
            return clearRelations(conn, removalRequirement);
        } catch (SQLException e) {
            String message = "clearRelations(" + removalRequirement + ") encountered a SQLException";
            log.warn(message, e);
            throw new IOException(message, e);
        }
    }
    /**
     * Clear all relations where the removalRequirement is satisfied.
      * @param removalRequirement thr grounds for removal of a relation.
     * @return the number of relations that were removed.
     */
    private long clearRelations(Connection conn, REMOVAL_REQUIREMENT removalRequirement) throws SQLException {
        // TODO: Add check for isDeleted
        final String REL = DatabaseStorage.RELATIONS;
        final String RECORDS = DatabaseStorage.RECORDS;
        final String PARENT = DatabaseStorage.PARENT_ID_COLUMN;
        final String CHILD = DatabaseStorage.CHILD_ID_COLUMN;
        final String ID = DatabaseStorage.ID_COLUMN;
        final String DELETED = DatabaseStorage.DELETED_COLUMN;

        // Creates a virtual table with [parent, child, parent_deleted, child_deleted], where the deleted-entries are
        // null if no matching Record could be found.
        //language=PostgreSQL
        final String BASE =
                "delete " +
                "from " + REL + " rel " +
                "where exists ( " +
                "    select 1 " +
                "    from ( " +
                "    select " + PARENT + ", " + CHILD + ", parent_deleted, child_deleted " +
                "       from " + REL + " as orig " +
                "                left join (select " + ID + ", " + DELETED + " as parent_deleted from " + RECORDS + ") as join1 on join1." + ID + " = " + PARENT + " " +
                "                left join (select " + ID + ", " + DELETED + " as child_deleted from " + RECORDS + ") as join2 on join2." + ID + " = orig." + CHILD +
                "    ) as joined " +
                "    where (rel." + PARENT + "=joined." + PARENT + " and rel." + CHILD + "=joined." + CHILD + ") and (";

        final String NONE = "((parent_deleted or parent_deleted is null) and (child_deleted or child_deleted is null))";
        final String PVAL = "(parent_deleted=false and (child_deleted or child_deleted is null))";
        final String CVAL = "((parent_deleted or parent_deleted is null) and child_deleted=false)";

        final long preDelete = getRowCount(conn, RELATIONS);
        switch (removalRequirement) {
            case none_valid: {
                executeStatement(conn, BASE + NONE + "))");
                break;
            }
            case only_child_valid: {
                executeStatement(conn, BASE + NONE + " or " + CVAL + "))");
                break;
            }
            case only_parent_valid: {
                executeStatement(conn, BASE + NONE + " or " + PVAL + "))");
                break;
            }
            case only_one_valid: {
                executeStatement(conn, BASE + NONE + " or " + PVAL + " or " + CVAL + "))");
                break;
            }
            default: throw new UnsupportedOperationException(
                    "The removal requirement '" + removalRequirement + " is not supported");
        }
        return preDelete-getRowCount(conn, RELATIONS);
    }

    private long getValidRelationCount(Connection conn) throws SQLException {
        String query =
                "SELECT COUNT(*) FROM " + DatabaseStorage.RELATIONS + " rel " +
                " LEFT JOIN " + DatabaseStorage.RECORDS + " rec " +
                " WHERE (rel." + DatabaseStorage.PARENT_ID_COLUMN + "=" + "rec." + DatabaseStorage.ID_COLUMN +
                " OR rel." + DatabaseStorage.PARENT_ID_COLUMN + "=" + "rec." + DatabaseStorage.ID_COLUMN +
                ") AND rec." + DatabaseStorage.DELETED_COLUMN + "=false";

        return getSingleLong(conn, query);
    }


    private String getHumanStats() {
        // TODO: When sbutil 0.5.51 comes out, pretty-print timing with newlines as separator
        return timing.toString() + "\n\n" + getIterationStats();
    }

    /**
     * Expand child records if we need to and there indeed
     * are any children to expand.
     */
    private void expandChildRecords(Record record, RecursionQueryOptions options, Connection conn) throws IOException {
        if (options.childRecursionDepth() == 0) {
            if (options.childDepth() > 1) {
                log.debug("Skipping further expansion of child records for " + record + " as the maximum expansion "
                          + "depth of " + options.childDepth() + " has been reached");
            }
            return;
        }

        List<String> childIds = record.getChildIds();

        if (childIds != null && !childIds.isEmpty()) {

            if (log.isTraceEnabled()) {
                log.trace("Expanding children of record '" + record.getId() + "': " + Strings.join(childIds, ", "));
            }

            // Make sure we don't go into an infinite parent/child
            // expansion ping-pong
            options = RecursionQueryOptions.asChildOnlyOptions(options);

            List<Record> children = getRecordsWithConnection(childIds, options.decChildRecursionDepth(), conn);

            if (children.isEmpty()) {
                record.setChildren(null);
            } else {
                record.setChildren(children);
            }
        }
    }

    /**
     * Expand parent records if we need to and there indeed
     * are any parents to expand.
     */
    private void expandParentRecords(Record record, RecursionQueryOptions options, Connection conn) throws IOException {
//        log.debug("expandParentRecords*** id=" + record.getId());
        if (options.parentRecursionHeight() == 0) {
            if (options.parentHeight() > 1) {
                log.debug("Skipping further expansion of parent records for " + record + " as the maximum expansion "
                          + "height of " + options.parentHeight() + " has been reached");
            }
            return;
        }

        List<String> parentIds = record.getParentIds();
//        log.debug("expandParentRecords*** got parentIDs=" + (parentIds == null ? "null" : parentIds.size() == 1 ? parentIds.get(0) : parentIds.size() + " parents"));

        if (parentIds != null && !parentIds.isEmpty()) {

            if (log.isTraceEnabled()) {
                log.trace("Expanding parents of record '" + record.getId() + "': " + Strings.join(parentIds, ", "));
            }

            // Make sure we don't go into an infinite parent/child
            // expansion ping-pong
            options = RecursionQueryOptions.asParentsOnlyOptions(options);

//            long start = System.nanoTime();
            List<Record> parents = getRecordsWithConnection(parentIds, options.decParentRecursionHeight(), conn);
//            log.debug("expandParentRecords*** called deprecated getRecordsWithConnection in "
//                      + (System.nanoTime()-start) / M + "ms");

            if (parents.isEmpty()) {
                record.setParents(null);
            } else {
                record.setParents(parents);
            }
        }
    }

    @Override
    public Record next(long iteratorKey) throws IOException {
        final long startNS = System.nanoTime();
        if (iteratorKey == EMPTY_ITERATOR_KEY) {
            throw new NoSuchElementException("Empty cursor");
        }
        lastNextTimeNS = -System.nanoTime();

        cursorGet -= System.nanoTime();
        Cursor cursor = iterators.get(iteratorKey);
        cursorGet += System.nanoTime();

        if (cursor == null) {
            throw new IllegalArgumentException("No result cursor with key " + iteratorKey);
        }

        cursorNext -= System.nanoTime();
        if (!cursor.hasNext() || (lastIterated = cursor.next()) == null) {
            cursorNext += System.nanoTime();
            cursor.close();
            iterators.remove(cursor.getKey());
            throw new NoSuchElementException("Iterator " + iteratorKey + " depleted");
        }
        cursorNext += System.nanoTime();

        try {
            if (cursor.needsExpansion()) {
                expand -= System.nanoTime();
                // Ugly hack with the casting to ConnectionCursor
                this.lastIterated = expandRelationsWithConnection(
                        lastIterated, cursor.getQueryOptions(), ((ConnectionCursor)cursor).getConnection());
                timingExpandRelationsWithConnection.addNS(System.nanoTime() + expand);
                //Record expanded = expandRelations(r, cursor.getQueryOptions());
                expand += System.nanoTime();
            }
        } catch (Exception e) {
            log.warn("Failed to expand relations for '" + lastIterated.getId() + "'", e);
            return lastIterated;
        }
        nextCalls++;
        contentRawSize += this.lastIterated.getContent(false).length;
        lastNextTimeNS += System.nanoTime();

        if (System.currentTimeMillis() >= logNextMS) {
            log.info(getIterationStats());
            logNextMS = System.currentTimeMillis() + logEveryMS;
        }
        timingNext.addNS(System.nanoTime()-startNS);
        return this.lastIterated;
    }

    private String getIterationStats() {
        if (lastIterated == null) {
            return "iteration(N/A)";
        }
        return "iteration(lastRecord in " + lastNextTimeNS/M + "ms, totalCalls=" + nextCalls
               + ", totalRawSize=" + contentRawSize/1048576 + "MB, "
               + (contentRawSize/1024/nextCalls) + " KB/Record avg"
               + ", cursorGet=" + stat(cursorGet, nextCalls) + ", cursorNext=" + stat(cursorNext, nextCalls)
               + ", expandRelations=" + stat(expand, nextCalls) + " id=" + lastIterated.getId() + ", parents=" +
               count(lastIterated.getParents()) + ", children=" + count(lastIterated.getChildren()) + ")";
    }
    private Record lastIterated = null;
    private int count(List<Record> records) {
        return records == null ? 0 : records.size();
    }
    private String stat(long ns, long calls) {
        if (calls == 0) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "[total=%.2fms, %.2f ms/call, %.2f calls/ms]",
                             ns/M, ns/M/calls, calls*M/ns);
    }

    // TODO: Make the interval adjustable
    private final long logEveryMS = 60000;
    private long logNextMS = System.currentTimeMillis() + logEveryMS;
    private long lastNextTimeNS = 0;
    private long nextCalls = 0;
    private long cursorGet = 0;
    private long cursorNext = 0;
    private long expand = 0;
    private long contentRawSize = 0;

    private Record expandRelations(Record r, QueryOptions options) throws IOException, SQLException {
        if (options == null) { // No need for opening a connection as expansion is disabled
            return r;
        }

        Connection conn = getTransactionalConnection();


        try {
            return expandRelationsWithConnection(r, options, conn);
        } finally {
            closeConnection(conn);
        }
    }

    private Record expandRelationsWithConnection(Record r, QueryOptions options, Connection conn) throws IOException {
        if (options == null) {
            return r;
        }

        // This also makes sure that the recursion levels are reset
        RecursionQueryOptions opts;

        if (options.childDepth() != 0) {
            //opts = RecursionQueryOptions.wrap(options);
            opts = RecursionQueryOptions.asChildOnlyOptions(options);
//            log.debug("*** Expanding child records");
            expandChildRecords(r, opts, conn);
        }

        if (options.parentHeight() != 0) {
            //opts = RecursionQueryOptions.wrap(options);
            opts = RecursionQueryOptions.asParentsOnlyOptions(options);
            //log.debug("*** Expanding parent records");
            expandParentRecords(r, opts, conn);
        }
        return r;
    }

    /**
     * Flush a single record to storage.
     * Note: the 'synchronized' part of this method decl is paramount to
     * allowing us to set our transaction level to
     * Connection.TRANSACTION_READ_UNCOMMITTED
     *
     * @param record  The record to flush.
     * @param options Query options to filter out record from flushing.
     * @throws IOException If error occur while flushing.
     */
    @Override
    public synchronized void flush(Record record, QueryOptions options) throws IOException {
        final long startNS = System.nanoTime();
        timingFlush.start();
        Connection conn = getTransactionalConnection();
        // Brace yourself for the try-catch-finally hell, but we really don't
        // want to leak them pooled connections!
        String error = null;
        try {

            flushWithConnection(record, options, conn);
            // Update last modification time.
            updateLastModficationTimeForBase(record.getBase());
            setBaseStatisticInvalid(record.getBase(), conn);
        } catch (SQLException e) {
            // This error is logged in the finally clause below
            error = e.getMessage() + '\n' + Strings.getStackTrace(e);

            // We can not throw the SQLException over RPC as the receiver
            // probably does not have the relevant exception class
            throw new IOException(String.format(Locale.ROOT, "flush(...): Failed to flush %s: %s", record, e.getMessage()));
        } finally {
            try {
                if (error == null) {
                    // All is OK, write to the DB
                    conn.commit();
                    log.debug("Committed " + record.getId() + " in " + (System.nanoTime() - startNS)/1000000 + "ms");
                } else {
                    log.warn(String.format(Locale.ROOT, "Not committing %s because of error: %s", record.getId(), error));
                }
            } catch (SQLException e) {
                error = "flush: Failed to commit " + record.getId() + ": " + e.getMessage();
                log.warn(error, e);
                throw new IOException(error, e);
            } finally {
                closeConnection(conn);
            }
            timingFlush.stop();
        }
    }

    private void storeRelativeIDs(Record record) {
        if (!basesWithStoredRelations.contains(record.getBase())) {
            return;
        }
        visitRelatives(record, new RecordCallback() {
            @Override
            public void process(Record record) {
                record.addMeta(STORED_PARENT_IDS, record.getParentIds() == null ? "" :
                        Strings.join(record.getParentIds(), STORED_ID_DELIMITER));
                record.addMeta(STORED_CHILD_IDS, record.getChildIds() == null ? "" :
                        Strings.join(record.getChildIds(), STORED_ID_DELIMITER));
            }
        });
    }

    private void visitRelatives(Record record, RecordCallback callback) {
        visitRelatives(record, callback, new HashSet<String>());
    }
    private void visitRelatives(Record record, RecordCallback callback, HashSet<String> visitedIDs) {
        if (!visitedIDs.add(record.getId())) {
            return;
        }
        callback.process(record);
        if (record.getParents() != null) {
            for (Record parent: record.getParents()) {
                visitRelatives(parent, callback, visitedIDs);
            }
        }
        if (record.getChildren() != null) {
            for (Record child: record.getChildren()) {
                visitRelatives(child, callback, visitedIDs);
            }
        }
    }
    private interface RecordCallback {
        void process(Record record);
    }

    long totalFlushed = 0;
    /**
     * Flush a list of records to the storage.
     * Note: the 'synchronized' part of this method decl is paramount to
     * allowing us to set our transaction level to
     * Connection.TRANSACTION_READ_UNCOMMITTED
     *
     * @param recs    The list of options to flush.
     * @param options The query options to filter out records.
     * @throws IOException If error occur while flushing.
     */
    @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
    @Override
    // TODO: Race conditions in FacetTest indicates that flush is guaranteed to have written everything before returning
    public synchronized void flushAll(List<Record> recs, QueryOptions options) throws IOException {
        timingFlushAll.start();
        Connection conn = getTransactionalConnection();


        // Brace yourself for the try-catch-finally hell, but we really don't
        // want to leak them pooled connections!
        String error = null;
        Record lastRecord = null;
        long start = System.nanoTime();
        boolean isDebug = log.isDebugEnabled();
        Map<String, Boolean> bases = new HashMap<>();
        try {
            for (Record r : recs) {
                lastRecord = r;
                flushWithConnection(r, options, conn);
                bases.put(r.getBase(), true);
            }
            for (String base : bases.keySet()) {
                log.debug("Updates last modification time for base '" + base + "'");
                updateLastModficationTimeForBase(base);
                setBaseStatisticInvalid(base, conn);
            }
            // TODO Introduce time-based logging on info
            totalFlushed += recs.size();
            log.debug("Flushed " + recs.size() + " records in " + (System.nanoTime() - start) / 1000000 + "ms"
                      + "(" + totalFlushed + " flushed since " + START_TIME + ")");
        } catch (SQLException e) {
            error = e.getMessage();
            throw new IOException(String.format(Locale.ROOT, "flushAll(%d records): Failed to flush %s: %s",
                                                recs.size(), lastRecord, e.getMessage()), e);
        } finally {
            try {
                if (error == null) {
                    // All is OK, write to the DB
                    log.debug("Commiting transaction of " + recs.size() + " records");
                    start = System.nanoTime();
                    conn.commit();
                    log.debug("Transaction of " + recs.size() + " records completed in "
                              + (System.nanoTime() - start) / 1000000D + "ms");
                    if (isDebug) { // TODO: Make this a single log-entry
                        for (Record r : recs) {
                            // It may seem dull to iterate over all records
                            // *again*, but time has taught us that this info is
                            // really nice to have in the log...
                            log.debug("Committed: " + r.getId());
                        }
                    }
                } else {
                    log.warn(String.format(Locale.ROOT, "Not committing the last %d records because of error '%s'. The records was"
                                           + " %s", recs.size(), error, Logs.expand(recs, 10)));
                }
            } catch (SQLException e) {
                error = "Failed to commit the last " + recs.size() + " records: " + e.getMessage();
                log.warn(error, e);
                throw new IOException(error, e);
            } finally {
                try {
                    if (error != null) {
                        conn.rollback();
                        log.info("Transaction rolled back succesfully");
                    }
                } catch (SQLException e) {
                    log.error("Transaction rollback failed: " + e.getMessage(), e);
                }
                closeConnection(conn);
                timingFlushAll.stop();
            }
        }
    }

    /*
     * When updating a record we still need to touch parent/childs that are no longer related to the record if they were before update
     */

    private void touchOldParentChildRelations(Record r, Connection conn) throws IOException, SQLException{
        final long startNS = System.nanoTime();

        List<String> newChildIds = r.getChildIds();
        List<String> newParentsIds = r.getChildIds();
        if (newChildIds == null){
            newChildIds= new ArrayList<>();
        }
        if (newParentsIds == null){
            newParentsIds= new ArrayList<>();
        }
        List<String> oldParentsIds = new ArrayList<>();
        List<String> oldChildrenIds = new ArrayList<>();
        switch (relationsTouch){

            case none:
                break;
            case child:
                oldChildrenIds=getChildIdsOnly(r.getId(), conn);
                oldChildrenIds.removeAll(newChildIds);

                for (String c :  oldChildrenIds){ //touch children that are not children anymore
                    touchRecord(c, conn, false);
                    touchChildren(c,null,conn);
                }
                break;
            case parent:
                oldParentsIds=getParentsIdsOnly(r.getId(), conn);
                oldParentsIds.removeAll(newParentsIds);
                for (String p :  oldParentsIds){ //touch children that are not children anymore
                    touchRecord(p, conn, false);
                    touchParents(p,null,conn);
                }
                break;
            case all:
                oldParentsIds.removeAll(newParentsIds);
                for (String p :  oldParentsIds){ //touch parents that are not parents anymore
                    touchRecord(p,conn, false);
                    touchParents(p,null,conn);
                }
                oldChildrenIds.removeAll(newChildIds);
                for (String c :  oldChildrenIds){ //touch children that are not children anymore
                    touchRecord(c, conn, false);
                    touchChildren(c,null,conn);
                }
                break;
        }
        timingTouchOldParentChildRelations.addNS(System.nanoTime()-startNS);
    }

    /**
     * Flush a record to the database.
     *
     * @param r       The record to flush.
     * @param options The query options.
     * @param conn    The database connection.
     * @throws IOException  If error occur with connection.
     * @throws SQLException If error occur while executing SQL.
     */
    protected void flushWithConnection(
            Record r, QueryOptions options, Connection conn) throws IOException, SQLException {
        final long startNS = System.nanoTime();
        if (log.isTraceEnabled()) {
            log.trace("Flushing: " + r.toString(true));
        } else if (log.isDebugEnabled()) {
            log.debug("Flushing with connection: " + r.toString(false));
        }

        RELATION relationsTouch= this.relationsTouch;
        RELATION clearRelation = this.relationsClear;
        // Update the timestamp we check against in getRecordsModifiedAfter
        updateModificationTime(r.getBase());

        boolean recordExist = recordExist(conn, r.getId());

        try {
            if (!recordExist) {
                createNewRecordWithConnection(r, options, conn);
            } else {
                //Special case. The method below is extremely slow for yet unknown reasons for large object trees (10000+) and scales badly.
                //avoid calling it for some settings where we know it is unnessecary. It can be improved by a different implementation
                //by loading the full object tree(ID's only) from the old record and comparing.


                if (relationsTouch.equals(RELATION.child)
                    && clearRelation.equals(RELATION.parent)
                    && (r.getParents() == null || r.getParents().size()==0) //Single object with no tree-changes
                    && (r.getChildren() == null || r.getChildren().size()==0) //Single object with no tree-changes
                        ){
                    //do not update  old tree recursive
                }
                else{
                    touchOldParentChildRelations(r, conn);
                }


                // We already had the record stored, so fire an update instead
                updateRecordWithConnection(r, options, conn);
            }
        } catch (SQLException e) {
            throw new IOException(String.format(Locale.ROOT, "flushWithConnection: Internal error in DatabaseStorage, "
                                                + "failed to flush %s: %s", r.getId(), e.getMessage()), e);
        }

        // Recursively add child records
        List<Record> children = r.getChildren();
        if (children != null) {
            log.debug("Flushing " + children.size() + " nested child records of '" + r.getId() + "'");
            for (Record child : children) {
                flushWithConnection(child, options, conn);
            }
        }

        try {
            switch (relationsTouch){
                case none:
                    break;
                case child:
                    touchChildren(r.getId(), null, conn);
                    break;
                case parent:
                    touchParents(r.getId(), null, conn);
                    break;
                case all:
                    touchParents(r.getId(), null, conn);
                    touchChildren(r.getId(), null, conn);
                    break;
            }
        } catch (IOException e) {
            // Consider this non-fatal
            log.error("Failed to touch parents of '" + r.getId(), e);
        }



        //Again - update the timestamp we check against in
        // getRecordsModifiedAfter. This is also done in the end of the flush()
        // because the operation is non-instantaneous
        updateModificationTime(r.getBase());
        timingFlushWithConnection.addNS(System.nanoTime()-startNS);
    }


    protected void touchParents(String id, QueryOptions options, Connection conn) throws IOException, SQLException {
        touchParents(id, options, conn, new HashSet<String>());
    }

    /**
     * Touch (that is, set the 'mtime' to now) the parents
     * of <code>id</code> recursively upwards.
     *
     * @param id      the id of the records which parents to touch.
     * @param options any query options that may affect how the touching is
     *                handled.
     * @param conn    the SQL connection.
     * @param parentsVisited  To avoid cycle detection
     * @throws IOException  if error is experienced when closing statement.
     * @throws SQLException if {@link Connection#prepareStatement} fails.
     */
    protected void touchParents(String id, QueryOptions options, Connection conn, HashSet<String> parentsVisited)
            throws IOException, SQLException {

        List<String> parents= getParentsIdsOnly(id, conn);

        if (parents == null || parents.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace("No parents to update for record " + id);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Touching " + parents.size() + " parents of " + id);
        }

        // Use handle directly
        StatementHandle handle = statementHandler.getTouchParents();
        PreparedStatement stmt = conn.prepareStatement(handle.getSql());
        try {
            long nowStamp = timestampGenerator.next();
            stmt.setLong(1, nowStamp);
            stmt.setString(2, id);
            stmt.executeUpdate();

            // It would be a tempting optimization to drop the getParents() call
            // at the top and simply return here if stmt.getUpdateCount() == 0.
            // This would avoid the creation of a ResultSet in getParents().
            // We can't do this because there might be a ref to a non-existing
            // parent which in turn might have a parent that actually exist.
            // If we returned on zero updates we wouldn't touch the topmost
            // parent

        } catch (SQLException e) {
            log.error("Failed to touch parents of '" + id + "': " + e.getMessage(), e);
            // Consider this non-fatal
            return;
        } finally {
            closeStatement(stmt);
        }

        // Recurse upwards
        for (String parentId : parents) {
            if (parentsVisited.contains(parentId)){
                Logging.logProcess("DatabaseStorage#touchParents", "Parent/child cycle detected",
                                   Logging.LogLevel.WARN, parentId);
                break;
//                throw new SQLException("Parent/child cycle detected for id:"+parentId);
            }
            parentsVisited.add(parentId);
            touchParents(parentId, options, conn);
        }
    }


    protected void touchChildren(String id, QueryOptions options, Connection conn) throws IOException, SQLException {
        touchChildren(id, options, conn, new HashSet<String>());
    }

    /**
     * Touch (that is, set the 'mtime' to now) the parents
     * of <code>id</code> recursively upwards.
     *
     * @param id      the id of the records which parents to touch.
     * @param options any query options that may affect how the touching is
     *                handled.
     * @param conn    the SQL connection.
     * @param childrenVisited  To avoid cycle detection
     * @throws IOException  if error is experienced when closing statement.
     * @throws SQLException if {@link Connection#prepareStatement} fails.
     */
    protected void touchChildren(String id, QueryOptions options, Connection conn, HashSet<String> childrenVisited) throws IOException, SQLException {
        List<String> children = getChildIdsOnly(id, conn);

        if (children == null || children.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace("No children to update for record " + id);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.info("Touching " + children.size() + " children of " + id);
        }

        int touched = 0;

        // Use handle directly

        PreparedStatement stmt = null;
        try {

            //Single touch
            StatementHandle handle = statementHandler.getTouchRecord();
            stmt = conn.prepareStatement(handle.getSql());

            //Single touch, unique modified time
            for (String r : children) {
                long nowStamp = timestampGenerator.next();
                stmt.setLong(1, nowStamp);
                stmt.setString(2, r);
                stmt.executeUpdate();

                if (touched % 100000==0){
                    log.info("Total touched so far:"+touched);

                }
                touched++;

            }
                        
                /* Multitouch code, not used
                StatementHandle handle = statementHandler.getTouchChildren();
                stmt = conn.prepareStatement(handle.getSql());

                //Instead multitouch children, not unique modified time
                long nowStamp = timestampGenerator.next();
                stmt.setLong(1, nowStamp);
                stmt.setString(2, id);
                int updated = stmt.executeUpdate();
                log.info("Multitouched #children:"+updated);
                 */


            // It would be a tempting optimization to drop the getParents() call
            // at the top and simply return here if stmt.getUpdateCount() == 0.
            // This would avoid the creation of a ResultSet in getParents().
            // We can't do this because there might be a ref to a non-existing
            // parent which in turn might have a parent that actually exist.
            // If we returned on zero updates we wouldn't touch the topmost
            // parent

        } catch (SQLException e) {
            log.error("Failed to touch child of '" + id + "': " + e.getMessage(), e);
            // Consider this non-fatal
            return;
        } finally {
            if (stmt != null) {
                closeStatement(stmt);
            }
        }

        // Recursive downwards
        for (String childId : children) {
            if (childrenVisited.contains(childId)){
                Logging.logProcess("DatabaseStorage#touchChildren", "Parent/child cycle detected",
                                   Logging.LogLevel.WARN, childId);
                break;
                //throw new SQLException("Parent/child cycle detected for id:"+childId);
            }
            childrenVisited.add(childId);
            touchChildren(childId, options, conn, childrenVisited);
        }
    }

    /**
     * Return a list of parent records.
     *
     * @param id      The child ID.
     * @param options The query option to filter out parents from the return
     *                set.
     * @param conn    The database connection.
     * @return A list of parent records.
     * @throws IOException  If error occur with database connection.
     * @throws SQLException If error occur executing the SQL.
     */
    protected List<Record> getParents(
            String id, QueryOptions options, Connection conn) throws IOException, SQLException {
        final long startNS = System.nanoTime();
        // TODO: Use handle directly
        StatementHandle handle = statementHandler.getGetParents(options);
        PreparedStatement stmt = conn.prepareStatement(handle.getSql());

        List<Record> parents = new ArrayList<>(1);
        Cursor iter = null;

        try {
            stmt.setString(1, id);
            stmt.executeQuery();

            ResultSet results = stmt.getResultSet();
            iter = new ResultSetCursor(this, stmt, results, true); // Anon cursor

            while (iter.hasNext()) {
                Record r = iter.next();
                if (options != null && options.allowsRecord(r) || options == null) {
                    parents.add(r);
                } else if (log.isTraceEnabled()) {
                    log.trace("Parent record '" + r.getId() + "' not allowed by query options");
                }
            }

            if (log.isTraceEnabled()) {
                log.trace("Looked up parents for '" + id + "': " + Strings.join(parents, ";"));
            }

            return parents;

        } catch (SQLException e) {
//            Logging.logProcess("DatabaseStorage#getParents", "Failed to get parents. Returning empty list",
//                               Logging.LogLevel.WARN, id, e);
            throw new IOException("Failed to get parents for record '" + id, e);
        } finally {
            if (iter != null) {
                iter.close();
            } else {
                closeStatement(stmt);
            }
            timingGetParents.addNS(System.nanoTime() - startNS);
        }
    }


    protected List<String> getParentsIdsOnly( String id,Connection conn) throws IOException, SQLException {
        final long startNS = System.nanoTime();
        StatementHandle handle = statementHandler.getParentIdsOnly();
        PreparedStatement stmt = conn.prepareStatement(handle.getSql());

        List<String> parentsIds = new ArrayList<>();


        try {
            stmt.setString(1, id);
            stmt.executeQuery();

            ResultSet results = stmt.getResultSet();

            while (results.next()) {
                parentsIds.add(results.getString("PARENTID"));
            }
            return parentsIds;

        } catch (SQLException e) {
//            Logging.logProcess("DatabaseStorage#getParentsIdsOnly", "Failed to get parents. Returning empty list",
//                               Logging.LogLevel.WARN, id, e);
            throw new IOException("Failed to get getParentsIdsOnly for record '" + id, e);
        } finally {
            if (stmt != null) {
                closeStatement(stmt);
            }
            timingGetParentsIDsOnly.addNS(System.nanoTime() - startNS);
        }
    }

    protected List<String> getChildIdsOnly( String id,Connection conn) throws IOException, SQLException {
        final long startNS = System.nanoTime();
        StatementHandle handle = statementHandler.getChildIdsOnly();
        PreparedStatement stmt = conn.prepareStatement(handle.getSql());

        List<String> childrenIds = new ArrayList<>();

        try {
            stmt.setString(1, id);
            stmt.executeQuery();

            ResultSet results = stmt.getResultSet();

            while (results.next()) {
                childrenIds.add(results.getString("CHILDID"));
            }
            return childrenIds;

        } catch (SQLException e) {
            Logging.logProcess("DatabaseStorage#getChildIdsOnly", "Failed to get children IDs. Returning empty list",
                               Logging.LogLevel.WARN, id, e);
            return childrenIds;
//            throw new IOException("Failed to get getChildIdsOnly for record '" + id, e);
        } finally {
            if (stmt != null) {
                closeStatement(stmt);
            }
            timingGetChildIDsOnly.addNS(System.nanoTime() - startNS);
        }
    }


    /**
     * Returns a list of children.
     *
     * @param id      The parent record ID.
     * @param options The query options to filter the children.
     * @param conn    The connection.
     * @return A list of children to the parent.
     * @throws IOException  If error occur while communication with storage.
     * @throws SQLException If error occur with executing the storage.
     */
    @SuppressWarnings("unused")
    private List<Record> getChildren(
            String id, QueryOptions options, Connection conn) throws IOException, SQLException {
        final long startNS = System.nanoTime();
        // TODO: Use handle directly
        StatementHandle handle = statementHandler.getGetChildren(options);

        List<Record> children = new ArrayList<>(3);
        ResultSetCursor iter = null;

        try (PreparedStatement stmt = conn.prepareStatement(handle.getSql())) {
            stmt.setString(1, id);
            stmt.executeQuery();

            ResultSet results = stmt.getResultSet();
            iter = new ResultSetCursor(this, stmt, results, true);

            while (iter.hasNext()) {
                Record r = iter.next();
                if (options != null && options.allowsRecord(r) || options == null) {
                    children.add(r);
                } else if (log.isTraceEnabled()) {
                    log.trace("Parent record '" + r.getId() + "' not allowed by query options");
                }
            }

            if (log.isTraceEnabled()) {
                log.trace("Looked up children for '" + id + "': " + Strings.join(children, ";"));
            }
            results.close();
            return children;
        } catch (SQLException e) {
            throw new IOException(String.format(Locale.ROOT, "Failed to get children for record '%s': %s", id, e.getMessage()), e);
        } finally {
            if (iter != null) {
                iter.close();
            }
            timingGetChildren.addNS(System.nanoTime() - startNS);
        }
    }

    /**
     * Find all records related to {@code rec} and add them to {@code rec}
     * as parents and children accordingly.
     *
     * @param rec  the record to have its parent and child ids expanded
     *             to real nested records
     * @param conn the SQL connection to use for the lookups
     * @throws SQLException if stuff is bad
     */
    protected void resolveRelatedIds(Record rec, Connection conn, QueryOptions options)
            throws SQLException, IOException {
        final long startNS = System.nanoTime();
        boolean resolveParents = hasAttribute(options, QueryOptions.ATTRIBUTES.PARENTS);
        boolean resolveChildren = hasAttribute(options, QueryOptions.ATTRIBUTES.CHILDREN);
        if (expandRelativesLists || (resolveParents && resolveChildren)) {
            resolveRelatedParentsAndChildrenIDs(rec, conn);
        } else if (!resolveParents && resolveChildren) {
            List<String> cIDs = getChildIdsOnly(rec.getId(), conn);
            if (!cIDs.isEmpty()) {
                rec.setChildIds(cIDs);
            }
        } else if (resolveParents) {
            List<String> pIDs = getParentsIdsOnly(rec.getId(), conn);
            if (!pIDs.isEmpty()) {
                rec.setParentIds(pIDs);
            }
        }
        // Do nothing if none are to be resolved
        timingResolveRelatedIDs.addNS(System.nanoTime() - startNS);
    }

    protected void resolveRelatedParentsAndChildrenIDs(Record rec, Connection conn) throws SQLException {
        if (log.isTraceEnabled()) {
            log.trace("Preparing to resolve parent and child relations for " + rec.getId());
        }
        // TODO: Use handle directly
        StatementHandle handle = statementHandler.getRelatedIds();
        PreparedStatement stmt = conn.prepareStatement(handle.getSql());
        List<String> parentIds = new LinkedList<>();
        List<String> childIds = new LinkedList<>();

        if (log.isTraceEnabled()) {
            log.trace("Querying relations for " + rec.getId());
        }

        try {
            stmt.setString(1, rec.getId());
            stmt.setString(2, rec.getId());
            stmt.executeQuery();
            ResultSet results = stmt.getResultSet();

            // Collect all relations from the result set. Note that each row
            // will mention rec.getId() in one of the columns and we don't want
            // to list rec as a parent or child of itself
            while (results.next()) {
                String parentId = results.getString(1);
                String childId = results.getString(2);

                if (parentId != null && !"".equals(parentId) && !rec.getId().equals(parentId)) {
                    parentIds.add(parentId);
                }

                if (childId != null && !"".equals(childId) && !rec.getId().equals(childId)) {
                    childIds.add(childId);
                }
            }
            results.close();
        } catch (SQLException e) {
            log.warn("Failed to resolve related ids for " + rec.getId(), e);
        } finally {
            closeStatement(stmt);
        }

        if (log.isTraceEnabled()) {
            if (parentIds.isEmpty() && childIds.isEmpty()) {
                log.trace("No relations for " + rec.getId());
            } else {
                log.trace("Found relations for " + rec.getId() + ": Parents[" + Strings.join(parentIds, ",") + "] "
                          + " Children[" + Strings.join(childIds, ",") + "]");
            }
        }

        // Update the record
        if (parentIds.isEmpty()) {
            rec.setParentIds(null);
        } else {
            rec.setParentIds(parentIds);
        }

        if (childIds.isEmpty()) {
            rec.setChildIds(null);
        } else {
            rec.setChildIds(childIds);
        }
    }

    /**
     * The synchronized modifier on clearBase() is paramount to have the
     * TRANSACTION_READ_UNCOMMITTED transaction isolation level work properly,
     * in a nutshell this is the only isolation level that gives us the
     * throughput we want
     *
     * @param base The records base.
     */
    @Override
    public synchronized void clearBase(String base) throws IOException {
        timingClearBase.start();
        log.debug(String.format(Locale.ROOT, "clearBase(%s) called", base));
        Connection conn = null;

        if (base == null) {
            throw new NullPointerException("Can not clear base 'null'");
        }

        try {
            conn = getDefaultConnection();
            clearBaseWithConnection(base, conn);
        } catch (SQLException e) {
            String msg = "Error clearing base '" + base + "'";
            log.error(msg, e);
            throw new IOException(msg, e);
        } finally {
            closeConnection(conn);
            timingClearBase.stop();
        }
    }

    /**
     * Clears the given base, on a connection.
     *
     * @param base The base.
     * @param conn The connection.
     * @throws IOException  If error occur while communicating with database
     *                      storage.
     * @throws SQLException If error executing the SQL.
     */
    private void clearBaseWithConnection(String base, Connection conn) throws IOException, SQLException {
        long startTime = System.nanoTime();
        log.info("Clearing base '" + base + "'");

        final int _ID = 1;
        final int _MTIME = 2;
        final int _DELETED = 3;
        // TODO: Reinstate old
        //String sql = "SELECT id, mtime, deleted  FROM " + RECORDS + " WHERE " + BASE_COLUMN + " = ? AND "
        //             + MTIME_COLUMN + " > ? AND " + MTIME_COLUMN + " < ? AND " + DELETED_COLUMN + " = 0";
        String sql = "SELECT id, mtime, deleted  FROM " + RECORDS + " WHERE " + BASE_COLUMN + " = ? AND "
                     + MTIME_COLUMN + " > ? AND " + DELETED_COLUMN + " = 0 ORDER BY MTIME ASC ";
        sql = getPagingStatement(sql, true);

        // TODO: Remove param
        PreparedStatement stmt = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        // Set the statement up for fetching of large result sets, see fx.
        // http://jdbc.postgresql.org/documentation/83/query.html#query-with-cursor
        // This prevents an OOM for backends like Postgres
        try {
            // TODO: Remove
            //stmt.setFetchDirection(ResultSet.FETCH_FORWARD);

            assignFetchSize(stmt, false);
        } catch (SQLException e) {
            log.error("Error setting fetchdirection and size");
            closeStatement(stmt);
            throw new IOException("Error preparing connection for clearing base '" + base + "': " + e.getMessage(), e);
        }

        // Convert time to the internal binary format used by DatabaseStorage
        long lastMtimeTimestamp = UniqueTimestampGenerator.baseTimestamp(0);
        //        long startTimestamp = timestampGenerator.next();
        String id = null;
        long totalCount = 0;
        long pageCount = pageSizeUpdate;
        try {
            // Page through all records in base and mark them as deleted
            // in one transaction
            while (pageCount >= pageSizeUpdate) {
                log.debug(String.format(Locale.ROOT,
                        "Preparing page for deletion on base '%s' for records from %s and onwards",
                        base, timestampGenerator.formatTimestamp(lastMtimeTimestamp)));
                pageCount = 0;
                stmt.setString(1, base);
               System.out.println("sql:"+sql  + " :"+ lastMtimeTimestamp);
                stmt.setLong(2, lastMtimeTimestamp);
//                stmt.setLong(3, startTimestamp);
                stmt.execute();
                ResultSet cursor = stmt.getResultSet();
                while (cursor.next()) {
                    // We read the data before we start updating the row, not
                    // all JDBC backends like if we update the row before we
                    // read it
  
                    id = cursor.getString(_ID);
                    lastMtimeTimestamp = cursor.getLong(_MTIME);

                    System.out.println(id);
                    cursor.updateInt(_DELETED, 1);
                    cursor.updateLong(_MTIME, timestampGenerator.next());
                    cursor.updateRow();
                    totalCount++;
                    pageCount++;
                    if ((totalCount & 0x3FF) == 0) { // Every 1024
                        log.debug("Deleted record #" + totalCount + ". Latest deleted Record: " + id);
                    }
                    log.trace("Deleted " + id);
                }
                log.trace("Closing cursor");
                cursor.close();
                log.debug("Committing at update " + totalCount);
                stmt.getConnection().commit();
            }
            log.debug("Committing (last)");
            stmt.getConnection().commit();
            long ns = (System.nanoTime()-startTime);
            log.info(String.format(Locale.ROOT,
                    "Cleared base '%s' in %d ms. Marked %d records as deleted at %.2f records/s. Last record ID was %s",
                    base, ns/1000000, totalCount, 1.0*totalCount/(ns/1000000.0), id));
        } catch (SQLException e) {
            String msg =
                    "Error clearing base '" + base + "' after " + totalCount + " records, last record id was '" + id
                    + "': " + e.getMessage();
            log.error(msg, e);
            stmt.getConnection().rollback();
            throw new IOException(msg, e);
        } finally {
            closeStatement(stmt);
        }

        // This will only be reached if the clear was successful
        log.info("Clear for base " + base + " successfull, updating overall timestamps and statistics");
        updateBaseMTimeAndStats(base);
    }

    public void updateBaseMTimeAndStats(String base) throws IOException {
        log.debug("Updating base mtime");
        try {
            updateLastModficationTimeForBase(base);
            updateModificationTime(base);
            invalidateCachedStats();
        } catch (SQLException e) {
            throw new IOException("Exception updating stats", e);
        }
    }

    /**
     * @param jobName  The name of the job to instantiate.
     *                 The job name must match the regular expression
     *                 {@code [a-zA-z_-]+.job.[a-zA-z_-]+} and correspond to a
     *                 resource in the classpath of the storage process.
     *                 eq. {@code count.job.js}
     * @param base     Restrict the batch jobs to records in this base. If
     *                 {@code base} is {@code null} the records from all bases will
     *                 be included in the batch job
     * @param minMtime Only records with modification times strictly greater
     *                 than {@code minMtime} will be included in the batch job
     * @param maxMtime Only records with modification times strictly less than
     *                 {@code maxMtime} will be included in the batch job
     * @param options  Restrict to records for which
     *                 {@link QueryOptions#allowsRecord} returns true
     * @return The result of the batch job run.
     * @throws IOException
     */
    @Override
    public synchronized String batchJob(
            String jobName, String base, long minMtime, long maxMtime, QueryOptions options) throws IOException {
        final long startNS = System.nanoTime();
        timingBatchJob.start();
        if (INTERNAL_BATCH_JOB.equals(jobName)) {
            jobName = options.meta(INTERNAL_JOB_NAME);
            if (jobName == null) {
                throw new IllegalArgumentException("The QueryOptions.meta-property " + INTERNAL_JOB_NAME
                                                   + " must be specified for internal jobs");
            }
            log.info(String.format(Locale.ROOT,
                    "Starting internal batch job: %s, Base: %s, Min mtime: %s, Max mtime: %s, Query options: %s",
                    jobName, base, minMtime, maxMtime, options));
            String result;
            if ((result = handleInternalBatchJob(
                    jobName, base, minMtime, Math.min(maxMtime, System.currentTimeMillis()), options)) == null) {
                log.info(String.format(Locale.ROOT, "Batch job %s completed in %ds",
                                       jobName, (System.nanoTime() - startNS) / 1000000000L));
            } else {
                log.error("Unknown internal batch job " + jobName + " ");
            }
            return result;
        }

        log.info(String.format(Locale.ROOT, "\n  Batch job: %s\n  Base: %s\n  Min mtime: %s\n  Max mtime: %s\n  Query options: %s",
                               jobName, base, minMtime, maxMtime, options));
        Connection conn = null;

        try {
            conn = getDefaultConnection();
            String result = batchJobWithConnection(jobName, base, minMtime, maxMtime, options, conn);
            log.info("Batch job completed in " + (System.nanoTime() - startNS) / 1000000000L + "s");
            return result;
        } catch (SQLException e) {
            String msg = "Error running batch job: " + e.getMessage();
            log.error(msg, e);
            throw new IOException(msg, e);
        } finally {
            closeConnection(conn);
            timingBatchJob.stop();
        }
    }

    /**
     * Process the given job is possible and return the result. If the job is unknown, return null.
     * @param jobName  the name of the internal job.
     * @param base     base-limiter.
     * @param minMtime time-limiter.
     * @param maxMtime time-limiter.
     * @param options  custom options.
     * @return the result of the job or null if the job is unknown.
     */
    protected String handleInternalBatchJob(
            String jobName, String base, long minMtime, long maxMtime, QueryOptions options) {
        return null;
    }

    /**
     * Performs a scripted batch job on the database.
     * @param conn     The database connection.
     * @param jobName  The name of the job to instantiate.
     *                 The job name must match the regular expression
     *                 {@code [a-zA-z_-]+.job.[a-zA-z_-]+} and correspond to a
     *                 resource in the classpath of the storage process.
     *                 eg. {@code count.job.js}
     * @param base     Restrict the batch jobs to records in this base. If
     *                 {@code base} is {@code null} the records from all bases will
     *                 be included in the batch job
     * @param minMtime Only records with modification times strictly greater
     *                 than {@code minMtime} will be included in the batch job.
     * @param maxMtime Only records with modification times strictly less than {@code maxMtime} will be included in the
     *                 batch job.
     *                 Important: In order to guard against re-processing of Records due to changed mtime, the maxTime
     *                 will be modified to Math.min(maxMTime, now) before execution. Thus it is impossible to perform
     *                 jobs on Records with MTime in the future.
     * @param options  Restrict to records for which
     *                 {@link QueryOptions#allowsRecord} returns true
     * @return Output from batch job.
     * @throws IOException  if any error when preparing for or running batch
     *                      job.
     * @throws SQLException if any error occur while closing SQL connection.
     */
    private String batchJobWithConnection(String jobName, String base, long minMtime, long maxMtime,
                                          QueryOptions options, Connection conn) throws IOException, SQLException {
        maxMtime = Math.min(maxMtime, System.currentTimeMillis());
        // Make sure options is always != null to ease logic later
        options = options != null ? options : new QueryOptions();

        // TODO: Use handle directly
        StatementHandle handle = statementHandler.getBatchJob(options, base);
        PreparedStatement stmt = conn.prepareStatement(
                handle.getSql(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        /*        String sql = "SELECT " + allColumns
                + " FROM " + RECORDS
                + " WHERE ( mtime<? AND mtime>? )";

        if (base != null) {
            sql += " AND base=?";
        }

        sql += " ORDER BY " + MTIME_COLUMN;

        if (usePagingModel) {
            sql = getPagingStatement(sql);
        }

        PreparedStatement stmt = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
         */
        // Set the statement up for fetching of large result sets, see fx.
        // http://jdbc.postgresql.org/documentation/83/query.html#query-with-cursor
        // This prevents an OOM for backends like Postgres
        try {
            stmt.setFetchDirection(ResultSet.FETCH_FORWARD);

            assignFetchSize(stmt, false);
        } catch (SQLException e) {
            log.error("Error setting fetch size/direction");
            closeStatement(stmt);
            throw new IOException("Error preparing connection for batch job", e);
        }

        BatchJob job;
        try {
            job = new BatchJob(jobName, log, base, minMtime, maxMtime, options);
        } catch (ScriptException e) {
            throw new IOException("Error creating batch job '" + jobName + "'", e);
        }

        // Convert time to the internal binary format used by DatabaseStorage
        long maxTimestamp = timestampGenerator.baseTimestamp(
                maxMtime > UniqueTimestampGenerator.MAX_TIME ? UniqueTimestampGenerator.MAX_TIME : maxMtime);
        long minTimestamp = timestampGenerator.baseTimestamp(
                minMtime > UniqueTimestampGenerator.MAX_TIME ? UniqueTimestampGenerator.MAX_TIME : minMtime);
        long totalCount = 0;
        long pageCount = getPageSizeUpdate();
        String previousRecordId = null;
        Record previousRecord = null;
        AtomicBoolean invalidateCachedStats = new AtomicBoolean(false);
        Set<String> modifiedBases = new HashSet<>();

        try {
            // Page through all records in the result set in one transaction
            // We apply the job to the previous record so that we can detect
            // when we are at the last record and set 'last' variable in
            // the batch job context correctly upon the last iteration. This
            // is also why we need the odd -1 in the while-condition below
            while (pageCount >= getPageSizeUpdate() - 1) {
                pageCount = 0;
                stmt.setLong(1, maxTimestamp);
                stmt.setLong(2, minTimestamp);
                if (base != null) {
                    stmt.setString(3, base);
                }
                stmt.execute();
                ResultSet cursor = stmt.getResultSet();

                // Step into the result set if there are any results
                if (cursor.next()) {
                    // Read the current page. Note that we must track
                    // the record id since it's in effect a new record
                    // if it's changed (we must purge the old one).
                    while (!cursor.isAfterLast()) {
                        minTimestamp = cursor.getLong(MTIME_COLUMN);
                        Record record = scanRecord(cursor, options); // advances cursor
                        //System.out.println("Processing " + record);
                        if (previousRecord != null
                            && applyJobToRecord(job, previousRecord, previousRecordId, base, options,
                                                totalCount == 0, false, conn, invalidateCachedStats, modifiedBases)) {
                            totalCount++;
                            pageCount++;
                        }
                        previousRecord = record;
                        previousRecordId = record.getId();
                    }
                    cursor.close();
                }
            }

            // The last iteration, now with last=true
            if (previousRecord != null) {
                applyJobToRecord(job, previousRecord, previousRecordId, base, options, totalCount == 0, true, conn,
                                 invalidateCachedStats, modifiedBases);
            }
            if (invalidateCachedStats.get()) {
                log.debug("Invalidating stats caches as some records were modified due by the job '" + jobName + "'");
                invalidateCachedStats();
                for (String modifiedBase: modifiedBases) {
                    updateModificationTime(modifiedBase);
                }
            }
            // Commit the full transaction
            // FIXME: It would probably save memory to do incremental commits
            stmt.getConnection().commit();
        } catch (SQLException e) {
            String msg = String.format(Locale.ROOT, "Error running batch job '%s': %s", job, e.getMessage());
            log.error(msg, e);
            stmt.getConnection().rollback();
            throw new IOException(msg, e);
        } finally {
            closeStatement(stmt);
        }

        return job.getOutput();
    }

    private void assignFetchSize(PreparedStatement stmt, boolean readOnly) throws SQLException {
        if (usePagingModel) {
            stmt.setFetchSize(readOnly ? pageSize : pageSizeUpdate);
        } else {
            stmt.setFetchSize(FETCH_SIZE);
        }
    }

    /**
     * Run a BatchJob on a given record. Returns false if the job was not
     * run because the record was filtered by the query options.
     *
     * @param job         The batch job to run.
     * @param record      The record to run the batch job on.
     * @param oldRecordId The old record ID.
     * @param jobBase     The job record base.
     * @param options     The query options, used to validate the records.
     * @param isFirst     True if it is the first record.
     * @param isLast      True if it is the last record.
     * @param conn        The database connection.
     * @return False if the batch job wasn't runned because the record was filtered out.
     */
    private boolean applyJobToRecord(
            BatchJob job, Record record, String oldRecordId, String jobBase, QueryOptions options, boolean isFirst,
            boolean isLast, Connection conn, AtomicBoolean invalidateStats, Set<String> changedBases)
            throws SQLException, IOException {

        if (!options.allowsRecord(record)) {
            return false;
        }
        timingApplyJobToRecord.stop(); // Always called inside synchronized

        // Set up the batch job context and run it
        log.debug(String.format(Locale.ROOT, "Running batch job '%s' on '%s'", job, record.getId()));
        job.setContext(record, isFirst, isLast);
        boolean preDeleted = record.isDeleted();
        try {
            job.eval();
        } catch (ScriptException e) {
            throw new IOException(String.format(Locale.ROOT, "Error running batch job '%s': %s", job, e.getMessage()), e);
        }
        if (job.shouldCommit()) {
            // If the record id has changed we must flush() the
            // new record (in order to insert/update it) and then
            // delete the old one from the DB
            if (oldRecordId.equals(record.getId())) {
                updateRecordWithConnection(record, options, conn);
            } else {
                log.debug(String.format(Locale.ROOT, "Record renamed '%s' -> '%s'", oldRecordId, record.getId()));
                flushWithConnection(record, options, conn);
                PreparedStatement delete = conn.prepareStatement(
                        "DELETE FROM " + RECORDS + " WHERE " + ID_COLUMN + "=?");
                delete.setString(1, oldRecordId);
                delete.executeUpdate();
            }

            // Mark all caches as dirty
            invalidateStats.set(true);
            changedBases.add(record.getBase());
            //invalidateCachedStats();
            //updateModificationTime(record.getBase());
            if (jobBase != null && !jobBase.equals(record.getBase())) {
                // The record base was changed by the batch job
                updateModificationTime(jobBase);
            }
        }
        timingApplyJobToRecord.stop();
        return true;
    }


    private boolean relationExist(Connection conn, String parentId,String childId)  throws SQLException{
        final long startNS = System.nanoTime();
        StatementHandle handleCheckPC = statementHandler.getParentAndChildCount();
        PreparedStatement stmtCheckPC = conn.prepareStatement(handleCheckPC.getSql());
        stmtCheckPC.setString(1, parentId);
        stmtCheckPC.setString(2, childId);

        ResultSet rs = stmtCheckPC.executeQuery();
        rs.next();
        int number = rs.getInt(1);
        timingRelationsExists.addNS(System.nanoTime() - startNS);
        return number>0;
    }



    private boolean recordExist(Connection conn, String id)  throws SQLException{
        final long startNS = System.nanoTime();

        StatementHandle handleCheckExists = statementHandler.getRecordExist();
        PreparedStatement stmtCheckExists = conn.prepareStatement(handleCheckExists.getSql());
        stmtCheckExists.setString(1, id);

        ResultSet rs = stmtCheckExists.executeQuery();
        rs.next();
        int number = rs.getInt(1);
        if (number>0) {
            log.debug("Record already exist:" + id);
        }
        timingRecordExists.addNS(System.nanoTime() - startNS);
        return number>0;
    }

    /**
     * Create parent/child and child/parent relations for the given record.
     *
     * @param rec  The record.
     * @param conn The database connection.
     */
    private void createRelations(Record rec, Connection conn) throws SQLException {
        final long startNS = System.nanoTime();
        // TODO: Use handle directly
        StatementHandle handle = statementHandler.getCreateRelation();
        PreparedStatement stmt = conn.prepareStatement(handle.getSql());
        if (rec.hasChildren()) {
            List<String> childIds = rec.getChildIds();

            for (String childId : childIds) {
                if (log.isDebugEnabled()) {
                    log.debug("Creating relation: " + rec.getId() + " -> " + childId);
                }
                boolean exist = relationExist(conn, rec.getId(), childId);


                stmt.setString(1, rec.getId());
                stmt.setString(2, childId);

                try {
                    if (!exist){
                        stmt.executeUpdate();
                    }

                } catch (SQLException e) {
                    closeStatement(stmt);
                    throw new SQLException(String.format(Locale.ROOT, "Error creating child relations for %s", rec.getId()), e);

                }
            }

            // Make sure that all children are tagged as having relations,
            // unless the record is excepted from relations tracking
            if (shouldTrackRelations(rec)) {
                markHasRelations(childIds, conn);
            }
        }


        if (rec.hasParents()) {
            List<String> parentIds = rec.getParentIds();

            for (String parentId : parentIds) {
                if (log.isDebugEnabled()) {
                    log.debug("Creating relation: " + parentId + " -> " + rec.getId());
                }
                boolean exist = relationExist(conn, parentId,rec.getId());


                stmt.setString(1, parentId);
                stmt.setString(2, rec.getId());

                try {
                    if (!exist){
                        stmt.executeUpdate();
                    }
                } catch (SQLException e) {
                    closeStatement(stmt);
                    throw new SQLException("Error creating parent relations for " + rec.getId(), e);
                }
            }

            // Make sure that all parents are tagged as having relations
            // unless the record is excepted from relations tracking
            if (shouldTrackRelations(rec)) {
                markHasRelations(parentIds, conn);
            }
        }
        closeStatement(stmt);
        timingCreateRelations.addNS(System.nanoTime()-startNS);
    }

    /**
     * Return false if the record should be excepted from relations tracking.
     *
     * @param rec The record.
     * @return False if the record should be excepted from relations tracking.
     */
    private boolean shouldTrackRelations(Record rec) {
        return !disabledRelationsTracking.contains(rec.getBase());
    }

    /**
     * Set the {@code hasRelations} column to {@code true} on the listed
     * childIds.
     *
     * @param childIds children ID's.
     * @param conn     the database connection.
     */
    private void markHasRelations(List<String> childIds, Connection conn) {
        // We can't use a PreparedStatement here because the parameter list
        // to the IN clause is of varying length
        // TODO: Push this to StatementHandler
        String sql = "UPDATE " + RECORDS + " SET " + HAS_RELATIONS_COLUMN + "=" + boolToInt(true) + " WHERE id IN ('"
                     + Strings.join(childIds, "','") + "')";

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            log.warn("Failed to mark " + Strings.join(childIds, ", ") + " as having relations: " + e.getMessage(), e);
        } finally {
            closeStatement(stmt);
        }
    }

    /**
     * Updates has relation for a record ID.
     *
     * @param id   The record ID.
     * @param conn The connection to the database.
     * @throws SQLException If error occur with SQL execution.
     */
    private void checkHasRelations(String id, Connection conn) throws SQLException {
        final long startNS = System.nanoTime();
        // TODO: Use handle directly
        StatementHandle handle = statementHandler.getRelatedIds();
        PreparedStatement stmt = conn.prepareStatement(handle.getSql());

        boolean hasRelations = false;

        log.debug("Checking relations for: " + id);

        /* Detect if we have any relations for 'id' */
        try {
            stmt.setString(1, id);
            stmt.setString(2, id);
            stmt.executeQuery();
            ResultSet results = stmt.getResultSet();

            if (results.next()) {
                hasRelations = true;
            }

            results.close();
        } catch (SQLException e) {
            log.warn("Failed to check relations for " + id, e);
        } finally {
            closeStatement(stmt);
        }

        /* Return if we don't have any relations */
        if (!hasRelations) {
            if (log.isTraceEnabled()) {
                log.trace("No relations for record " + id);
            }
            timingCheckHasRelations.addNS(System.nanoTime()-startNS);
            return;
        }

        /* Set a check mark in the hasRelations column */
        log.debug("Marking " + id + " as having relations");
        try {
            // TODO: Use handle directly
            StatementHandle handleMark = statementHandler.getMarkHasRelations();
            stmt = conn.prepareStatement(handleMark.getSql());
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.warn("Failed to mark " + id + " as having relations: " + e.getMessage(), e);
        } finally {
            closeStatement(stmt);
            timingCheckHasRelations.addNS(System.nanoTime()-startNS);
        }
    }

    /**
     * Create a new record on the given connection.
     *
     * @param record  The record object to flush into the database.
     * @param options The query options.
     * @param conn    The database connection.
     * @throws IOException  If error occur while communicating with storage.
     * @throws SQLException If error occur with SQL execution.
     */
    private void createNewRecordWithConnection(Record record, QueryOptions options,
                                               Connection conn) throws IOException, SQLException {
        final long startNS = System.nanoTime();
        if (log.isTraceEnabled()) {
            log.trace("Creating: " + record.getId());
        }

        // TODO: Use handle directly
        StatementHandle handle = statementHandler.getCreateRecord();
        PreparedStatement stmt = conn.prepareStatement(handle.getSql());

        long nowStamp = timestampGenerator.next();
        boolean hasRelations = record.hasParents() || record.hasChildren();

        // TODO: Optimize with options to allow partial updates (e.g. ID + DELETED + INDEXABLE + MTIME)
        try {
            stmt.setString(ID_KEY, record.getId());
            stmt.setString(BASE_KEY, record.getBase());
            stmt.setInt(DELETED_FLAG_KEY, boolToInt(record.isDeleted()));
            stmt.setInt(INDEXABLE_FLAG_KEY, boolToInt(record.isIndexable()));
            stmt.setInt(HAS_RELATIONS_FLAG_KEY, boolToInt(hasRelations));
            stmt.setLong(6, nowStamp);
            stmt.setLong(7, nowStamp);
            // Content might already be compressed but _must_ always be compressed in storage
            stmt.setBytes(8, record.isContentCompressed() ?
                    record.getContent(false) :
                    Zips.gzipBuffer(record.getContent()));
            storeRelativeIDs(record);
            stmt.setBytes(9, record.hasMeta() ? record.getMeta().toFormalBytes() : new byte[0]);
            stmt.executeUpdate();
        } finally {
            closeStatement(stmt);
        }

        if (hasRelations) {
            createRelations(record, conn);
        } else {
            // If the record does not have explicit relations we have to check
            // if we know any relations for it already, unless the record
            // is excepted from relations tracking of course
            if (shouldTrackRelations(record)) {
                checkHasRelations(record.getId(), conn);
            }
        }
        timingCreateNewRecordWithConnection.addNS(System.nanoTime()-startNS);
    }

    /**
     * Sets a base statistic row invalid, this is done when a record is flushed,
     * because the ingester don't know when it is done ingesting and therefore
     * don't know when to update the deleted, indexable and count.
     *
     * @param base The base to set invalid.
     * @param conn The connection.
     */
    private void setBaseStatisticInvalid(String base, Connection conn) throws SQLException {
        // TODO: Use handle directly
        StatementHandle handle = statementHandler.getSetBasetatsInvalid();
        PreparedStatement stmt = conn.prepareStatement(handle.getSql());
        try {
            stmt.setString(1, base);
            stmt.execute();
            conn.commit();
        } catch (SQLException e) {
            log.warn("setBaseStatisticInvalid(" + base + ", ...) failed. Rolling back");
            conn.rollback();
            throw new SQLException("setBaseStatisticInvalid(" + base + ", ...) failed", e);
        } finally {
            closeStatement(stmt);
        }
    }

    /**
     * Update the last modification time for a single base.
     *
     * @param base The base.
     * @throws SQLException If error occur while executing SQL.
     */
    public void updateLastModficationTimeForBase(String base) throws SQLException {
        final long startNS = System.nanoTime();

        long mtime = System.currentTimeMillis();
        Connection conn = getTransactionalConnection();
        log.debug("updateLastModficationTimeForBase: Updating mtime for base '" + base + " to " + mtime);
        // TODO: Use handle directly
        StatementHandle handle = statementHandler.getUpdateMtimeForBase();
        log.debug("updateLastModficationTimeForBase: Preparing statement");
        PreparedStatement stmt = conn.prepareStatement(handle.getSql());
        PreparedStatement insertStmt = null;
        try {
            // update mtime and base
            stmt.setLong(1, mtime);
            stmt.setString(2, base);
            log.debug("updateLastModficationTimeForBase: Executing statement");
            if (stmt.executeUpdate() == 0) { // This is a new base, update
                // insert base and mtime
                log.debug("Base row didn't exists in " + BASE_STATISTICS + " insert new row");
                // TODO: Use handle directly
                StatementHandle handleBaseStats = statementHandler.getInsertBaseStats();
                insertStmt = conn.prepareStatement(handleBaseStats.getSql());
                insertStmt.setString(1, base);
                insertStmt.setLong(2, mtime);
                insertStmt.execute();
            }
            log.debug("updateLastModficationTimeForBase: Committing");
            conn.commit();
        } catch (SQLException e) {
            log.warn("updateLastModficationTimeForBase(" + base + ", ...) failed. Rolling back");
            conn.rollback();
            throw new SQLException("updateLastModficationTimeForBase(" + base + ", ...) failed", e);
        } finally {
            log.debug("updateLastModficationTimeForBase: Closing statement");
            closeStatement(stmt);
            closeStatement(insertStmt);
            conn.close();
            timingUpdateLastModficationTimeForBase.addNS(System.nanoTime()-startNS);
        }
    }

    protected boolean isConnectionValid(Connection conn) {
        try {
            return conn.isValid(IS_CONNECTION_VALID_TIMEOUT_SECONDS);
        } catch (Exception e) {
            log.warn("isConnectionValid: Exception which calling connection-isValid()", e);
            return false;
        }
    }

    /**
     * Update a record.
     * Create a connection and delegate to updateRecordWithConnection(Record record, QueryOptions options, Connection conn) 
     */

    public void updateRecord(Record record, QueryOptions options) throws IOException, SQLException {
        final long startNS = System.nanoTime();
        Connection conn = null;
        try{
            conn = getTransactionalConnection();
            updateRecordWithConnection(record, options, conn);
            conn.commit();
        }
        catch(Exception e){
            log.error("Error updateRecord for record:"+record.getId());
            throw e;
        } finally {
            closeConnection(conn);
            timingUpdateRecord.addNS(System.nanoTime()-startNS);
        }

    }

    /*
     * Delete all child relations for the record
     * 
     */
    private void clearChildrenWithConnection(String recordID, Connection conn) throws  SQLException, IOException{
        StatementHandle handle = statementHandler.getClearChildren();
        PreparedStatement stmt =  conn.prepareStatement(handle.getSql());
        try{

            stmt.setString(1, recordID);
            int numberCleared = stmt.executeUpdate();
        }
        catch (SQLException e) {
            throw new SQLException("Exception in clearChildrenWithConnection for record:"+recordID,e);
        } finally {
            closeStatement(stmt);
        }
    }

    /*
     *  Delete all parent relations for the record
     * 
     */
    private void clearParentsWithConnection(String recordID, Connection conn) throws  SQLException,IOException{
        StatementHandle handle = statementHandler.getClearParents();
        PreparedStatement stmt =  conn.prepareStatement(handle.getSql());
        try{
            stmt.setString(1, recordID);
            int numberCleared = stmt.executeUpdate();
        }
        catch (Exception e) {
            throw new SQLException("Exception in clearParentsWithConnection for record:"+recordID,e);
        } finally {
            closeStatement(stmt);
        }
    }

    /**
     * Update a record.
     * Note: that creationTime isn't touched
     *
     * @param record  The record to update, the record updated in storage is
     *                depended on the record id.
     * @param options The query options, see {@link QueryOptions} for
     *                documentation.
     * @param conn    The database connection.
     * @throws IOException  If error communicating with storage.
     * @throws SQLException If error executing SQL.
     */
    private void updateRecordWithConnection(
            Record record, QueryOptions options, Connection conn) throws IOException, SQLException {
        final long startNS = System.nanoTime();

        log.debug("Updating: " + record.getId());
        // Respect the TRY_UPDATE meta flag. See docs for {@link QueryOptions}
        if (options != null && "true".equals(options.meta(TRY_UPDATE))) {
            Record old = getRecordWithConnection(record.getId(), options, conn);
            if (record.equals(old)) {
                log.debug("Record '%s' already up to date, skipping update");
                return;
            }
        }

        long nowStamp = timestampGenerator.next();
        boolean hasRelations = record.hasParents() || record.hasChildren();

        // TODO: Use handle directly
        StatementHandle handle = statementHandler.getUpdateRecord();
        PreparedStatement stmt = conn.prepareStatement(handle.getSql());

        try {
            stmt.setString(1, record.getBase());
            stmt.setInt(2, boolToInt(record.isDeleted()));
            stmt.setInt(3, boolToInt(record.isIndexable()));
            stmt.setInt(4, boolToInt(hasRelations));
            stmt.setLong(5, nowStamp);
            // Content might already be compressed but _must_ always be compressed in storage
            stmt.setBytes(6, record.isContentCompressed() ?
                    record.getContent(false) :
                    Zips.gzipBuffer(record.getContent()));
            storeRelativeIDs(record);
            stmt.setBytes(7, record.hasMeta() ? record.getMeta().toFormalBytes() : new byte[0]);
            stmt.setString(8, record.getId());
            stmt.executeUpdate();

            if (stmt.getUpdateCount() == 0) {
                String msg = "The record with id '" + record.getId() + "' was marked as modified, but did not exist in"
                             + " the database";
                log.warn(msg);
                throw new IOException(msg);
            }
        } finally {
            closeStatement(stmt);
        }

        //Clear parent/childs relations
        RELATION clearRelation = this.relationsClear;
        switch (clearRelation){
            case none:
                break;
            case child:
                clearChildrenWithConnection(record.getId(), conn);
            case parent:
                clearParentsWithConnection(record.getId(), conn);
                break;
            case all:
                clearChildrenWithConnection(record.getId(), conn);
                clearParentsWithConnection(record.getId(), conn);
                break;
        }

        if (hasRelations) {
            createRelations(record, conn);
        } else {
            // If the record does not have explicit relations we have to check
            // if we know any relations for it already, unless the record
            // is excepted from relations tracking of course
            if (shouldTrackRelations(record)) {
                checkHasRelations(record.getId(), conn);
            }
        }
        timingUpdateRecordWithConnection.addNS(System.nanoTime()-startNS);
    }

    /**
     * Touch a record means updating the records timestamp.
     *
     * @param id   The record ID.
     * @param conn The database connection, changes are not committed.
     * @param updateStats if true, this method will update database statistics (this can take > 100 ms), if false
     *                    the statistics should be updated by the caller.
     * @throws IOException  If error occurs while communication with the storage.
     * @throws SQLException If error occur while executing the SQL.
     */
    protected void touchRecord(String id, Connection conn, boolean updateStats) throws IOException, SQLException {
        // TODO: Use handle directly
        final long startNS = System.nanoTime();
        long pointTime = System.nanoTime();
        StatementHandle handle = statementHandler.getTouchRecord();
        PreparedStatement stmt = conn.prepareStatement(handle.getSql());
        final long prepareStatementTime = System.nanoTime()-pointTime;
        Record r = null;

        pointTime = System.nanoTime();
        long executeTime;
        final long closeTime;
        try {
            stmt.setLong(1, timestampGenerator.next());
            stmt.setString(2, id);
            stmt.executeUpdate();
            executeTime = System.nanoTime()-pointTime;
            if (updateStats) {
                r = getRecordWithConnection(id, null, conn);
                if (r != null) {
                    updateLastModficationTimeForBase(r.getBase());
                }
            }
        } catch (NullPointerException e) {
            log.warn("touch(" + id + ") failed as the retrieved Record was " + r);
            executeTime = System.nanoTime()-pointTime;
        } finally {
            pointTime = System.nanoTime();
            closeStatement(stmt);
            closeTime = System.nanoTime()-pointTime;
        }
        if (updateStats) {
            invalidateCachedStats();
        }

        if (log.isDebugEnabled()) {
            log.debug("Touched: " + id + " in "
                      + prepareStatementTime + " prepareNS + "
                      + executeTime + " executeNS + "
                      + closeTime + " closeNS = " + (prepareStatementTime+executeTime+closeTime)
                      + ". updateStats=" + (updateStats ? "true, but not counted in time spend" : "false"));
        }
        timingTouchRecord.addNS(System.nanoTime()-startNS);
    }


    /**
     * Return the last modification time for a base or the storage, if no
     * changes to a base has happened yet. This is drawn from the database.
     *
     * @param base The base.
     * @return The last modification time.
     * @throws IOException If error occur while executing the SQL.
     */
    @Override
    public long getModificationTime(String base) throws IOException {
        final long startNS = System.nanoTime();
        Connection conn = null;
        try {
            conn = getDefaultConnection();
            conn.commit();
            return getModifcationTimeWithConnection(base, conn);
        } catch (SQLException e) {
            throw new IOException("Error fetching last modification time", e);
        } finally {
            closeConnection(conn);
            timingGetModificationTime.addNS(System.nanoTime()-startNS);
        }
    }

    /**
     * Return the last modification time for a single base. If base is
     * {@code null} then the last modification the for the storage is returned.
     * If not changes has been made yet, then this is the storage start time
     * that is returned.
     *
     * @param base The base.
     * @param conn The connection, the SQL isn't committed in this method.
     * @return The last modification time of the storage or storage startup
     *         time.
     * @throws SQLException If error executing the SQL.
     */
    private long getModifcationTimeWithConnection(String base, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet resultSet = null;
        try {
            if (base == null) {
                // TODO: Move this to StatementHandler
                String sql = "SELECT " + MTIME_COLUMN + " FROM " + BASE_STATISTICS + " ORDER BY " + MTIME_COLUMN
                             + " DESC LIMIT 1";
                stmt = conn.prepareStatement(sql);
                stmt.execute();
                long maxTime = 0;
                resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    maxTime = resultSet.getLong(1);
                }
                return Math.max(getStorageStartTime(), maxTime);
            } else {
                // TODO: Use the handle directly
                StatementHandle handle = statementHandler.getGetLastModificationTime();
                stmt = conn.prepareStatement(handle.getSql());
                stmt.setString(1, base);
                stmt.execute();
                // We don't have a storage match
                resultSet = stmt.getResultSet();
                if (resultSet.next()) {
                    return resultSet.getLong(resultSet.findColumn(MTIME_COLUMN));
                } else {
                    long time = 0;
                    try {
                        time = super.getModificationTime(base);
                    } catch (IOException e) {
                        log.warn("Unable to get modification time for base '" + base + "' from super. Assuming 0");
                    }
                    log.debug("No data for base, returning storage start time (" + getStorageStartTime()
                              + ", should have been " + time + ")");
                    return getStorageStartTime();
                }
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            closeStatement(stmt);
        }
    }

    /**
     * WARNING: <i>This will remove all data from the storage!</i>.
     * Destroys and removes all table definitions from the underlying database.
     * Caveat emptor.
     *
     * @throws SQLException if there are problems executing the required SQL
     *                      statements
     */
    public void destroyDatabase() throws SQLException {
        log.warn("Preparing to destroy database. All data will be lost");
        Connection conn = getDefaultConnection();
        try {
            log.warn("Destroying all record data");
            Statement stmt = conn.createStatement();
            stmt.execute("DROP TABLE " + RECORDS);
            stmt.close();

            log.warn("Destroying all relations");
            stmt = conn.createStatement();
            stmt.execute("DROP TABLE " + RELATIONS);
            stmt.close();

            destroyBaseStatistic();
        } finally {
            closeConnection(conn);
        }
        log.info("All Summa data wiped from database");
    }

    /**
     * Destroy the base statistic table.
     */
    protected void destroyBaseStatistic() {
        Connection conn = null;
        try {
            conn = getDefaultConnection();
            log.warn("Destryoing all statistic");
            Statement stmt = conn.createStatement();
            stmt.execute("DROP TABLE " + BASE_STATISTICS);
            stmt.close();
        } catch (SQLException e) {
            log.info("Base statistic table not deleted reason: " + e.getCause() + "'", e);
        } finally {
            closeConnection(conn);
        }
    }


    protected abstract String getMetaColumnDataDeclaration();

    protected abstract String getDataColumnDataDeclaration();

    private long dummyID = 0;
    private long dummyBase = 0;

    /**
     * Extract elements from a SQL result set and create a Record from these elements.
     * <p/>
     * This method will position the result set at the beginning of the next record.
     *
     * @param resultSet a SQL result set. The result set will be stepped to the beginning of the following record.
     * @param iter      a result iterator on which to update record availability via
     *                  {@link ResultSetCursor#setResultSetHasNext}.
     * @return a Record based on the result set.
     * @throws SQLException if there was a problem extracting values from the SQL result set.
     * @throws IOException  If the data (content) could not be uncompressed with gunzip.
     */
    public Record scanRecord(ResultSet resultSet, ResultSetCursor iter, QueryOptions options)
            throws SQLException, IOException {
        boolean hasNext;
        long start = System.nanoTime();
        String id = resultSet.getString(ID_KEY);

        if ("".equals(id)) {
            id = "dummy" + dummyID++;
            log.debug("No ID found in scanRecord, substituting with dummy value '" + id + "'");
        }
        String base = resultSet.getString(BASE_KEY);
        if ("".equals(base)) {
            base = "dummy" + dummyBase++;
            log.debug("No base found in scanRecord, substituting with dummy value '" + base + "'");
        }

        boolean deleted = getIntBool(resultSet, DELETED_FLAG_KEY, false);
        boolean indexable = getIntBool(resultSet, INDEXABLE_FLAG_KEY, true);
        boolean hasRelations = getIntBool(resultSet, HAS_RELATIONS_FLAG_KEY, false);
//        log.debug("scanRecord***: Booleans " + (System.nanoTime()-start)/M);
        byte[] gzippedContent = resultSet.getBytes(GZIPPED_CONTENT_FLAG_KEY);
//        log.debug("scanRecord***: content " + (System.nanoTime()-start)/M);
        long ctime = getLong(resultSet, CTIME_KEY, 0);
        long mtime = getLong(resultSet, MTIME_KEY, 0);
        byte[] meta = resultSet.getBytes(META_KEY);
//        log.debug("scanRecord***: meta" + (System.nanoTime()-start)/M);
        // Only possible because the records are joined with relations
        String parentIds = resultSet.getString(PARENT_IDS_KEY);
        String childIds = resultSet.getString(CHILD_IDS_KEY);
//        log.debug("DatabaseStorage***: parent/child ids" + (System.nanoTime()-start)/M + " parents=" + parentIds + ", childIDs=" + childIds);

        if (log.isTraceEnabled()) {
            log.trace("Scanning record: " + id);
        }

        /* If the record is listed as parent or child of something this will
         * appear in the parent/child columns, so ignore these cases.
         * Also if the parent/child ids are empty strings they are injected
         * because we do lazy relation lookups */
        if (id.equals(parentIds) || "".equals(parentIds)) {
            parentIds = null;
        }
        if (id.equals(childIds) || "".equals(childIds)) {
            childIds = null;
        }
        int nextCount = 1;
        /* The way the result is returned from the DB is several identical rows
         * with different parent and child column values. We need to iterate
         * through all rows with the same id and collect the different parents
         * and children listed */
        int relativesCount = 0;
        while ((hasNext = resultSet.next()) && id.equals(resultSet.getString(ID_KEY))) {
            relativesCount++;
            if (!expandRelativesLists) { // TODO: Check if we can skip ahead or maybe limit the results to 1
                continue;
            }
//            log.debug("DatabaseStorage***: next#" + nextCount++ + " " + (System.nanoTime()-start)/M);

            /* If we log on debug we do sanity checking of the result set.
             * Of course the parent and child columns should not be checked,
             * since they are the ones changing */

            if (log.isDebugEnabled() && false) { // TODO: Update the checker to match the getter
                log.trace("Sanity checking record block for: " + id);
                if (!base.equals(resultSet.getString(BASE_KEY))) {
                    log.warn("Base mismatch for record: " + id);
                    return null;
                } else if (deleted != intToBool(resultSet.getInt(DELETED_FLAG_KEY))) {
                    log.warn("Deleted state mismatch for record: " + id);
                    return null;
                } else if (indexable != intToBool(resultSet.getInt(INDEXABLE_FLAG_KEY))) {
                    log.warn("Indexable state mismatch for record: " + id);
                    return null;
                } else if (hasRelations != intToBool(resultSet.getInt(HAS_RELATIONS_FLAG_KEY))) {
                    log.warn("hasRelations state mismatch for record: " + id);
                    return null;
                } else if (!Arrays.equals(gzippedContent, resultSet.getBytes(GZIPPED_CONTENT_FLAG_KEY))) {
                    log.warn("Content mismatch for record: " + id);
                    return null;
                } else if (ctime != resultSet.getLong(CTIME_KEY)) {
                    log.warn("CTime state mismatch for record: " + id);
                    return null;
                } else if (mtime != resultSet.getLong(MTIME_KEY)) {
                    log.warn("MTime state mismatch for record: " + id);
                    return null;
                } else if (!Arrays.equals(meta, resultSet.getBytes(META_KEY))) {
                    log.warn("Meta tags mismatch for record: " + id);
                    return null;
                }
            }

            /* Pick up parent and child IDs */
            String newParent = resultSet.getString(PARENT_IDS_KEY);
            String newChild = resultSet.getString(CHILD_IDS_KEY);
            //          log.debug("DatabaseStorage***: nextrelations " + (System.nanoTime()-start)/M + " parentLength=" + newParent.length() + ", childrenLength=" + newChild.length());

            /* If the record is listed as parent or child of something this
             * will appear in the parent/child columns, so ignore these cases
             */
            if (id.equals(newParent)) {
//                log.debug("DatabaseStorage***: removing newParent(" + newParent + ") due to id equality " + (System.nanoTime()-start)/M);
                newParent = null;
            }
            if (id.equals(newChild)) {
//                log.debug("DatabaseStorage***: removing newChild(" + newChild + ") due to id equality " + (System.nanoTime()-start)/M);
                newChild = null;
            }

            /* Treat empty strings as nulls because we inject empty strings in
             * the result set when we are doing lazy relation lookups */
            if (newParent != null && !"".equals(newParent)) {
                parentIds = parentIds != null ? parentIds + ";" + newParent : newParent;
            }

            if (newChild != null && !"".equals(newChild)) {
                childIds = childIds != null ? childIds + ";" + newChild : newChild;
            }

            if (log.isTraceEnabled()) {
                log.trace("For record '" + id + "', collected children: " + childIds + ", collected parents: "
                          + parentIds);
            }
//            log.debug("DatabaseStorage***: nextRelations assigned " + (System.nanoTime()-start)/M);
        }

        if (iter != null) {
            iter.setResultSetHasNext(hasNext);
            iter.setRecordMtimeTimestamp(mtime);
        }
//        log.debug("scanRecord***: readyfornext " + (System.nanoTime()-start)/M + " with " + relativesCount + " relatives count");

        // We use salted unique timestamps generated by a
        // UniqueTimestampGenerator so we have to extract the system time from
        // the timestamp
        ctime = timestampGenerator.systemTime(ctime);
        mtime = timestampGenerator.systemTime(mtime);

        /* The result set cursor will now be on the start of the next record */

        /* Create a record with gzipped content. The content will be unzipped
         * lazily by the Record class upon access */
        Record rec = new Record(id, base, deleted, indexable, gzippedContent, ctime, mtime,
                                Record.idStringToList(parentIds), Record.idStringToList(childIds),
                                meta.length == 0 ?
                                        null :
                                        StringMap.fromFormal(meta), gzippedContent.length != 0);
        rec.setHasRelations(hasRelations);
//        log.debug("DatabaseStorage***: produced record " + (System.nanoTime()-start)/M);

        /* Only resolve relations if we have to, that is, if
         * "useRelations && hasRelations". Moreover some codepaths will have
         * queried the relations even though useLazyRelations==true so don't
         * resolve the relations in that case
         */
        if (useLazyRelations && hasRelations) { //  && parentIds == null && childIds == null removed as it broke QueryOptions
            resolveRelatedIds(rec, resultSet.getStatement().getConnection(), options);
        }
//        log.debug("DatabaseStorage***: ResolvedRelatedID " + (System.nanoTime()-start)/M);

        return rec;
    }

    private long getLong(ResultSet resultSet, int index, int defaultValue) throws SQLException {
        String content = resultSet.getString(index);
        return "".equals(content) ? defaultValue : resultSet.getLong(index);
    }

    /*
    The int value as a boolean or defaultValue if there was only the empty String.
     */
    private boolean getIntBool(ResultSet resultSet, int index, boolean defaultValue) throws SQLException {
        String content = resultSet.getString(index);
        return "".equals(content) ? defaultValue : intToBool(resultSet.getInt(index));
    }

    /**
     * As {@link #scanRecord(ResultSet, ResultSetCursor, QueryOptions)} with {@code resultSet = null}.
     *
     * @param resultSet a SQL result set. The result set will be stepped to the beginning of the following record.
     * @return a Record based on the result set.
     * @throws SQLException if there was a problem extracting values from the SQL result set.
     * @throws IOException  If the data (content) could not be uncompressed with gunzip.
     */
    public Record scanRecord(ResultSet resultSet, QueryOptions options) throws SQLException, IOException {
        return scanRecord(resultSet, null, options);
    }

    /**
     * Given a query, execute this query and transform the {@link ResultSet} to a {@link ResultSetCursor}.
     *
     * @param stmt    The statement to execute.
     * @param base    The base we are iterating over.
     * @param options Query options.
     * @return a RecordIterator of the result.
     * @throws IOException - also on no getConnection() and SQLExceptions.
     */
    private ResultSetCursor startIterator(
            PreparedStatement stmt, String base, QueryOptions options) throws IOException {
        ResultSet resultSet;

        log.trace("Getting results for '" + stmt + "'");
        long startQuery = System.currentTimeMillis();

        try {
            resultSet = stmt.executeQuery();
            log.debug("Got results from '" + stmt + "' in " + (System.currentTimeMillis() - startQuery) + "ms");
        } catch (SQLException e) {
            log.error("SQLException in startIterator", e);
            throw new IOException("SQLException", e);
        }

        ResultSetCursor iter;
        try {
            iter = new ResultSetCursor(this, stmt, resultSet, base, options);
        } catch (SQLException e) {
            log.error("SQLException creating record iter for base '" + base + "'", e);
            throw new IOException("SQLException creating record iter for base '" + base + "'", e);
        }

        return iter;
    }

    /**
     * Register a {@link Cursor} and return the iterator key for it.
     * Iterators registered with this call will automatically be reaped
     * after periods of inactivity.
     *
     * @param iter the Cursor to register with the DatabaseStorage
     * @return a key to access the iterator remotely via the {@link #next}
     *         methods
     */
    private long registerCursor(Cursor iter) {
        log.trace("registerCursor: Got iter " + iter.getKey() + " adding to iterator list");
        iterators.put(iter.getKey(), iter);
        return iter.getKey();
    }

    /**
     * Our version of a boolean packed as integer is that 0 = false, everything
     * else = true. This should match common practice.
     *
     * @param isTrue Boolean to convert to integer.
     * @return Zero if boolean is false, one otherwise.
     */
    private int boolToInt(boolean isTrue) {
        return isTrue ? 1 : 0;
    }

    /**
     * Convert integer to boolean.
     *
     * @param anInt Integer to convert to boolean.
     * @return True if integer is one, false otherwise.
     * @see #boolToInt(boolean)
     */
    private static boolean intToBool(int anInt) {
        return anInt != 0;
    }

    /**
     * Close the database storage, which involves closing all iterators.
     */
    @Override
    public void close() throws IOException {
        log.info("Closing DatabaseStorage");
        iteratorReaper.stop();
        log.info("DatabaseStorage closed: " + getIterationStats() + ", " + timing);
    }

    /**
     * Returns the statistic of the used storage. This class holds a cached copy of the statistic and don't update.
     *
     * @return List of storage statistic.
     * @throws IOException If error occur while communicating storage.
     */
    public List<BaseStats> getStats() throws IOException {
        Connection conn = getConnection();
        try {
            return getStatsWithConnection(conn);
        } finally {
            closeConnection(conn);
        }
    }

    private List<BaseStats> getStatsWithConnection(Connection conn) throws IOException {
        final long startNS = System.nanoTime();
        log.trace("getStatsWithConnection()");

        String isBaseStatsInvalid = "SELECT * FROM " + BASE_STATISTICS + " WHERE " + VALID_COLUMN + " = 0";
        String isBastStatsAva = "SELECT * FROM " + BASE_STATISTICS;

        try {
            Statement stmt = conn.createStatement();
            Statement available = conn.createStatement();
            boolean resultSetAva = available.execute(isBastStatsAva);

            boolean valid= stmt.execute(isBaseStatsInvalid);
            if (valid && stmt.getResultSet().next() || resultSetAva && !available.getResultSet().next()) {
                log.debug("Return slow statistic");
                return getHeavyStatsWithConnection(conn);
            } else {
                log.debug("Return fast statistic");
                return getFastStatsWithConnection(conn);
            }
        } catch (SQLException e) {
            throw new IOException("Could not get database stats", e);
        } finally {
            timingGetStats.addNS(System.nanoTime()-startNS);
        }
    }

    /**
     * Invalidate cached statistic in database.
     */
    void invalidateCachedStats() throws SQLException {
        final long startNS = System.nanoTime();
        String invalidateCachedStats = "UPDATE " + BASE_STATISTICS + " SET " + VALID_COLUMN + " = 0";
        log.debug("Invalidating cached stats");
        Connection conn = null;
        try {
            conn = getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute(invalidateCachedStats);
            conn.commit();
        } catch (SQLException e) {
            log.warn("invalidateCachedStats() failed. Rolling back");
            conn.rollback();
            throw new SQLException("invalidateCachedStats failed", e);
        } finally {
            closeConnection(conn);
            timingInvalidateStats.addNS(System.nanoTime()-startNS);
        }
    }

    private List<BaseStats> getFastStatsWithConnection(Connection conn) throws SQLException {
        long startTime = System.currentTimeMillis();
        List<BaseStats> stats = new LinkedList<>();

        String query = "SELECT * FROM " + BASE_STATISTICS;
        Statement stmt = conn.createStatement();
        ResultSet results = stmt.executeQuery(query);

        try {
            while(results.next()){
                String baseName = results.getString(1);
                long lastModified = results.getLong(2);
                long deletedIndexables = results.getLong(3);
                long nonDeletedIndexables = results.getLong(4);
                long deletedNonIndexables = results.getLong(5);
                long nonDeletedNonIndexables = results.getLong(6);
                stats.add(new BaseStats(baseName, lastModified, startTime, deletedIndexables, nonDeletedIndexables,
                                        deletedNonIndexables, nonDeletedNonIndexables));
            }
        } finally {
            if (results != null) {
                results.close();
            }
        }
        log.debug(String.format(Locale.ROOT, "Extracted storage stats in %sms", System.currentTimeMillis() - startTime));
        return stats;
    }

    /**
     * Updates the base statistic table, if no row exists for this table a new row is inserted into the database.
     * Note: This method returns a new instantiated {@link BaseStats} object.
     *
     * @param baseName                The base name.
     * @param lastModified            Last modification time for the base.
     * @param generationTime          The generation time of the base statistic object.
     * @param deletedIndexables       The number of deleted and indexable records.
     * @param nonDeletedIndexables    The number of non deleted and indexable records.
     * @param deletedNonIndexables    The number of deleted and non indexable records.
     * @param nonDeletedNonIndexables the number of non deleted and non indexable records.
     * @param conn                    The database connection.
     * @return
     */
    private BaseStats updateBaseStatsTable(
            String baseName, long lastModified, long generationTime, long deletedIndexables, long nonDeletedIndexables,
            long deletedNonIndexables, long nonDeletedNonIndexables, Connection conn) throws SQLException {
        // Update base stats table
        log.debug("Updating base statistic for base '" + baseName + ".");
        // TODO: Use handle directly
        StatementHandle handleUpdate = statementHandler.getUpdateFullBaseStats();
        PreparedStatement stmt = conn.prepareStatement(handleUpdate.getSql());
        PreparedStatement insertStmt = null;
        try {
            stmt.setLong(1, deletedIndexables);
            stmt.setLong(2, nonDeletedIndexables);
            stmt.setLong(3, deletedNonIndexables);
            stmt.setLong(4, nonDeletedNonIndexables);
            stmt.setString(5, baseName);
            if (stmt.executeUpdate() < 1) { // This is a new base, update
                // insert base and mtime
                log.debug("Base row didn't exists in " + BASE_STATISTICS + " insert new row");
                // TODO: Use handle directly
                StatementHandle handleInsert = statementHandler.getInsertFullBaseStats();
                insertStmt = conn.prepareStatement(handleInsert.getSql());
                insertStmt.setString(1, baseName);
                insertStmt.setLong(2, lastModified);
                insertStmt.setLong(3, deletedIndexables);
                insertStmt.setLong(4, nonDeletedIndexables);
                insertStmt.setLong(5, deletedNonIndexables);
                insertStmt.setLong(6, nonDeletedNonIndexables);
                insertStmt.execute();
            }
        } finally {
            closeStatement(stmt);
            closeStatement(insertStmt);
        }
        // Return new base stats element 
        return new BaseStats(baseName, lastModified, generationTime, deletedIndexables, nonDeletedIndexables,
                             deletedNonIndexables, nonDeletedNonIndexables);
    }

    /**
     * Return the database statistic for a given connection.
     *
     * @param conn The connection.
     * @return A list of {@link BaseStats}.
     * @throws SQLException If error getting the SQL exception.
     * @throws IOException  If error handling RMI.
     */
    private List<BaseStats> getHeavyStatsWithConnection(Connection conn) throws SQLException, IOException {
        long startTime = System.currentTimeMillis();
        List<BaseStats> stats = new LinkedList<>();
        final String query = "SELECT base, deleted, indexable, count(base) FROM summa_records "
                             + "GROUP BY base,deleted,indexable";
        final int baseKey = 1;
        final int deletedKey = 2;
        final int indexableKey = 3;
        final int countKey = 4;

        Statement stmt = conn.createStatement();
        log.debug("Getting resultset for '" + query + "'");
        long st = System.currentTimeMillis();
        log.debug("Got ResultSet in " + (System.currentTimeMillis() - st) + "ms. Iterating...");

        try (ResultSet result = stmt.executeQuery(query)) {
            if (result.next()) {
                while (!result.isAfterLast()) {
                    String base = result.getString(baseKey);
                    String lastBase = base;
                    long deletedIndexables = 0;
                    long nonDeletedIndexables = 0;
                    long deletedNonIndexables = 0;
                    long nonDeletedNonIndexables = 0;

                    // Collect all statistics for the current base and append it
                    // to the statistics list
                    while (lastBase.equals(base)) {
                        boolean deleted = intToBool(result.getInt(deletedKey));
                        boolean indexable = intToBool(result.getInt(indexableKey));
                        long count = result.getLong(countKey);

                        // These boolean cases could be simplified, but we list
                        // them all explicitly to help the (human) reader
                        if (deleted && indexable) {
                            deletedIndexables = count;
                        } else if (deleted && !indexable) {
                            deletedNonIndexables = count;
                        } else if (!deleted && indexable) {
                            nonDeletedIndexables = count;
                        } else if (!deleted && !indexable) {
                            nonDeletedNonIndexables = count;
                        }

                        result.next();
                        if (!result.isAfterLast()) {
                            base = result.getString(1);
                        } else {
                            break;
                        }
                    }
                    // Insert / update database and create {@link BaseStats}
                    // object
                    log.debug("Got stats for " + lastBase + " with " + nonDeletedIndexables + " live records");
                    stats.add(updateBaseStatsTable(
                            lastBase, getModificationTime(lastBase), startTime, deletedIndexables, nonDeletedIndexables,
                            deletedNonIndexables, nonDeletedNonIndexables, conn));
                }
            }
        }
        log.debug(String.format(Locale.ROOT, "Extracted storage stats in %sms", System.currentTimeMillis() - startTime));
        return stats;
    }

    public enum OPTIMIZATION {
        /**
         * Optimized bulk record getter that has implicit {@link QueryOptions}
         * attributes == null
         * deleted == null
         * indexable == null
         * child.depth == 0
         * parent.depth == 1
         * The batch size is loaded in a single statement so when iterating over records,
         * there are no additional SQL queries.
         */
        singleParent
    }

    /**
     * Entry point for calls optimized by special-casing.
     * See {@link OPTIMIZATION} for details on the individual optimizations.
     * @param mTime a timestamp as returned by a {@link UniqueTimestampGenerator}.
     *        to convert from epoch milliseconds to mTime, use UniqueTimestampGenerator.baseTimestamp(epoch_ms).
     * @param base the record base or null if the base should be ignored in the request (= all bases).
     * @param optimization the optimization strategy.
     * @return the retrieved Records and a cursor, directly usable for subsequent calls to this method.
     *         A cursor value of -1 means no more Records beyond the ones returned.
     */
    public SimplePair<List<Record>, Long> getRecordsModifiedAfterOptimized(
            long mTime, String base, QueryOptions options, OPTIMIZATION optimization) throws Exception {
        switch (optimization) {
            case singleParent: return getRecordsModifiedAfterSingleParent(mTime, base);
            default: throw new UnsupportedOperationException("The optimization '" + optimization + " ' is unknown");
        }
    }


    /**
     * Optimized bulk record getter that has implicit {@link QueryOptions}
     * attributes == null
     * deleted == null
     * indexable == null
     * child.depth == 0
     * parent.depth == 1
     * The batch size is loaded in a single statement so when iterating over records,
     * there is no additional SQL queries.
     * @param mTime a timestamp as returned by a {@link UniqueTimestampGenerator}.
     * @param base the record base or null if the base should be ignored in the request (= all bases).
     * @return the retrieved Records and a cursor, directly usable for subsequent calls to this method.
     *         A cursor value of -1 means no more Records beyond the ones returned.
     */
    // TODO: Add support for deleted and indexable
    private SimplePair<List<Record>, Long> getRecordsModifiedAfterSingleParent(long mTime, String base)
            throws SQLException {
        final long startNS = System.nanoTime();
        if (mTime == 1) {
            throw new IllegalArgumentException("mTime == -1 which signifies no more records");
        }
        if (pageSize <1 || pageSize > 10000){ //The 10000 limit is to prevent locking up DB
            throw new IllegalArgumentException(
                    "pageSize (option " + CONF_PAGE_SIZE + ") is " + pageSize +
                    ", but must be between 1 and 10000 for single parent optimization");
        }
        log.debug("Performing optimized getRecordsModifiedAfter with mTime=" + mTime + ", base=" + base +
                  ", pageSize=" + pageSize);
        List<Record> records = new ArrayList<>();

        Connection conn = getTransactionalConnection();
        final String sql =
                "SELECT A.id,A.base,A.deleted,A.indexable,A.hasRelations,A.data,A.ctime,A.mtime,A.meta,R.parentId," +
                "'' AS childId , B.id as p_Id ,B.base as p_base, B.deleted as p_deleted ,B.indexable as p_indexable, " +
                "B.hasRelations as p_hasRelations, B.data as p_data, B.ctime as p_ctime, B.mtime as p_mtime, " +
                "B.meta as p_meta "+
                "FROM summa_records A "+
                "LEFT JOIN summa_relations R ON A.id=R.childId OR A.id=R.parentid "+
                "LEFT JOIN summa_records B ON B.id=R.parentid "+
                "WHERE" + (base == null ? "" : " A.base= ? AND") +
                " A.mtime > ? ORDER BY mtime LIMIT ?";

        PreparedStatement stmt = conn.prepareStatement(sql);
        long cursor = -1;
        long queryTime = -1;
        try {
            if (base == null) {
                stmt.setLong(1, mTime);
                stmt.setInt(2, pageSize);
            } else {
                stmt.setString(1, base);
                stmt.setLong(2, mTime);
                stmt.setInt(3, pageSize);
            }

            queryTime = -System.nanoTime();
            ResultSet rs = stmt.executeQuery();
            queryTime += System.nanoTime();

            while (rs.next()) {
                addRecordFromResultSet(rs, records);
                cursor = rs.getLong("mtime");
            }
        } finally {
            closeStatement(stmt);
            conn.close();
        }
        log.debug("Finished optimized getRecordsModifiedAfter with mTime=" + mTime + ", base=" + base +
                  ", pageSize=" + pageSize + " in " + (System.nanoTime()-startNS)/1000000 +
                  "ms (query_time=" + queryTime/1000000 + "ms)");
        return new SimplePair<>(records, cursor);
    }

    private void addRecordFromResultSet(ResultSet rs, List<Record> records) throws SQLException {
        //Load the record
        String id = rs.getString("id");
        String base = rs.getString("base");
        boolean deleted = rs.getBoolean("deleted");
        boolean indexable = rs.getBoolean("indexable");
        boolean hasRelations = rs.getBoolean("hasRelations");
        byte[] data = rs.getBytes("data");
        long ctime = timestampGenerator.systemTime(rs.getLong("ctime"));
        long mtime = timestampGenerator.systemTime(rs.getLong("mtime"));
        byte[] meta = rs.getBytes("meta");
        String parentId = rs.getString("parentid");
        //childid not loaded

        //Now load the parent record
        String p_id = rs.getString("p_id");
        String p_base = rs.getString("p_base");
        boolean p_deleted = rs.getBoolean("p_deleted");
        boolean p_indexable = rs.getBoolean("p_indexable");
        boolean p_hasRelations = rs.getBoolean("p_hasRelations");
        byte[] p_data = rs.getBytes("p_data");
        long p_ctime = timestampGenerator.systemTime(rs.getLong("p_ctime"));
        long p_mtime = timestampGenerator.systemTime(rs.getLong("p_mtime"));
        byte[] p_meta = rs.getBytes("p_meta");


        if (p_id == null) {
            Record r = new Record(id, base, deleted, indexable, data, ctime, mtime, null, null,
                                  meta.length == 0 ? null : StringMap.fromFormal(meta), true);
            r.setHasRelations(hasRelations);
            records.add(r);
            return;
        }

        //Create the record object tree.
        Record r = new Record(id, base, deleted, indexable, data, ctime, mtime,
                              Collections.singletonList(parentId), null,
                              meta.length == 0 ? null : StringMap.fromFormal(meta), true);
        Record p = new Record(p_id, p_base, p_deleted, p_indexable, p_data, p_ctime, p_mtime,
                              null, Collections.singletonList(id),
                              meta.length == 0 ? null : StringMap.fromFormal(p_meta), true);

        r.setHasRelations(hasRelations);
        p.setHasRelations(p_hasRelations);

        //And set the parent/children again as objects
        r.setParents(Collections.singletonList(p));
        p.setChildren(Collections.singletonList(r));
        records.add(r);
    }

    /**
     * Implementation specific dump of the full database. If possible, the dump should be in the same format as the
     * database itself (e.g. a H2 database dump should itself be a H2 database, directly usable as Storage).
     * @param dest absolute path to a folder on the file system. The folder will be created.
     * @param dumpDeleted if true, records marked as deleted are also dumped.
     * @return status information on the dump..
     * @throws IOException if the database could not be dumped.
     */
    public String dumpToFilesystem(String dest, boolean dumpDeleted) throws IOException {
        return "Error: dumpToFilesystem not implemented for " + this.getClass().getCanonicalName();
    }


    /**
     * @return all the record fields in the same order as {@link QueryOptions.ATTRIBUTES}.
     */
    public static List<String> getAllRecordFields() {
        List<String> all = new ArrayList<>();
        for (QueryOptions.ATTRIBUTES attribute: QueryOptions.ATTRIBUTES.values()) {
            all.add(StatementHandler.attributeToField(attribute));
        }
        return all;
    }

    /**
     * @return all the relation fields.
     */
    public static List<String> getAllRelationFields() {
        return Arrays.asList(PARENT_ID_COLUMN, CHILD_ID_COLUMN);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "DatabaseStorage(#iterators=%d, useLazyRelations=%b, usePagingModel=%b, pageSize=%d, "
                             + "pageSizeUpdate=%d, expandRelativesList=%b, defaultGetOptions=%s, " +
                             "pruneRelativesOnGet=%b, basesWithStoredRelations=%s) %s",
                             iterators.size(), useLazyRelations, usePagingModel, pageSize,
                             pageSizeUpdate, expandRelativesLists, defaultGetOptions,
                             pruneRelativesOnGet, Strings.join(basesWithStoredRelations, ","), getIterationStats());
    }


    /**
     * Recreate the indexes for the table, cleaning up cruft.
     * Note: Request performance might suffer temendously during this operation.
     * @return
     */
    public String recreateIndexes() throws IOException {
        try (Connection conn = getConnection()) {
            return deleteIndexes(conn) + "\n" + createIndexes(conn);
        } catch (SQLException e) {
            throw new IOException("SQLException attempting to delete and create indexes", e);
        }
    }

    private String createIndexes(Connection conn) {

        return "createIndexes not implemented yet";
    }

    private String deleteIndexes(Connection conn) throws SQLException {
        StringBuilder sb = new StringBuilder();
        log.info("Dropping existing indexes on the " + RECORDS + " table");
        for (String index: new String[]{
                "i", "m", "b", "bdi", // Records
                "p", "c", "pc"}) {    // Relations
            long startTime = System.currentTimeMillis();
            this.executeStatement(conn, "DROP INDEX IF EXISTS " + index);
            sb.append("Dropped index ").append(index).append(" in ").
                    append(System.currentTimeMillis() - startTime).append("ms");
        }
        return sb.toString();
    }

}
