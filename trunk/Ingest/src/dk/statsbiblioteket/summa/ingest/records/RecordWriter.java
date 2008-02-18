/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.ingest.records;

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.net.MalformedURLException;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.ingest.RecordFilter;
import dk.statsbiblioteket.summa.ingest.StreamFilter;
import dk.statsbiblioteket.summa.storage.io.Access;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Connects to a MetadataStorage and ingests received Records into the storage.
 * The MetadataStorage is accessed via RMI at the address specified by
 * {@link #CONF_METADATA_STORAGE}.
 * </p><p>
 * Note: This RecordFilter can only be chained after another RecordFilter.
 */
public class RecordWriter implements RecordFilter {
    private static final Log log = LogFactory.getLog(RecordWriter.class);

    public static final String CONF_METADATA_STORAGE =
            "RecordWriter.MetadataStorage";

    private RecordFilter source;
    private Access access;

    public RecordWriter(Configuration configuration) throws RemoteException {
        log.trace("Constructing RecordWriter");
        String accessPoint;
        try {
            accessPoint = configuration.getString(CONF_METADATA_STORAGE);
        } catch (Exception e) {
            throw new RemoteException("Unable to get the RMI address for the "
                                      + "remote MetadataStorage from the "
                                      + "configuration with key '"
                                      + CONF_METADATA_STORAGE + "'");
        }
        log.debug("Connecting to the access point '" + accessPoint + "'");
        try {
            access = (Access)Naming.lookup(accessPoint);
        } catch (NotBoundException e) {
            throw new RemoteException("NotBoundException for RMI lookup for '"
                                      + accessPoint + "'", e);
        } catch (MalformedURLException e) {
            throw new RemoteException("MalformedURLException for RMI lookup "
                                      + "for '" + accessPoint + "'", e);
        } catch (RemoteException e) {
            throw new RemoteException("RemoteException performing RMI lookup "
                                      + "for '" + accessPoint + "'", e);
        } catch (Exception e) {
            throw new RemoteException("Exception performing RMI lookup "
                                      + "for '" + accessPoint + "'", e);
        }
        log.debug("Connected to MetadataStorage at '" + accessPoint + "'");
        // TODO: Perform a check to see if the MetadataStorage is alive
    }

    public void setSource(StreamFilter source) {
        throw new UnsupportedOperationException("RecordWriter cannot use a "
                                                + "StreamFilter as a source");
    }

    public void setSource(RecordFilter source) {
        log.trace("setSource(" + source + ") called");
        this.source = source;
    }

    /**
     * Pumping Records with this methis has the side-effect of flushing them
     * to the MetadataStorage.
     * @return the next Record or null if there are no more Records.
     * @throws IOException if the Record could not be retrieved or if a fatal
     *         error occured when storing it to MetadataStorage.
     */
    public Record getNextRecord() throws IOException {
        log.trace("getNextRecord called");
        Record record = source.getNextRecord();
        if (record == null) {
            log.debug("null received in getNextRecord. This means that there "
                      + "are no more Records available");
            return null;
        }
        try {
            if (log.isTraceEnabled()) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("Flushing " + record);
            } else {
                log.debug("Flushing record '" + record.getId() + "'");
            }
            access.flush(record);
        } catch (RemoteException e) {
            log.error("Exception flushing " + record, e);
            // TODO: Consider checking for fatal errors (the connection is down)
        }
        return record;
    }

    public void close(boolean success) {
        log.trace("Closing RecordWriter with success " + success);
        // TODO: Close the access properly
        log.trace("Closing the source for RecordWriter");
        source.close(success);
    }

    public boolean hasMoreRecords() throws IOException {
        return source.hasMoreRecords();
    }
}
