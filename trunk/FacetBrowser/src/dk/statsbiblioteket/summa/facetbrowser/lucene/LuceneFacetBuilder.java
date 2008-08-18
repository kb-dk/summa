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
package dk.statsbiblioteket.summa.facetbrowser.lucene;

import java.io.IOException;
import java.io.File;

import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.Facet;
import dk.statsbiblioteket.summa.facetbrowser.build.Builder;
import dk.statsbiblioteket.summa.facetbrowser.build.BuilderImpl;
import dk.statsbiblioteket.summa.common.lucene.index.IndexConnector;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.Profiler;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * te forgot to document this class.
 */
public class LuceneFacetBuilder extends BuilderImpl {
    private static Log log = LogFactory.getLog(LuceneFacetBuilder.class);
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

    public void updateRecord(Record record) {
    }

    public void build(boolean keepTags) throws IOException {
    }

    public void add(int docID, String facet, String tag) throws IOException {
    }

    public void clear(int docID, boolean shift) throws IOException {
    }

    public void clear(boolean keepTags) throws IOException {
    }

    public void save(File directory) throws IOException {
    }
}
