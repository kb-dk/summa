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

import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.StringWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Takes care of construction and re-use of statements for databases.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class StatementHandler {
    private static Log log = LogFactory.getLog(StatementHandler.class);

    private Map<String, MiniConnectionPoolManager.StatementHandle> statements =
        new HashMap<>(30);
    final private boolean lazy;

    public StatementHandler(boolean lazy) {
        this.lazy = lazy;
    }

    public abstract String getPagingStatement(String sql, boolean readOnly);
    public abstract MiniConnectionPoolManager.StatementHandle prepareStatement(String sql) throws SQLException;

    private static final String RELATIONS_CLAUSE_BOTH =
        DatabaseStorage.RECORDS + "." + DatabaseStorage.ID_COLUMN + "="
        + DatabaseStorage.RELATIONS + "." + DatabaseStorage.PARENT_ID_COLUMN
        + " OR " + DatabaseStorage.RECORDS + "." + DatabaseStorage.ID_COLUMN + "="
        + DatabaseStorage.RELATIONS + "." + DatabaseStorage.CHILD_ID_COLUMN;

    private static final String RELATIONS_CLAUSE_ONLY_CHILDREN =
        DatabaseStorage.RECORDS + "." + DatabaseStorage.ID_COLUMN + "="
        + DatabaseStorage.RELATIONS + "." + DatabaseStorage.PARENT_ID_COLUMN;

    private static final String RELATIONS_CLAUSE_ONLY_PARENTS =
        DatabaseStorage.RECORDS + "." + DatabaseStorage.ID_COLUMN + "="
        + DatabaseStorage.RELATIONS + "." + DatabaseStorage.CHILD_ID_COLUMN;

    /**
     * Creates a sql query that return the wanted columns for the given base where mtime is >=  a given point in time.
     * @param options the columns to return.
     * @return an initialized StatementHandle.
     */
    public MiniConnectionPoolManager.StatementHandle getGetModifiedAfter(QueryOptions options) {
        return generateStatementHandle(getPagingStatement(
            lazy ?
            "SELECT " + getColumns(options, true)
            + " FROM " + DatabaseStorage.RECORDS
            + " WHERE " + DatabaseStorage.BASE_COLUMN + "=?"
            + " AND " + DatabaseStorage.MTIME_COLUMN + ">?"
            + " ORDER BY " + DatabaseStorage.MTIME_COLUMN
                 :
                    "SELECT " + getColumns(options, false)
                    + " FROM " + DatabaseStorage.RECORDS
                    + " LEFT JOIN " + DatabaseStorage.RELATIONS
                    + " ON " + RELATIONS_CLAUSE_BOTH
                    + " WHERE " + DatabaseStorage.BASE_COLUMN + "=?"
                    + " AND " + DatabaseStorage.MTIME_COLUMN + ">?"
                    + " ORDER BY " + DatabaseStorage.MTIME_COLUMN,
            true
        ));
    }

    /**
     * Creates a sql query that return the wanted columns where mtime is >=  a given point in time.
     * @param options the columns to return.
     * @return an initialized StatementHandle.
     */
    public MiniConnectionPoolManager.StatementHandle getGetModifiedAfterAll(QueryOptions options) {
        return generateStatementHandle(getPagingStatement(
            lazy ?
            "SELECT " + getColumns(options, true)
            + " FROM " + DatabaseStorage.RECORDS
            + " WHERE " + DatabaseStorage.MTIME_COLUMN + ">?"
            + " ORDER BY " + DatabaseStorage.MTIME_COLUMN
                 :
                    "SELECT " + getColumns(options, false)
                    + " FROM " + DatabaseStorage.RECORDS
                    + " LEFT JOIN " + DatabaseStorage.RELATIONS
                    + " ON " + RELATIONS_CLAUSE_BOTH
                    + " WHERE " + DatabaseStorage.MTIME_COLUMN + ">?"
                    + " ORDER BY " + DatabaseStorage.MTIME_COLUMN,
            true
        ));
    }

    public MiniConnectionPoolManager.StatementHandle getGetRecord(QueryOptions options) {
        if (options.hasAttribute(QueryOptions.ATTRIBUTES.PARENTS)
            && !options.hasAttribute(QueryOptions.ATTRIBUTES.CHILDREN)) {
            return generateStatementHandle(
                    "SELECT " + getColumns(options, false)
                    + " FROM " + DatabaseStorage.RECORDS
                    + " LEFT JOIN " + DatabaseStorage.RELATIONS
                    + " ON " + RELATIONS_CLAUSE_ONLY_PARENTS
                    + " WHERE " + DatabaseStorage.RECORDS + "." + DatabaseStorage.ID_COLUMN + "=?"
            );
        }
        if (!options.hasAttribute(QueryOptions.ATTRIBUTES.PARENTS)
            && options.hasAttribute(QueryOptions.ATTRIBUTES.CHILDREN)) {
            return generateStatementHandle(
                    "SELECT " + getColumns(options, false)
                    + " FROM " + DatabaseStorage.RECORDS
                    + " LEFT JOIN " + DatabaseStorage.RELATIONS
                    + " ON " + RELATIONS_CLAUSE_ONLY_CHILDREN
                    + " WHERE " + DatabaseStorage.RECORDS + "." + DatabaseStorage.ID_COLUMN + "=?"
            );
        }
        if (!options.hasAttribute(QueryOptions.ATTRIBUTES.PARENTS)
            && !options.hasAttribute(QueryOptions.ATTRIBUTES.CHILDREN)) {
            return generateStatementHandle(
                    "SELECT " + getColumns(options, false)
                    + " FROM " + DatabaseStorage.RECORDS
                    + " WHERE " + DatabaseStorage.RECORDS + "." + DatabaseStorage.ID_COLUMN + "=?"
            );
        }
        return generateStatementHandle(
                "SELECT " + getColumns(options, false)
                + " FROM " + DatabaseStorage.RECORDS
                + " LEFT JOIN " + DatabaseStorage.RELATIONS
                + " ON " + RELATIONS_CLAUSE_BOTH
                + " WHERE " + DatabaseStorage.RECORDS + "." + DatabaseStorage.ID_COLUMN + "=?"
        );

        }
    
    public MiniConnectionPoolManager.StatementHandle getGetRecordFullObjectTree() {
        return generateStatementHandle(
            "SELECT *"
            + " FROM " + DatabaseStorage.RECORDS
            + " WHERE " + DatabaseStorage.RECORDS + "." + DatabaseStorage.ID_COLUMN + "=?"
        );
    }    

    // TODO: Extend this to use attributes and change DatabaseStorage.createNewRecord/updateRecord to use partial update
    public MiniConnectionPoolManager.StatementHandle getCreateRecord() {
        return generateStatementHandle(
            "INSERT INTO " + DatabaseStorage.RECORDS + " ("
            + DatabaseStorage.ID_COLUMN + ", "
            + DatabaseStorage.BASE_COLUMN + ", "
            + DatabaseStorage.DELETED_COLUMN + ", "
            + DatabaseStorage.INDEXABLE_COLUMN + ", "
            + DatabaseStorage.HAS_RELATIONS_COLUMN + ", "
            + DatabaseStorage.CTIME_COLUMN + ", "
            + DatabaseStorage.MTIME_COLUMN + ", "
            + DatabaseStorage.DATA_COLUMN + ", "
            + DatabaseStorage.META_COLUMN
            + ") VALUES (?,?,?,?,?,?,?,?,?)"
        );
    }

    public MiniConnectionPoolManager.StatementHandle getUpdateRecord() {
        return generateStatementHandle(
            "UPDATE "
            + DatabaseStorage.RECORDS + " SET "
            + DatabaseStorage.BASE_COLUMN + "=?, "
            + DatabaseStorage.DELETED_COLUMN + "=?, "
            + DatabaseStorage.INDEXABLE_COLUMN + "=?, "
            + DatabaseStorage.HAS_RELATIONS_COLUMN + "=?, "
            + DatabaseStorage.MTIME_COLUMN + "=?, "
            + DatabaseStorage.DATA_COLUMN + "=?, "
            + DatabaseStorage.META_COLUMN + "=? "
            + "WHERE " + DatabaseStorage.ID_COLUMN + "=?"
        );
    }

    public MiniConnectionPoolManager.StatementHandle getTouchRecord() {
        return generateStatementHandle(
            "UPDATE " + DatabaseStorage.RECORDS + " SET "
            + DatabaseStorage.MTIME_COLUMN + "=? "
            + "WHERE " + DatabaseStorage.ID_COLUMN + "=?"
        );
    }
    
    public MiniConnectionPoolManager.StatementHandle getRecordExist() {
        return generateStatementHandle(
                "SELECT COUNT(*) FROM " + DatabaseStorage.RECORDS+           
                " WHERE " + DatabaseStorage.ID_COLUMN +" = ?"
        );
    }
    


    public MiniConnectionPoolManager.StatementHandle getTouchParents() {
        return generateStatementHandle(
            "UPDATE " + DatabaseStorage.RECORDS
            + " SET " + DatabaseStorage.MTIME_COLUMN + "=? "
            + " WHERE " + DatabaseStorage.ID_COLUMN + " IN ("
            + " SELECT " + DatabaseStorage.PARENT_ID_COLUMN
            + " FROM " + DatabaseStorage.RELATIONS
            + " WHERE " + DatabaseStorage.CHILD_ID_COLUMN + "=? )"
        );
    }
    
    //This will cause modified_time violation and fail.  
    public MiniConnectionPoolManager.StatementHandle getTouchChildren() {
        return generateStatementHandle(
            "UPDATE " + DatabaseStorage.RECORDS
            + " SET " + DatabaseStorage.MTIME_COLUMN + "=? "
            + " WHERE " + DatabaseStorage.ID_COLUMN + " IN ("
            + " SELECT " + DatabaseStorage.CHILD_ID_COLUMN
            + " FROM " + DatabaseStorage.RELATIONS
            + " WHERE " + DatabaseStorage.PARENT_ID_COLUMN + "=? )"
        );
    }


    public MiniConnectionPoolManager.StatementHandle getGetChildren(QueryOptions options) {
        return generateStatementHandle(
            "SELECT " + getColumns(options, false)
            + " FROM " + DatabaseStorage.RELATIONS
            + " JOIN " + DatabaseStorage.RECORDS
            + " ON " + DatabaseStorage.RECORDS + "." + DatabaseStorage.ID_COLUMN + "="
            + DatabaseStorage.RELATIONS + "." + DatabaseStorage.CHILD_ID_COLUMN
            + " WHERE " + DatabaseStorage.RELATIONS + "."
            + DatabaseStorage.PARENT_ID_COLUMN + "=?"
            + " ORDER BY " + DatabaseStorage.RECORDS + "." + DatabaseStorage.ID_COLUMN
        );
    }

    public MiniConnectionPoolManager.StatementHandle getGetParents(QueryOptions options) {
        return generateStatementHandle(
            "SELECT " + getColumns(options, false)
            + " FROM " + DatabaseStorage.RELATIONS
            + " JOIN " + DatabaseStorage.RECORDS
            + " ON " + DatabaseStorage.RECORDS + "." + DatabaseStorage.ID_COLUMN + "="
            + DatabaseStorage.RELATIONS + "." + DatabaseStorage.PARENT_ID_COLUMN
            + " WHERE " + DatabaseStorage.RELATIONS + "."
            + DatabaseStorage.CHILD_ID_COLUMN + "=?"
            + " ORDER BY " + DatabaseStorage.RECORDS + "." + DatabaseStorage.ID_COLUMN
        );
    }
    
    public MiniConnectionPoolManager.StatementHandle getParentIdsOnly() {
        return generateStatementHandle(
            "SELECT "+DatabaseStorage.PARENT_ID_COLUMN+" FROM "+ DatabaseStorage.RELATIONS+
            " WHERE "+DatabaseStorage.CHILD_ID_COLUMN +" = ?"                    
        );
    }

    public MiniConnectionPoolManager.StatementHandle getParentAndChildCount() {
        return generateStatementHandle(
            "SELECT COUNT(*) FROM " + DatabaseStorage.RELATIONS+           
             " WHERE " + DatabaseStorage.PARENT_ID_COLUMN +" = ? AND " + DatabaseStorage.CHILD_ID_COLUMN+" = ?"
        );
    }
    
    
    public MiniConnectionPoolManager.StatementHandle getChildIdsOnly() {
        return generateStatementHandle(
            "SELECT "+DatabaseStorage.CHILD_ID_COLUMN+" FROM "+ DatabaseStorage.RELATIONS+
            " WHERE "+DatabaseStorage.PARENT_ID_COLUMN +" = ?"                    
        );
    }

    
    // The obvious thing to do here was to use an OR instead of the UNION,
    // however some query optimizers have problems using the right indexes
    // when ORing (H2 for instance). Using a UNION is easier for the
    // optimizer
    public MiniConnectionPoolManager.StatementHandle getRelatedIds() {
        return generateStatementHandle(
            "SELECT " + DatabaseStorage.PARENT_ID_COLUMN
            + ", " + DatabaseStorage.CHILD_ID_COLUMN
            + " FROM " + DatabaseStorage.RELATIONS
            + " WHERE " + DatabaseStorage.PARENT_ID_COLUMN + "=?"
            + " UNION "
            + "SELECT " + DatabaseStorage.PARENT_ID_COLUMN
            + ", " + DatabaseStorage.CHILD_ID_COLUMN
            + " FROM " + DatabaseStorage.RELATIONS
            + " WHERE " + DatabaseStorage.CHILD_ID_COLUMN + "=?"
        );
    }

    public MiniConnectionPoolManager.StatementHandle getMarkHasRelations() {
        return generateStatementHandle(
            "UPDATE " + DatabaseStorage.RECORDS
            + " SET " + DatabaseStorage.HAS_RELATIONS_COLUMN + "=1 "
            + " WHERE " + DatabaseStorage.ID_COLUMN + "=?"
        );
    }

    public MiniConnectionPoolManager.StatementHandle getCreateRelation() {
        return generateStatementHandle(
            "INSERT INTO " + DatabaseStorage.RELATIONS
            + " (" + DatabaseStorage.PARENT_ID_COLUMN + ","
            + DatabaseStorage.CHILD_ID_COLUMN
            + ") VALUES (?,?)"
        );
    }

    public MiniConnectionPoolManager.StatementHandle getBatchJob(QueryOptions options, String base) {
        return generateStatementHandle(getPagingStatement(
            "SELECT " + getColumns(options, true)
            + " FROM " + DatabaseStorage.RECORDS
            + " WHERE ( mtime<? AND mtime>? )"
            + (base == null ? "" : " AND base=?")
            + " ORDER BY " + DatabaseStorage.MTIME_COLUMN,
            false
        ));
    }

    public MiniConnectionPoolManager.StatementHandle getUpdateMtimeForBase() {
        return generateStatementHandle(
            "UPDATE " + DatabaseStorage.BASE_STATISTICS
            + " SET " + DatabaseStorage.MTIME_COLUMN + " = ? WHERE " + DatabaseStorage.BASE_COLUMN
            + " = ?"
        );
    }

    public MiniConnectionPoolManager.StatementHandle getInsertBaseStats() {
        return generateStatementHandle(
            "INSERT INTO " + DatabaseStorage.BASE_STATISTICS
            + " (" + DatabaseStorage.BASE_COLUMN + ", " + DatabaseStorage.MTIME_COLUMN + ", "
            + DatabaseStorage.DELETE_INDEXABLES_COLUMN + ", " + DatabaseStorage.NON_DELETED_INDEXABLES_COLUMN
            + ", " + DatabaseStorage.DELETED_NON_INDEXABLES_COLUMN + ", "
            + DatabaseStorage.NON_DELETED_NON_INDEXABLES_COLUMN + ", " + DatabaseStorage.VALID_COLUMN
            + ") VALUES (?, ?, 0, 0, 0, 0, 0)"
        );
    }

    public MiniConnectionPoolManager.StatementHandle getSetBasetatsInvalid() {
        return generateStatementHandle(
            "UPDATE " + DatabaseStorage.BASE_STATISTICS + " SET "
            + DatabaseStorage.VALID_COLUMN + " = 0 WHERE " + DatabaseStorage.BASE_COLUMN + " = ?"
        );
    }

    public MiniConnectionPoolManager.StatementHandle getGetLastModificationTime() {
        return generateStatementHandle(
            "SELECT " + DatabaseStorage.MTIME_COLUMN + " FROM "
            + DatabaseStorage.BASE_STATISTICS + " WHERE " + DatabaseStorage.BASE_COLUMN + " = ?"
        );
    }
    
    public MiniConnectionPoolManager.StatementHandle getClearChildren() {
        return generateStatementHandle(
            "DELETE FROM " + DatabaseStorage.RELATIONS + " WHERE "  + DatabaseStorage.PARENT_ID_COLUMN +" = ?"
        );
    }
    
    public MiniConnectionPoolManager.StatementHandle getClearParents() {
        return generateStatementHandle(
            "DELETE FROM " + DatabaseStorage.RELATIONS + " WHERE " + DatabaseStorage.CHILD_ID_COLUMN +" = ?"
        );
    }
    
    

    public MiniConnectionPoolManager.StatementHandle getUpdateFullBaseStats() {
        return generateStatementHandle(
            "UPDATE " + DatabaseStorage.BASE_STATISTICS + " SET "
            + DatabaseStorage.DELETE_INDEXABLES_COLUMN + " = ?, "
            + DatabaseStorage.NON_DELETED_INDEXABLES_COLUMN + " = ?, "
            + DatabaseStorage.DELETED_NON_INDEXABLES_COLUMN + " = ?, "
            + DatabaseStorage.NON_DELETED_NON_INDEXABLES_COLUMN + " = ?, "
            + DatabaseStorage.VALID_COLUMN + " = 1 "
            +" WHERE " + DatabaseStorage.BASE_COLUMN + " = ?"
        );
    }

    public MiniConnectionPoolManager.StatementHandle getInsertFullBaseStats() {
        return generateStatementHandle(
            "INSERT INTO " + DatabaseStorage.BASE_STATISTICS
            + " (" + DatabaseStorage.BASE_COLUMN + ", " + DatabaseStorage.MTIME_COLUMN + ", "
            + DatabaseStorage.DELETE_INDEXABLES_COLUMN + ", " + DatabaseStorage.NON_DELETED_INDEXABLES_COLUMN
            + ", " + DatabaseStorage.DELETED_NON_INDEXABLES_COLUMN + ", "
            + DatabaseStorage.NON_DELETED_NON_INDEXABLES_COLUMN + ", " + DatabaseStorage.VALID_COLUMN
            + ") VALUES (?, ?, ?, ?, ?, ?, 1)"
        );
    }

/*
    public MiniConnectionPoolManager.StatementHandle get() {
        return generateStatementHandle(

        );
    }
*/

    private MiniConnectionPoolManager.StatementHandle generateStatementHandle(String sql) {
        MiniConnectionPoolManager.StatementHandle sh = statements.get(sql);
        if (sh != null) {
            return sh;
        }
        try {
            sh = prepareStatement(sql);
        } catch (SQLException e) {
            throw new IllegalArgumentException(
                "The received SQL could not be prepared. SQL statement was '" + sql + "'", e);
        }
        statements.put(sql, sh);
        log.debug("Generated new StatementHandle for sql '" + sql + "'");
        return sh;
    }

    private String NULL_ATTRIBUTES = null;
    private String NULL_ATTRIBUTES_RELATIONS = null;
    /**
     * Produces a SQL-query part meant for requesting the fields matching the given attributes in options.
     * Example result: {@code summa_records.id,'' AS base,summa_records.deleted...}. All attributes are always
     * present, although those missing from options are represented with an {@code '' AS attribute}.
     * @param options the source of the attributes. If null, all fields are requested.
     * @return SQL for retrieving content from the columns corresponding to the attributes in options.
     */
    private String getColumns(QueryOptions options, boolean noRelations) {
        if (options == null || !options.newRecordNeeded()) {
            if (noRelations && NULL_ATTRIBUTES != null) {
                return NULL_ATTRIBUTES;
            }
            if (!noRelations && NULL_ATTRIBUTES_RELATIONS != null) {
                return NULL_ATTRIBUTES_RELATIONS;
            }
        }
        StringWriter records = new StringWriter(200);
        boolean first = true;
        for (QueryOptions.ATTRIBUTES attribute: QueryOptions.ATTRIBUTES.values()) {
/*            if (noRelations &&
                (attribute == QueryOptions.ATTRIBUTES.PARENTS || attribute == QueryOptions.ATTRIBUTES.CHILDREN)) {
                continue;
            }*/
            if (first) {
                first = false;
            } else {
                records.append(",");
            }

            boolean found = options == null || !options.newRecordNeeded();
            if (!found) {
                for (QueryOptions.ATTRIBUTES queryAttribute: options.getAttributes()) {
                    if (attribute == queryAttribute) {
                        if (!(noRelations && (attribute == QueryOptions.ATTRIBUTES.PARENTS ||
                                              attribute == QueryOptions.ATTRIBUTES.CHILDREN))) {
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (noRelations && (attribute == QueryOptions.ATTRIBUTES.PARENTS ||
                                attribute == QueryOptions.ATTRIBUTES.CHILDREN)) {
                found = false;
            }
            if (found) {
                if (attribute == QueryOptions.ATTRIBUTES.PARENTS || attribute == QueryOptions.ATTRIBUTES.CHILDREN) {
                    records.append(DatabaseStorage.RELATIONS);
                } else {
                    records.append(DatabaseStorage.RECORDS);
                }
                records.append(".");
            } else {
                records.append("'' AS ");
            }
            records.append(attributeToField(attribute));
        }
        if (options == null || !options.newRecordNeeded()) {
            if (noRelations && NULL_ATTRIBUTES != null) {
                NULL_ATTRIBUTES = records.toString();
            }
            if (!noRelations && NULL_ATTRIBUTES_RELATIONS != null) {
                NULL_ATTRIBUTES_RELATIONS = records.toString();
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("Generated columns '" + records.toString());
        }
        return records.toString();
    }

    private String attributeToField(QueryOptions.ATTRIBUTES attribute) {
        switch (attribute) {
            case ID:               return DatabaseStorage.ID_COLUMN;
            case BASE:             return DatabaseStorage.BASE_COLUMN;
            case DELETED:          return DatabaseStorage.DELETED_COLUMN;
            case INDEXABLE:        return DatabaseStorage.INDEXABLE_COLUMN;
            case HAS_RELATIONS:    return DatabaseStorage.HAS_RELATIONS_COLUMN;
            case CONTENT:          return DatabaseStorage.DATA_COLUMN;
            case CREATIONTIME:     return DatabaseStorage.CTIME_COLUMN;
            case MODIFICATIONTIME: return DatabaseStorage.MTIME_COLUMN;
            case META:             return DatabaseStorage.META_COLUMN;
            case PARENTS:          return DatabaseStorage.PARENT_ID_COLUMN;
            case CHILDREN:         return DatabaseStorage.CHILD_ID_COLUMN;
            default: throw new UnsupportedOperationException(
                "The QueryOptions attribute '" + attribute + "' is unknown");
        }
    }
}
