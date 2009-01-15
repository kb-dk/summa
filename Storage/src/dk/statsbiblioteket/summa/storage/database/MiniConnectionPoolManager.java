package dk.statsbiblioteket.summa.storage.database;

/*
 * Copyright 2007 Christian d'Heureuse, www.source-code.biz
 * Copyright 2009 Mikkel Kamstrup Erlandsen statsbiblioteket.dk
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.Semaphore;
import java.util.Stack;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.concurrent.TimeUnit;
import javax.sql.*;

/**
 * A simple standalone JDBC connection pool manager.
 * <p>
 * The public methods of this class are thread-safe.
 * <p>
 * This class is based on the work of Christian d'Heureuse, www.source-code.biz.
 * For more information see
 * <a href="http://www.source-code.biz/snippets/java/8.htm">www.source-code.biz/snippets/java/8.htm</a>
 */
public class MiniConnectionPoolManager {

    private ConnectionPoolDataSource       dataSource;
    private int                            maxConnections;
    private int                            timeout;
    private Log                            log;
    private Semaphore                      semaphore;
    private Stack<PooledConnection>        recycledConnections;
    private int                            activeConnections;
    private PoolConnectionEventListener    poolConnectionEventListener;
    private PoolStatementEventListener     statementEventListener;
    private boolean                        isDisposed;

    /**
     * Thrown in {@link MiniConnectionPoolManager#getConnection()} when no free
     * connection becomes available within <code>timeout</code> seconds.
     */
    public static class TimeoutException extends RuntimeException {

        public TimeoutException () {
            super ("Timeout while waiting for a free database connection.");
        }
    }

    /**
     * Thrown in {@link MiniConnectionPoolManager#getConnection()} when there
     * is an error looking up a connection from the pooling data source.
     */
    public static class ConnectionException extends RuntimeException {

        public ConnectionException (String msg) {
            super (msg);
        }

        public ConnectionException (String msg, Throwable cause) {
            super (msg, cause);
        }
    }

    /**
     * Opaque handle type used for representing pooled prepared statements
     */
    public static class PooledStatementHandle
                                    implements DatabaseStorage.StatementHandle {

        private static int handleCount = 0;
        private int handle;
        private String sql;

        PooledStatementHandle(String sql) {
            synchronized (this.getClass()) {
                handle = handleCount;
                handleCount++;
            }

            this.sql = sql;
        }

        int get() {
            return handle;
        }

        @Override
        public int hashCode() {
            return handle;
        }

        public String getSql() {
            return sql;
        }

        public String toString() {
            return "" + handle;
        }
    }

    /**
     * Constructs a MiniConnectionPoolManager object with a timeout of 60 seconds.
     * @param dataSource      the data source for the connections.
     * @param maxConnections  the maximum number of connections.
     */
    public MiniConnectionPoolManager (ConnectionPoolDataSource dataSource,
                                      int maxConnections) {
        this (dataSource, maxConnections, 60);
    }

    /**
     * Constructs a MiniConnectionPoolManager object.
     * @param dataSource      the data source for the connections.
     * @param maxConnections  the maximum number of connections.
     * @param timeout         the maximum time in seconds to wait for a
     *                        free connection.
     */
    public MiniConnectionPoolManager (ConnectionPoolDataSource dataSource,
                                      int maxConnections, int timeout) {
        log = LogFactory.getLog(this.getClass().getName());

        this.dataSource = dataSource;
        this.maxConnections = maxConnections;
        this.timeout = timeout;

        if (maxConnections < 1) {
            throw new IllegalArgumentException("Invalid maxConnections value.");
        }

        semaphore = new Semaphore(maxConnections,true);
        recycledConnections = new Stack<PooledConnection>();

        poolConnectionEventListener = new PoolConnectionEventListener();
        statementEventListener = new PoolStatementEventListener();

        log.debug("Created for source " + dataSource.getClass()
                  + " and max connections " + maxConnections
                  + " and timeout " + timeout);
    }

    /**
     * Closes all unused pooled connections.
     */
    public synchronized void dispose() throws SQLException {
        if (isDisposed) {
            log.debug("Already disposed");
            return;
        }

        log.debug("Disposing of all connections");

        isDisposed = true;

        while (!recycledConnections.isEmpty()) {
            PooledConnection pconn = recycledConnections.pop();

            try {
                pconn.close();
                if (log.isTraceEnabled ()) {
                    log.trace("Closed connection " + pconn);
                }
            } catch (SQLException e) {
                log.warn("Error disposing of " + pconn + ": " + e.getMessage (),
                         e);
            }
        }
    }

    public PooledStatementHandle prepareStatement(String sql) {
        return new PooledStatementHandle(sql);
    }

    public PreparedStatement getStatement(PooledStatementHandle handle)
                                                            throws SQLException{
        PooledConnection pconn = getPooledConnection();
        Connection conn = pconn.getConnection();

        if (log.isTraceEnabled()) {
            log.trace("Getting statement for handle " + handle
                      + " on connection " + pconn.hashCode());
        }

        // We prepare a new statement on each invocation. This might look insane
        // but the JDBC _should_ be caching the statements for us
        // (Derby does this at least)
        PreparedStatement stmt = conn.prepareStatement(handle.getSql());

        // Set our selves up for closing the connection when
        // the statement is closed
        pconn.addStatementEventListener(statementEventListener);

        return stmt;
    }

    /**
     * Retrieves a connection from the connection pool.
     * If <code>maxConnections</code> connections are already in use, the method
     * waits until a connection becomes available or <code>timeout</code>
     * seconds elapsed.
     * <p/>
     * When the application is finished using the connection, it <i>must</i>
     * close it in order to return it to the pool.
     * @return a new Connection object
     * @throws TimeoutException when no connection becomes available within
     *                          <code>timeout</code> seconds
     */
    public Connection getConnection() {
        PooledConnection pconn = getPooledConnection();

        try {
            return pconn.getConnection();
        } catch (SQLException e) {
            throw new ConnectionException("Error extracting physical connection"
                                          + " from pooled connection: "
                                          + e.getMessage(), e);
        }
    }

    /**
     * Get a PooledConnection from the connection pool.
     * @return
     */
    protected PooledConnection getPooledConnection() {
        // This routine is unsynchronized, because semaphore.tryAcquire() may block.

        if (log.isTraceEnabled ()) {
            log.trace("Getting connection, with " + getActiveConnections()
                      + " connections currently active");
        }

        synchronized (this) {
            if (isDisposed) {
                throw new IllegalStateException("Connection pool"
                                                + " has been disposed.");
            }
        }

        try {
            if (!semaphore.tryAcquire(timeout,TimeUnit.SECONDS)) {
                throw new TimeoutException();
            }
        } catch (InterruptedException e) {
            throw new ConnectionException("Interrupted while waiting for "
                                       + "a database connection.",e);
        }

        boolean ok = false;
        try {
            PooledConnection pconn = _getPooledConnection();
            ok = true;
            return pconn;
        } finally {
            if (!ok) {
                semaphore.release();
            }
        }
    }

    /**
     * Internal helper method to fetch a pooled connection. Should only be used
     * inside getPooledConnection().
     * <p/>
     * One <i>must</i> be holding the {@code semaphore} when entering
     * this method
     * @return a pooled connection, new, or recycled
     */
    private synchronized PooledConnection _getPooledConnection() {
        if (isDisposed) {
            throw new IllegalStateException("Connection pool has "
                                            + "been disposed.");   // test again with lock
        }

        PooledConnection pconn;

        if (!recycledConnections.empty()) {
            // Recycle an old connection
            if (log.isTraceEnabled()) {
                log.trace("Getting pooled connection");
            }
            pconn = recycledConnections.pop();
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Requesting new pooled connection");
            }
            try {
                // Request new pooled connection from data source
                // and register a connection context for prepared statements
                pconn = dataSource.getPooledConnection();
            } catch (Exception e) {
                throw new ConnectionException("Error creating new pooled "
                                              + "connection: " + e.getMessage(),
                                              e);
            }
        }

        activeConnections++;
        pconn.addConnectionEventListener (poolConnectionEventListener);
        assertInnerState();

        return pconn;
    }

    private synchronized void recycleConnection (PooledConnection pconn) {
        if (isDisposed) {
            disposeConnection (pconn);
            return;
        }

        if (activeConnections <= 0) {
            throw new AssertionError();
        }

        activeConnections--;
        semaphore.release();
        recycledConnections.push (pconn);
        assertInnerState();

        if (log.isTraceEnabled()) {
            log.trace("Recycled connection " + pconn.hashCode()
                      + ", now " + getActiveConnections()
                      + " active connections");
        }
    }

    private synchronized void disposeConnection (PooledConnection pconn) {
        if (activeConnections <= 0) {
            throw new AssertionError();
        }

        log.debug("Disposing of connection " + pconn.hashCode());

        activeConnections--;
        semaphore.release();
        closeConnectionNoEx(pconn);
        assertInnerState();
    }

    private void closeConnectionNoEx (PooledConnection pconn) {
        try {
            pconn.close();
        } catch (SQLException e) {
            log.warn ("Error while closing database connection: "
                      + e.getMessage(), e);
        }
    }

    private void assertInnerState() {
        if (activeConnections < 0) {
            throw new AssertionError();
        }

        if (activeConnections+recycledConnections.size() > maxConnections) {
            throw new AssertionError();
        }

        if (activeConnections+semaphore.availablePermits() > maxConnections) {
            throw new AssertionError();
        }
    }

    private class PoolConnectionEventListener implements
                                                       ConnectionEventListener {
        public void connectionClosed (ConnectionEvent event) {
            PooledConnection pconn = (PooledConnection)event.getSource();
            pconn.removeConnectionEventListener (this);
            recycleConnection (pconn); // Reuse the connection

            if (log.isTraceEnabled()) {
                log.trace("Handled ConnectionClosedEvent for "
                          + pconn.hashCode()
                          + ", now " + getActiveConnections()
                          + " active connections");
            }
        }

        public void connectionErrorOccurred (ConnectionEvent event) {
            log.trace("ConnectionErrorEvent: "
                      + event.getSQLException().getMessage(),
                      event.getSQLException());

            PooledConnection pconn = (PooledConnection)event.getSource();
            pconn.removeConnectionEventListener (this);
            disposeConnection (pconn); // Drop the connection

            log.info("Handled ConnectionErrorEvent, now "
                     + getActiveConnections() + " active connections. "
                     + "Error was: " + event.getSQLException().getMessage(),
                     event.getSQLException());
        }
    }

    /**
     * The purpose of this listener is to close the parent connection
     * when a prepared statement is closed.
     */
    private class PoolStatementEventListener implements StatementEventListener {

        public void statementClosed(StatementEvent event) {
            PooledConnection pconn = (PooledConnection)event.getSource();
            pconn.removeStatementEventListener(this);
            recycleConnection(pconn);

            if (log.isTraceEnabled()) {
                log.trace("Handled StatementClosedEvent for " + pconn.hashCode()
                          + ", now " + getActiveConnections()
                          + " active connections");
            }

        }

        public void statementErrorOccurred(StatementEvent event) {
            log.trace("StatementErrorEvent: "
                      + event.getSQLException().getMessage(),
                      event.getSQLException());

            PooledConnection pconn = (PooledConnection)event.getSource();
            pconn.removeStatementEventListener(this);
            disposeConnection(pconn); // Mark connection for disposal

            log.info("Handled ConnectionErrorEvent, now "
                     + getActiveConnections() + " active connections. "
                     + "Error was: " + event.getSQLException().getMessage(),
                     event.getSQLException());
        }
    }

    /**
     * Returns the number of active (open) connections of this pool.
     * This is the number of <code>Connection</code> objects that have been
     * issued by {@link #getConnection()} for which
     * <code>Connection.close()</code> has not yet been called.
     * @return the number of active connections.
     **/
    public synchronized int getActiveConnections() {
        return activeConnections;
    }

}
