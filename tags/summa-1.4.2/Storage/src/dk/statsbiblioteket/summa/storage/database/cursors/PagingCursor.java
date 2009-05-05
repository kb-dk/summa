package dk.statsbiblioteket.summa.storage.database.cursors;

import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.common.Record;

import java.util.NoSuchElementException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 */
public class PagingCursor implements Cursor {

    private static final Log log = LogFactory.getLog(PagingCursor.class);

    private long pageRecords;
    private long totalRecords;
    private long firstAccess;
    private long lastAccess;
    
    private long key;
    private long lastMtimeTimestamp;
    private ResultSetCursor page;
    private Record nextRecord;
    private DatabaseStorage db;

    public PagingCursor(DatabaseStorage db,
                        ResultSetCursor firstPage) {
        this.db = db;
        page = firstPage;

        // This will always be unique, so no key collision
        key = db.getTimestampGenerator().next();
        lastAccess = db.getTimestampGenerator().systemTime(key);
        firstAccess = 0;

        lastMtimeTimestamp = 0;
        pageRecords = 0;
        totalRecords = 0;

        if (page.hasNext()) {
            nextRecord = page.next();
        } else {
            nextRecord = null;
        }

        log.debug("Created " + this + " for " + firstPage);
    }

    @Override
    public String getBase() {
        return page.getBase();
    }

    @Override
    public long getLastAccess() {
        return lastAccess;
    }

    @Override
    public long getKey () {
        return key;
    }

    @Override
    public QueryOptions getQueryOptions() {
        return page.getQueryOptions();
    }

    @Override
    public boolean hasNext() {
        lastAccess = System.currentTimeMillis();
        return nextRecord != null;
    }

    @Override
    public Record next () {
        lastAccess = System.currentTimeMillis();

        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        if (pageRecords == 0) {
            firstAccess = lastAccess; // Set to 'now'
        }

        Record rec = nextRecord;
        lastMtimeTimestamp = page.currentMtimeTimestamp();

        nextRecord = nextValidRecord();
        pageRecords++;
        totalRecords++;

        return rec;
    }

    private Record nextValidRecord () {
        // Note that ths method does not need to care about filtering out
        // records that do not match the query options. This is done by
        // the 'page' (ResultSetCursor)

        if (page.hasNext()) {
            return page.next();
        }

        if (log.isDebugEnabled()) {
            log.debug(this + " depleted " + page + " after " + pageRecords
                      + " records. Total " + totalRecords + " records in "
                      + (System.currentTimeMillis() - firstAccess) + "ms");
        }

        // page is depleted
        page.close();
        pageRecords = 0;

        try {
            page = db.getRecordsModifiedAfterCursor(lastMtimeTimestamp,
                                                    getBase(),
                                                    getQueryOptions());
            if (log.isTraceEnabled()) {
                log.trace("Got new page for " + this + ": " + page);
            }
        } catch (IOException e) {
            log.warn("Failed to execute query for next page: "
                     + e.getMessage(), e);
            logDepletedStats();
            return null;
        }

        if (page.hasNext()) {
            return page.next();
        }

        page.close();
        logDepletedStats();
        return null;
    }

    private void logDepletedStats () {
        log.debug(this + " depleted after " + totalRecords + " records and "
                  + (lastAccess - firstAccess) + "ms");
    }

    @Override
    public void remove () {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        log.debug("Closing " + this);
        page.close();
    }

    public String toString() {
        return "PagingCursor[" + key + "]";
    }

}
