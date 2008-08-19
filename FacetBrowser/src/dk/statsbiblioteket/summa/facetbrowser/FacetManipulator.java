/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.facetbrowser;

import java.io.IOException;
import java.io.File;

import dk.statsbiblioteket.summa.index.IndexManipulator;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

/**
 * To be used under the Summa Index-framework. This manipulator maintains a
 * persistent view on Facet-information coupled to a document index (such as
 * Lucene). The manipulator allows for iterative updates and is capable of
 * doing a complete rebuild if needed.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class FacetManipulator implements IndexManipulator {
    private static Logger log = Logger.getLogger(FacetManipulator.class);

    /**
     * The folder containing all persistent facet-information.
     */
    public static final String FACET_FOLDER = "facet";

    /**
     * If true, both the mapping from docID=>Tag and the Tags themselves are
     * cleared when a clear is called. If false, only the mapping is cleared.
     * Clearing Tags means that non-used Tags are removed at the cost of
     * increased rebuild-time.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_CLEAR_TAGS_ON_CLEAR =
            "summa.facet.clear-tags-on-clear";
    public static final boolean DEFAULT_CLEAR_TAGS_ON_CLEAR = true;

    /**
     * If true, both the mapping from docID=>Tag and the Tags themselves are
     * cleared when a consolidate is called. If false, only the mapping is
     * cleared. Clearing Tags means that non-used Tags are removed at the cost
     * of increased consolidate-time.
     */
    public static final String CONF_CLEAR_TAGS_ON_CONSOLIDATE =
            "summa.facet.clear-tags-on-consolidate";
    public static final boolean DEFAULT_CLEAR_TAGS_ON_CONSOLIDATE = false;

    protected boolean clearTagsOnClear =
            DEFAULT_CLEAR_TAGS_ON_CLEAR;
    protected boolean clearTagsOnConsolidate =
            DEFAULT_CLEAR_TAGS_ON_CONSOLIDATE;

    private TagHandler tagHandler;
    private CoreMap core;

    private File indexRoot = null;

    public FacetManipulator(Configuration conf) {
        log.info("Constructing FacetManipulator");
        clearTagsOnClear = conf.getBoolean(CONF_CLEAR_TAGS_ON_CLEAR,
                                           DEFAULT_CLEAR_TAGS_ON_CLEAR);
        clearTagsOnConsolidate =
                conf.getBoolean(CONF_CLEAR_TAGS_ON_CONSOLIDATE,
                                DEFAULT_CLEAR_TAGS_ON_CONSOLIDATE);
    }

    public void clear() throws IOException {
        tagHandler.clearTags();
        core.clear();
    }

    public void close() throws IOException {
        tagHandler.close();
        core.clear(); // TODO: Consider adding a close to CoreMap
        //noinspection AssignmentToNull
        indexRoot = null;
    }

    public void commit() throws IOException {
        log.trace("Committing Tags to '" + getLocation() + "'");
        tagHandler.store(getLocation());
        log.trace("Committing CoreMap to '" + getLocation() + "'");
        core.store(getLocation());
        log.trace("Commit finished");
    }

    public void open(File indexRoot) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.debug("open(" + indexRoot + ") called");
        this.indexRoot = indexRoot;
        if (indexRoot == null) {
            log.debug("open(null) called, which is equivalent to close()");
            close();
            return;
        }
        tagHandler.load(getLocation());
        core.load(getLocation());
        //noinspection DuplicateStringLiteralInspection
        log.trace("open(" + indexRoot + ") finished");
    }

    public boolean update(Payload payload) throws IOException {
        return false;
    }

    private File getLocation() {
        if (indexRoot == null) {
            throw new IllegalStateException("indexRoot not specified. "
                                            + "open(File) must be called");
        }
        return new File(indexRoot, FACET_FOLDER);
    }
}
