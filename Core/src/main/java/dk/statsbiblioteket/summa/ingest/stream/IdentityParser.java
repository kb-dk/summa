/*
 * Created: te 31-03-2008 23:40:14
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.ingest.split.ThreadedStreamParser;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Takes the received payload and immediately adds it to the out queue.
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

    public IdentityParser(Configuration conf) {
        super(conf);
        log.debug("IdentityParser created");
    }

    @Override
    protected void protectedRun(Payload source) throws Exception {
        Logging.logProcess("IdentityParser", "Passing Payload unmodified", Logging.LogLevel.TRACE, source);
        addToQueue(source);
    }
}
