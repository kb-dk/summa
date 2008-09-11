package dk.statsbiblioteket.summa.search.dummy;

import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.dummy.DummyResponse;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Dummy implementattion of a {@link dk.statsbiblioteket.summa.search.api.SummaSearcher} returning
 * {@link dk.statsbiblioteket.summa.search.api.dummy.DummyResponse}s.
 *
 * @see SearchNodeDummy
 * @see dk.statsbiblioteket.summa.search.api.dummy.DummyResponse
 * @see SummaSearcherImpl
 */
public class SummaSearcherDummy implements SummaSearcher {

    private static final Log log = LogFactory.getLog (SummaSearcherDummy.class);

    private int closeCount;
    private int searchCount;

    public SummaSearcherDummy(Configuration conf) {
        closeCount = 0;
        searchCount = 0;
    }

    public ResponseCollection search(Request request) throws IOException {
        log.info ("Got request (" + searchCount + "): " + request);

        ResponseCollection resp = new ResponseCollection ();
        resp.add (new DummyResponse(0, 0, closeCount, searchCount));

        searchCount++;
        return resp;
    }

    public void close() throws IOException {
        log.info ("Got close request (" + closeCount + ")");
        closeCount++;
    }
}



