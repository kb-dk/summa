/**
 * Created: te 31-03-2008 23:40:14
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.ingest.stream;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
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
public class GUNZIPFilter implements ObjectFilter {
    private static Log log = LogFactory.getLog(GUNZIPFilter.class);

    private ObjectFilter source;
    private Payload lastPayload = null;

    /**
     * The GUNZIPFilter does not use the provided configuration for anything.
     * @param configuration setup for GUNZIPFilter.
     */
    public GUNZIPFilter(Configuration configuration) {
        log.debug("Filter created");
    }

    public boolean hasNext() {
        if (source == null) {
            log.warn("hasNext() called with empty source");
            return false;
        }
        return source.hasNext();
    }

    /**
     * Requests the next payload from the source, clones it and replaces the
     * stream with new GZIPInputStream(oldStream).
     * @return the source payload, with stream wrapped in gunzip.
     */
    public synchronized Payload next() {
        if (source == null) {
            log.warn("next() called with empty source");
            throw new NoSuchElementException("No source defined, so no payloads"
                                             + " can be returned");
        }
        lastPayload = source.next().clone();
        while (lastPayload.getStream() == null) {
            log.error("No stream found in Payload '" + lastPayload
                      + "'. Skipping to next payload");
            lastPayload = source.next().clone();
        }
        try {
            log.trace("Wrapping stream in GZIPInputStream");
            lastPayload.setStream(new GZIPInputStream(lastPayload.getStream()));
        } catch (IOException e) {
            throw new RuntimeException("Unable to create GZIPInputstream", e);
        }
        return lastPayload;
    }

    public void remove() {
        log.warn("Remove not supported");
    }

    public void setSource(Filter filter) {
        if (filter instanceof ObjectFilter) {
            source = (ObjectFilter)filter;
        } else {
            throw new IllegalArgumentException("This filter only accepts "
                                               + "ObjectFilter as source. "
                                               + "Got " + filter.getClass());
        }
    }

    /**
     * Pumps the last delivered payload empty, then moved onto the next payload
     * until there are no payloads left.
     * @return true if pumping should continue.
     * @throws IOException in case of a read error.
     */
    public boolean pump() throws IOException {
        if (source == null) {
            log.warn("pump() called with empty source");
            return false;
        }
        return !(lastPayload == null || lastPayload.getStream().read() == -1)
               || hasNext() && next() != null;
    }

    public void close(boolean success) {
        if (source == null) {
            log.warn("close() called with empty source");
            return;
        }
        source.close(true);
    }
}
