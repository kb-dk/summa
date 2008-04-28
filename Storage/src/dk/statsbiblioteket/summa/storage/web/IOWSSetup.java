/* $Id: IOWSSetup.java,v 1.5 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/05 10:20:23 $
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
package dk.statsbiblioteket.summa.storage.web;

import dk.statsbiblioteket.summa.storage.io.Access;
import dk.statsbiblioteket.summa.storage.io.AccessRead;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import java.rmi.RMISecurityManager;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.net.MalformedURLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class IOWSSetup {
    public static final int DEFAULT_RMI_PORT = 1099;
    public static final int DEFAULT_SERVICE_PORT = 9901;

    private static String remoteStoreServer;
    private static String remoteStoreServiceName;
    private static String remoteStoreServicePort;

    private static final String prop = "IOAccessWS.properties";

    private static AccessRead store = null;
    private static Log log = LogFactory.getLog(IOWSSetup.class);

    public static void setup(){
        System.out.println("is in io constructor");
        final ClassLoader loader =
                Thread.currentThread().getContextClassLoader();

        final InputStream in = loader.getResourceAsStream(prop);
        Properties p = new Properties();

        try {
            p.loadFromXML(in);
            log.info("loading settings from IOAccessWS.properties");
            remoteStoreServer = p.getProperty("remoteStoreServer");
            remoteStoreServiceName = p.getProperty("remoteStoreServiceName");
            remoteStoreServicePort = p.getProperty("remoteStoreServicePort",
                                        Integer.toString(DEFAULT_SERVICE_PORT));
            in.close();
        } catch (IOException e) {
            log.error("Cannot load properties", e);
        }

        System.out.println("Properties loaded");
        if (System.getSecurityManager() == null)
            System.setSecurityManager(new RMISecurityManager());
        try {
            store = (Access) Naming.lookup("//" + remoteStoreServer + ":"
                                           + remoteStoreServicePort + "/"
                                           + remoteStoreServiceName);
            log.info("Got ref to remoteStore: //" + remoteStoreServer + ":"
                     + remoteStoreServicePort + "/" + remoteStoreServiceName );
            System.out.println("got rmi store");
        } catch (MalformedURLException e) {
            log.fatal("Error getting remote store:", e);

        } catch (NotBoundException e) {
            log.fatal("Error getting remote store:", e);

        } catch (RemoteException e) {
            log.fatal("Error getting remote store:", e);

        }
    }


    static AccessRead getAccess(){
        if (store == null) setup();
        return store;
    }

}
