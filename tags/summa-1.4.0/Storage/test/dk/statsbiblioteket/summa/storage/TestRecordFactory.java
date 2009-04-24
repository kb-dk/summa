package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.common.Record;

/**
 * A test-helper tool for creating dummy records
 */
public class TestRecordFactory {

    public static long recordId = 0;
    public static String recordContent = "Dummy content";
    public static String recordBase = "dummyBase";

    /**
     * Create a new dummy record.
     * <p/>
     * The public static variable {@link #recordId} is guaranteed to contain
     * the id of the last produced record.
     *
     * @return A newly created record. After this method call {@link #recordId}
     *         will contain the id of the returned record
     */
    public static Record next () {
        recordId++;
        return new Record (""+ recordId, recordBase,
                           recordContent.getBytes());

    }
}
