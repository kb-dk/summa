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
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.summa.plugins.SaxonXSLT;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.summa.storage.api.StorageWriterClient;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke, hbk")
public class StorageTool {
    public static final String DEFAULT_RPC_TARGET = "//localhost:28000/summa-storage";

    /**
     * Helper method for printing a record on {@link System#out} with or without content.
     *
     * @param rec          The record to print.
     * @param withContents True if this record should be printed with its content false otherwise.
     */
    public static void printRecord(Record rec, boolean withContents, Boolean expand) {
        printRecord(rec, System.out, withContents, expand);
    }

    /**
     * Helper method for printing a {@link Record} to a defined output stream.
     *
     * @param rec          The record which should be printed.
     * @param out          The output stream.
     * @param withContents True if this record should be printed with its content false otherwise.
     */
    public static void printRecord(Record rec, OutputStream out, boolean withContents, Boolean expand) {
        PrintWriter output = new PrintWriter(out, true);

        if (rec == null) {
            output.println("Record is 'null'");
            return;
        }
        if (Boolean.TRUE.equals(expand)) {
            try {
                output.println(RecordUtil.toXML(rec, false));
                return;
            } catch (IOException e) {
                throw new RuntimeException("Exception generating XML representation for record " + rec);
            }
        }
        output.println(rec.toString(true));

        if (withContents) {
            output.println(rec.getContentAsUTF8());
        }
    }

    /**
     * Method for the 'get' command. This method retrieves the record(s) from
     * storage and print this(these) to {link System#out}.
     *
     * @param argv    Arguments from commandline. Should be a list of ids.
     * @param storage The storage to connect to, to retrieve the records.
     * @return 0 if everything happened without errors, non-zero value if error occur.
     * @throws IOException If error occur while communicating to storage.
     */
    private static int actionGet(String[] argv, StorageReaderClient storage, Boolean expand) throws IOException {
        if (argv.length == 1) {
            System.err.println("You must specify at least one record id to the 'get' action");
            return 1;
        }

        List<String> ids = new ArrayList<>(argv.length - 1);
        ids.addAll(Arrays.asList(argv).subList(1, argv.length));

        System.err.println("Getting record(s): " + Strings.join(ids, ", "));
        long startTime = System.currentTimeMillis();
        List<Record> recs = storage.getRecords(ids, getOptions(expand)); // Use the default options from Storage
        System.err.println("Got " + recs.size() + " records in " + (System.currentTimeMillis() - startTime) + " ms");

        for (Record r : recs) {
            printRecord(r, true, expand);
            System.out.println("===================");
        }
        return 0;
    }

    private static QueryOptions getOptions(Boolean expand) {
        if (expand == null) {
            return null; // Storage default
        }
        QueryOptions options;
        if (expand) { // Expand parent & children
            options = new QueryOptions(null, null, 10, 10);
            options.setAttributes(QueryOptions.ATTRIBUTES_ALL);
            options.removeAttribute(QueryOptions.ATTRIBUTES.META); // TODO: Consider enabling this for get
        } else { // No expansion
            options = new QueryOptions();
        }
        options.meta("ALLOW_PRIVATE", "true");
        return options;
    }

    /**
     * Method for the 'put' commande. Loads record content from the provided file and stores it
     * using the provided id and base. Usable for experiments.
     * @param args   id, base, file.
     * @param writer Storage writer.
     * @return 0 if everything went well.
     */
    private static int actionPut(String[] args, StorageWriterClient writer) throws IOException {
        if (args.length != 4) {
            System.err.println("Error: Illegal number of arguments. Please provide id, base, file where");
            System.err.println("  id = recordID");
            System.err.println("  base = recordBase");
            System.err.println("  file = local file path to record content, expected to be UTF-8");
            return 8;
        }
        final String recordID = args[1];
        final String recordBase = args[2];
        final String file = args[3];
        // TODO: Shortcut the unnecessary decode+encode of UTF-8 (this also allows arbitrary content)
        final String content = Files.loadString(new File(file));
        Record record = new Record(recordID, recordBase, content.getBytes("utf-8"));
        writer.flush(record);
        return 0;
    }

    /**
     * Method for the 'delete' command. This method retrieves the record(s) from storage, sets the deleted flag and
     * stores it back into Storage.
     *
     * @param argv    Arguments from commandline. Should be a list of ids.
     * @param storage The storage to connect to, to retrieve the records.
     * @return 0 if everything happened without errors, non-zero value if error occur.
     * @throws IOException If error occur while communicating to storage.
     */
    private static int actionDelete(
            String[] argv, StorageReaderClient storage, StorageWriterClient writer) throws IOException {
        if (argv.length == 1) {
            System.err.println("You must specify at least one record id to the 'delete' action");
            return 1;
        }

        // Allow this call to access private storage records, like __holdings__
        QueryOptions options = new QueryOptions();
        options.meta("ALLOW_PRIVATE", "true");

        List<String> ids = new ArrayList<>(argv.length - 1);
        ids.addAll(Arrays.asList(argv).subList(1, argv.length));

        System.err.println("Retrieving record(s): " + Strings.join(ids, ", "));
        long startTime = System.currentTimeMillis();
        List<Record> recs = storage.getRecords(ids, options);
        System.err.println("Got " + recs.size() + " records in " + (System.currentTimeMillis() - startTime) + " ms");

        for (Record r : recs) {
            System.err.println("Marking record '" + r.getId() + "' as deleted and flushing");
            r.setDeleted(true);
            r.touch();
            writer.flush(r);
        }
        return 0;
    }

    /**
     * This action, touches a record, thereby update the last modified
     * timestamp for this record.
     *
     * @param argv   Command line arguments, needs to specify at least one record.
     * @param reader Storage reader client.
     * @param writer Storage writer client.
     * @return 0 if everything happened without errors, non-zero value if error occur.
     * @throws IOException If error occur while communicating with storage.
     */
    private static int actionTouch(
            String[] argv, StorageReaderClient reader, StorageWriterClient writer) throws IOException {
        if (argv.length == 1) {
            System.err.println("You must specify at least one record id to the 'touch' action");
            return 1;
        }

        List<String> ids = new ArrayList<>(argv.length - 1);
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
     *
     * @param argv    Command line arguments, specifying the base or nothing, which results in all record being printed.
     * @param storage The storage reader client.
     * @return 0 if everything happened without errors, non-zero value if error occur.
     * @throws IOException If error occur while communicating to storage.
     */
    private static int actionPeek(String[] argv, StorageReaderClient storage) throws IOException {
        int numPeek;
        String base;

        if (argv.length == 1) {
            System.err.println("Peeking on all bases");
            base = null;
        } else {
            base = argv[1];
            System.err.println("Getting records from base '" + base + "'");
        }

        numPeek = argv.length <= 2 ? 5 : Integer.parseInt(argv[2]);

        long startTime = System.currentTimeMillis();
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);

        System.err.println("Got iterator after " + (System.currentTimeMillis() - startTime) + " ms");

        Iterator<Record> records = new StorageIterator(storage, iterKey, numPeek);
        Record rec;
        int count = 0;
        while (records.hasNext()) {
            rec = records.next();
            printRecord(rec, true, false);

            count++;
            if (count >= numPeek) {
                break;
            }
        }

        if (count == 0) {
            System.err.println("Base '" + base + "' is empty");
        }

        if (base != null) {
            System.err.println("Peek on base '" + base + "' completed in " + (System.currentTimeMillis() - startTime)
                               + " ms (incl. iterator lookup time)");
        } else {
            System.err.println("Peek on all bases completed in " + (System.currentTimeMillis() - startTime)
                               + " ms (incl. iterator lookup time)");
        }
        return 0;
    }

    /**
     * This action lists recordIDs from the storage along with basic info.
     *
     * @param argv    Command line arguments, specifying the base or nothing, which results in all record being printed.
     * @param storage The storage reader client.
     * @return 0 if everything happened without errors, non-zero value if error occur.
     * @throws IOException If error occur while communicating to storage.
     */
    private static int actionList(String[] argv, StorageReaderClient storage) throws IOException {
        int numPeek;
        String base;

        if (argv.length == 1) {
            System.err.println("Listing all bases");
            base = null;
        } else {
            base = argv[1];
            System.err.println("Listing records from base '" + base + "'");
        }

        numPeek = argv.length <= 2 ? 20 : Integer.parseInt(argv[2]);

        long startTime = System.currentTimeMillis();
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);

        System.err.println("Got iterator after " + (System.currentTimeMillis() - startTime) + " ms");

        Iterator<Record> records = new StorageIterator(storage, iterKey, numPeek);
        Record rec;
        int count = 0;
        while (records.hasNext() && (numPeek == -1 || count++ < numPeek)) {
            printRecord(records.next(), false, false);
        }

        if (count == 0) {
            System.err.println("Base '" + base + "' is empty");
        }

        if (base != null) {
            System.err.println("List on base '" + base + "' completed in " + (System.currentTimeMillis() - startTime)
                               + " ms (incl. iterator lookup time)");
        } else {
            System.err.println("List on all bases completed in " + (System.currentTimeMillis() - startTime)
                               + " ms (incl. iterator lookup time)");
        }
        return 0;
    }

    /**
     * This action dumps the storage records data. It can either be for all
     * bases or just one, specified on the command line.
     *
     * @param argv    Command line argument specifying the base.
     * @param storage The storage reader client.
     * @return 0 if everything happened without errors, non-zero value if error occur.
     * @throws IOException If error occur while communicating to storage.
     */
    private static int actionDump(String[] argv, StorageReaderClient storage) throws IOException {
        long count = 0;

        final String base = argv.length == 1 ? null : argv[1];
        final long maxRecords = argv.length < 3 ? -1 : Long.parseLong(argv[2]);
        final String format = argv.length < 4 ? "content" : argv[3];

        System.err.println((base == null ? "Dumping on all bases" : "Dumping base '" + base + "'")
                           + " with maxRecords=" + (maxRecords == -1 ? "unlimited" : maxRecords)
                           + ", format=" + format);

        long startTime = System.currentTimeMillis();
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);
        Iterator<Record> records = new StorageIterator(storage, iterKey);

        System.err.println("Got iterator after " + (System.currentTimeMillis() - startTime) + " ms");

        Record rec;
        while (records.hasNext() && count < maxRecords) {
            count++;
            rec = records.next();
            if ("meta".equals(format) || "full".equals(format)) {
                printRecord(rec, "full".equals(format), false);
            } else {
                System.out.println(rec.getContentAsUTF8());
            }
        }

        if (base != null) {
            System.err.println("Dumped " + count + " records from base '" + base + "' in "
                               + (System.currentTimeMillis() - startTime) + " ms (including iterator lookup time)");
        } else {
            System.err.println(
                    "Dumped " + count + " records from all bases in " + (System.currentTimeMillis() - startTime)
                    + " ms (including iterator lookup time)");
        }
        return 0;
    }

    private static int actionClear(String[] args, StorageWriterClient writer) throws IOException {
        if (args.length != 2) {
            System.err.println("Please provide a base to clear");
            return 1;
        }
        String base = args[1];
        System.err.println("Clearing base '" + base + "'");
        long startTime = System.currentTimeMillis();
        writer.clearBase(base);
        System.err.println(
                "Finished clearing base '" + base + "' in " + (System.currentTimeMillis() - startTime) + "ms");
        return 0;
    }

    /**
     * Get the holdings, for a storage. This is info about the different bases,
     * like number of records, last modified time stamp.
     *
     * @param storage The storage reader client.
     * @return 0 if everything happened without errors, non-zero value if error
     *         occur.
     * @throws IOException If error occur while communicatinh to storage.
     */
    private static int actionHoldings(StorageReaderClient storage) throws IOException {
        return privateCommand(storage, "holdings");
    }

    /**
     * Print instrumentation data for a storage, such as average getRecord-time.
     *
     * @param storage The storage reader client.
     * @return 0 if everything happened without errors, non-zero value if error occur.
     * @throws IOException If error occur while communicatinh to storage.
     */
    private static int actionStatistics(StorageReaderClient storage) throws IOException {
        return privateCommand(storage, "statistics");
    }

    private static int actionRelationStatistics(String[] args, StorageReaderClient reader) throws IOException {
        if (args.length == 2 && "true".equals(args[1].toLowerCase(Locale.ENGLISH))) {
            return privateCommand(reader, "relation_stats_extended");
        } else {
            return privateCommand(reader, "relation_stats");
        }
    }

    private static int actionDumpToFile(String[] args, StorageReaderClient reader) throws IOException {
        if (args.length == 1) {
            throw new IllegalArgumentException("A path must be stated");
        }
        String path = args[1];
        boolean dumpDeleted = args.length == 3 && Boolean.parseBoolean(args[2]);
        return privateCommand(reader, "dump_to_file_" + dumpDeleted + "_" + path);
    }

    private static int actionRelationCleanup(String[] args, StorageReaderClient reader) throws IOException {
        return privateCommand(reader, "relation_cleanup_" + (args.length == 2 ? args[1] : "none_valid"));
    }

    private static int privateCommand(StorageReaderClient storage, String command) throws IOException {
        StringMap meta = new StringMap();
        meta.put("ALLOW_PRIVATE", "true");
        QueryOptions opts = new QueryOptions(null, null, 0, 0, meta);
        long start = System.currentTimeMillis();
        String prID = "__" + command + "__";
        Record holdings = storage.getRecord(prID, opts);
        if (holdings == null) {
            System.err.println("Unable to retrieve private record '" + prID + "'");
        } else {
            System.out.println(holdings.getContentAsUTF8());
        }
        System.err.printf("Retrieved %s in %dms%n", command, System.currentTimeMillis() - start);
        return 0;
    }


    /**
     * This action runs a single batch job on the storage.
     *
     * @param argv   Specifying the batch job to run.
     * @param writer The storage writer client.
     * @return 0 if everything happened without errors, non-zero value if error occur.
     * @throws IOException If error occur while communicating to storage.
     */
    private static int actionBatchJob(String[] argv, StorageWriterClient writer) throws IOException {

        if (argv.length < 2) {
            System.err.println("You must provide exactly one job name to run");
            return 1;
        }

        String jobName = argv[1];
        String base = argv.length > 2 ? argv[2].isEmpty() ? null : argv[2] : null;
        long minMtime = argv.length > 3 ? Long.parseLong(argv[3]) : 0;
        long maxMtime = argv.length > 4 ? Long.parseLong(argv[4]) : Long.MAX_VALUE;

        long start = System.currentTimeMillis();
        String result = writer.batchJob(jobName, base, minMtime, maxMtime, null);

        // We flush() the streams in order not to interweave the output
        System.err.println("Result:\n----------");
        System.err.flush();
        System.out.println(result);
        System.out.flush();
        System.err.println(String.format("----------\nRan job '%s' in %sms",
                                         jobName, System.currentTimeMillis() - start));
        return 1;
    }

    /**
     * This action performs a full backup of the storage.
     *
     * @param argv   The destination for the backup..
     * @param writer The storage writer client.
     * @return 0 if everything happened without errors, non-zero value if error occur.
     * @throws IOException If error occur while communicating to storage.
     */
    private static int actionBackup(String[] argv, StorageWriterClient writer) throws IOException {

        if (argv.length < 2) {
            System.err.println("You must provide a destination for the backup");
            return 1;
        }

        String destination = argv[1];

        long start = System.currentTimeMillis();
        // Yes, this is a giant hack
        QueryOptions options = new QueryOptions();
        options.meta(DatabaseStorage.INTERNAL_JOB_NAME, H2Storage.JOB_BACKUP);
        options.meta(H2Storage.JOB_BACKUP_DESTINATION, destination);

        String result = writer.batchJob(DatabaseStorage.INTERNAL_BATCH_JOB, null, 0, Long.MAX_VALUE, options);

        // We flush() the streams in order not to interweave the output
        System.err.println("Result:\n----------");
        System.err.flush();
        System.out.println(result);
        System.out.flush();
        System.err.println(String.format("----------\nPerformed backup in %sms",
                                         System.currentTimeMillis() - start));
        return 1;
    }

    /**
     * This action, applies a single XSLT to a single record, both must be
     * specified on the command line.
     *
     * @param argv    Command line arguments, should specify both a record id and
     *                a URL to an XSLT.
     * @param storage The storage reader client.
     * @return 0 if everything happened without errors, non-zero value if error occur.
     * @throws IOException if error occur while communicating to storage.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    private static int actionXslt(String[] argv, StorageReaderClient storage) throws IOException {
        if (argv.length <= 2) {
            System.err.println("You must specify a record id and a URL for the XSLT to apply");
            return 1;
        }

        String recordId = argv[1];
        String xsltUrl = argv[2];
        Boolean expand = argv.length == 4 ? Boolean.parseBoolean(argv[3]) : null;

        Record r = storage.getRecord(recordId, getOptions(expand));

        if (r == null) {
            System.err.println("No such record '" + recordId + "'");
            return 2;
        }
        final String xml = expand != null && expand ? RecordUtil.toXML(r, false) : r.getContentAsUTF8();

        System.out.println(r.toString(true) + "\n");
        System.out.println("Original content (expand==" + expand + "):\n\n" + xml + "\n");
        System.out.println("\n===========================\n");

        Transformer t = compileTransformer(xsltUrl);
        StreamResult input = new StreamResult();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        input.setOutputStream(out);
        Source so = new StreamSource(new ByteArrayInputStream(xml.getBytes("utf-8")));

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
     *
     * @param xsltUrl The URL pointing to the XSLT.
     * @return A transformer based on the XSLT.
     */
    public static Transformer compileTransformer(String xsltUrl) {
        Transformer transformer;
        TransformerFactory tFactory = SaxonXSLT.getTransformerFactory();
        InputStream in = null;

        try {
            URL url = Resolver.getURL(xsltUrl);
            in = url.openStream();
            transformer = tFactory.newTransformer(new StreamSource(in, url.toString()));
        } catch (Exception e) {
            throw new RuntimeException("Error compiling XSLT: " + e.getMessage(), e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                System.err.println("Non-fatal IOException while closing stream for '" + xsltUrl + "'");
            }
        }
        return transformer;
    }

    /**
     * Print usage to {@link System#err}.
     */
    private static void printUsage() {
        System.err.println("USAGE:\n\tstorage-tool.sh <action> [arg]...");
        System.err.println(
                "Actions:\n"
                + "\tget <record_id>+ (get record, expanding parent/childs based on default expansion for the storage)\n"
                + "\tget_single  <record_id>+ (get record, no parent/child expansion)\n"
                + "\tget_expand  <record_id>+ (get record, expanding parent/childs)\n"
                + "\tput <record_id> <record_base> <file>\n"
                + "\tdelete  <record_id>\n"
                + "\tpeek [base] [max_count=5]\n"
                + "\tlist [base] [max_count=20]\n"
                + "\ttouch <record_id>+\n"
                // TODO: touch_base
                + "\txslt <record_id> <xslt_url> [expand]\n"
                + "\tdump [base [maxrecords [format]]]   (dump storage on stdout)\n"
                + "\t                        format=content|meta|full\n"
                //                + "\tdump_to_file <destination> [deleted] (dump storage to file system at the server)\n"
                //                + "\t              destination=absolute folder path on the server. The folder must not exist\n"
                //                + "\t                            deleted=true|false. If false, records marked as deleted are skipped.\n"
                + "\tclear base   (clear all records from base)\n"
                + "\tholdings     (show information on the records in the storage - potentially very slow)\n"
                + "\tstats        (show performance statistics)\n"
                // Stats disables for now af they are extremely slow with H2
                //                + "\trelation_stats [extended] (show statistics on relations. Slow if extended: true)\n"
                //                + "\trelation_cleanup [condition] (clean up of relations)\n"
                //                + "\t                  condition: none_valid(default)|only_parent_valid|only_child_valid|only_one_valid\n"
                + "\tbatchjob <jobname> [base] [minMtime] [maxMtime]   (empty base string means all bases)\n"
                + "\tbackup <destination>   (full copy of the running storage at the point of command execution)\n");
    }

    /**
     * Main method for the Storage Tool, arguments given to this should be the
     * command that should be run and possible arguments to this command.
     *
     * @param args Command line arguments telling which command to run and
     *             possible some arguments needed by this command.
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
            System.err.println("Unable to load system config: " + e.getMessage() + ".\nUsing default configuration");
            conf = Configuration.newMemoryBased();
        }

        /* Make sure the summa.rpc.vendor property is set */
        if (!conf.valueExists(ConnectionConsumer.CONF_RPC_TARGET)) {
            rpcVendor = System.getProperty(ConnectionConsumer.CONF_RPC_TARGET);

            if (rpcVendor != null) {
                conf.set(ConnectionConsumer.CONF_RPC_TARGET, rpcVendor);
            } else {
                conf.set(ConnectionConsumer.CONF_RPC_TARGET, DEFAULT_RPC_TARGET);
            }
        }

        System.err.println("Using storage on: " + conf.getString(ConnectionConsumer.CONF_RPC_TARGET));

        StorageReaderClient reader = new StorageReaderClient(conf);
        StorageWriterClient writer = new StorageWriterClient(conf);

        int exitCode;
        switch (action) {
            case "get":
                exitCode = actionGet(args, reader, null);
                break;
            case "get_single":
                exitCode = actionGet(args, reader, false);
                break;
            case "get_expand":
                exitCode = actionGet(args, reader, true);
                break;
            case "put":
                exitCode = actionPut(args, writer);
                break;
            case "peek":
                exitCode = actionPeek(args, reader);
                break;
            case "list":
                exitCode = actionList(args, reader);
                break;
            case "touch":
                exitCode = actionTouch(args, reader, writer);
                break;
            case "delete":
                exitCode = actionDelete(args, reader, writer);
                break;
            case "xslt":
                exitCode = actionXslt(args, reader);
                break;
            case "dump":
                exitCode = actionDump(args, reader);
                break;
            case "dump_to_file":
                exitCode = actionDumpToFile(args, reader);
                break;
            case "clear":
                exitCode = actionClear(args, writer);
                break;
            case "holdings":
                exitCode = actionHoldings(reader);
                break;
            case "stats":
                exitCode = actionStatistics(reader);
                break;
            case "relation_stats":
                exitCode = actionRelationStatistics(args, reader);
                break;
            case "relation_cleanup":
                exitCode = actionRelationCleanup(args, reader);
                break;
            case "batchjob":
                exitCode = actionBatchJob(args, writer);
                break;
            case "backup":
                exitCode = actionBackup(args, writer);
                break;
            default:
                System.err.println("Unknown action '" + action + "'");
                printUsage();
                exitCode = 2;
                break;
        }
        System.exit(exitCode);
    }

}
