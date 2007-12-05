/* $Id: RemoteCacheService.java,v 1.2 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.2 $
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
package dk.statsbiblioteket.summa.dice.caching;

import dk.statsbiblioteket.summa.dice.util.RegistryManager;
import dk.statsbiblioteket.summa.dice.Config;
import dk.statsbiblioteket.summa.dice.Constants;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.registry.Registry;
import java.io.IOException;

/**
 * Simple RMI service that wraps an underlying local {@link Cache}.
 * It will load default settings from {@link Constants#DEFAULT_RESOURCE}.
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment = "Needs javadocs")
public class RemoteCacheService<E> extends UnicastRemoteObject implements RemoteCache<E> {

    private Cache<E> cache;
    private String serviceName;

    public RemoteCacheService (Cache<E> backend,
                               String serviceName,
                               int port,
                               RMIClientSocketFactory csf,
                               RMIServerSocketFactory ssf) throws RemoteException {
        super (port, csf, ssf);
        this.cache = backend;
        this.serviceName = serviceName;

        System.setSecurityManager(new RMISecurityManager());

        // TODO: The registry port should be passed to the constructor to avoid having to load a config manually
        Config conf = new Config();
        try {
            conf.loadDefaults();
        } catch (IOException e) {
            throw new RuntimeException ("Failed to load default config from " + Config.DEFAULT_RESOURCE);
        }
        
        Registry reg;
        try {
            reg = RegistryManager.getRegistry(conf);
        } catch (RemoteException e) {
            throw new RemoteException("Failed to get Registy on port: " + conf.getRegistryPort(), e);
        }

        try {
            reg.rebind(serviceName, this);
        } catch (RemoteException e) {
            throw new RemoteException ("Failed to bind remote cache to: " + serviceName, e);
        }
    }

    public long startPut() throws RemoteException {
        try {
            return cache.startPut();
        } catch (IOException e) {
            throw new RemoteException("Failed to startPut, " + e);
        }

    }

    public void putPart(E part, long id) throws RemoteException {
        try {
            cache.putPart(part, id);
        } catch (IOException e) {
            throw new RemoteException("Failed to putPart for id " + id + ", " + e);
        }
    }

    public void endPut(long id) throws RemoteException {
        try {
            cache.endPut(id);
        } catch (IOException e) {
            throw new RemoteException("Failed to endPut for id " + id + ", " + e);
        }
    }

    public long lookup(long id) throws RemoteException {
        try {
            return cache.lookup(id);
        } catch (IOException e) {
            throw new RemoteException("Failed to open item with id "+ id +", " + e);
        }
    }

    public E readPart(long handle) throws RemoteException {
        try {
            return cache.readPart(handle);
        } catch (IOException e) {
            throw new RemoteException("Failed to readPart from handle " + handle + ", " + e);
        }
    }

    public void close(long handle) throws RemoteException {
        try {
            cache.close(handle);
        } catch (IOException e) {
            throw new RemoteException("Failed to close handle " + handle + ", " + e);
        }
    }

    public String getDataURL(long id) throws RemoteException {
        try {
            return cache.getDataURL(id);
        } catch (IOException e) {
            throw new RemoteException("Failed to obtain data url for id " + id + ", " + e);
        }
    }
}
