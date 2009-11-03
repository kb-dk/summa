package dk.statsbiblioteket.summa.storage.database;

import java.sql.*;
import java.math.BigDecimal;
import java.io.InputStream;
import java.io.Reader;
import java.util.Calendar;
import java.net.URL;

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
            throw new IllegalArgumentException("Nested ManagedStatement "
                                               + "would cause infinite loop");
        }

        this.stmt = stmt;
    }

    public ResultSet executeQuery() throws SQLException {
        return stmt.executeQuery();
    }

    public int executeUpdate() throws SQLException {
        return stmt.executeUpdate();
    }

    public void setNull(int i, int i1) throws SQLException {
        stmt.setNull(i, i1);
    }

    public void setBoolean(int i, boolean b) throws SQLException {
        stmt.setBoolean(i, b);
    }

    public void setByte(int i, byte b) throws SQLException {
        stmt.setByte(i, b);
    }

    public void setShort(int i, short s) throws SQLException {
        stmt.setShort(i, s);
    }

    public void setInt(int i, int i1) throws SQLException {
        stmt.setInt(i, i1);
    }

    public void setLong(int i, long l) throws SQLException {
        stmt.setLong(i, l);
    }

    public void setFloat(int i, float v) throws SQLException {
        stmt.setFloat(i, v);
    }

    public void setDouble(int i, double v) throws SQLException {
        stmt.setDouble(i, v);
    }

    public void setBigDecimal(int i, BigDecimal bigDecimal) throws SQLException {
        stmt.setBigDecimal(i, bigDecimal);
    }

    public void setString(int i, String s) throws SQLException {
        stmt.setString(i, s);
    }

    public void setBytes(int i, byte[] bytes) throws SQLException {
        stmt.setBytes(i, bytes);
    }

    public void setDate(int i, Date date) throws SQLException {
        stmt.setDate(i, date);
    }

    public void setTime(int i, Time time) throws SQLException {
        stmt.setTime(i, time);
    }

    public void setTimestamp(int i, Timestamp timestamp) throws SQLException {
        stmt.setTimestamp(i, timestamp);
    }

    public void setAsciiStream(int i, InputStream inputStream, int i1) throws SQLException {
        stmt.setAsciiStream(i, inputStream, i1);
    }

    public void setUnicodeStream(int i, InputStream inputStream, int i1) throws SQLException {
        stmt.setUnicodeStream(i, inputStream, i1);
    }

    public void setBinaryStream(int i, InputStream inputStream, int i1) throws SQLException {
        stmt.setBinaryStream(i, inputStream, i1);
    }

    public void clearParameters() throws SQLException {
        stmt.clearParameters();
    }

    public void setObject(int i, Object o, int i1) throws SQLException {
        stmt.setObject(i, o, i1);
    }

    public void setObject(int i, Object o) throws SQLException {
        stmt.setObject(i, o);
    }

    public boolean execute() throws SQLException {
        return stmt.execute();
    }

    public void addBatch() throws SQLException {
        stmt.addBatch();
    }

    public void setCharacterStream(int i, Reader reader, int i1) throws SQLException {
        stmt.setCharacterStream(i, reader, i1);
    }

    public void setRef(int i, Ref ref) throws SQLException {
        stmt.setRef(i, ref);
    }

    public void setBlob(int i, Blob blob) throws SQLException {
        stmt.setBlob(i, blob);
    }

    public void setClob(int i, Clob clob) throws SQLException {
        stmt.setClob(i, clob);
    }

    public void setArray(int i, Array array) throws SQLException {
        stmt.setArray(i, array);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return stmt.getMetaData();
    }

    public void setDate(int i, Date date, Calendar calendar) throws SQLException {
        stmt.setDate(i, date, calendar);
    }

    public void setTime(int i, Time time, Calendar calendar) throws SQLException {
        stmt.setTime(i, time, calendar);
    }

    public void setTimestamp(int i, Timestamp timestamp, Calendar calendar) throws SQLException {
        stmt.setTimestamp(i, timestamp, calendar);
    }

    public void setNull(int i, int i1, String s) throws SQLException {
        stmt.setNull(i, i1, s);
    }

    public void setURL(int i, URL url) throws SQLException {
        stmt.setURL(i, url);
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        return stmt.getParameterMetaData();
    }

    public void setRowId(int i, RowId rowId) throws SQLException {
        stmt.setRowId(i, rowId);
    }

    public void setNString(int i, String s) throws SQLException {
        stmt.setNString(i, s);
    }

    public void setNCharacterStream(int i, Reader reader, long l) throws SQLException {
        stmt.setNCharacterStream(i, reader, l);
    }

    public void setNClob(int i, NClob nClob) throws SQLException {
        stmt.setNClob(i, nClob);
    }

    public void setClob(int i, Reader reader, long l) throws SQLException {
        stmt.setClob(i, reader, l);
    }

    public void setBlob(int i, InputStream inputStream, long l) throws SQLException {
        stmt.setBlob(i, inputStream, l);
    }

    public void setNClob(int i, Reader reader, long l) throws SQLException {
        stmt.setNClob(i, reader, l);
    }

    public void setSQLXML(int i, SQLXML sqlxml) throws SQLException {
        stmt.setSQLXML(i, sqlxml);
    }

    public void setObject(int i, Object o, int i1, int i2) throws SQLException {
        stmt.setObject(i, o, i1, i2);
    }

    public void setAsciiStream(int i, InputStream inputStream, long l) throws SQLException {
        stmt.setAsciiStream(i, inputStream, l);
    }

    public void setBinaryStream(int i, InputStream inputStream, long l) throws SQLException {
        stmt.setBinaryStream(i, inputStream, l);
    }

    public void setCharacterStream(int i, Reader reader, long l) throws SQLException {
        stmt.setCharacterStream(i, reader, l);
    }

    public void setAsciiStream(int i, InputStream inputStream) throws SQLException {
        stmt.setAsciiStream(i, inputStream);
    }

    public void setBinaryStream(int i, InputStream inputStream) throws SQLException {
        stmt.setBinaryStream(i, inputStream);
    }

    public void setCharacterStream(int i, Reader reader) throws SQLException {
        stmt.setCharacterStream(i, reader);
    }

    public void setNCharacterStream(int i, Reader reader) throws SQLException {
        stmt.setNCharacterStream(i, reader);
    }

    public void setClob(int i, Reader reader) throws SQLException {
        stmt.setClob(i, reader);
    }

    public void setBlob(int i, InputStream inputStream) throws SQLException {
        stmt.setBlob(i, inputStream);
    }

    public void setNClob(int i, Reader reader) throws SQLException {
        stmt.setNClob(i, reader);
    }

    public ResultSet executeQuery(String s) throws SQLException {
        return stmt.executeQuery(s);
    }

    public int executeUpdate(String s) throws SQLException {
        return stmt.executeUpdate(s);
    }

    /**
     * Special version of close() that will also close the underlying
     * connection of the statement. This is used to get connection pooling
     * working together with PreparedStatements for H2.
     * @throws SQLException on communication errors with the base
     */
    public void close() throws SQLException {
        try {
            stmt.getConnection().close();
        } finally {
            stmt.close();
        }
    }

    public int getMaxFieldSize() throws SQLException {
        return stmt.getMaxFieldSize();
    }

    public void setMaxFieldSize(int i) throws SQLException {
        stmt.setMaxFieldSize(i);
    }

    public int getMaxRows() throws SQLException {
        return stmt.getMaxRows();
    }

    public void setMaxRows(int i) throws SQLException {
        stmt.setMaxRows(i);
    }

    public void setEscapeProcessing(boolean b) throws SQLException {
        stmt.setEscapeProcessing(b);
    }

    public int getQueryTimeout() throws SQLException {
        return stmt.getQueryTimeout();
    }

    public void setQueryTimeout(int i) throws SQLException {
        stmt.setQueryTimeout(i);
    }

    public void cancel() throws SQLException {
        stmt.cancel();
    }

    public SQLWarning getWarnings() throws SQLException {
        return stmt.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        stmt.clearWarnings();
    }

    public void setCursorName(String s) throws SQLException {
        stmt.setCursorName(s);
    }

    public boolean execute(String s) throws SQLException {
        return stmt.execute(s);
    }

    public ResultSet getResultSet() throws SQLException {
        return stmt.getResultSet();
    }

    public int getUpdateCount() throws SQLException {
        return stmt.getUpdateCount();
    }

    public boolean getMoreResults() throws SQLException {
        return stmt.getMoreResults();
    }

    public void setFetchDirection(int i) throws SQLException {
        stmt.setFetchDirection(i);
    }

    public int getFetchDirection() throws SQLException {
        return stmt.getFetchDirection();
    }

    public void setFetchSize(int i) throws SQLException {
        stmt.setFetchSize(i);
    }

    public int getFetchSize() throws SQLException {
        return stmt.getFetchSize();
    }

    public int getResultSetConcurrency() throws SQLException {
        return stmt.getResultSetConcurrency();
    }

    public int getResultSetType() throws SQLException {
        return stmt.getResultSetType();
    }

    public void addBatch(String s) throws SQLException {
        stmt.addBatch(s);
    }

    public void clearBatch() throws SQLException {
        stmt.clearBatch();
    }

    public int[] executeBatch() throws SQLException {
        return stmt.executeBatch();
    }

    public Connection getConnection() throws SQLException {
        return stmt.getConnection();
    }

    public boolean getMoreResults(int i) throws SQLException {
        return stmt.getMoreResults(i);
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return stmt.getGeneratedKeys();
    }

    public int executeUpdate(String s, int i) throws SQLException {
        return stmt.executeUpdate(s, i);
    }

    public int executeUpdate(String s, int[] ints) throws SQLException {
        return stmt.executeUpdate(s, ints);
    }

    public int executeUpdate(String s, String[] strings) throws SQLException {
        return stmt.executeUpdate(s, strings);
    }

    public boolean execute(String s, int i) throws SQLException {
        return stmt.execute(s, i);
    }

    public boolean execute(String s, int[] ints) throws SQLException {
        return stmt.execute(s, ints);
    }

    public boolean execute(String s, String[] strings) throws SQLException {
        return stmt.execute(s, strings);
    }

    public int getResultSetHoldability() throws SQLException {
        return stmt.getResultSetHoldability();
    }

    public boolean isClosed() throws SQLException {
        return stmt.isClosed();
    }

    public void setPoolable(boolean b) throws SQLException {
        stmt.setPoolable(b);
    }

    public boolean isPoolable() throws SQLException {
        return stmt.isPoolable();
    }

    public <T> T unwrap(Class<T> tClass) throws SQLException {
        return stmt.unwrap(tClass);
    }

    public boolean isWrapperFor(Class<?> aClass) throws SQLException {
        return stmt.isWrapperFor(aClass);
    }
}
