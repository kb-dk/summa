/* $Id: Builder.java,v 1.5 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.5 $
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
package dk.statsbiblioteket.summa.facetbrowser.build;

import org.apache.lucene.document.Document;

import java.io.IOException;
import java.util.List;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Builds or updates the structures needed for facet browsing.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface Builder {
    /**
     * Store the internal representation to disk.
     * @param directory the location of the data.
     * @throws IOException if the internal representation could not be stored.
     */
    public void save(String directory) throws IOException;

    /**
     * Discard any previous mapping and optionally tags and perform a complete
     * build of the internal structure.
     * @param keepTags if true, any existing tags are re-used.
     */
    public void build(boolean keepTags);

    /**
     * Adds the given documents to the internal structure, assuming that the
     * ID's for the documents follows without gaps right after the last known
     * document in the structure.<br />
     * @param documents facets and tags are extracted from these and added to
     *                  the internal structure.
     */
    // TODO: Replace with Record
    public void addDocuments(List<Document> documents);

    /**
     * Refresh the internal representation from the Lucene index.
     * Note that iterative updates does not guarantee completely correct
     * representation, as changed documents are not handled.
     * Note that onlyMap updates will gradually make the facet browsing less
     * and less complete, as new tags found in the new documents are not
     * reflected in the representation. It is advisable to perform a complete
     * build once in a while, if onlyMap is used as the standard update. 
     * @param iterate suggest that the build is iterative, which means that
     *                new documents are parsed and reflected in the internal
     *                representation.
     *                Note: Changed documents are not handled and the order
     *                of the previously parsed documents are assumed to be
     *                untouched.
     *                Iterative updates are not necessarily supported. If they
     *                are not supported,  a complete build should be performed
     *                instead.
     *  @param onlyMap only update the mapping between documents and tags.
     *                 If a record contains tags that are not part of the
     *                 threadpool, these tags are ignored.
     */
//    public void refresh(boolean iterate, boolean onlyMap);

    /**
     * Performs a complete rebuild of the mapping from documents to tags.
     * During the rebuild, the FacetBrowser will still be responsive, but it
     * will return result based on its state befor the rebuildMap-call.
     */
    public void rebuildMap();
}
