/* $Id: MemoryTagHandler.java,v 1.5 2007/10/04 13:28:18 te Exp $
 * $Revision: 1.5 $
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
 * CVS:  $Id: MemoryTagHandler.java,v 1.5 2007/10/04 13:28:18 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core.tags;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.StringWriter;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.log4j.Logger;
import dk.statsbiblioteket.summa.facetbrowser.util.ClusterCommon;
import dk.statsbiblioteket.summa.facetbrowser.util.Pair;
import dk.statsbiblioteket.summa.facetbrowser.util.SortedHash;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;

/**
 * @deprecated with no current replacement.
 */
public class MemoryTagHandler implements TagHandler {
    private static Logger log = Logger.getLogger(MemoryTagHandler.class);

    private static final String FACET_INFO_FILENAME = "facets.dat";
    private static final String TAG_FILENAME_PRE =    "facet_";
    private static final String TAG_FILENAME_POST =   ".dat";

    private ArrayList<SortedHash<String>> facets;
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
    public MemoryTagHandler(File folder, StructureDescription structure)
            throws IOException {
        init(structure);
        load(folder);
    }

    /**
     * Create a TagHandler and populate it from a Fedora index. This will
     * take an amount of time directly proportional to the size of the index.
     * @param ir           a Fedora index reader.
     * @param structure    the general structure definition for the facet
     *                     browser.
     * @throws IOException in case of problems with the index.
     */
    public MemoryTagHandler(IndexReader ir, StructureDescription structure)
            throws IOException {
        init(structure);
        fill(ir);
    }

    /**
     * Construct a MemoryTagHandler with the given Facets and the given Tags.
     * @param structure    the general structure definition for the facet
     *                     browser.
     * @param tagNames   the Tags to put in the Facets.
     */
    public MemoryTagHandler(StructureDescription structure,
                               String[][] tagNames) {
        this.structure = structure;
        facets = new ArrayList<SortedHash<String>>(
                structure.getFacetNames().size());
        for (String[] tags: tagNames) {
            SortedHash<String> store = new SortedHash<String>();
            for (String tag: tags) {
                store.add(tag);
            }
            facets.add(store);
        }
    }

    private void init(StructureDescription structure) {
        facets = new ArrayList<SortedHash<String>>(
                                              structure.getFacetNames().size());
        this.structure = structure;
    }

    public int getTagID(int facetID, String tagName) {
        return facets.get(facetID).get(tagName);
    }

    public String getTagName(int facetID, int tagID) {
        return facets.get(facetID).get(tagID);
    }

    public int getFacetID(String facetName) {
        return structure.getFacetID(facetName);
    }

    public int getFacetSize(int facetID) {
        return facets.get(facetID).size();
    }

    public int getMaxTagCount() {
        int max = 0;
        for (SortedHash<String> facet: facets) {
            max = Math.max(max, facet.size());
        }
        return max;
    }

    public int getTagCount(String facetName) {
        return facets.get(structure.getFacetID(facetName)).size();
    }

    public int getTagCount() {
        int sum = 0;
        for (SortedHash<String> facet: facets) {
            sum += facet.size();
        }
        return sum;
    }

    public List<String> getFacetNames() {
        return structure.getFacetNames();
    }

    /**
     * Fill the internal representation with tagnames from the Lucene index.
     * @param ir           a Lucene index reader.
     * @throws IOException in case of index problems.
     */
    protected void fill(IndexReader ir) throws IOException {
        int counter = 0;
        for (String facet: structure.getFacetNames()) {
            log.info("  Filling " + facet +
                     " (" + ++counter + "/"
                     + structure.getFacetNames().size() + ")");
            Term searchTerm = new Term(facet, "");
            TermEnum terms = ir.terms(searchTerm);
            SortedHash<String> result = new SortedHash<String>(10000);
            while (true) {
                Term term = terms.term();
                if (term == null) {
                    break;
                }
                if (!term.field().equals(facet)) {
                    break;
                }
                String shortTerm = term.text().replaceAll("\n", "");
                result.add(shortTerm);
                if (!terms.next()) {
                    break;
                }
            }
            log.debug("  Facet " + facet + " filled with " +
                      result.size() + " tags");
            facets.add(result);
        }
    }

    public void store(File folder) throws IOException {
        log.info("Storing facet info");
        if (!folder.exists()) {
            log.debug("Folder " + folder + " does not exist. Creating new one");
            folder.mkdirs();
        }

        PrintStream facetPrint =
                ClusterCommon.stringPrinter(folder, FACET_INFO_FILENAME);
        facetPrint.println("Facet-name\tTag-count");
        for (int facetID = 0 ;
             facetID < structure.getFacetNames().size() ;
             facetID++) {
            String facetName = structure.getFacetName(facetID);
            facetPrint.print(facetName + "\t");
            facetPrint.println(Integer.toString(getFacetSize(facetID)));
        }
        facetPrint.close();

        log.info("Storing tag names");
        // TODO: Handle newline and tab in terms
        for (int facetID = 0 ;
             facetID < structure.getFacetNames().size() ;
             facetID ++) {
            String facetName = structure.getFacetName(facetID);
            log.info("  " + facetName);
            PrintStream tagPrint = ClusterCommon.stringPrinter(folder,
                                                 TAG_FILENAME_PRE +
                                                 facetName +
                                                 TAG_FILENAME_POST);
            tagPrint.println("Tag-position\tTag-name");
            SortedHash<String> expanded = facets.get(facetID);
            int tagPos = 0;
            for (String tagName: expanded.getSorted()) {
                tagPrint.println(tagPos++ + "\t" + tagName);
            }
            tagPrint.close();
        }
    }

    /** Load the structure from disk.
    * @param folder       the folder to load the content from.
    * @throws IOException in case of disk-related problems during store.
    */
    public void load(File folder) throws IOException {
        log.info("Loading facet info from " + FACET_INFO_FILENAME);
        BufferedReader facetLoader =
                ClusterCommon.stringLoader(folder, FACET_INFO_FILENAME);
        log.debug("  Got header " + facetLoader.readLine());
        String facetLine = facetLoader.readLine();
        ArrayList<Pair<String, Integer>> facetPairs =
                new ArrayList<Pair<String, Integer>>(100);
        while (facetLine != null) {
            // Facet-name   Tag-count
            String[] tokens = facetLine.split("\t");
            if (tokens.length == 2) {
                facetPairs.add(new Pair<String, Integer>(
                        tokens[0], Integer.parseInt(tokens[1])));
            } else {
                if (!"".equals(facetLine)) {
                    log.error("Unexpected content in " + FACET_INFO_FILENAME);
                }
            }
            facetLine = facetLoader.readLine();
        }
        facetLoader.close();

        /* Verify that the wanted facets are in the datafile and create a
           mapping from the stored facets index to the wanted facets index*/

        Integer[] storedToWanted = new Integer[facetPairs.size()];

        int added = 0;
        int loadCounter = 0;
        for (Pair<String, Integer> loadedFacet: facetPairs) {
            int wantCounter = 0;
            boolean found = false;
            for (String wantedFacet: structure.getFacetNames()) {
                wantCounter++;
                if (wantedFacet.equals(loadedFacet.getKey())) {
                    storedToWanted[loadCounter] = wantCounter;
                    added++;
                    found = true;
                    break;
                }
            }
            if (!found) {
                log.warn("The loaded facet " + loadedFacet +
                         " was not specified in the settings");
            }
            loadCounter++;
        }
        if (!(added == structure.getFacetNames().size())) {
            String error = "Could not locate the wanted facets in " +
                           "the stored cluster map. Got " + added +
                           " but expected " + structure.getFacetNames().size();
            log.error(error);
            throw new IOException(error);
        }

        // Create and fill the expandedFacets
//        facets = new ArrayList<SortedHash<String>>(facetNames.size());

        log.info("Loading tag names");
        // TODO: Handle newline and tab in terms
        for (int facetID = 0 ;
             facetID < structure.getFacetNames().size() ;
             facetID ++) {
            // Tag-position Tag-name (tag-positions are ignored)
            String facetName = structure.getFacetNames().get(facetID);
            SortedHash<String> expandedFacet = new SortedHash<String>();
            String tagFile = TAG_FILENAME_PRE +facetName + TAG_FILENAME_POST;
            BufferedReader tagLoader =
                    ClusterCommon.stringLoader(folder, tagFile);
            log.debug("  " + tagFile);
            tagLoader.readLine(); // Header
            String tagLine = tagLoader.readLine();
            int tagCount = 0;
            while (tagLine != null) {
                String[] tokens = tagLine.split("\t", 2);
                if (tokens.length == 2) {
                    expandedFacet.add(tokens[1]);
                    if (++tagCount % 50000 == 0 ) {
                        log.trace("    " + tagCount + " - " + tokens[1]);
                    }
                } else {
                    if (!"".equals(tagLine)) {
                        log.error("Unexpected content \"" + tagLine +
                                  "\" in " + tagFile);
                    }
                }
                tagLine = tagLoader.readLine();
            }
            tagLoader.close();
            expandedFacet.createVectorHack();
            facets.add(facetID, expandedFacet);
//            log.debug("  (" + expandedFacet.size() + " tags)");
        }
    }

    public void close() {
        facets.clear();
    }

    public String getStats() {
        StringWriter sw = new StringWriter(1000);
        for (int i = 0 ; i < structure.getFacetNames().size() ; i++) {
            sw.append(structure.getFacetNames().get(i)).append(" (").
                    append(String.valueOf(facets.get(i).size())).append(")\n");
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
