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
package dk.statsbiblioteket.summa.facetbrowser;

import dk.statsbiblioteket.summa.common.util.SimplePair;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.exposed.facet.CollectorPool;
import org.apache.lucene.search.exposed.facet.CollectorPoolFactory;
import org.apache.lucene.search.exposed.facet.TagCollector;
import org.apache.lucene.search.exposed.facet.request.FacetRequest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles communication with the Exposed CollectorPoolFactory. Mainly used for logging and error handling.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class PoolFactoryGate {
    private static Log log = LogFactory.getLog(PoolFactoryGate.class);

    /**
     * Acquire a CollectorPool and TagCollector based on the given reader and request.
     * </p><p>
     * Important: The TagCollector _must_ be released after usage with the code
     * {@code collectorPool.release(query, tagCollector)}, where query can be null.
     * Failure to do so will result in a blocked CollectorPool.
     * @param factory used for generating the CollectorPool.
     * @param reader  the current top level reader for the index.
     * @param key   the facet or index lookup request.
     * @param request the query to use for finding filled TagCollectors. This can be null.
     * @param caller  human readable designation of the calling code (normally "facet" or "index lookup").
     * @return a CollectorPool and a TagCollector ready for use.
     * @throws IOException if the CollectorPool could not be constructed.
     */
    public static synchronized SimplePair<CollectorPool, TagCollector> acquire(
            CollectorPoolFactory factory, IndexReader reader, String key, FacetRequest request, String caller)
            throws IOException {
        CollectorPool collectorPool;
        try {
            boolean hasPool = factory.hasPool(reader, request);
            if (!hasPool) {
                log.info("The CollectorPoolFactory has no structures for the given request from " + caller + ". "
                         + "A new structure will be generated, which can take several minutes. The request was "
                         + request.getBuildKey());
            }
            long buildTime = -System.currentTimeMillis();
            collectorPool = factory.acquire(reader, request);
            buildTime += System.currentTimeMillis();
            if (!hasPool) {
                log.info("The PoolFactory was successfully updated in " + buildTime/1000 + " seconds. "
                         + "Total allocation is " + factory.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to acquire a CollectorPool for " + request, e);
        }

        TagCollector tagCollector;
        try {
            CollectorPool.AVAILABILITY availability = collectorPool.getAvailability(key);
            switch (availability) {
                case hasFresh:
                    log.debug("Acquiring fresh tagCollector for '" + key + "'");
                    break;
                case hasFilled:
                    log.debug("Acquiring filled tagCollector for '" + key + "'");
                    break;
                case mustWait:
                    log.debug("All allowed TagCollectors are active for key '" + key + "' for " + caller);
                    break;
                case mustCreateNew:
                    log.info("A new TagCollector will be created for key '" + key + "' for " + caller
                             + " from " + collectorPool);
                    break;
                case mightCreateNew:
                    log.info("A new TagCollector might be created for key '" + key + "' for " + caller);
                    break;
                default:
                    log.warn("Unknown availability state: " + availability);
            }
            tagCollector = collectorPool.acquire(key);
            if (tagCollector.isNewborn()) {
                log.info("Allocated " + tagCollector + " (availability was " + availability + "). "
                         + "CollectorPoolFactory memory allocation is now at least " + factory.getMem()/1048576 + "MB");
                log.debug("CollectorPoolFactory structure allocation is " + factory.toString());
            }
            return new SimplePair<CollectorPool, TagCollector>(collectorPool, tagCollector);
        } catch (OutOfMemoryError e) {
            Writer writer = new StringWriter(1000);
            PrintWriter pw = new PrintWriter(writer);
            e.printStackTrace(pw);
            pw.flush();
            throw new OutOfMemoryError(
                "Encountered OOM when acquiring TagCollector for '" + key + "' with full request " + request
                + " from pool " + collectorPool + " with full factory " + factory + "\nCaused by: "
                + writer.toString());
        }

    }

    private static AtomicLong releaseCounter = new AtomicLong(0);
    /**
     * Release a TagCollector after use.
     * @param pool      the pool for the collector.
     * @param collector a used collector.
     * @param key       the key identifying the collector content. null is valid.
     */
    public static void release(CollectorPool pool, TagCollector collector, String key) {
        boolean freed = pool.release(key, collector);
        if (freed) {
            long r = releaseCounter.incrementAndGet();
            if (log.isDebugEnabled()) {
                log.debug("The release of a TagCollector with size " + collector.getMemoryUsage()/1048576
                          + "MB for query key '" + key + "' resulted in the collector being freed. "
                          + "Consider increasing the limits for the CollectorPool " + pool.getKey());
            }
            if (r % 100 == 0) {
                log.info(r + " TagCollectors has been freed. Freeing TagCollectors means more GC activity on the heap. "
                         + "Consider increasing the sizes of the CollectorPools to avoid this. "
                         + "The pool for the last freed TagCollector was " + pool.getKey());
            }
        }
    }
}
