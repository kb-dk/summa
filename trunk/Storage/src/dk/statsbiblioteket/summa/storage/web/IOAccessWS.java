/* $Id: IOAccessWS.java,v 1.4 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.4 $
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

import dk.statsbiblioteket.summa.storage.io.AccessRead;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.RemoteException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory; 

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class IOAccessWS {



    private static AccessRead store;


    private static final Log log = LogFactory.getLog(IOAccessWS.class);
    private static IOAccessWS _myInstance = null;

    public IOAccessWS(){
        store = IOWSSetup.getAccess();
    }

    private static synchronized IOAccessWS getInstance(){
        if (_myInstance == null) _myInstance = new IOAccessWS();
        return _myInstance;
    }

    public static String getRecord(String name) {
        System.out.println("get " + name);
        log.debug("Getting record:" + name);
        if (_myInstance == null) getInstance();
        try {
            return new String(store.getRecord(name).getContent(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
            return null;
        } catch (RemoteException e) {
            log.error("", e);
            return null;
        }
    }
}
