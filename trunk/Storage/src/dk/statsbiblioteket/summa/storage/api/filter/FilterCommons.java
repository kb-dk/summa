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
package dk.statsbiblioteket.summa.storage.api.filter;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.RMIConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.rmi.RemoteStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class for Storage-related filters.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FilterCommons {
    private static final Log log = LogFactory.getLog(FilterCommons.class);

    /* FIXME: Remove this hard coded hack to RMI */
    private static ConnectionFactory<? extends Storage> storageConnectionFactory =
            new RMIConnectionFactory<RemoteStorage>();
    private static ConnectionManager<Storage> accessConnectionManager =
            new ConnectionManager<Storage>(storageConnectionFactory);

    /**
     * Creates a ConnectionContext for the Storage specified in accessKey.
     * After use, resources are released with {@link #releaseAccess}.
     * @param conf contains the Storage address.
     * @param accessKey     the key for the Storage address. The Storage address
     *                      is an standard RMI address, such as
     *                      "//localhost:6789/storage".
     * @return a Connection Context to access.
     *         See {@link ConnectionContext} for usage.
     */
    public static synchronized ConnectionContext<Storage> getAccess(
                                         Configuration conf, String accessKey) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("getAccess(..., " + accessKey + ") called");
        String accessPoint;
        accessPoint = conf.getString(accessKey,
                                     "//localhost:28000/summa-storage");

        log.debug("Connecting to the access point '" + accessPoint + "'");
        return accessConnectionManager.get(accessPoint);
    }

    /**
     * Release the resources taken by the Storage-context. Call this when the
     * context aren't used anymore.
     * @param context the context for Storage.
     */
    public static synchronized void releaseAccess(
                                           ConnectionContext<Storage> context) {
        accessConnectionManager.release(context);
    }
}
