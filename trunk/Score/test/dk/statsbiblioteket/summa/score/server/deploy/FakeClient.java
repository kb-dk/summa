/* $Id: FakeClient.java,v 1.7 2007/10/11 12:56:25 te Exp $
 * $Revision: 1.7 $
 * $Date: 2007/10/11 12:56:25 $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: FakeClient.java,v 1.7 2007/10/11 12:56:25 te Exp $
 */
package dk.statsbiblioteket.summa.score.server.deploy;

import java.rmi.RemoteException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

import dk.statsbiblioteket.summa.score.api.ClientConnection;
import dk.statsbiblioteket.summa.score.api.Status;
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

    public void deployService(String id,
                              String confLocation) throws RemoteException {
        log.info("Fake deploying " + id + " with config location " + confLocation);
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

    public List<String> getServices() throws RemoteException {
        log.info("Faking getServices");
        ArrayList<String> list = new ArrayList<String>(1);
        list.add("Fake ID");
        return list;
    }

    public String getId() throws RemoteException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
