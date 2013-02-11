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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;

/**
 * Special wrapper for PreparedStatements that will also close the underlying
 * connection of the statement when the ManagedStatement is closed.
 * <p/>
 * This is used to get connection pooling working together with
 * PreparedStatements for databases like H2 and PostgresQL that does not support
 * {@link javax.sql.StatementEventListener}s.
 */
public class ManagedStatement implements PreparedStatement {

    private PreparedStatement stmt;

    public ManagedStatement(PreparedStatement stmt) {
        if (stmt instanceof ManagedStatement) {
            throw new IllegalArgumentException("Nested ManagedStatement would cause infinite loop");
        }

        this.stmt = stmt;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return stmt.executeQuery();
    }

    @Override
    public int executeUpdate() throws SQLException {
        return stmt.executeUpdate();
    }

    @Override
    public void setNull(int i, int i1) throws SQLException {
        stmt.setNull(i, i1);
    }

    @Override
    public void setBoolean(int i, boolean b) throws SQLException {
        stmt.setBoolean(i, b);
    }

    @Override
    public void setByte(int i, byte b) throws SQLException {
        stmt.setByte(i, b);
    }

    @Override
    public void setShort(int i, short s) throws SQLException {
        stmt.setShort(i, s);
    }

    @Override
    public void setInt(int i, int i1) throws SQLException {
        stmt.setInt(i, i1);
    }

    @Override
    public void setLong(int i, long l) throws SQLException {
        stmt.setLong(i, l);
    }

    @Override
    public void setFloat(int i, float v) throws SQLException {
        stmt.setFloat(i, v);
    }

    @Override
    public void setDouble(int i, double v) throws SQLException {
        stmt.setDouble(i, v);
    }

    @Override
    public void setBigDecimal(int i, BigDecimal bigDecimal) throws SQLException {
        stmt.setBigDecimal(i, bigDecimal);
    }

    @Override
    public void setString(int i, String s) throws SQLException {
        stmt.setString(i, s);
    }

    @Override
    public void setBytes(int i, byte[] bytes) throws SQLException {
        stmt.setBytes(i, bytes);
    }

    @Override
    public void setDate(int i, Date date) throws SQLException {
        stmt.setDate(i, date);
    }

    @Override
    public void setTime(int i, Time time) throws SQLException {
        stmt.setTime(i, time);
    }

    @Override
    public void setTimestamp(int i, Timestamp timestamp) throws SQLException {
        stmt.setTimestamp(i, timestamp);
    }

    @Override
    public void setAsciiStream(int i, InputStream inputStream, int i1) throws SQLException {
        stmt.setAsciiStream(i, inputStream, i1);
    }

    @Override
    @Deprecated
    public void setUnicodeStream(int i, InputStream inputStream, int i1) throws SQLException {

        stmt.setUnicodeStream(i, inputStream, i1);
    }

    @Override
    public void setBinaryStream(int i, InputStream inputStream, int i1) throws SQLException {
        stmt.setBinaryStream(i, inputStream, i1);
    }

    @Override
    public void clearParameters() throws SQLException {
        stmt.clearParameters();
    }

    @Override
    public void setObject(int i, Object o, int i1) throws SQLException {
        stmt.setObject(i, o, i1);
    }

    @Override
    public void setObject(int i, Object o) throws SQLException {
        stmt.setObject(i, o);
    }

    @Override
    public boolean execute() throws SQLException {
        return stmt.execute();
    }

    @Override
    public void addBatch() throws SQLException {
        stmt.addBatch();
    }

    @Override
    public void setCharacterStream(int i, Reader reader, int i1) throws SQLException {
        stmt.setCharacterStream(i, reader, i1);
    }

    @Override
    public void setRef(int i, Ref ref) throws SQLException {
        stmt.setRef(i, ref);
    }

    @Override
    public void setBlob(int i, Blob blob) throws SQLException {
        stmt.setBlob(i, blob);
    }

    @Override
    public void setClob(int i, Clob clob) throws SQLException {
        stmt.setClob(i, clob);
    }

    @Override
    public void setArray(int i, Array array) throws SQLException {
        stmt.setArray(i, array);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return stmt.getMetaData();
    }

    @Override
    public void setDate(int i, Date date, Calendar calendar) throws SQLException {
        stmt.setDate(i, date, calendar);
    }

    @Override
    public void setTime(int i, Time time, Calendar calendar) throws SQLException {
        stmt.setTime(i, time, calendar);
    }

    @Override
    public void setTimestamp(int i, Timestamp timestamp, Calendar calendar) throws SQLException {
        stmt.setTimestamp(i, timestamp, calendar);
    }

    @Override
    public void setNull(int i, int i1, String s) throws SQLException {
        stmt.setNull(i, i1, s);
    }

    @Override
    public void setURL(int i, URL url) throws SQLException {
        stmt.setURL(i, url);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return stmt.getParameterMetaData();
    }

    @Override
    public void setRowId(int i, RowId rowId) throws SQLException {
        stmt.setRowId(i, rowId);
    }

    @Override
    public void setNString(int i, String s) throws SQLException {
        stmt.setNString(i, s);
    }

    @Override
    public void setNCharacterStream(int i, Reader reader, long l) throws SQLException {
        stmt.setNCharacterStream(i, reader, l);
    }

    @Override
    public void setNClob(int i, NClob nClob) throws SQLException {
        stmt.setNClob(i, nClob);
    }

    @Override
    public void setClob(int i, Reader reader, long l) throws SQLException {
        stmt.setClob(i, reader, l);
    }

    @Override
    public void setBlob(int i, InputStream inputStream, long l) throws SQLException {
        stmt.setBlob(i, inputStream, l);
    }

    @Override
    public void setNClob(int i, Reader reader, long l) throws SQLException {
        stmt.setNClob(i, reader, l);
    }

    @Override
    public void setSQLXML(int i, SQLXML sqlxml) throws SQLException {
        stmt.setSQLXML(i, sqlxml);
    }

    @Override
    public void setObject(int i, Object o, int i1, int i2) throws SQLException {
        stmt.setObject(i, o, i1, i2);
    }

    @Override
    public void setAsciiStream(int i, InputStream inputStream, long l) throws SQLException {
        stmt.setAsciiStream(i, inputStream, l);
    }

    @Override
    public void setBinaryStream(int i, InputStream inputStream, long l) throws SQLException {
        stmt.setBinaryStream(i, inputStream, l);
    }

    @Override
    public void setCharacterStream(int i, Reader reader, long l) throws SQLException {
        stmt.setCharacterStream(i, reader, l);
    }

    @Override
    public void setAsciiStream(int i, InputStream inputStream) throws SQLException {
        stmt.setAsciiStream(i, inputStream);
    }

    @Override
    public void setBinaryStream(int i, InputStream inputStream) throws SQLException {
        stmt.setBinaryStream(i, inputStream);
    }

    @Override
    public void setCharacterStream(int i, Reader reader) throws SQLException {
        stmt.setCharacterStream(i, reader);
    }

    @Override
    public void setNCharacterStream(int i, Reader reader) throws SQLException {
        stmt.setNCharacterStream(i, reader);
    }

    @Override
    public void setClob(int i, Reader reader) throws SQLException {
        stmt.setClob(i, reader);
    }

    @Override
    public void setBlob(int i, InputStream inputStream) throws SQLException {
        stmt.setBlob(i, inputStream);
    }

    @Override
    public void setNClob(int i, Reader reader) throws SQLException {
        stmt.setNClob(i, reader);
    }

    @Override
    public ResultSet executeQuery(String s) throws SQLException {
        return stmt.executeQuery(s);
    }

    @Override
    public int executeUpdate(String s) throws SQLException {
        return stmt.executeUpdate(s);
    }

    /**
     * Special version of close() that will also close the underlying
     * connection of the statement. This is used to get connection pooling
     * working together with PreparedStatements for H2.
     *
     * @throws SQLException on communication errors with the base
     */
    @Override
    public void close() throws SQLException {
        try {
            stmt.getConnection().close();
        } finally {
            stmt.close();
        }
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return stmt.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int i) throws SQLException {
        stmt.setMaxFieldSize(i);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return stmt.getMaxRows();
    }

    @Override
    public void setMaxRows(int i) throws SQLException {
        stmt.setMaxRows(i);
    }

    @Override
    public void setEscapeProcessing(boolean b) throws SQLException {
        stmt.setEscapeProcessing(b);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return stmt.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int i) throws SQLException {
        stmt.setQueryTimeout(i);
    }

    @Override
    public void cancel() throws SQLException {
        stmt.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return stmt.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        stmt.clearWarnings();
    }

    @Override
    public void setCursorName(String s) throws SQLException {
        stmt.setCursorName(s);
    }

    @Override
    public boolean execute(String s) throws SQLException {
        return stmt.execute(s);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return stmt.getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return stmt.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return stmt.getMoreResults();
    }

    @Override
    public void setFetchDirection(int i) throws SQLException {
        stmt.setFetchDirection(i);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return stmt.getFetchDirection();
    }

    @Override
    public void setFetchSize(int i) throws SQLException {
        stmt.setFetchSize(i);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return stmt.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return stmt.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return stmt.getResultSetType();
    }

    @Override
    public void addBatch(String s) throws SQLException {
        stmt.addBatch(s);
    }

    @Override
    public void clearBatch() throws SQLException {
        stmt.clearBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return stmt.executeBatch();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return stmt.getConnection();
    }

    @Override
    public boolean getMoreResults(int i) throws SQLException {
        return stmt.getMoreResults(i);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return stmt.getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String s, int i) throws SQLException {
        return stmt.executeUpdate(s, i);
    }

    @Override
    public int executeUpdate(String s, int[] ints) throws SQLException {
        return stmt.executeUpdate(s, ints);
    }

    @Override
    public int executeUpdate(String s, String[] strings) throws SQLException {
        return stmt.executeUpdate(s, strings);
    }

    @Override
    public boolean execute(String s, int i) throws SQLException {
        return stmt.execute(s, i);
    }

    @Override
    public boolean execute(String s, int[] ints) throws SQLException {
        return stmt.execute(s, ints);
    }

    @Override
    public boolean execute(String s, String[] strings) throws SQLException {
        return stmt.execute(s, strings);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return stmt.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return stmt.isClosed();
    }

    @Override
    public void setPoolable(boolean b) throws SQLException {
        stmt.setPoolable(b);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return stmt.isPoolable();
    }

    @Override
    public <T> T unwrap(Class<T> tClass) throws SQLException {
        return stmt.unwrap(tClass);
    }

    @Override
    public boolean isWrapperFor(Class<?> aClass) throws SQLException {
        return stmt.isWrapperFor(aClass);
    }
}

