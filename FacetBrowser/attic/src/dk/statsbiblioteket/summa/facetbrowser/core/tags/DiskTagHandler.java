/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
 * CVS:  $Id$
 */
package dk.statsbiblioteket.summa.facetbrowser.core.tags;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.DiskTagHandlerFacet;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;


/**
 * A TagHandler that fatches the Strings from disk upon use. No caching are
 * done, so the reliance on disk-cache is high.
 * @deprecated use TagHandlerImpl and TagHandlerFactory instead.
 */
public class DiskTagHandler implements TagHandler {
    private static Logger log = Logger.getLogger(DiskTagHandler.class);

//    private ArrayList<String> facetNames;
    private DiskTagHandlerFacet[] facets;
    private StructureDescription structure;

    /**
     * Create a TagHandler, based on previously stored content.
     * Important: By using this constructor, it becomes impossible to update
     *            the TagHandler content further.
     * @param folder       the location of the content.
     * @param structure    the general structure definition for the facet
     *                     browser.
     * @throws IOException in case of retrieval problems.
     */
    public DiskTagHandler(File folder, StructureDescription structure)
            throws IOException {
        init(structure);
        load(folder);
    }

    public void load(File folder) throws IOException {
        log.debug("Loading tags");
        facets = new DiskTagHandlerFacet[structure.getFacetNames().size()];
        int counter = 0;
        for (String facetName: structure.getFacetNames()) {
            facets[counter++] = new DiskTagHandlerFacet(folder, facetName);
        }
        log.debug("Tag-loading finished without errors. Maximum tag-count " +
                  "for any facet was " + getMaxTagCount());
    }

    private void init(StructureDescription structure) {
        this.structure = structure;
    }

    public int getTagID(int facetID, String tagName) {
        throw new RuntimeException("getTagID not supported by DiskTagHandler!");
    }

    public String getTagName(int facetID, int tagID) {
        return facets[facetID].getTagName(tagID);
    }

    public int getFacetID(String facetName) {
        return structure.getFacetID(facetName);
    }

    public int getFacetSize(int facetID) {
        return facets[facetID].size();
    }

    public int getMaxTagCount() {
        int max = 0;
        for (DiskTagHandlerFacet facet : facets) {
            max = Math.max(max, facet.size());
        }
        return max;
    }

    public int getTagCount(String facetName) {
        return getFacetSize(structure.getFacetID(facetName));
    }

    public int getTagCount() {
        int sum = 0;
        for (DiskTagHandlerFacet facet: facets) {
            sum += facet.size();
        }
        return sum;
    }

    public List<String> getFacetNames() {
        return structure.getFacetNames();
    }

    public List<Facet> getFacets() {
        throw new UnsupportedOperationException("This class is deprecated");
    }

    public void store(File folder) throws IOException {
        throw new RuntimeException("Store not supported for DiskTagHandler");
    }

    public void close() {
        for (DiskTagHandlerFacet facet: facets) {
            try {
                facet.close();
            } catch (IOException e) {
                log.error("Could not close a facet");
            }
        }
    }

    public String getStats() {
        StringWriter sw = new StringWriter(1000);
        for (int i = 0 ; i < structure.getFacetNames().size() ; i++) {
            sw.append(structure.getFacetName(i)).append(" (").
                    append(String.valueOf(facets[i].size())).append(")\n");
        }
        return sw.toString();
    }

    public void dirtyAddTag(int facetID, String tagName) {
        throw new UnsupportedOperationException("This is not implemented and "
                                                + "never will be, as this class"
                                                + " is deprecated");
    }

    public void cleanup() {
        throw new UnsupportedOperationException("This is not implemented and "
                                                + "never will be, as this class"
                                                + " is deprecated");
    }

    public int addTag(int facetID, String tagName) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void removeTag(int facetID, int tagID) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void clearTags() {
        throw new UnsupportedOperationException("This is not implemented and " 
                                                + "never will be, as this class"
                                                + " is deprecated");
    }
}


