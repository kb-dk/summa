/* $Id: RemoteStorageMBean.java,v 1.4 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/04 13:28:21 $
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
package dk.statsbiblioteket.summa.common.configuration.storage;

import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.Serializable;
import java.util.Map;
import java.util.Iterator;

/**
 * Interface for exposing a remote {@link ConfigurationStorage} both as a JMX
 * bean and rmi service.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface RemoteStorageMBean extends Remote, ConfigurationStorage {

    public static final String THIS_PORT = "this.service.port";
    public static final String THIS_NAME = "this.service.name";
    public static final String THIS_REGISTRY_PORT = "this.registry.port";
    public static final String THIS_REGISTRY_HOST = "this.registry.host";

    public void put(String key, Serializable value) throws RemoteException;

    public Serializable get(String key) throws RemoteException;

    public Iterator<Map.Entry<String,Serializable>> iterator() throws
                                                               RemoteException;

    public void purge(String key) throws RemoteException;

    public String[] getConfigDump() throws RemoteException;

    public boolean supportsSubStorage() throws RemoteException;

    public ConfigurationStorage getSubStorage(String key) throws
                                                          RemoteException;

    public ConfigurationStorage createSubStorage(String key) throws
                                                             RemoteException;
}
