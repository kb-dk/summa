/* $Id: RecordInspector.java,v 1.6 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.6 $
 * $Date: 2007/10/05 10:20:24 $
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
package dk.statsbiblioteket.summa.storage.tools;

import org.xml.sax.InputSource;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.rmi.Naming;
import java.io.*;
import java.util.Date;
import java.util.Properties;


import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageConnectionFactory;

import dk.statsbiblioteket.summa.storage.RecordIterator;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.xml.DefaultNamespaceContext;
import dk.statsbiblioteket.util.qa.QAInfo;


import java.rmi.RMISecurityManager;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, te")
public class RecordInspector {

    String storageUrl;
    Storage io;
    XPathExpression statusPath;

    String[] testRecords = {"horizon_1258465", "horizon_1996560", "horizon_400166", "horizon_262248",
                            "horizon_3013330", "horizon_3013309", "horizon_3013342", "horizon_3013352"};

    public RecordInspector (String storageUrl) {
        try {
            io = new StorageConnectionFactory().createConnection(storageUrl);

            XPath xp = XPathFactory.newInstance().newXPath();
            DefaultNamespaceContext nsc = new DefaultNamespaceContext();
            nsc.setNameSpace("http://www.loc.gov/MARC21/slim", "marc");
            xp.setNamespaceContext(nsc);
            statusPath = xp.compile("/marc:record/marc:datafield[@tag='004']/marc:subfield[@code='r']");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Record fetch (String recordId) {
        try {
            return io.getRecord(recordId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void printRecord (Record rec, boolean withContents) {
        printRecord (rec, System.out, withContents);
    }

    public void printRecord (Record rec, OutputStream out, boolean withContents) {
        PrintWriter output = new PrintWriter (out, true);

/*        String stateInvalid = "";
        if (!validateRecordState(rec)) {
            stateInvalid = " (CORRUPTED STATUS)";
        }

        String header = "";
        header += "ID: \t\t" + rec.getId() + "\n";
        header += "Base: \t\t" + rec.getBase() + "\n";
        header += "Status: \t" + rec.getState() + stateInvalid + "\n";
        header += "MTime: \t\t" + new Date(rec.getLastModified()) + "\n";
        header += "Size: \t\t" + rec.getUTF8Content().length/1024.0 + " kB";
  */
        output.println (rec);

        if (withContents) {
            String content = "";
            try {
                if (rec.getContent() == null) {
                    content = "NULL";
                } else {
                    content = new String (rec.getContent(), "UTF-8");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            output.println ("\nCONTENT:\n" + content);
        }

    }

    public boolean validateRecordState (Record rec) {
        if (! rec.getBase().equals ("horizon")) {
            System.err.println ("Only Horizon record validation supported atm");
            return true;
        }

        try {

            String val = statusPath.evaluate (new InputSource(
                                    new BufferedInputStream(
                                            new ByteArrayInputStream(rec.getContent())
                                    )
                                 ));

            if ("d".equals(val) && !rec.isDeleted()) {
                return false;
            }

            return true;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getStorageUrl (String configFile) {
        String port = "";
        String host = "";
        String service = "";

        try {
            Properties props = new Properties();
            props.load(new FileInputStream(configFile));

            for (Object _key : props.keySet()) {
                String key = (String) _key;
                if (key.equals("registry_port") || key.equals("registry.port")) {
                    port = (String) props.get(key);
                } else if (key.equals("registry_host") || key.equals("registry.host")) {
                    host = (String) props.get(key);
                } else if (key.equals("storage.service") || key.equals("service_name") || key.equals("service.name")) {
                    service = (String) props.get(key);
                } else if (key.equals("storage.url") || key.equals("storage_url") || key.equals("service.name")) {
                    service = (String) props.get(key);
                }
            }

            return "//" + host + ":" + port + "/" + service;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Exec through the horizon database and mark everything deleted that should be deleted
     */
    public void fixRecordStates () {
        Date startDate = new Date();
        try {
            System.out.println ("Connecting to horizon base");
            RecordIterator iter = io.getRecords("horizon");
            System.out.println ("Got connection. Starting validation...");
            Record rec = null;

            int fixes = 0, count = 0;

            while (iter.hasNext()) {
                count ++;
                rec = iter.next();
                if (!validateRecordState(rec)) {
                    rec.setDeleted(true);;
                    io.flush(rec);
                    fixes++;
                    if (fixes % 10 == 0) {
                        System.out.println ("Fixed " + fixes + " records");
                    }
                }
                if (count % 1000 == 0) {
                    System.out.println ("Checked " + count + " records");
                }
            }

            String report =  "REPORT"
                            +"\n\tStart date: " + startDate.getTime()
                            +"\n\tEnd date: " + new Date().getTime()
                            +"\n\tTotal records: " + count
                            +"\n\tFixed states: " + fixes;

            System.out.println (report);


        } catch (Exception e) {
            throw new RuntimeException (e);
        }
    }

    public static void main (String[] args) {
        System.setSecurityManager(new RMISecurityManager());

        String storageUrl = null;
        String recordId = null;
        boolean doFix = false;

        for (String arg : args) {

            if (arg.startsWith("storage=")) {
                if (storageUrl != null) {
                    System.err.println ("ERROR: 'storage' and 'config' parameters are not allowed at the same time");
                    System.exit (1);
                }
                storageUrl = arg.substring(8);
            }

            else if (arg.startsWith("config=")) {
                 if (storageUrl != null) {
                    System.err.println ("ERROR: 'storage' and 'config' parameters are not allowed at the same time");
                    System.exit (1);
                }
                String config = arg.substring(7);
                storageUrl = getStorageUrl(config);
            }

            else if (arg.startsWith("id=")) {
                recordId = arg.substring(3);
            }

            else if (arg.equals("fix=true")) {
                doFix = true;
            }
        }

        if (storageUrl == null) {
            System.err.println ("You must specify a storage url with 'storage=//mystorage:port'");
            System.exit(1);
        }

        if (doFix) {
            // Iterate over all horizon records and fix their states (if they are corrupted) and then exit
            RecordInspector inspector = new RecordInspector (storageUrl);
            inspector.fixRecordStates();
            System.out.println ("Fix complete");
            System.exit (0);
        }

        if (recordId != null) {
            // Just inspect a single record
            RecordInspector inspector = new RecordInspector (storageUrl);
            Record rec = inspector.fetch(recordId);

            if (rec == null) {
                System.err.println ("Got 'null' record. There is probably no"
                                    + " record with id '" + recordId + "' in"
                                    + " the storage");
                System.exit (1);
            }

            inspector.printRecord (rec, true);
            System.exit (0);
        }

        RecordInspector inspector = new RecordInspector (storageUrl);

        for (String id : inspector.testRecords) {
            Record rec = inspector.fetch(id);
            inspector.printRecord (rec, false);
            System.out.println ("******************************************");
        }
    }

}

