package dk.statsbiblioteket.summa.storage.api.filter;

import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import dk.statsbiblioteket.summa.storage.api.StorageWriterClient;
import dk.statsbiblioteket.summa.storage.api.WritableStorage;
import dk.statsbiblioteket.util.Strings;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple filter that clears a given set of bases on the first invocation
 * and is a no-op on any subsequent calls to {@code pump()}.
 * <p/>
 * To configure the target storage set the
 * {@link dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer#CONF_RPC_TARGET}
 * property.
 */
public class ClearBaseFilter extends ObjectFilterImpl {

    private static final Log log = LogFactory.getLog(ClearBaseFilter.class);

    /**
     * A list of bases to clear. Default is the empty list
     */
    public static final String CONF_CLEAR_BASES = "summa.storage.clearbases";

    private WritableStorage storage;
    private List<String> bases;
    private boolean fired;

    public ClearBaseFilter(WritableStorage storage, List<String> bases) {
        super(Configuration.newMemoryBased());
        this.storage = storage;
        this.bases = bases;
        fired = false;

        log.info("Created ClearBaseFilter directly on Storage for bases: "
                 + Strings.join(bases, ", "));
    }

    public ClearBaseFilter(Configuration conf) {
        super(conf);
        log.trace("Creating StorageClient");
        storage = new StorageWriterClient(conf);
        log.trace("Created StorageClient");
        bases = conf.getStrings(CONF_CLEAR_BASES, new ArrayList<String>());
        fired = false;

        log.info("Created ClearBaseFilter for bases: "
                 + Strings.join(bases, ", "));
    }

    @Override
    protected synchronized boolean processPayload(Payload payload)
                                                       throws PayloadException {
        if (fired) {
            if (log.isTraceEnabled()) {
                log.trace("Already fired. No-op");
            }
            return true;
        }
        fired = true;

        for (String base : bases) {
            try {
                //noinspection DuplicateStringLiteralInspection
                log.info("Clearing base: " + base);
                storage.clearBase(base);
            } catch (IOException e) {
                log.error("Error clearing base: " + base, e);
            }
        }

        return true;
    }

    public boolean hasFired() {
        return fired;
    }
}
