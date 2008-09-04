/* $Id: StorageTool.java,v 1.6 2007/10/05 10:20:24 te Exp $
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
package dk.statsbiblioteket.summa.storage.api.tools;

import org.xml.sax.InputSource;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.Date;
import java.util.Properties;


import dk.statsbiblioteket.summa.storage.api.*;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.xml.DefaultNamespaceContext;
import dk.statsbiblioteket.util.qa.QAInfo;


import java.rmi.RMISecurityManager;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, te")
public class StorageTool {

    public static void printRecord (Record rec, boolean withContents) {
        printRecord (rec, System.out, withContents);
    }

    public static void printRecord (Record rec, OutputStream out, boolean withContents) {
        PrintWriter output = new PrintWriter (out, true);

        output.println (rec.toString(true));

        if (withContents) {
            output.println(rec.getContentAsUTF8());
        }

    }

    private static void actionGet (String[] argv, StorageReaderClient storage)
                                                             throws IOException{
        if (argv.length == 1) {
            System.err.println("You must specify at least one record id to "
                               + "the 'get' action");
            return;
        }

        for (int i = 1; i < argv.length; i++) {
            System.err.println("Getting '" + argv[i] + "'");
            Record rec = storage.getRecord (argv[i]);

            if (rec == null) {
                System.err.println ("No such record: " + argv[i]);
                continue;
            }

            printRecord(rec, true);
            System.out.println ("===================");
        }
    }

    private static void actionTouch (String[] argv,
                                     StorageReaderClient reader,
                                     StorageWriterClient writer)
                                                             throws IOException{
        if (argv.length == 1) {
            System.err.println("You must specify at least one record id to"
                               + " the 'touch' action");
            return;
        }

        for (int i = 1; i < argv.length; i++) {
            System.err.println("Touching '" + argv[i] + "'");
            Record rec = reader.getRecord (argv[i]);

            if (rec == null) {
                System.err.println ("No such record: " + argv[i]);
                continue;
            }

            rec.touch();
            writer.flush(rec);
        }
    }

    private static void actionPeek (String[] argv, StorageReaderClient storage)
                                                             throws IOException{

        int numPeek;

        if (argv.length == 1) {
            System.err.println("You must specify a base to peek on");
            return;
        }

        if (argv.length == 2) {
            numPeek = 5;
        } else {
            numPeek = Integer.parseInt(argv[2]);
        }

        String base = argv[1];

        System.err.println ("Getting records from base '" + base + "'");
        RecordIterator records = storage.getRecords(base);
        Record rec;

        int count = 0;
        while (records.hasNext()) {
            rec = records.next ();
            printRecord(rec, true);

            count++;
            if (count >= numPeek) {
                break;
            }
        }

        if (count == 0) {
            System.err.println ("Base '" + base + "' is empty");
        }

    }

    private static void printUsage () {
        System.err.println ("USAGE:\n\t" +
                            "storage-tool.sh <action> [arg]...");
        System.err.println ("Actions:\n"
                            + "\tget  <record_id>\n"
                            + "\tpeek <base> [max_count=5]\n"
                            + "\ttouch <record_id> [record_id...]\n");        
    }

    public static void main (String[] args) throws Exception {
        if (args.length < 1) {
            printUsage();
            System.exit (1);
        }

        Configuration conf;
        String rpcVendor;
        String action;

        action = args[0];

        /* Try and open the system config */
        try {
            conf = Configuration.getSystemConfiguration(false);
        } catch (Configurable.ConfigurationException e) {
            System.err.println ("Unable to load system config: " + e.getMessage()
                                +".\nUsing default configuration");
            conf = Configuration.newMemoryBased();
        }

        /* Make sure the summa.rpc.vendor property is set */
        if (!conf.valueExists(ConnectionConsumer.PROP_RPC_TARGET)) {
            rpcVendor = System.getProperty(ConnectionConsumer.PROP_RPC_TARGET);

            if (rpcVendor != null) {
                conf.set (ConnectionConsumer.PROP_RPC_TARGET, rpcVendor);
            } else {
                conf.set (ConnectionConsumer.PROP_RPC_TARGET,
                          "//localhost:28000/summa-storage");
            }
        }


        System.err.println("Using storage on: "
                           + conf.getString(ConnectionConsumer.PROP_RPC_TARGET));

        StorageReaderClient reader = new StorageReaderClient (conf);
        StorageWriterClient writer = new StorageWriterClient (conf);

        if ("get".equals(action)) {
            actionGet(args, reader);
        } else if ("peek".equals(action)) {
            actionPeek(args, reader);
        } else if ("touch".equals(action)) {
            actionTouch(args, reader, writer);
        } else {
            System.err.println ("Unknown action '" + action + "'");
            printUsage();
            System.exit (2);
        }
    }

}

