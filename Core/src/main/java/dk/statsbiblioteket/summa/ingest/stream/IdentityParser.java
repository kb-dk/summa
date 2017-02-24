/*
 * Created: te 31-03-2008 23:40:14
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.ingest.split.ThreadedStreamParser;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Takes the received payload and immediately adds it to the out-queue.
 * </p><p>
 * Sample use it to act as as pre-fetcher placed immediately after a RecordReader, to avoid the wait in the
 * {@code request_batch -> wait -> process_batch -> repeat} process pattern.
 * </p><p>
 * It is highly recommended to define fitting queue sizes using the {@link ThreadedStreamParser} properties.
 * </p><p>
 * This Class is normally used by a {@link dk.statsbiblioteket.summa.ingest.split.StreamController}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IdentityParser extends ThreadedStreamParser {
    private static Log log = LogFactory.getLog(IdentityParser.class);

    /**
     * Not exactly identity... If true, the content of records is uncompressed (if it is not already uncompressed).
     * This can be used for optimization if subsequent filters access otherwise compressed content multiple times.
     * This has no effect if the Payload contains a Stream and not a Record.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_UNCOMPRESS = "identity.uncompress";
    public static final boolean DEFAULT_UNCOMPRESS = false;

    private final boolean uncompress;

    public IdentityParser(Configuration conf) {
        super(conf);
        uncompress = conf.getBoolean(CONF_UNCOMPRESS, DEFAULT_UNCOMPRESS);
        log.debug("Created " + this + " with uncompress=" + uncompress);
    }

    @Override
    protected boolean acceptStreamlessPayloads() {
        return true;
    }

    @Override
    protected void protectedRun(Payload source) throws Exception {
        if (uncompress && source.getRecord() != null && source.getRecord().isContentCompressed()) {
            Logging.logProcess("IdentityParser", "Uncompressing Payload content", Logging.LogLevel.TRACE, source);
            RecordUtil.adjustCompression(source.getRecord(), null, false);
        } else if (log.isDebugEnabled()) {
            Logging.logProcess("IdentityParser", "Passing Payload unmodified", Logging.LogLevel.TRACE, source);
        }
        addToQueue(source);
    }
}
