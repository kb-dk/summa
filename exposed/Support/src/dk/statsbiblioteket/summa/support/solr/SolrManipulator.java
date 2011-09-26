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

import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.index.IndexManipulator;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;

/**
 * Received Records containing Solr Documents intended for indexing in Solr
 * and delivers them to an external Solr using HTTP.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrManipulator implements IndexManipulator {
    private static Log log = LogFactory.getLog(SolrManipulator.class);

    private boolean orderChanged = false;

    @Override
    public synchronized void open(File indexRoot) throws IOException {
        log.info("Open(" + indexRoot + ") is ignored as this IndexManipulator "
                 + "uses external indexing");
    }

    @Override
    public synchronized void clear() throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean update(Payload payload) throws IOException {
        if (payload.getRecord().isDeleted()) {
            orderChanged = true;
        }
        return false;
    }

    @Override
    public synchronized void commit() throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
        orderChanged  = false;
    }

    @Override
    public synchronized void consolidate() throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public synchronized void close() throws IOException {
        log.info("Closing down SolrManipulator");
    }

    @Override
    public void orderChangedSinceLastCommit() throws IOException {
        log.debug("orderChangedsinceLastCommit() called. No effect for this "
                  + "index manipulator");
    }

    @Override
    public boolean isOrderChangedSinceLastCommit() throws IOException {
        return orderChanged;
    }
}
