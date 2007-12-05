/* $Id: TagHandlerFactory.java,v 1.9 2007/10/04 13:28:18 te Exp $
 * $Revision: 1.9 $
 * $Date: 2007/10/04 13:28:18 $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: TagHandlerFactory.java,v 1.9 2007/10/04 13:28:18 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core.tags;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.log4j.Logger;
import dk.statsbiblioteket.summa.facetbrowser.util.pool.SortedPool;
import dk.statsbiblioteket.summa.facetbrowser.util.pool.DiskStringPool;
import dk.statsbiblioteket.summa.facetbrowser.util.pool.MemoryStringPool;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.index.IndexConnector;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;


/**
 * A simple factory for TagHandlers. The purpose is to make it possible to
 * select TagHandlers based on properties.
 */
@QAInfo(state=QAInfo.State.IN_DEVELOPMENT)
public class TagHandlerFactory {
    private static Logger log = Logger.getLogger(TagHandlerFactory.class);

    /**
     * The name of the TagHandler to use. Valid values are
     * DiskTagHandler:   Disk-based handler with low memory usage.
     * MemoryTagHandler: RAM-based handler with low response time.
     * Default: MemoryTagHandler.
     */
    public static final String TAG_HANDLER = "facetbrowser.TagHandler";

    /**
     * @deprecated use another getTagHandler.
     */
    @SuppressWarnings({"UnusedDeclaration", "JavaDoc"})
    public static TagHandler getTagHandler(IndexReader ir,
                                           StructureDescription clusterDescription) {
        throw new IllegalAccessError("The method getTagHandler(IndexReader, "
                                    + "StructureDescription) has been removed");
    }

    public static enum TAGHANDLERS { DiskTagHandler, MemoryTagHandler }

    /**
     * The location of the data for the TagHandlers.
     * Mandatory if {@link #STORE_AFTER_CHANGE} is true. No default.
     */
    public static final String TAG_DATA_LOCATION =
            "facetbrowser.TagDataLocation";

    /**
     * Force a rebuild of the TagHandler, when a TagHandler is requested from
     * the TagHandlerFactory. No stored structures are used.
     * Optional. Default: false.
     */
    public static final String FORCE_REBUILD =
            "facetbrowser.ForceRebuild";
    /**
     * Ensure that the TagHandler is up to date with the index after loading
     * stored data. If no previous data are stored, setting this to true will
     * build new structures.
     * Note: If both FORCE_REBUILD and UPDATE_AFTER_LOAD are false and if there
     *       is no stored structure, an IOException is thrown. If there is a
     *       stored structure that is not up to date, a warning is logged.
     * Optional. Default: true.
     */
    public static final String UPDATE_AFTER_LOAD =
            "facetbrowser.UpdateAfterLoad";
    /**
     * If the TagHandler was changed as part of the construction, store the
     * changes automatically.
     * Optional. Default: true.
     */
    public static final String STORE_AFTER_CHANGE =
            "facetbrowser.StoreAfterChange";

    /**
     *
     * @param folder    the location of the stored data.
     * @param structure description of wanted facets et al.
     * @return a TagHandler, ready for use.
     * @throws IOException if stored data could not be read.
     * @deprecated use {@link #getTagHandler(Configuration, IndexConnector)}.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public static TagHandler getTagHandler(File folder,
                                           StructureDescription structure)
            throws IOException {
        throw new UnsupportedOperationException("Deprecated. Use getTagHandler"
                                                + "(Configuration, "
                                                + "IndexConnector) instead");
    }

    public static TagHandler getTagHandler(Configuration configuration,
                                           IndexConnector connector)
                                                            throws IOException {
        StructureDescription structure =
                new StructureDescription(configuration);
        boolean forceRebuild =
                configuration.getBoolean(FORCE_REBUILD);
/*        boolean updateAfterLoad =
                configuration.getBoolean(UPDATE_AFTER_LOAD);
        boolean storeAfterChange =
                configuration.getBoolean(STORE_AFTER_CHANGE);*/
        String tagHandlerValue =
                configuration.getString(TAG_HANDLER);
        TAGHANDLERS tagHandler = TAGHANDLERS.valueOf(tagHandlerValue);
        if (tagHandler == null) {
            log.warn("No tag handler specified in settings. Defaulting to"
                     + " memory based");
            tagHandler = TAGHANDLERS.MemoryTagHandler;
        }
        String dataLocationValue = configuration.getString(TAG_DATA_LOCATION);
        if (dataLocationValue == null) {
            throw new IllegalArgumentException("The data location property "
                                               + TAG_DATA_LOCATION
                                               + " must be specified");
        }
        File dataLocation = new File(dataLocationValue);
        if (!dataLocation.exists()) {
            if (!dataLocation.mkdirs()) {
                throw new IOException("The data location \"" + dataLocation
                                      + "\" is not a valid path and could "
                                      + "not be created");
            }
        }
        if (!dataLocation.isDirectory())  {
            throw new IOException("The data location \"" + dataLocation
                                   + "\" is not a directory");
        }
        if (!dataLocation.canRead()) {
            throw new IOException("The data location \"" + dataLocation
                                   + "\" cannot be accessed");
        }

        SortedPool<String>[] pools;
        int position = 0;
        switch (tagHandler) {
            case DiskTagHandler:
                pools = new DiskStringPool[structure.getFacetNames().size()];
                for (String facetName: structure.getFacetNames()) {
                    pools[position++] =
                            new DiskStringPool(dataLocation,
                                               TagHandlerImpl.PERSISTENCE_PREFIX
                                               + facetName,
                                               forceRebuild);
                }
                break;
            case MemoryTagHandler:
                pools = new MemoryStringPool[structure.getFacetNames().size()];
                for (String facetName: structure.getFacetNames()) {
                    pools[position++] = new MemoryStringPool();
                    if (!forceRebuild) {
                        pools[position-1].load(dataLocation,
                                               TagHandlerImpl.PERSISTENCE_PREFIX
                                               + facetName);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown tag handler \""
                                                   + tagHandler + "\"");
        }
        TagHandlerImpl handler = new TagHandlerImpl(structure, pools);
        if (forceRebuild) {
            fill(handler, connector);
        }
        return handler;
    }

    public static void fill(TagHandler tagHandler,
                            IndexConnector connector) throws IOException {
        log.debug("Filling tag handler from index");
        Profiler profiler = new Profiler();
        tagHandler.clearTags();
        IndexReader ir = connector.getReader();
        long termCount = 0;
        int counter = 0;
        for (String facet: tagHandler.getFacetNames()) {
            log.debug("Filling " + facet
                      + " (" + ++counter + "/"
                      + tagHandler.getFacetNames().size() + ")");
            Term searchTerm = new Term(facet, "");
            TermEnum terms = ir.terms(searchTerm);
            while (true) {
                Term term = terms.term();
                if (term == null) {
                    break;
                }
                if (!term.field().equals(facet)) {
                    break;
                }
                String shortTerm = term.text().replaceAll("\n", " ");
                if (log.isTraceEnabled()) {
                    log.trace("Adding tag '" + shortTerm
                              + "' to facet '" + facet + "'");
                }
                tagHandler.dirtyAddTag(counter-1, shortTerm);
                termCount++;
                if (!terms.next()) {
                    break;
                }
            }
            log.debug("Facet \"" + facet + "\" filled with " +
                      tagHandler.getTagCount(facet) + " tags");
        }
        log.trace("Cleaning up tag handler");
        tagHandler.cleanup();
        log.info("Finished filling tag handler with " + termCount
                 + " tags in " + tagHandler.getFacetNames().size()
                 + " facets from te index of " + ir.numDocs() 
                 + " documents in " + profiler.getSpendTime());
    }
}
