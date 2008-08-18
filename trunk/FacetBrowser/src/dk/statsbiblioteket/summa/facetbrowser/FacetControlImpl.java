/**
 * Created: te 13-08-2008 10:29:07
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.facetbrowser;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The default implementation of FacetControl. Unless otherwise configured,
 * this will use a Lucene backend for building the underlying structure.
 * This implementation relies on a DocumentSearcher being present  
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class FacetControlImpl implements FacetControl, Configurable {
    private Log log = LogFactory.getLog(FacetControlImpl.class);

    public FacetControlImpl(Configurable conf) {
        log.info("Constructing FacetControlImpl");

    }

}
