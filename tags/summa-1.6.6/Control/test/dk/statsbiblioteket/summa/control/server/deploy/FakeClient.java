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
/*
 * The State and University Library of Denmark
 * CVS:  $Id$
 */
package dk.statsbiblioteket.summa.control.server.deploy;

import java.rmi.RemoteException;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.bundle.BundleRepository;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class FakeClient implements ClientConnection {
    private Log log = LogFactory.getLog(FakeClient.class);

    public FakeClient(Configuration configuration) {
        log.info("Got created");
    }

    public void stop() throws RemoteException {
        log.info("Stop called");
        return; //new Status(Status.CODE.stopped, "Fake status says stopped");
    }

    public Status getStatus() throws RemoteException {
        log.info("Status called");
        return new Status(Status.CODE.running, "Fake status");
    }

    public String deployService(String bundleId, String instanceId,
                              String confLocation) throws RemoteException {
        log.info("Fake deploying bundle " + bundleId + " with config location "
                + confLocation + " using instance id '" + instanceId + "'");
        return "fake-instance-id";
    }

    public void removeService(String instanceId) throws IOException {
        log.info("Remove service '" + instanceId + "'");
    }

    public void startService(String id,
                             String confLocation) throws RemoteException {
        log.info("Fake starting " + id);
    }


    public void stopService(String id) throws RemoteException {
        log.info("Fake stopping " + id);
    }

    public Status getServiceStatus(String id) throws RemoteException {
        log.info("Faking status for " + id);
        return new Status(Status.CODE.running, "Fake status for " + id);
    }

    public Service getServiceConnection(String id) throws IOException {
       throw new UnsupportedOperationException();
    }

    public List<String> getServices() throws RemoteException {
        log.info("Faking getServices");
        ArrayList<String> list = new ArrayList<String>(1);
        list.add("Fake ID");
        return list;
    }

    public String getId() throws RemoteException {
        return "Fake Id";
    }

    public BundleRepository getRepository() throws IOException {
        return null;
    }

    public String getBundleSpec(String instanceId) throws IOException {
        return "<bundle></bundle>";
    }

    public void reportError(String id) throws IOException {
        log.info ("Error reported on: " + id);
    }
}




