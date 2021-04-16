/**
 * Created: te 31-03-2009 12:20:27
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.util.CopyingInputStream;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.io.Streams;

import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Dumps received Payloads in a designated folder. Useful for debugging.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te-refind")
public class DumpFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(DumpFilter.class);

    /**
     * The output folder for the dumps.
     * </p><p>
     * Optional. Default is "temp-folder/payloads", where the temp-folder is
     *           system-dependent.
     */
    public static final String CONF_OUTPUTFOLDER = "summa.dumpfilter.outputfolder";

    /**
     * The passing Payload's base must match this Pattern-expression to be
     * dumped.
     * </p><p>
     * Optional. Default is ".*" (all bases).
     */
    public static final String CONF_BASEEXP = "summa.dumpfilter.baseexp";
    public static final String DEFAULT_BASEEXP = ".*";

    /**
     * The passing Payload's id must match this Pattern-expression to be dumped.
     * </p><p>
     * Optional. Default is ".*" (all ids).
     */
    public static final String CONF_IDEXP = "summa.dumpfilter.idexp";
    public static final String DEFAULT_IDEXP = ".*";

    /**
     * If true, Payloads without Records are dumped as well.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_DUMP_NONRECORDS = "summa.dumpfilter.dumpnonrecord";
    public static final boolean DEFAULT_DUMP_NONRECORDS = true;

    /**
     * If true, Payloads with Records are processed with
     * RecordUtil.toXML(false) and the result dumped.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_DUMP_XML = "summa.dumpfilter.dumpxml";
    public static final boolean DEFAULT_DUMP_XML = false;

    /**
     * If true, the content of streams in Payloads are dumped. The dumping
     * normally takes place when the stream is read. However, if close is called
     * before the input stream is emptied, the rest of the bytes from the
     * input stream will be copied to the output file.
     * </p><p>
     * Optional. Default is false;
     */
    public static final String CONF_DUMP_STREAMS = "summa.dumpfilter.dumpstreams";
    public static final boolean DEFAULT_DUMP_STREAMS = false;

    /**
     * If true, the raw content of Records is dumped.
     * Important: This is truly raw, so if the content is compressed, it will be the compressed
     * bytes that are dumped.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_DUMP_RAW_CONTENT = "summa.dumpfilter.dumprawcontent";
    public static final boolean DEFAULT_DUMP_RAW_CONTENT = false;

    /**
     * The maximum number of Records to dump. After this number is reached,
     * no more dumps will be made.
     * </p><p>
     * Optional. The default is 500. Setting this to -1 means infinite dumps.
     */
    public static final String CONF_MAXDUMPS = "summa.dumpfilter.maxdumps";
    public static final int DEFAULT_MAXDUMPS = 50;

    /**
     * The maximum number of Records to dump from any recordBase.
     * This setting only has effect if the received Payloads contains Records.
     * Setting this without explicitly setting {@link #CONF_MAXDUMPS} will set CONF_MAXDUMPS to -1.
     * </p><p>
     * Optional. Default is -1 (infinite).
     */
    public static final String CONF_MAXBASEDUMPS = "summa.dumpfilter.maxbasedumps";
    public static final int DEFAULT_MAXBASEDUMPS = -1;

    /**
     * If the dumper has not received any Payloads for this number of ms,
     * the max dumps counter is reset to 0. The intendedscenario is a
     * watching ingester or indexer which starts with a huge amount of Payloads,
     * then has a period of inactivity followed by a new batch.
     * </p><p>
     * Optional. The default is 60,000 (1 minute). Setting this to -1 disables
     * the reset og the max dumps counter.
     */
    public static final String CONF_RESET_MAXDUMPS_MS = "summa.dumpfilter.reset.maxdumps.ms";
    public static final int DEFAULT_RESET_MAXDUMPS_MS = 60 * 1000;

    private File output;
    private Pattern basePattern;
    private Pattern idPattern;
    private boolean dumpNonRecords = DEFAULT_DUMP_NONRECORDS;
    private boolean dumpStreams = DEFAULT_DUMP_STREAMS;
    private final boolean dumpRawContent;
    private boolean dumpXML = DEFAULT_DUMP_XML;
    private final int maxDumps;
    private int resetReceivedDumpsMS = DEFAULT_RESET_MAXDUMPS_MS;
    private final int maxBaseDumps;
    private final Map<String, Long> baseCounters;
    // TODO: Implement this

    private long payloadsDumpedSinceReset = 0;
    private long lastPayloadReceivedTimestamp = 0;

    public DumpFilter(Configuration conf) {
        super(conf);
        log.trace("Creating DumpFilter");
        String outputStr = conf.getString(CONF_OUTPUTFOLDER, null);
        output = outputStr == null ?
                 new File(new File(System.getProperty("java.io.tmpdir")), "payloads") :
                 new File(outputStr);
        log.debug("Output folder is '" + outputStr + "'");
        if (!output.exists() && !output.mkdirs()) {
            log.warn("Output folder '" + outputStr + "' does not exist and was not created.");
        }
        basePattern = Pattern.compile(conf.getString(CONF_BASEEXP, DEFAULT_BASEEXP));
        idPattern = Pattern.compile(conf.getString(CONF_IDEXP, DEFAULT_IDEXP));
        dumpNonRecords = conf.getBoolean(CONF_DUMP_NONRECORDS, dumpNonRecords);
        dumpStreams = conf.getBoolean(CONF_DUMP_STREAMS, dumpStreams);
        dumpXML = conf.getBoolean(CONF_DUMP_XML, dumpXML);
        maxBaseDumps = conf.getInt(CONF_MAXBASEDUMPS, DEFAULT_MAXBASEDUMPS);
        maxDumps = conf.getInt(CONF_MAXDUMPS, conf.containsKey(CONF_MAXBASEDUMPS) ? -1 : DEFAULT_MAXDUMPS);
        baseCounters = maxBaseDumps >= 0 ? new HashMap<String, Long>() : null;
        resetReceivedDumpsMS = conf.getInt(CONF_RESET_MAXDUMPS_MS, resetReceivedDumpsMS);
        dumpRawContent = conf.getBoolean(CONF_DUMP_RAW_CONTENT, DEFAULT_DUMP_RAW_CONTENT);
        feedback = false; // No timestats on dump
        setStatsDefaults(conf, false, false, false, false);
        log.info(String.format(
                Locale.ROOT, "Created DumpFilter '%s' with base='%s', id='%s', dumpNonRecords=%b, maxDumps=%d, " +
                             "maxBaseDumps=%d, resetMaxDumpsMS=%d",
                getName(), conf.getString(CONF_BASEEXP, DEFAULT_BASEEXP), conf.getString(CONF_IDEXP, DEFAULT_IDEXP),
                dumpNonRecords, maxDumps, maxBaseDumps, resetReceivedDumpsMS));
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (resetReceivedDumpsMS != -1 &&
            System.currentTimeMillis() - lastPayloadReceivedTimestamp > resetReceivedDumpsMS) {
            payloadsDumpedSinceReset = 0;
        }
        lastPayloadReceivedTimestamp = System.currentTimeMillis();
        if (baseCounters != null && payload.getRecord() != null) {
            Long count = baseCounters.get(payload.getRecord().getBase());
            if (count == null) {
                count = 0L;
            }
            count++;
            baseCounters.put(payload.getRecord().getBase(), count);
            if (count > maxBaseDumps) {
                Logging.logProcess(getName(),
                                   "Not dumping as received payloads (" + count + ") for recordBase=" +
                                   payload.getRecord().getBase() + " was > than max base dumps " + maxBaseDumps,
                                   Logging.LogLevel.TRACE, payload);
                return true;
            }
        }
        if (maxDumps != -1 && payloadsDumpedSinceReset > maxDumps) {
            //noinspection DuplicateStringLiteralInspection
            Logging.logProcess(getName(), "Not dumping as received payloads since reset " + payloadsDumpedSinceReset
                               + " was > than max dumps " + maxDumps, Logging.LogLevel.TRACE, payload);
            return true;
        }
        if (payload.getRecord() == null && dumpNonRecords
            || basePattern.matcher(payload.getRecord().getBase()).matches()
               && idPattern.matcher(payload.getRecord().getId()).matches()) {
            payloadsDumpedSinceReset++;
            dump(payload);
        } else {
            //noinspection DuplicateStringLiteralInspection
            Logging.logProcess(getName(), "Not dumping", Logging.LogLevel.TRACE, payload);
        }
        return true;
    }

    private int errorCounter = 0;
    private void dump(Payload payload) throws PayloadException {
        String fileName = RecordUtil.getFileName(payload);
        StringWriter meta = new StringWriter(1000);
        meta.append(payload.toString(true));
        try {
            if (payload.getRecord() != null) {
                meta.append("\n").append(payload.getRecord().toString(true));
                Files.saveString(payload.getRecord().getContentAsUTF8(), new File(output, fileName + ".content"));
                if (dumpXML) {
                    Files.saveString(RecordUtil.toXML(payload.getRecord(), false), new File(output, fileName + ".xml"));
                }
            }
            Files.saveString(meta.toString(), new File(output, fileName + ".meta"));
        } catch (IOException e) {
            errorCounter++;
            if (errorCounter <= 20) {
                log.warn("Exception #" + errorCounter + " while attempting to dump " + fileName + ".meta. " +
                         "This message is only shown for the first 20 exceptions", e);
            }
            throw new PayloadException("Unable to dump content", e, payload);
        }
        if (dumpRawContent && payload.getRecord() != null) {
            rawContent(payload.getRecord());
        }
        if (!dumpStreams || payload.getStream() == null) {
            return;
        }
        log.trace("Wrapping " + payload + " stream in dumping stream");
        wrapStream(payload);
    }

    private void rawContent(Record record) {
        File outFile = new File(output, RecordUtil.getFileName(record.getId()) + ".rawcontent");
        OutputStream out;
        try {
            out = new FileOutputStream(outFile);
            Streams.pipeAll(new ByteArrayInputStream(record.getContent(false)), out);
        } catch (FileNotFoundException e) {
            log.warn(String.format(Locale.ROOT,
                    "Unable to create an output stream for %s with name '%s'",
                    record.getId(), outFile));
        } catch (IOException e) {
            log.warn(String.format(Locale.ROOT,
                    "Unable to dump raw content from %s to '%s'",
                    record.getId(), outFile));
        }
    }

    private void wrapStream(Payload payload) {
        //noinspection DuplicateStringLiteralInspection
        File outFile = new File(output, RecordUtil.getFileName(payload) + ".stream");
        OutputStream out;
        try {
            out = new FileOutputStream(outFile);
        } catch (FileNotFoundException e) {
            log.warn(String.format(Locale.ROOT,
                    "Unable to create an output stream for %s with name '%s'",
                    payload, outFile));
            return;
        }
        payload.setStream(new CopyingInputStream(payload.getStream(), out, true));
    }
}
