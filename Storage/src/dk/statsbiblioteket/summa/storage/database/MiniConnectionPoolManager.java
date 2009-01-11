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
import java.util.concurrent.Semaphore;
import java.util.Stack;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;

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
    private PrintWriter                    logWriter;
    private Semaphore                      semaphore;
    private Stack<PooledConnection>        recycledConnections;
    private int                            activeConnections;
    private PoolConnectionEventListener    poolConnectionEventListener;
    private boolean                        isDisposed;

    /**
     * Thrown in {@link MiniConnectionPoolManager#getConnection()} when no free
     * connection becomes available within <code>timeout</code> seconds.
     */
    public static class TimeoutException extends RuntimeException {

        private static final long serialVersionUID = 1;

        public TimeoutException () {
            super ("Timeout while waiting for a free database connection."); }
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
        this.dataSource = dataSource;
        this.maxConnections = maxConnections;
        this.timeout = timeout;

        try {
            logWriter = dataSource.getLogWriter(); }
        catch (SQLException e) {

        }

        if (maxConnections < 1) {
            throw new IllegalArgumentException("Invalid maxConnections value.");
        }

        semaphore = new Semaphore(maxConnections,true);
        recycledConnections = new Stack<PooledConnection>();
        poolConnectionEventListener = new PoolConnectionEventListener(); }

    /**
     * Closes all unused pooled connections.
     */
    public synchronized void dispose() throws SQLException {
        if (isDisposed) {
            return;
        }

        isDisposed = true;
        SQLException e = null;

        while (!recycledConnections.isEmpty()) {
            PooledConnection pconn = recycledConnections.pop();
            try {
                pconn.close();
            } catch (SQLException e2) {
                if (e == null) {
                    e = e2;
                }
            }
        }

        if (e != null) {
            throw e;
        }
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
    public Connection getConnection() throws SQLException {
        // This routine is unsynchronized, because semaphore.tryAcquire() may block.
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
            throw new RuntimeException("Interrupted while waiting for "
                                       + "a database connection.",e);
        }

        boolean ok = false;
        try {
            Connection conn = getConnection2();
            ok = true;
            return conn;
        } finally {
            if (!ok) {
                semaphore.release();
            }
        }
    }

    private synchronized Connection getConnection2() throws SQLException {
        if (isDisposed) {
            throw new IllegalStateException("Connection pool has "
                                            + "been disposed.");   // test again with lock
        }

        PooledConnection pconn;

        if (!recycledConnections.empty()) {
            pconn = recycledConnections.pop();
        } else {
            pconn = dataSource.getPooledConnection();
        }

        Connection conn = pconn.getConnection();
        activeConnections++;
        pconn.addConnectionEventListener (poolConnectionEventListener);
        assertInnerState();

        return conn;
    }

    private synchronized void recycleConnection (PooledConnection pconn) {
        if (isDisposed) {
            disposeConnection (pconn); return;
        }

        if (activeConnections <= 0) {
            throw new AssertionError();
        }

        activeConnections--;
        semaphore.release();
        recycledConnections.push (pconn);
        assertInnerState();
    }

    private synchronized void disposeConnection (PooledConnection pconn) {
        if (activeConnections <= 0) {
            throw new AssertionError();
        }

        activeConnections--;
        semaphore.release();
        closeConnectionNoEx (pconn);
        assertInnerState();
    }

    private void closeConnectionNoEx (PooledConnection pconn) {
        try {
            pconn.close();
        } catch (SQLException e) {
            log ("Error while closing database connection: "+e.toString());
        }
    }

    private void log (String msg) {
        String s = "MiniConnectionPoolManager: "+msg;

        try {
            if (logWriter == null)
                System.err.println (s);
            else
                logWriter.println (s);
        } catch (Exception e) {

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
            recycleConnection (pconn);
        }

        public void connectionErrorOccurred (ConnectionEvent event) {
            PooledConnection pconn = (PooledConnection)event.getSource();
            pconn.removeConnectionEventListener (this);
            disposeConnection (pconn);
        }
    }

    /**
     * Returns the number of active (open) connections of this pool.
     * This is the number of <code>Connection</code> objects that have been
     * issued by {@link #getConnection()} for which <code>Connection.close()</code>
     * has not yet been called.
     * @return the number of active connections.
     **/
    public synchronized int getActiveConnections() {
        return activeConnections;
    }

}
