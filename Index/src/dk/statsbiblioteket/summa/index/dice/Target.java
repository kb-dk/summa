/* $Id: Target.java,v 1.4 2007/10/05 10:20:23 te Exp $
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
package dk.statsbiblioteket.summa.index.dice;

import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;


import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.HashMap;
import java.io.IOException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class Target {

    private ReadableStorage _io;
    Iterator source;
    String io_service;
    String xslt_url;
    HashMap<String, String> jobHints;

    public Target (HashMap<String,String> jobHints) {
        this.jobHints = jobHints;
        io_service = jobHints.get(IndexConfig.RECORD_SERVICE);
    }

    /**
     * Connect to base
     */
    public void initialize () throws IOException {
        String base = jobHints.get("base");
        if (base == null) {
            throw new RuntimeException ("No \"base\" in jobHints");
        }

        try {
            _io = (ReadableStorage) Naming.lookup (io_service);
        } catch (Exception e) {
            throw new RemoteException ("Failed to look up io_service: " + io_service, e);
        }

        // TODO: Read jobHints and see if we should resume or what
        source = _io.getRecords(base);
    }

    public Record getNextRecord () {
        if (!source.hasNext()) {
            return null;
        }
        return (Record) source.next();
    }

    public HashMap<String,String> getJobHints() {
        return jobHints;
    }

    public String toString () {
        return jobHints.get(IndexConfig.BASE);
    }
}
