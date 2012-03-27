/*
 * Created: te 31-03-2008 23:40:14
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.ingest.stream;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.ingest.split.ThreadedStreamParser;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Extract locations (URLs, everything accepted by the ClassLoader, files etc.)
 * from Payload Streams, creating a new Payload for each location with a Stream
 * to the content at the location.
 * </p><p>
 * This Class is normally used by a 
 * {@link dk.statsbiblioteket.summa.ingest.split.StreamController}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class PullParser extends ThreadedStreamParser {
    private static Log log = LogFactory.getLog(PullParser.class);

    /**
     * The charset to use when opening the Stream in the source Payload.
     * </p><p>
     * Optional. Default is utf-8.
     */
    public static final String CONF_CHARSET = "ingest.pullfilter.charset";
    public static final String DEFAULT_CHARSET = "utf-8";

    private String charset = DEFAULT_CHARSET;

    public PullParser(Configuration conf) {
        super(conf);
        log.debug("PullParser created");
    }

    @Override
    protected void protectedRun() throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(
                sourcePayload.getStream(), charset));
        Logging.logProcess("PullParser", "Processing source",
                           Logging.LogLevel.TRACE, sourcePayload);
        while (in.ready()) {
            String line = in.readLine();
            if (line == null || "".equals(line)) {
                continue;
            }
            URL url = Resolver.getURL(line);
            if (url == null) {
                //noinspection DuplicateStringLiteralInspection
                log.warn(String.format("Unable to resolve '%s' to URL", line));
                continue;
            }
            Payload payload = null;
            try {
                payload = new Payload(url.openStream());
                payload.getData().put(Payload.ORIGIN, url);
                log.debug(String.format("Created Payload with Stream to '%s'",
                                        url));
            } catch (Exception e) {
                log.warn(String.format("Unable to create Payload for line '%s'",
                                       line));
            }
            addToQueue(payload);
        }
        log.debug("Finished processing " + sourcePayload);
    }
}