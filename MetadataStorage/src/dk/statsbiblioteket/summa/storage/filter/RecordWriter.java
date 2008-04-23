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
package dk.statsbiblioteket.summa.storage.filter;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.storage.io.Access;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Connects to a MetadataStorage and ingests received Records into the storage.
 * The MetadataStorage is accessed via RMI at the address specified by
 * {@link #CONF_METADATA_STORAGE}.
 * </p><p>
 * Note: This ObjectFilter can only be chained after another ObjectFilter.
 * </p><p>
 * Note: Only Record is stored. All other data in Payload is ignored.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RecordWriter extends ObjectFilterImpl {
    private static final Log log = LogFactory.getLog(RecordWriter.class);

    /**
     * The Storage to connect to. This is a standard RMI address.
     * Example: //localhost:6789/;

     */
    public static final String CONF_METADATA_STORAGE =
            "summa.storage.RecordWriter.MetadataStorage";

    private Access access;

    public RecordWriter(Configuration configuration) {
        log.trace("Constructing RecordWriter");
        String accessPoint;
        try {
            accessPoint = configuration.getString(CONF_METADATA_STORAGE);
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Unable to get the RMI address for the remote "
                    + "MetadataStorage from the configuration with key '"
                    + CONF_METADATA_STORAGE + "'");
        }
        log.debug("Connecting to the access point '" + accessPoint + "'");
        try {
            access = (Access)Naming.lookup(accessPoint);
        } catch (NotBoundException e) {
            throw new ConfigurationException("NotBoundException for RMI lookup "
                                             + "for '" + accessPoint + "'", e);
        } catch (MalformedURLException e) {
            throw new ConfigurationException("MalformedURLException for RMI "
                                             + "lookup for '" + accessPoint
                                             + "'", e);
        } catch (RemoteException e) {
            throw new ConfigurationException("RemoteException performing RMI "
                                             + "lookup for '" + accessPoint
                                             + "'", e);
        } catch (Exception e) {
            throw new ConfigurationException("Exception performing RMI lookup "
                                             + "for '" + accessPoint + "'", e);
        }
        log.debug("Connected to MetadataStorage at '" + accessPoint + "'");
        // TODO: Perform a check to see if the MetadataStorage is alive
    }

    /**
     * Flushes Records to Storage.
     * @param payload the Payload containing the Record to flush.
     */
    protected void processPayload(Payload payload) {
        Record record = payload.getRecord();
        if (record == null) {
            throw new IllegalStateException("null received in Payload in next()"
                                            + ". This should not happen");
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
    }

}