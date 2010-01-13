/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2009  The State and University Library
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
package dk.statsbiblioteket.summa.storage.api.filter;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.summa.storage.api.StorageWriterClient;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;

/**
 * Handles full dumps for a given base into a given Storage. When all data has
 * been ingested, a script is called on Storage. Normally the script deletes all
 * Records for the given base that has not been processed since creation of
 * the FullDumpFilter,
 * </p><p>
 * Upon creation, the timestamp for the latest updated Record for the given
 * base in the Storage is noted. When close (true) is called, all Records for
 * the given base, up to the given timestamp, are processed with a script
 * (normally delete), if at least n Records has passed through the filter.
 * <p><p>
 * Note: The Storage connection is defined with
 * {@link ConnectionConsumer#CONF_RPC_TARGET}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FullDumpFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(FullDumpFilter.class);

    /**
     * The base to get the timestamp from and to call the script on.
     * </p><p>
     * Mandatory. The empty String ("") designates all bases.
     */
    public static final String CONF_BASE = "fulldump.base";

    /**
     * The minimum Records that must pass before close, in order for the
     * Storage script to be called.
     * </p><p>
     * If more than 1 but less than the given number of Records has passed when
     * close is called, a warning is raised.
     * </p><p>
     * Optional. Default is 1.
     */
    public static final String CONF_MIN_RECORDS = "fulldump.minrecords";
    public static final int DEFAULT_MIN_RECORDS = 1;

    /**
     * The script that is to be called on Storage when {link #CONF_MIN_RECORDS}
     * has passed and {@code close(true)} is called.
     * </p><p>
     * Optional. Default is "delete.job.js" that deletes all Records for the
     * base up to the timestamp extracted upon creation.
     */
    public static final String CONF_SCRIPT = "fulldump.script";
    public static final String DEFAULT_SCRIPT = "delete.job.js";

    private String base;
    private int minRecords = DEFAULT_MIN_RECORDS;
    private String script = DEFAULT_SCRIPT;
    private long startupTimestamp;
    private long received = 0;
    private StorageWriterClient writer;

    public FullDumpFilter(Configuration conf) {
        super(conf);
        if (!conf.valueExists(CONF_BASE)) {
            throw new ConfigurationException(String.format(
                    "The entry %s is mandatory but not present", CONF_BASE));
        }
        base = conf.getString(CONF_BASE);
        minRecords = conf.getInt(CONF_MIN_RECORDS, minRecords);
        script = conf.getString(CONF_SCRIPT, script);
        try {
            startupTimestamp = getStartupTimestamp(conf);
        } catch (IOException e) {
            throw new ConfigurationException(String.format(
                    "Exception while trying to retrieve last modified timestamp"
                    + " for base '%s' from '%s'",
                    base,
                    conf.getString(ConnectionConsumer.CONF_RPC_TARGET, "N/A")));
        }
        writer = new StorageWriterClient(conf);
        feedback = false;
    }

    private long getStartupTimestamp(Configuration conf) throws IOException {
        StorageReaderClient reader = new StorageReaderClient(conf);
        return reader.getModificationTime(base);
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        received++;
        return true;
    }

    // TODO: Consider if shutdown should be handled in hasNext instead
    @Override
    public void close(boolean success) {
        super.close(success);
        if (!success) {
            log.warn(String.format(
                    "close(false): The close-script '%s' for base %s will not "
                    + "be called",
                    script, base));
        } else if (received == 0) {
            log.info(String.format(
                    "close(true): No Record received for base %s. "
                    + "The close-script '%s' will not be called",
                    base, script));
        } else  if (received < minRecords) {
            log.warn(String.format(
                    "close(true): %d Records received, but %d is required in "
                    + "order to run the script '%s'",
                    received, minRecords, script));
        } else {
            log.info(String.format(
                    "close(true): %d Records received, calling script '%s' on "
                    + "base '%s' for Records with timestamp <= %s",
                    received, script, base,
                    String.format(ISO_TIME, startupTimestamp)));
            try {
                // +1 as batch maxMTime is <, not <=
                String result = writer.batchJob(
                               script, base, 0, startupTimestamp+1, null);
                if (log.isTraceEnabled()) {
                    log.info(String.format(
                            "Script '%s' successfully executed with output"
                            + " '%s'",
                            script, result));
                } else {
                    log.info(String.format(
                            "Script '%s' successfully executed with %d lines "
                            + "in the output",
                            script, countLines(result)));

                }
                } catch (IOException e) {
                String message = String.format(
                        "Exception while calling script '%s' on base '%s' with"
                        + " endTime %s",
                        script, base,
                        String.format(ISO_TIME, startupTimestamp));
                log.error(message, e);
                throw new RuntimeException(message, e);
            }
        }
    }

    private long countLines(String result) {
        if (result == null || "".equals(result)) {
            return 0;
        }
        long nls = 0;
        final char NL = "\n".charAt(0); // We cheat a bit
        for (int i = 0 ; i < result.length() ; i++) {
            if (NL == result.charAt(i)) {
                nls++;
            }
        }
        return nls;
    }

    private static final String ISO_TIME = "%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS";

}
