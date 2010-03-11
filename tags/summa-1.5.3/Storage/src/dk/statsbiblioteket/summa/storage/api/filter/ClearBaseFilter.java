/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.storage.api.filter;

import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.PayloadMatcher;

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
 * and is a no-op on any subsequent calls to {@code pump()}. This filter is
 * somewhat dangerous to use: If the base is cleared and the subsequent ingest
 * is interrupted, a lot of records will be left marked deleted. If possible,
 * it is highly recommended to use {@link UpdateFromFulldumpFilter} as this
 * ensures that a failed ingest leaves the Storage in a usable stage.
 * </p><p>
 * If any property from {@link PayloadMatcher} is defined, the bases will be
 * cleared if and only if the PayloadMatcher matches a Payload.
 * </p><p>
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
    private PayloadMatcher payloadMatcher;

    public ClearBaseFilter(WritableStorage storage, List<String> bases) {
        super(Configuration.newMemoryBased());
        this.storage = storage;
        this.bases = bases;
        payloadMatcher = new PayloadMatcher(Configuration.newMemoryBased());
        fired = false;

        log.info("Created ClearBaseFilter directly on Storage for bases: "
                 + Strings.join(bases, ", "));
    }

    public ClearBaseFilter(WritableStorage storage, Configuration conf) {
        super(conf);
        this.storage = storage;
        bases = conf.getStrings(CONF_CLEAR_BASES, new ArrayList<String>());
        payloadMatcher = new PayloadMatcher(conf);
        fired = false;

        log.info("Created ClearBaseFilter directly on Storage "
                 + "with configuration");
    }

    public ClearBaseFilter(Configuration conf) {
        super(conf);
        log.trace("Creating StorageClient");
        storage = new StorageWriterClient(conf);
        log.trace("Created StorageClient");
        bases = conf.getStrings(CONF_CLEAR_BASES, new ArrayList<String>());
        payloadMatcher = new PayloadMatcher(conf, false);
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
        if (payloadMatcher.isMatcherActive()) {
            log.trace("Performing match check with PayloadMatcher");
            if (!payloadMatcher.isMatch(payload)) {
                if (log.isTraceEnabled()) {
                    log.trace("The PayloadMatcher did not match " + payload);
                }
                return true;
            }
        }

        fired = true;
        log.debug("Performing clear on all bases");
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

