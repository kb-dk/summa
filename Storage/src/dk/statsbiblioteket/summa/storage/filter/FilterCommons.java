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

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.RMIConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.summa.storage.io.Access;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.net.MalformedURLException;
import java.util.Map;

/**
 * Utility class for Storage-related filters.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FilterCommons {
    private static final Log log = LogFactory.getLog(FilterCommons.class);

    private static ConnectionFactory<Access> accessConnectionFactory =
            new RMIConnectionFactory<Access>();
    private static ConnectionManager<Access> accessConnectionManager =
            new ConnectionManager<Access>(accessConnectionFactory);

    /**
     * Creates a ConnectionContext for the Access specified in accessKey.
     * After use, resources are released with {@link #releaseAccess}.
     * @param configuration contains the Access address.
     * @param accessKey     the key for the Access address. The Access address
     *                      is an standard RMI address, such as
     *                      "//localhost:6789/storage".
     * @return a Connection Context to access.
     *         See {@link ConnectionContext} for usage.
     */
    public static synchronized ConnectionContext<Access> getAccess(
            Configuration configuration, String accessKey) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("getAccess(..., " + accessKey + ") called");
        String accessPoint;
        try {
            accessPoint = configuration.getString(accessKey);
        } catch (Exception e) {
            throw new Configurable.ConfigurationException(
                    "Unable to get the RMI address for the remote "
                    + "Storage from the configuration with key '"
                    + accessKey + "'");
        }
        log.debug("Connecting to the access point '" + accessPoint + "'");
        return accessConnectionManager.get(accessPoint);
    }

    /**
     * Release the resources taken by the Access-context. Call this when the
     * context aren't used anymore.
     * @param context the context for Access.
     */
    public static synchronized void releaseAccess(
            ConnectionContext<Access> context) {
        accessConnectionManager.release(context);
    }
}
