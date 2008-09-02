/* $Id: RegistryManager.java,v 1.4 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/04 13:28:17 $
 * $Author: te $
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
package dk.statsbiblioteket.summa.dice.util;

import dk.statsbiblioteket.summa.dice.Config;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Manage a {@link Registry} running on the port given by the
 * <code>summa.dice.registry.port</code> property.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class RegistryManager {

    private static final Log log = LogFactory.getLog(RegistryManager.class);

    /**
     * Return a ref to a registry running on the port specified by the
     * <code>summa.dice.registry.port</code> property. Create the registry
     * if necessary.
     * The value of the <code>summa.dice.rmi.socket</code> can be one of
     * <code>gzip</code>, <code>normal</code>, or <code>ssl</code>.
     * @param conf configuration to extract above mentioned parameters from
     * @return a ref to the running registry
     * @throws RemoteException If the registry could not be created or retrieved
     */
    public synchronized static Registry getRegistry (Config conf) throws RemoteException {

        Registry reg = null;

        try {
            // Just try and create the registry instead of checking first.
            // This is to avoid the implied race condition on registry creation
            reg = LocateRegistry.createRegistry(conf.getRegistryPort(),
                                                conf.getClientSocketFactory(),
                                                conf.getServerSocketFactory());
            log.info ("Created registry with socket type: " + conf.getSocketType() + " on port " + conf.getRegistryPort());

        } catch (RemoteException e1) {
            try {
                   // A Registry might already be running on the configured port
                   reg = LocateRegistry.getRegistry("localhost", conf.getRegistryPort(), conf.getClientSocketFactory());
                   log.info ("Registry already running. Returning referance.");

            } catch (Exception e2) {
                throw new RemoteException("RegistryManager failed to retrieve Registry", e2);
            }
        }

        return reg;
    }

    /**
     * Get a referance to a remote registry. The registry is automatically configured to
     * use the {@link RMIClientSocketFactory} specified in <code>summa.dice.rmi.socket</code.
     * @param hostname Hostname of remote registry
     * @param port Port the remote registry is running on
     * @param conf needed to retrieve a {@link RMIClientSocketFactory}
     * @throws RemoteException
     */
    public static Registry getRemoteRegistry (String hostname, int port, Config conf) throws RemoteException {
        Registry reg = LocateRegistry.getRegistry(hostname, port, conf.getClientSocketFactory());
        return reg;
    }

}
