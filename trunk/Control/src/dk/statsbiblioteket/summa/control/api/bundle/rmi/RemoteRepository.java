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
package dk.statsbiblioteket.summa.control.api.bundle.rmi;

import dk.statsbiblioteket.summa.control.api.bundle.BundleRepository;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.File;
import java.util.List;

/**
 * Stub interface for RMI implementations of {@link dk.statsbiblioteket.summa.control.api.bundle.BundleRepository}
 */
public interface RemoteRepository extends BundleRepository, Remote {

    @Override
    public File get (String bundleId) throws RemoteException;

    @Override
    public List<String> list (String regex) throws RemoteException;

    @Override
    public String expandApiUrl (String jarFileName) throws RemoteException;
}




