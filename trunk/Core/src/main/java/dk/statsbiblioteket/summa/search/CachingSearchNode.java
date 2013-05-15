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
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.util.caching.TimeSensitiveCache;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Wrapper that provides caching of Search responses.
 * </p></p>
 * The underlying Search node is specified by the property {@link SearchNodeFactory#CONF_NODE} and are constructed using
 * {@link SearchNodeFactory}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class CachingSearchNode extends ArrayList<SearchNode> implements SearchNode {
    private static final long serialVersionUID = 89755568541L;
    private static Log log = LogFactory.getLog(CachingSearchNode.class);

    /**
     * The timeout before a cache entry is evicted.
     *</p><p>
     * Milliseconds, optional. Default is 120000 (2 minutes).
     */
    public static final String CONF_TIMEOUT = "cache.timeoutms";
    public static final int DEFAULT_TIMEOUT = 2 * 60 * 1000;

    /**
     * The maximum number of responses to hold in cache.
     */
    public static final String CONF_MAXCACHE = "cache.maxcache";
    public static final int DEFAULT_MAXCACHE = 1000;

    private final SearchNode subNode;
    private final int timeout;
    private final int maxCache;
    private final TimeSensitiveCache<String, ResponseCollection> cache;

    public CachingSearchNode(Configuration conf) throws RemoteException {
        subNode = SearchNodeFactory.createSearchNode(conf);
        timeout = conf.getInt(CONF_TIMEOUT, DEFAULT_TIMEOUT);
        maxCache = conf.getInt(CONF_MAXCACHE, DEFAULT_MAXCACHE);
        cache = new TimeSensitiveCache<String, ResponseCollection>(timeout, false, maxCache);
        log.info("Constructed " + this);
    }

    @Override
    public void search(final Request request, final ResponseCollection responses) throws RemoteException {
        throw new UnsupportedOperationException("Not implemented yet");

        // The problem is that we need to copy the ResponseCollection but that is not part of the API
    }

    @Override
    public String toString() {
        return "CachingSearchNode(subNode=" + subNode + ", timeout=" + timeout + "ms, maxCache=" + maxCache + ")";
    }

    // Note: Warmup is not paged
    @Override
    public void warmup(final String request) {
        if (log.isTraceEnabled()) {
            log.trace("Performing warmup with " + request);
        }
        subNode.warmup(request);
    }

    @Override
    public void open(final String location) throws RemoteException {
        log.debug(String.format("open(%s) called", location));
        subNode.open(location);
    }
    @Override
    public void close() {
        log.trace("close() called");
        try {
            subNode.close();
        } catch (RemoteException e) {
            log.error("Got a RemoteException during close. This should not happen", e);
        }
    }

    /**
     * @return the minimum free slots for the underlying searchers.
     */
    @Override
    public int getFreeSlots() {
        log.trace("getFreeSlots called");
        return subNode.getFreeSlots();
    }
}
