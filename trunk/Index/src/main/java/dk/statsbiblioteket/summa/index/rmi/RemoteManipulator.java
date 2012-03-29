/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.index.rmi;

import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.index.IndexManipulator;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Proxy interface to facilitate exposing {@link IndexManipulator}s over RMI.
 * To expose a manipulator over RMI use the wrapper class
 * {@link RMIManipulatorProxy}
 */
public interface RemoteManipulator extends IndexManipulator, Remote {

    @Override
    public void open(File indexRoot) throws RemoteException;

    @Override
    public void clear() throws RemoteException;

    @Override
    public boolean update(Payload payload) throws RemoteException;

    @Override
    public void commit() throws RemoteException;

    @Override
    public void consolidate() throws RemoteException;

    @Override
    public void close() throws RemoteException;

    @Override
    void orderChangedSinceLastCommit() throws RemoteException;

    @Override
    boolean isOrderChangedSinceLastCommit() throws RemoteException;
}

