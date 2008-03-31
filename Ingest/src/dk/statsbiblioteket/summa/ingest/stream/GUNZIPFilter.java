/**
 * Created: te 31-03-2008 23:40:14
 * CVS:     $Id:$
 */
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The filter takes the content-parts of the received streams and gunzips it.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class GUNZIPFilter {
    // How to handle multiple substreams, when the length is unknown?
}
