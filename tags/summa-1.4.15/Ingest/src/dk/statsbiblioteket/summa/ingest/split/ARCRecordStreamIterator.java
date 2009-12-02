/**
 * Created: te 20-08-2009 00:08:37
 * CVS:     $Id:$
 */
package dk.statsbiblioteket.summa.ingest.split;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ARCRecordStreamIterator implements Iterator<ARCRecordStream> {
    private static Log log = LogFactory.getLog(ARCRecordStreamIterator.class);

    public boolean hasNext() {
        return false;  // TODO: Implement this
    }

    public ARCRecordStream next() {
        return null;  // TODO: Implement this
    }

    public void remove() {
        // TODO: Implement this
    }
}
