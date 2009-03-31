/**
 * Created: te 31-03-2009 12:20:27
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Logging;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Pattern;
import java.util.Calendar;

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
    public static final String CONF_OUTPUTFOLDER =
            "summa.dumpfilter.outputfolder";

    /**
     * The passing Payload's base must match this Pattern-expression to be
     * dumped.
     * </p><p>
     * Optional. Default is ".*" (all bases).
     */
    public static final String CONF_BASEEXP =
            "summa.dumpfilter.baseexp";
    public static final String DEFAULT_BASEEXP = ".*";

    /**
     * The passing Payload's id must match this Pattern-expression to be dumped.
     * </p><p>
     * Optional. Default is ".*" (all ids).
     */
    public static final String CONF_IDEXP =
            "summa.dumpfilter.idexp";
    public static final String DEFAULT_IDEXP = ".*";

    /**
     * If true, Payloads without Records are dumped as well.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_DUMP_NONRECORDS =
            "summa.dumpfilter.dumpnonrecord";
    public static final boolean DEFAULT_DUMP_NONRECORDS = true;

    private File output;
    private Pattern basePattern;
    private Pattern idPattern;
    private boolean dumpNonRecords = DEFAULT_DUMP_NONRECORDS;

    public DumpFilter(Configuration conf) {
        super(conf);
        log.trace("Creating DumpFilter");
        String outputStr = conf.getString(CONF_OUTPUTFOLDER, null);
        if (outputStr == null) {
            //noinspection DuplicateStringLiteralInspection
            output = new File(new File(System.getProperty("java.io.tmpdir")),
                                       "payloads");
        } else {
            output = new File(outputStr);
        }
        log.debug("Output folder is '" + outputStr + "'");
        if (!output.exists()) {
            output.mkdir();
        }
        basePattern =
                Pattern.compile(conf.getString(CONF_BASEEXP, DEFAULT_BASEEXP));
        idPattern = Pattern.compile(conf.getString(CONF_IDEXP, DEFAULT_IDEXP));
        dumpNonRecords = conf.getBoolean(CONF_DUMP_NONRECORDS, dumpNonRecords);
        log.info(String.format(
                "Created DumpFilter with base='%s', id='%s', dumpNonRecords=%b",
                conf.getString(CONF_BASEEXP, DEFAULT_BASEEXP),
                conf.getString(CONF_IDEXP, DEFAULT_IDEXP),
                dumpNonRecords));
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getRecord() == null && dumpNonRecords) {
            dump(payload);
        } else if (basePattern.matcher(payload.getRecord().getBase()).matches()
            && idPattern.matcher(payload.getRecord().getId()).matches()) {
            dump(payload);
        } else {
            Logging.logProcess("DumpFilter", "Not dumping",
                               Logging.LogLevel.TRACE, payload);
        }
        return true;
    }

    private void dump(Payload payload) throws PayloadException {
        String fileName = getFileName(payload);

        StringWriter meta = new StringWriter(1000);
        meta.append(payload.toString());
        try {
            if (payload.getRecord() != null) {
                meta.append("\n").append(payload.getRecord().toString(true));
                Files.saveString(payload.getRecord().getContentAsUTF8(),
                                 new File(output, fileName + ".content"));
            }
            Files.saveString(meta.toString(), new File(output, fileName + ".meta"));
        } catch (IOException e) {
            throw new PayloadException(
                    "Unable to dump content", e, payload);
        }
    }

    private Pattern safePattern = Pattern.compile("[a-zA-Z0-9\\-\\_\\.]");
    private int counter = 0;
    private String getFileName(Payload payload) {
        String candidate = payload.getId();
        if (candidate == null || "".equals(candidate)) {
            Calendar calendar = Calendar.getInstance();
            return String.format("%1$tF_%1$tH%1$tM%1$tS_", calendar)
                   + Integer.toString(counter++);
        }
        StringWriter fn = new StringWriter(candidate.length());
        for (char c: candidate.toCharArray()) {
            if (safePattern.matcher("" + c).matches()) {
                fn.append(c);
            } else {
                fn.append("_");
            }
        }
        String actual = fn.toString();
        log.trace("Transformed the name '" + candidate + "' into '"
                  + actual + "'");
        return actual;
    }
}


