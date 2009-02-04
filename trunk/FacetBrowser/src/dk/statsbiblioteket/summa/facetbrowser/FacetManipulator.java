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
import java.rmi.RemoteException;

import dk.statsbiblioteket.summa.index.IndexManipulator;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMapFactory;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandlerFactory;
import dk.statsbiblioteket.summa.facetbrowser.build.Builder;
import dk.statsbiblioteket.summa.facetbrowser.build.BuilderFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

/**
 * To be used under the Summa Index-framework. This manipulator maintains a
 * persistent view on Facet-information coupled to a document index (such as
 * Lucene). The manipulator allows for iterative updates and is capable of
 * doing a complete rebuild if needed.
 * </p><p>
 * This class is abstract and a document searcher specific handling of updates
 * needs to be implemented. The base case is Lucene support, where the
 * implementation extracts the contents of fields and rebuilds based on a
 * complete index.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetManipulator implements IndexManipulator {
    private static Logger log = Logger.getLogger(FacetManipulator.class);

    /**
     * If true, both the mapping from docID=>Tag and the Tags themselves are
     * cleared when a remove is called. If false, only the mapping is cleared.
     * Clearing Tags means that non-used Tags are removed at the cost of
     * increased rebuild-time.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_CLEAR_TAGS_ON_CLEAR =
            "summa.facet.cleartagsonclear";
    public static final boolean DEFAULT_CLEAR_TAGS_ON_CLEAR = true;

    /**
     * If true, both the mapping from docID=>Tag and the Tags themselves are
     * cleared when a consolidate is called. If false, only the mapping is
     * cleared. Clearing Tags means that non-used Tags are removed at the cost
     * of increased consolidate-time.
     */
    public static final String CONF_CLEAR_TAGS_ON_CONSOLIDATE =
            "summa.facet.cleartagsonconsolidate";
    public static final boolean DEFAULT_CLEAR_TAGS_ON_CONSOLIDATE = false;

    /**
     * If true, the facet structure isn't updated when {@link #update} is
     * called. A side-effect is that the facet structure is generated upon
     * commit (and consolidate, but that is always the case).
     * </p><p>
     * As iterative updates of the facet structure is O(n*m) and re-build is
     * O(n*log(m)), setting this to true is best for large batch-updates.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_SKIP_FACET_ON_UPDATE =
            "summa.facet.skipfacetonupdate";
    public static final boolean DEFAULT_SKIP_FACET_ON_UPDATE = false;

    protected boolean clearTagsOnClear = DEFAULT_CLEAR_TAGS_ON_CLEAR;
    protected boolean clearTagsOnConsolidate =
            DEFAULT_CLEAR_TAGS_ON_CONSOLIDATE;
    protected boolean skipFacetOnUpdate = false;

    /**
     * The builder is responsible for all manipulations of the structure for
     * facet/tags. The FacetManipulator is just a wrapper.
     */
    protected Builder builder;

    public FacetManipulator(Configuration conf) throws RemoteException {
        log.info("Constructing FacetManipulator");
        clearTagsOnClear = conf.getBoolean(CONF_CLEAR_TAGS_ON_CLEAR,
                                           DEFAULT_CLEAR_TAGS_ON_CLEAR);
        clearTagsOnConsolidate =
                conf.getBoolean(CONF_CLEAR_TAGS_ON_CONSOLIDATE,
                                DEFAULT_CLEAR_TAGS_ON_CONSOLIDATE);
        Structure structure = new Structure(conf);
        TagHandler tagHandler =
                TagHandlerFactory.getTagHandler(conf, structure, false);
        CoreMap coreMap = CoreMapFactory.getCoreMap(conf, structure);
        builder =
                BuilderFactory.getBuilder(conf, structure, coreMap, tagHandler);
    }

    public void clear() throws IOException {
        builder.clear(!clearTagsOnClear);
    }

    public void close() throws IOException {
        builder.close();
    }

    public void commit() throws IOException {
        if (skipFacetOnUpdate) {
            log.debug("Rebuilding facet structure as "
                      + CONF_SKIP_FACET_ON_UPDATE + " is true");
            builder.build(true);
        }
        builder.store();
    }

    public void consolidate() throws IOException {
        log.debug("Consolidating Facets");
        builder.build(!clearTagsOnConsolidate);
        builder.store();
    }

    // TODO: Auto-rebuild on missing facets
    public void open(File indexRoot) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.debug("open(" + indexRoot + ") called");
        if (indexRoot == null) {
            log.debug("open(null) called, which is equivalent to close()");
            close();
            return;
        }
        builder.open(indexRoot);
        //noinspection DuplicateStringLiteralInspection
        log.trace("open(" + indexRoot + ") finished");
    }

    public boolean update(Payload payload) throws IOException {
        if (skipFacetOnUpdate) {
            if (log.isTraceEnabled()) {
                log.trace("Skipping facet update as "
                          + CONF_SKIP_FACET_ON_UPDATE
                          + " is true. Payload skipped is " + payload);
            }
            return false;
        }
        return builder.update(payload);
    }

}



