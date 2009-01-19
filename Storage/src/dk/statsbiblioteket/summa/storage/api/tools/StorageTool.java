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

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;
import java.net.MalformedURLException;


import dk.statsbiblioteket.summa.storage.api.*;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Logs;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class StorageTool {

    public static void printRecord (Record rec, boolean withContents) {
        printRecord (rec, System.out, withContents);
    }

    public static void printRecord (Record rec, OutputStream out, boolean withContents) {
        PrintWriter output = new PrintWriter (out, true);

        if (rec == null) {
            output.println("Record is 'null'");
            return;
        }

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

        List<String> ids = new ArrayList<String>(argv.length - 1);
        for (int i = 1; i < argv.length; i++) {
            ids.add(argv[i]);
        }

        System.err.println("Getting records " + Logs.expand(ids, 10) + "");
        long startTime = System.currentTimeMillis();
        List<Record> recs = storage.getRecords (ids, null);
        System.err.println(ids.size() + " records fetched in "
                           + (System.currentTimeMillis() - startTime) + " ms");

        for (Record r : recs) {
            printRecord(r, true);
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

        List<String> ids = new ArrayList<String>(argv.length - 1);
        for (int i = 1; i < argv.length; i++) {
            ids.add(argv[i]);
        }

        System.err.println("Getting records " + Logs.expand(ids, 10) + "");
        List<Record> recs = reader.getRecords (ids, null);

        for (Record r : recs) {
            System.err.println("Touching '" + r.getId() + "'");

            r.touch();
            writer.flush(r);
        }
    }

    private static void actionPeek (String[] argv, StorageReaderClient storage)
                                                             throws IOException{

        int numPeek;
        String base;

        if (argv.length == 1) {
            System.err.println("Peeking on all bases");
            base = null;
        } else {
            base = argv[1];
            System.err.println ("Getting records from base '" + base + "'");
        }

        if (argv.length <= 2) {
            numPeek = 5;
        } else {
            numPeek = Integer.parseInt(argv[2]);
        }


        long startTime = System.currentTimeMillis();
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);
        Iterator<Record> records = new StorageIterator(storage,
                                                       iterKey, numPeek);
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

        if (base != null) {
            System.err.println("Peek on base '" + base + "' completed in "
                               + (System.currentTimeMillis() - startTime)
                               + " ms");
        } else {
            System.err.println("Peek on all bases completed in "
                               + (System.currentTimeMillis() - startTime)
                               + " ms");
        }
    }

    private static void actionDump(String[] argv, StorageReaderClient storage)
                                                            throws IOException {
        String base;
        long count = 0;

        if (argv.length == 1) {
            System.err.println("Dumping on all bases");
            base = null;
        } else {
            base = argv[1];
            System.err.println ("Dumping base '" + base + "'");
        }

        long startTime = System.currentTimeMillis();
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);
        Iterator<Record> records = new StorageIterator(storage, iterKey);
        Record rec;

        while (records.hasNext()) {
            count++;
            rec = records.next ();
            System.out.println(rec.getContentAsUTF8());
        }

        if (base != null) {
            System.err.println("Dumped " + count + " records from base '" + base
                               + "' in "
                               + (System.currentTimeMillis() - startTime)
                               + " ms");
        } else {
            System.err.println("Dumped " + count + " records from all bases in "
                               + (System.currentTimeMillis() - startTime)
                               + " ms");
        }


    }

    private static void actionXslt (String[] argv, StorageReaderClient storage)
                                                             throws IOException{
        if (argv.length <= 2) {
            System.err.println("You must specify a record id and a URL "
                               + "for the XSLT to apply");
            return;
        }

        String recordId = argv[1];
        String xsltUrl = argv[2];

        Record r = storage.getRecord(recordId, null);

        if (r == null) {
            System.err.println("No such record '" + recordId + "'");
            return;
        }

        System.out.println(r.toString(true) + "\n");
        System.out.println("Original content:\n\n"
                           + r.getContentAsUTF8() + "\n");
        System.out.println("\n===========================\n");

        Transformer t = compileTransformer(xsltUrl);
        StreamResult input = new StreamResult();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        input.setOutputStream(out);
        Source so = new StreamSource(new ByteArrayInputStream(r.getContent()));

        try {
            t.transform(so, input);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        System.out.println("Contents transformed with: " + xsltUrl + ":\n");
        System.out.println(new String(out.toByteArray()));


    }

    public static Transformer compileTransformer (String xsltUrl) {
        Transformer transformer;
        TransformerFactory tfactory = TransformerFactory.newInstance();
        InputStream in = null;

        try {
            URL url = Resolver.getURL(xsltUrl);
            in = url.openStream();
            transformer = tfactory.newTransformer(
                    new StreamSource(in, url.toString()));
        } catch (Exception e) {
            throw new RuntimeException("Error compiling XSLT: "
                                       + e.getMessage(), e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                System.err.println("Non-fatal IOException while closing "
                                   + "stream for '" + xsltUrl + "'");
            }
        }

        return transformer;
    }

    private static void printUsage () {
        System.err.println ("USAGE:\n\t" +
                            "storage-tool.sh <action> [arg]...");
        System.err.println ("Actions:\n"
                            + "\tget  <record_id>\n"
                            + "\tpeek [base] [max_count=5]\n"
                            + "\ttouch <record_id> [record_id...]\n"
                            + "\txslt <record_id> <xslt_url>\n"
                            + "\tdump [base]     (dump storage on stdout)\n");
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
        if (!conf.valueExists(ConnectionConsumer.CONF_RPC_TARGET)) {
            rpcVendor = System.getProperty(ConnectionConsumer.CONF_RPC_TARGET);

            if (rpcVendor != null) {
                conf.set (ConnectionConsumer.CONF_RPC_TARGET, rpcVendor);
            } else {
                conf.set (ConnectionConsumer.CONF_RPC_TARGET,
                          "//localhost:28000/summa-storage");
            }
        }


        System.err.println("Using storage on: "
                           + conf.getString(ConnectionConsumer.CONF_RPC_TARGET));

        StorageReaderClient reader = new StorageReaderClient (conf);
        StorageWriterClient writer = new StorageWriterClient (conf);

        if ("get".equals(action)) {
            actionGet(args, reader);
        } else if ("peek".equals(action)) {
            actionPeek(args, reader);
        } else if ("touch".equals(action)) {
            actionTouch(args, reader, writer);
        } else if ("xslt".equals(action)) {
            actionXslt(args, reader);
        } else if ("dump".equals(action)){
            actionDump(args, reader);
        } else {
            System.err.println ("Unknown action '" + action + "'");
            printUsage();
            System.exit (2);
        }
    }

}




