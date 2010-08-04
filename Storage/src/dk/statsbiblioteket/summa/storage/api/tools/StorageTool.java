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
package dk.statsbiblioteket.summa.storage.api.tools;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.summa.storage.api.StorageWriterClient;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke, hbk")
public class StorageTool {
    public static final String DEFAULT_RPC_TARGET =
                                              "//localhost:28000/summa-storage";

    /**
     * Helper method for printing a record on {@link System@out} with or without
     * content.
     * @param rec The record to print.
     * @param withContents True if this record should be printed with its
     * content false otherwise.
     */
    public static void printRecord(Record rec, boolean withContents) {
        printRecord(rec, System.out, withContents);
    }

    /**
     * Helper method for printing a {@link Record} to a defined output stream.
     * @param rec The record which should be printed.
     * @param out The output stream.
     * @param withContents True if this record should be printed with its
     * content false otherwise.
     */
    public static void printRecord(Record rec, OutputStream out,
                                                         boolean withContents) {
        PrintWriter output = new PrintWriter (out, true);

        if (rec == null) {
            output.println("Record is 'null'");
            return;
        }

        output.println(rec.toString(true));

        if(withContents) {
            output.println(rec.getContentAsUTF8());
        }
    }

    /**
     * Method for the 'get' command. This method retrieves the record(s) from
     * storage and print this(these) to {link System#out}.
     *
     * @param argv Arguments from commandline. Should be a list of ids. 
     * @param storage The storage to connect to, to retrieve the records.
     * @return 0 if everything happened without errors, non-zero value if error
     * occur.
     * @throws IOException If error occur while communicating to storage.
     */
    private static int actionGet(String[] argv, StorageReaderClient storage)
                                                             throws IOException{
        if(argv.length == 1) {
            System.err.println("You must specify at least one record id to "
                               + "the 'get' action");
            return 1;
        }

        // Allow this call to access private storage records, like __holdings__
        QueryOptions options = new QueryOptions();
        options.meta("ALLOW_PRIVATE", "true");

        List<String> ids = new ArrayList<String>(argv.length - 1);
        ids.addAll(Arrays.asList(argv).subList(1, argv.length));

        System.err.println("Getting record(s): " + Strings.join(ids, ", "));
        long startTime = System.currentTimeMillis();
        List<Record> recs = storage.getRecords (ids, options);
        System.err.println("Got " + recs.size() + " records in "
                           + (System.currentTimeMillis() - startTime) + " ms");

        for (Record r : recs) {
            printRecord(r, true);
            System.out.println ("===================");
        }
        return 0;
    }

    /**
     * This action, touches a record, thereby update the last modified
     * timestamp for this record.
     *
     * @param argv Command line arguments, needs to specify at least one
     * record.
     * @param reader Storage reader client.
     * @param writer Storage writer client.
     * @return 0 if everything happened without errors, non-zero value if error
     * occur.
     * @throws IOException If error occur while communicating with storage.
     */
    private static int actionTouch(String[] argv, StorageReaderClient reader,
                                    StorageWriterClient writer)
                                                             throws IOException{
        if (argv.length == 1) {
            System.err.println("You must specify at least one record id to"
                               + " the 'touch' action");
            return 1;
        }

        List<String> ids = new ArrayList<String>(argv.length - 1);
        ids.addAll(Arrays.asList(argv).subList(1, argv.length));

        System.err.println("Getting records " + Logs.expand(ids, 10) + "");
        List<Record> recs = reader.getRecords(ids, null);

        for (Record r : recs) {
            System.err.println("Touching '" + r.getId() + "'");

            r.touch();
            writer.flush(r);
        }
        return 0;
    }

    /**
     * This action peeks into the storage, which mean printing all records from
     * either the specified base or all records.
     * @param argv Command line arguments, specifying the base or nothing,
     * which results in all record being printed.
     * @param storage The storage reader client.
     * @return 0 if everything happened without errors, non-zero value if error
     * occur.
     * @throws IOException If error occur while communicating to storage.
     */
    private static int actionPeek(String[] argv, StorageReaderClient storage)
                                                             throws IOException{
        int numPeek;
        String base;

        if (argv.length == 1) {
            System.err.println("Peeking on all bases");
            base = null;
        } else {
            base = argv[1];
            System.err.println("Getting records from base '" + base + "'");
        }

        if (argv.length <= 2) {
            numPeek = 5;
        } else {
            numPeek = Integer.parseInt(argv[2]);
        }

        long startTime = System.currentTimeMillis();
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);

        System.err.println("Got iterator after "
                           + (System.currentTimeMillis() - startTime) + " ms");

        Iterator<Record> records = new StorageIterator(storage,
                                                       iterKey, numPeek);
        Record rec;
        int count = 0;
        while (records.hasNext()) {
            rec = records.next();
            printRecord(rec, true);

            count++;
            if (count >= numPeek) {
                break;
            }
        }

        if (count == 0) {
            System.err.println("Base '" + base + "' is empty");
        }

        if (base != null) {
            System.err.println("Peek on base '" + base + "' completed in "
                               + (System.currentTimeMillis() - startTime)
                               + " ms (incl. iterator lookup time)");
        } else {
            System.err.println("Peek on all bases completed in "
                               + (System.currentTimeMillis() - startTime)
                               + " ms (incl. iterator lookup time)");
        }
        return 0;
    }

    /**
     * This action dumps the storage records data. It can either be for all
     * bases or just one, specified on the command line.
     * @param argv Command line argument specifying the base.
     * @param storage The storage reader client.
     * @return 0 if everything happened without errors, non-zero value if error
     * occur.
     * @throws IOException If error occur while communicating to storage.
     */
    private static int actionDump(String[] argv, StorageReaderClient storage)
                                                            throws IOException {
        String base;
        long count = 0;

        if (argv.length == 1) {
            System.err.println("Dumping on all bases");
            base = null;
        } else {
            base = argv[1];
            System.err.println("Dumping base '" + base + "'");
        }

        long startTime = System.currentTimeMillis();
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);
        Iterator<Record> records = new StorageIterator(storage, iterKey);

        System.err.println("Got iterator after "
                           + (System.currentTimeMillis() - startTime) + " ms");

        Record rec;
        while (records.hasNext()) {
            count++;
            rec = records.next();
            System.out.println(rec.getContentAsUTF8());
        }

        if (base != null) {
            System.err.println("Dumped " + count + " records from base '" + base
                               + "' in "
                               + (System.currentTimeMillis() - startTime)
                               + " ms (including iterator lookup time)");
        } else {
            System.err.println("Dumped " + count + " records from all bases in "
                               + (System.currentTimeMillis() - startTime)
                               + " ms (including iterator lookup time)");
        }
        return 0;
    }

    /**
     * Get the holdings, for a storage. This is info about the different bases,
     * like number of records, last modified time stamp.
     * @param storage The storage reader client.
     * @return 0 if everything happened without errors, non-zero value if error
     * occur.
     * @throws IOException If error occur while communicatinh to storage.
     */
    private static int actionHoldings(StorageReaderClient storage)
                                                            throws IOException {
        StringMap meta = new StringMap();
        meta.put("ALLOW_PRIVATE", "true");
        QueryOptions opts = new QueryOptions(null, null, 0, 0, meta);
        long start = System.currentTimeMillis();
        Record holdings = storage.getRecord("__holdings__", opts);
        String xml = holdings.getContentAsUTF8();
        System.out.println(xml);
        System.err.println(String.format("Retrieved holdings in %sms",
                                         (System.currentTimeMillis() - start)));
        return 0;
    }

    /**
     * This action runs a single batch job on the storage.
     * @param argv Specifying the batch job to run.
     * @param writer The storage writer client.
     * @return 0 if everything happened without errors, non-zero value if error
     * occur.
     * @throws IOException If error occur while communicating to storage.
     */
    private static int actionBatchJob(String[] argv,
                                StorageWriterClient writer) throws IOException {

        if (argv.length < 2) {
            System.err.println("You must provide exactly one job name to run");
            return 1;
        }

        String jobName = argv[1];
        String base = argv.length > 2 ?
                          (argv[2].length() == 0 ? null : argv[2]) : null;
        long minMtime = argv.length > 3 ?
                                   Long.parseLong(argv[3]) : 0;
        long maxMtime = argv.length > 4 ?
                                   Long.parseLong(argv[4]) : Long.MAX_VALUE;

        long start = System.currentTimeMillis();
        String result =
                       writer.batchJob(jobName, base, minMtime, maxMtime, null);

        // We flush() the streams in order not to interweave the output
        System.err.println("Result:\n----------");
        System.err.flush();
        System.out.println(result);
        System.out.flush();
        System.err.println(String.format("----------\nRan job '%s' in %sms",
                           jobName, (System.currentTimeMillis() - start)));
        return 1;
    }

    /**
     * This action, applies a single XSLT to a single record, both must be
     * specified on the command line.
     * @param argv Command line arguments, should specify both a record id and
     * a URL to an XSLT.
     * @param storage The storage reader client.
     * @return 0 if everything happened without errors, non-zero value if error
     * occur.
     * @throws IOException if error occur while communicating to storage.
     */
    private static int actionXslt(String[] argv, StorageReaderClient storage)
                                                             throws IOException{
        if (argv.length <= 2) {
            System.err.println("You must specify a record id and a URL "
                               + "for the XSLT to apply");
            return 1;
        }

        String recordId = argv[1];
        String xsltUrl = argv[2];

        Record r = storage.getRecord(recordId, null);

        if (r == null) {
            System.err.println("No such record '" + recordId + "'");
            return 2;
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
            return 3;
        }

        System.out.println("Contents transformed with: " + xsltUrl + ":\n");
        System.out.println(new String(out.toByteArray()));
        return 0;
    }

    /**
     * Helper method for compiling a transformer from a XSLT URL.
     * @param xsltUrl The URL pointing to the XSLT.
     * @return A transformer based on the XSLT.
     */
    public static Transformer compileTransformer(String xsltUrl) {
        Transformer transformer;
        TransformerFactory tFactory = TransformerFactory.newInstance();
        InputStream in = null;

        try {
            URL url = Resolver.getURL(xsltUrl);
            in = url.openStream();
            transformer = tFactory.newTransformer(
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

    /**
     * Print usage to {@link System#err}.
     */
    private static void printUsage() {
        System.err.println("USAGE:\n\t" +
                            "storage-tool.sh <action> [arg]...");
        System.err.println("Actions:\n"
                           + "\tget  <record_id>\n"
                           + "\tpeek [base] [max_count=5]\n"
                           + "\ttouch <record_id> [record_id...]\n"
                           + "\txslt <record_id> <xslt_url>\n"
                           + "\tdump [base]     (dump storage on stdout)\n"
                           + "\tholdings\n"
                           + "\tbatchjob <jobname> [base] [minMtime] "
                                       +"[maxMtime]   "
                                       + "(empty base string means all bases)");
    }

    /**
     * Main method for the Storage Tool, arguments given to this should be the
     * command that should be run and possible arguments to this command.
     * @param args Command line arguments telling which command to run and
     * possible some arguments needed by this command.
     * @throws Exception If error occur while processing result.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        Configuration conf;
        String rpcVendor;
        String action;

        action = args[0];

        /* Try and open the system config */
        try {
            conf = Configuration.getSystemConfiguration(false);
        } catch (Configurable.ConfigurationException e) {
            System.err.println("Unable to load system config: " + e.getMessage()
                                +".\nUsing default configuration");
            conf = Configuration.newMemoryBased();
        }

        /* Make sure the summa.rpc.vendor property is set */
        if (!conf.valueExists(ConnectionConsumer.CONF_RPC_TARGET)) {
            rpcVendor = System.getProperty(ConnectionConsumer.CONF_RPC_TARGET);

            if (rpcVendor != null) {
                conf.set(ConnectionConsumer.CONF_RPC_TARGET, rpcVendor);
            } else {
                conf.set(ConnectionConsumer.CONF_RPC_TARGET,
                          DEFAULT_RPC_TARGET);
            }
        }

        System.err.println("Using storage on: "
                       + conf.getString(ConnectionConsumer.CONF_RPC_TARGET));

        StorageReaderClient reader = new StorageReaderClient (conf);
        StorageWriterClient writer = new StorageWriterClient (conf);

        int exitCode;
        if ("get".equals(action)) {
            exitCode = actionGet(args, reader);
        } else if ("peek".equals(action)) {
            exitCode = actionPeek(args, reader);
        } else if ("touch".equals(action)) {
            exitCode = actionTouch(args, reader, writer);
        } else if ("xslt".equals(action)) {
            exitCode = actionXslt(args, reader);
        } else if ("dump".equals(action)){
            exitCode = actionDump(args, reader);
        } else if ("holdings".equals(action)) {
            exitCode = actionHoldings(reader);
        } else if ("batchjob".equals(action)) {
            exitCode = actionBatchJob(args, writer);
        } else {
            System.err.println ("Unknown action '" + action + "'");
            printUsage();
            exitCode = 2;
        }
        System.exit(exitCode);
    }
}