package dk.statsbiblioteket.summa.storage.database.cursors;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;

import java.util.Iterator;

/**
 * Convenience interface to access and manage SQL ResultSets
*/
public interface Cursor extends Iterator<Record> {
    public void close();

    public long getKey();

    public long getLastAccess();

    public QueryOptions getQueryOptions();

    public String getBase();
}
