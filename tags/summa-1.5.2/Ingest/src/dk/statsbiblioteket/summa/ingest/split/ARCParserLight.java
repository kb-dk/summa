/**
 * Created: te 19-08-2009 23:03:17
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.ingest.split;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Performs basic parsing of the Internet Archive ARC format by storing
 * ARC-record meta-data as Payload meta data and ARC-record content as
 * an embedded stream in the Payload.
 * </p><p>
 * This parser will pause after creating a Payload until the Payload is closed.
 * This is necessary for the stream-oriented nature of the filter.
 * </p><p>
 * The ARC File Format 1.0 is supported. GZIPped ARC files are supported
 * indirectly by inserting a GUNZIPFilter in front of this parser.
 * @see {@url http://www.archive.org/web/researcher/ArcFileFormat.php}
 */
// TODO: Implement this class
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ARCParserLight extends ThreadedStreamParser {
    private static Log log = LogFactory.getLog(ARCParserLight.class);

    private long runCount = 0;
    private long producedPayloads = 0;
    private long runtimeNS = 0;

    public ARCParserLight(Configuration conf) {
        super(conf);
        log.debug("ARCParserLight conctructed");
    }

    @Override
    protected void protectedRun() throws Exception {
        log.trace("Entering protectedRun()");
        long startTime = System.nanoTime();
        long localPayloads = 0;

        while (running) {
            
        }
        extractARCHeader(sourcePayload);
        // Extract ARC header, create initial Payload (optionally?)
        // while (!EOF) { create ARCRecordStream, wait for close }

        long spendTime = System.nanoTime() - startTime;
        //noinspection DuplicateStringLiteralInspection
        log.trace("Produced " + localPayloads + " payloads in "
                  + (spendTime / 1000000.0) + "ns");

        runCount++;
        producedPayloads += localPayloads;
        runtimeNS += spendTime;
    }

    private void extractARCHeader(Payload sourcePayload) {
        // TODO: Implement this
    }
}
