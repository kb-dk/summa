/* $Id: BuilderImpl.java,v 1.4 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:24 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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



