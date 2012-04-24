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
package dk.statsbiblioteket.summa.support.solr;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.index.IndexManipulator;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;

/**
 * Receives Records containing Solr Documents intended for indexing in Solr and delivers them to an external Solr using
 * HTTP.
 * </p><p>
 * The first version is expected to flush received documents one at a time, which is inefficient but simple.
 * For later versions a batching model should be considered. See {@link RecordWriter} and {@link PayloadQueue} for
 * inspiration as they maintain a byte-size-controlled queue for batching. Streaming should also be examined, but this
 * is vulnerable to errors and has little gain over batching as documents are not visible in the searcher until
 * {@link #commit()} has been called.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrManipulator implements IndexManipulator {
    private static Log log = LogFactory.getLog(SolrManipulator.class);

    private boolean orderChanged = false;

    public SolrManipulator(Configuration conf) {
        // Configuration should contain host, port and other setup parameters for the remote Solr
    }

    @Override
    public synchronized void open(File indexRoot) throws IOException {
        log.info("Open(" + indexRoot + ") is ignored as this IndexManipulator uses external indexing");
    }

    @Override
    public synchronized void clear() throws IOException {
        throw new UnsupportedOperationException("Clear must be implemented for SolrManipulator");
    }

    @Override
    public boolean update(Payload payload) throws IOException {
        if (payload.getRecord().isDeleted()) {
            orderChanged = true;
        }
        throw new UnsupportedOperationException("Update needs to be implemented");
    }

    @Override
    public synchronized void commit() throws IOException {
        orderChanged  = false;
        throw new UnsupportedOperationException(
            "Commit is as valid for Solr as it is for Lucene. This must be implemented.");
    }

    @Override
    public synchronized void consolidate() throws IOException {
        log.info("Consolidate called. Currently there is no implementation of this functionality. Note that consolidate"
                 + " is becoming increasingly less important as Lucene/Solr develops");
    }

    @Override
    public synchronized void close() throws IOException {
        log.info("Closing down SolrManipulator");
        throw new UnsupportedOperationException(
            "Closing down means flushing any caches and ensuring that further calls to update fails");
    }

    @Override
    public void orderChangedSinceLastCommit() throws IOException {
        log.debug("orderChangedSinceLastCommit() called. No effect for this index manipulator");
    }

    @Override
    public boolean isOrderChangedSinceLastCommit() throws IOException {
        return orderChanged;
    }
}
