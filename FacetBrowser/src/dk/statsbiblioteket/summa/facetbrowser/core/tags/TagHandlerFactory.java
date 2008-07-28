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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.log4j.Logger;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;
import dk.statsbiblioteket.summa.facetbrowser.util.pool.SortedPool;
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
     * @param connector     a connection to a Lucene index.
     * @return a TagHandler ready for use.
     * @throws IOException if an I/O error occured.
     */
    public static TagHandler getTagHandler(Configuration configuration,
                                           IndexDescriptor descriptor,
                                           IndexConnector connector)
                                                            throws IOException {
        if (descriptor == null) {
            log.warn("No IndexDescriptor specified for getTagHandler. "
                     + "Group-expansion is disabled");
        }
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

        Facet[] facets = new Facet[structure.getFacetNames().size()];
        int position = 0;
        // TODO: Handle missing persistent data by signalling need for rebuild
        for (String facetName: structure.getFacetNames()) {
            String[] fieldNames;
            if (descriptor != null && descriptor.getGroup(facetName) != null) {
                // FIXME: Tweak this so the warning dissapears
                //noinspection unchecked
                Set<IndexField> fields =
                        descriptor.getGroup(facetName).getFields();
                fieldNames = new String[fields.size()];
                int counter = 0;
                for (IndexField field: fields) {
                    fieldNames[counter++] = field.getName();
                }
            } else {
                fieldNames = new String[]{facetName};
            }
            facets[position++] =
                    new Facet(dataLocation, facetName, null, fieldNames,
                              forceRebuild,
                              tagHandler == TAGHANDLERS.MemoryTagHandler);
        }
        TagHandlerImpl handler = new TagHandlerImpl(structure, facets);
        if (forceRebuild) {
            fill(handler, connector);
        }
        return handler;
    }

    /**
     * Clear and fill the given TagHandler from a Lucene index. This takes
     * a fair amount of time and resets the state of all underlying Facets.
     * @param tagHandler the structure of tags to fill.
     * @param connector a connection to a Lucene Index.
     * @throws IOException if an I/O error happened.
     */
    public static void fill(TagHandler tagHandler,
                            IndexConnector connector) throws IOException {
        // TODO: Implement fill
        log.debug("Filling tag handler from index");
        Profiler profiler = new Profiler();
        tagHandler.clearTags();
        IndexReader ir = connector.getReader();
        long termCount = 0;
        int counter = 0;
        for (Facet facet: tagHandler.getFacets()) {
            String facetName = facet.getName();
            log.debug("Filling " + facetName
                      + " (" + ++counter + "/"
                      + tagHandler.getFacetNames().size() + ")");
            for (String fieldName: facet.getFields()) {
                Term searchTerm = new Term(fieldName, "");
                TermEnum terms = ir.terms(searchTerm);
                while (true) {
                    Term term = terms.term();
                    if (term == null) {
                        break;
                    }
                    if (!term.field().equals(fieldName)) {
                        break;
                    }
                    String shortTerm = term.text().replaceAll("\n", " ");
                    if (log.isTraceEnabled()) {
                        log.trace("Adding tag '" + shortTerm
                                  + "' from field '" + fieldName
                                  + "' to facet '" + facetName + "'");
                    }
                    tagHandler.dirtyAddTag(counter-1, shortTerm);
                    termCount++;
                    if (!terms.next()) {
                        break;
                    }
                }
            }
            log.debug("Facet \"" + facetName + "\" filled with " +
                      tagHandler.getTagCount(facetName) + " tags");
        }
        log.trace("Cleaning up tag handler");
        tagHandler.cleanup();
        log.info(String.format(
                "Finished filling tag handler with %d tags in %d facets from "
                + "the index with %d documents in %s",
                termCount, tagHandler.getFacetNames().size(), ir.numDocs(),
                 profiler.getSpendTime()));
    }
}
