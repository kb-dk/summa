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
import java.util.Set;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.log4j.Logger;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;
import dk.statsbiblioteket.summa.facetbrowser.util.pool.SortedPool;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.index.IndexConnector;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexField;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;


/**
 * A simple factory for TagHandlers. The purpose is to make it possible to
 * select TagHandlers based on properties.
 */
// TODO: Remove Lucene-specific code
// TODO: Delay load until open
// TODO: Handle missing Facets
@QAInfo(state=QAInfo.State.IN_DEVELOPMENT)
public class TagHandlerFactory {
    private static Logger log = Logger.getLogger(TagHandlerFactory.class);

    /**
     * The class of the TagHandler to use.
     * <p><p>
     * Optional. Default is {@link TagHandlerImpl}.
     */
    public static final String TAG_HANDLER = "summa.facet.tag-handler";

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
     * @deprecated use {@link #getTagHandler}.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public static TagHandler getTagHandler(File folder,
                                           StructureDescription structure)
            throws IOException {
        throw new UnsupportedOperationException("Deprecated. Use getTagHandler"
                                                + "(Configuration, "
                                                + "IndexConnector) instead");
    }

    /**
     * Construct and potentially fill a TagHandler.
     * @param configuration setup for the TagHandler.
     * @param descriptor    a description of the index used for group-expansion.
     *                      If the descriptor is null, group-expansion is
     *                      disabled.
     * @return a TagHandler ready for use.
     * @throws IOException if an I/O error occured.
     */
    public static TagHandler getTagHandler(Configuration configuration,
                                           Structure structure,
                                           IndexDescriptor descriptor)
                                                            throws IOException {
        if (descriptor == null) {
            log.warn("No IndexDescriptor specified for getTagHandler. "
                     + "Group-expansion is disabled");
        }
        boolean forceRebuild = configuration.getBoolean(FORCE_REBUILD);
/*        boolean updateAfterLoad =
                configuration.getBoolean(UPDATE_AFTER_LOAD);
        boolean storeAfterChange =
                configuration.getBoolean(STORE_AFTER_CHANGE);*/
        String tagHandlerValue = configuration.getString(TAG_HANDLER);
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

        Facet[] facets = new Facet[structure.getFacetNames().size()];
        int position = 0;
        // TODO: Handle missing persistent data by signalling need for rebuild
        for (Map.Entry<String, FacetStructure> entry:
                structure.getFacets().entrySet()) {
            facets[position++] =
                    new Facet(dataLocation, entry.getValue(), forceRebuild,
                              tagHandler == TAGHANDLERS.MemoryTagHandler);
        }
        TagHandlerImpl handler = new TagHandlerImpl(structure, facets);
        if (forceRebuild) {
            throw new UnsupportedOperationException("Not supported yet");
//            fill(handler, connector);
        }
        return handler;
    }

}
