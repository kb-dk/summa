package dk.statsbiblioteket.summa.storage.api.filter;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test storage connection when constructing this object. This fails hard if
 * storage connection fails. This means that if this is the first in a filter
 * chain, then we shutdown filter, before marking any ingest files completed.
 *
 * @author Henrik Kirk <mailto:hbk@statsbiblioteket.dk> *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hbk")
public class StorageValidationFilter extends ObjectFilterImpl {
    /** Local logger. */
    private static Log log = LogFactory.getLog(StorageValidationFilter.class);
    /**
     * Constructs this filter, when constructing we trying to contact storage,
     * if this is not up, we are failing.
     * @param conf The configuration for setting up this filter.
     * @throws IOException If connection to storage fails and chain therefore
     * should be stopped.
     */
    public StorageValidationFilter(Configuration conf) throws IOException {
        super(conf);
        try {
            StorageReaderClient storage = new StorageReaderClient(conf);
            if(storage == null) {
                throw new IOException("Storage reader client is null");
            }
            long time = storage.getModificationTime(null);
            log.debug("Storage returns from getModifcationTime(null) value: "
                      + time);
        } catch (IOException e) {
            log.fatal("Connection to storage failed with message: "
                      + e.getMessage());
            throw new IOException("Connection to storage failed", e);
        }
    }

    /**
     * Doesn't add any functionality to the filter chain.
     * @param payload Not used. 
     * @return True in every case.
     */
    @Override
    protected boolean processPayload(Payload payload) {
        return true;
    }
}
