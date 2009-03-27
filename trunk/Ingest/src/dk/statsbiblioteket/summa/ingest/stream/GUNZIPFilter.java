/*
 * Created: te 31-03-2008 23:40:14
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.ingest.stream;

import java.io.IOException;
import java.util.zip.GZIPInputStream;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The filter takes the content-parts of the received streams and gunzips it.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment="Needs basic unit testing")
public class GUNZIPFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(GUNZIPFilter.class);

    /**
     * The GUNZIPFilter does not use the provided configuration for anything.
     * @param configuration setup for GUNZIPFilter.
     */
    public GUNZIPFilter(Configuration configuration) {
        super(configuration);
        log.debug("Filter created");
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getStream() == null) {
            log.debug("No Stream present in " + payload);
            throw new PayloadException("No Stream present", payload);
        }
        log.debug("Wrapping Stream in " + payload + " in GZIPInputStream");
        Logging.logProcess(getName(), "Wrapping Stream in GZIPInputStream",
                           Logging.LogLevel.TRACE, payload);
        try {
            payload.setStream(new GZIPInputStream(payload.getStream()));
        } catch (IOException e) {
            throw new PayloadException("Unable to create GZIPInputstream",
                                       e, payload);
        }
        return true;
    }
}
