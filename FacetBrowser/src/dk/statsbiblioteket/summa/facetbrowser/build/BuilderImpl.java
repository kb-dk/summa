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
/**
 * Created: te 2007-08-28 14:02:35
 * CVS:     $Id: BuilderImpl.java,v 1.4 2007/10/05 10:20:24 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.build;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.core.FacetMap;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract class with index-agnostic implementations of {@link Builder}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class BuilderImpl implements Builder {
    private Log log = LogFactory.getLog(BuilderImpl.class);

    protected Structure structure;
    protected CoreMap coreMap;
    protected TagHandler tagHandler;
    protected FacetMap facetMap;

    protected File location;

    @SuppressWarnings({"UnusedDeclaration"})  // We'll probably need it later
    public BuilderImpl(Configuration configuration, Structure structure,
                       CoreMap coreMap, TagHandler tagHandler) {
        this.structure = structure;
        this.tagHandler = tagHandler;
        this.coreMap = coreMap;
        facetMap = new FacetMap(structure, coreMap, tagHandler, false);
    }

    public synchronized void open(File location) throws IOException {
        log.debug(String.format("Open(%s) called", location));
        this.location = location;
        coreMap.open(location, false);
        tagHandler.open(location);
        log.trace("Open finished");
    }

    public synchronized void store() throws IOException {
        log.debug("Store called");
        facetMap.store();
    }

    public synchronized void clear(boolean keepTags) throws IOException {
        log.debug(String.format("clear(%b) called", keepTags));
        coreMap.clear();
        if (!keepTags) {
            tagHandler.clearTags();
        }
    }

    public synchronized void close() throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.debug("close() called");
        coreMap.clear();
        tagHandler.close();
        location = null;
        log.trace("close() finished");
    }

    // TODO: Examine concurrency-problems
    public void add(int docID, String facet, String tag) throws IOException {
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("add(" + docID + ", " + facet + ", " + tag + ") called");
        }
        facetMap.add(docID, facet, tag);
    }

    public synchronized void remove(int docID) throws IOException {
        facetMap.removeDocument(docID);
    }
}




