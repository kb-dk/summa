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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.build.BuilderImpl;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMapBuilder;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.Facet;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.FacetCore;
import dk.statsbiblioteket.util.Profiler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermDocs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * The LuceneFacetBuilder works correctly with non-tokenized fields in the
 * current implementation. This is due to the building of the facet map can be
 * done either from non-indexed Documents or a Lucene Index.
 * Fields can be both stored and indexed. Special data-types, such as numbers,
 * isn't currently supported as anything else than text.
 */
public class LuceneFacetBuilder extends BuilderImpl {
    private static Log log = LogFactory.getLog(LuceneFacetBuilder.class);

    /**
     * Whether or not to add tags by iterating over documents. This catches
     * terms that are stored but not indexed.
     * </p><p>
     * Note: Non-indexed terms cannot be searched, so they can only be used
     *       for display.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_BUILD_DOCS_TO_TERMS =
            "summa.facet.lucene.build.docstoterms";
    public static final boolean DEFAULT_BUILD_DOCS_TO_TERMS = false;

    /**
     * Whether or not to add tags by iterating over terms. This catches terms
     * that are indexed but not stored.
     * </p><p>
     * Note: This should normally (read: always) be true, in order to re-build
     *       the standard facet-structure.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_BUILD_TERMS_TO_DOCS =
            "summa.facet.lucene.build.termstodocs";
    public static final boolean DEFAULT_BUILD_TERMS_TO_DOC = true;

    private File luceneIndex;
    private boolean docsToTerms = DEFAULT_BUILD_DOCS_TO_TERMS;
    private boolean termsToDocs = DEFAULT_BUILD_TERMS_TO_DOC;

    public LuceneFacetBuilder(Configuration conf, Structure structure,
                              CoreMap coreMap, TagHandler tagHandler) {
        super(conf, structure, coreMap, tagHandler);
        docsToTerms = conf.getBoolean(CONF_BUILD_DOCS_TO_TERMS, docsToTerms);
        termsToDocs = conf.getBoolean(CONF_BUILD_TERMS_TO_DOCS, termsToDocs);
    }

    /**
     * Open persistent data (or create new data is empty) at the given location.
     * Note: All persistent Facet-data are stored in the sub-folder "facet" in
     * the location folder.
     * @param location     the root for the data.
     * @throws IOException if existing data was malformed.
     */
    @Override
    public synchronized void open(File location) throws IOException {
        super.open(new File(location, FacetCore.FACET_FOLDER));
        setLuceneIndexPath(new File(location, LuceneIndexUtils.LUCENE_FOLDER));
    }

    /**
     * Specifying the Lucene index is mandatory for using the method
     * {@link #build(boolean)}.
     * @param location the path to a Lucene index.
     */
    public void setLuceneIndexPath(File location) {
        log.debug("Lucene path set to '" + location + "'");
        luceneIndex = location;
    }

    /**
     * Clear and fill the given TagHandler from a Lucene index. This takes
     * a fair amount of time. {@link #setLuceneIndexPath(java.io.File)} must be
     * called before this method.
     * @throws IOException if an I/O error happened.
     */
    public void build(boolean keepTags) throws IOException {
        log.debug(String.format("build(%b) called", keepTags));

        if (!docsToTerms && !termsToDocs) {
            log.warn("Neither docs=>terms or terms=>docs was specified. "
                     + "No building will be done");
            return;
        }
        if (luceneIndex == null) {
            throw new IllegalStateException("setLuceneIndexPath must be called"
                                            + " before build");
        }
        log.debug(String.format("Opening IndexReader(%s)", luceneIndex));
        IndexReader ir;
        try {
            ir = IndexReader.open(luceneIndex);
        } catch (Exception e) {
            throw new IOException(String.format(
                    "Could not open IndexReader for location '%s'",
                    luceneIndex), e);
        }
        log.trace("build: Clearing existing core map");
        coreMap.clear();
        log.trace("build: IndexReader opened");
        buildTagsFromIndex(ir, keepTags, !keepTags);
        log.debug("build: Temporarily switching to CoreMapBuilder"); // Hack!
        try {
            CoreMap oldMap = coreMap;
            coreMap = new CoreMapBuilder(Configuration.newMemoryBased(),
                                         structure);
            facetMap.setCoreMap(coreMap);
            if (docsToTerms) {
                buildDocsToTerms(ir);
            }
            if (termsToDocs) {
                buildTermsToDocs(ir);
            }
            log.debug("Using copyTo to fill the standard CoreMapBitStuffed");
            coreMap.copyTo(oldMap);
            coreMap = oldMap;
            facetMap.setCoreMap(coreMap);
        } catch (IOException e) {
            throw new IOException("Failed building new facet index", e);
        } catch (Exception e) {
            throw new IOException("Unexpected exception building new facet"
                                  + " index", e);
        }
        log.debug("Filled tag handler from index");
    }

    // Note: This only catches indexable. Stored-only are added in core build
    private void buildTagsFromIndex(IndexReader ir, boolean keepTags,
                                    boolean dirtyAdd) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.debug("buildtagsFromIndex(..., keepTags: " + keepTags
                  + ", dirtyAdd: " + dirtyAdd + ") called");
        if (!keepTags) {
            log.trace("Clearing tags");
            tagHandler.clearTags();
        }
        Profiler profiler = new Profiler();
        long termCount = 0;
        int counter = 0;
        for (Facet facet: tagHandler.getFacets()) {
            String facetName = facet.getName();
            log.debug("Filling " + facetName
                      + " (" + ++counter + "/"
                      + tagHandler.getFacetNames().size() + ")");
            String lastTerm = null;
            boolean duplicateDetected = false;
            for (String fieldName: facet.getFields()) {
                Term searchTerm = new Term(fieldName, "");
                TermEnum terms = ir.terms(searchTerm);
                while (true) {
                    Term term = terms.term();
                    if (term == null) {
                        break;
                    }
                    if (term.text().equals(lastTerm)) {
                        if (!duplicateDetected) {
                            log.debug("Found the term duplicate " + term.field()
                                      + ":" + term.text()
                                      + ". Ignoring further duplicates from "
                                      + "this field");
                            duplicateDetected = true;
                        }
                        if (!terms.next()) {
                            break;
                        }
                        continue;
                    }
                    if (!term.field().equals(fieldName)) {
                        break;
                    }
                    String shortTerm = term.text();
                    lastTerm = shortTerm;
                    if (log.isTraceEnabled()) {
                        log.trace("Adding tag '" + shortTerm
                                  + "' from field '" + fieldName
                                  + "' to facet '" + facetName + "'");
                    }
                    if (dirtyAdd) {
                        tagHandler.dirtyAddTag(counter-1, shortTerm);
                    } else {
                        tagHandler.insertTag(counter-1, shortTerm);
                    }
                    termCount++;
                    if (!terms.next()) {
                        break;
                    }
                }
            }
            log.debug("Facet \"" + facetName + "\" filled with " +
                      tagHandler.getTagCount(facetName) + " tags");
        }
        if (dirtyAdd) {
            log.trace("Cleaning up tag handler");
            tagHandler.cleanup();
        }
        log.info(String.format(
                "Finished filling tag handler with %d tags in %d facets from "
                + "the index with %d documents in %s",
                termCount, tagHandler.getFacetNames().size(), ir.numDocs(),
                 profiler.getSpendTime()));
    }

    /**
     * Iterates through a Lucene index document=>term and term=>document
     * and fills the FacetMap with the extracted Tags.
     * @param ir a reader for a Lucene index.
     * @throws java.io.IOException if an I/O-error occured.
     */
    private void buildDocsToTerms(IndexReader ir) throws IOException {
        log.info("buildDocsToTerms: Filling core map for facet browsing with"
                 + " maxDoc = " + ir.maxDoc());

        Profiler totalProgress = new Profiler();
        int maxDoc = ir.maxDoc();

        log.trace("buildDocsToTerms: Iterating documents=>tags");
        noValuesSet.clear();
        Profiler progress = new Profiler();
        progress.setExpectedTotal(maxDoc);
        int feedbackEvery = Math.max(1, maxDoc / 100);
        progress.setBpsSpan(Math.min(10000, feedbackEvery));
        for (int docID = 0 ; docID < maxDoc ; docID++) {
            //noinspection OverlyBroadCatchBlock
            try {
                buildDocToTerms(ir, docID);
            } catch (Exception e) {
                log.warn("builddocstoTerms(): Exception while processing doc #"
                         + docID, e);
            }
            if (docID % feedbackEvery == 0 && log.isTraceEnabled()) {
                //noinspection DuplicateStringLiteralInspection
                log.trace("Mapped doc " + docID + "/" + maxDoc +
                          " ETA: " +  progress.getETAAsString(true) +
                          ". ");
            }
            progress.beat();
        }
        log.debug("buildDocsToTerms: Finished iteration of documents=>tags in "
                  + totalProgress.getSpendTime());
    }

    /* Keeps track of issued warnings due to missing values */
    private Set<String> noValuesSet = new HashSet<String>(10);
    private FieldSelector facetFields;
    private void checkFacetFields() {
        if (facetFields != null) {
            return;
        }
        log.debug("Constructing facetFields");
        List<String> fieldNames =
                new ArrayList<String>(structure.getFacets().size() * 5);
        for (Map.Entry<String, FacetStructure> entry:
                structure.getFacets().entrySet()) {
            fieldNames.addAll(Arrays.asList(entry.getValue().getFields()));
        }
        // TODO: Use a set instead of a list to ensure uniqueness
        facetFields = new MapFieldSelector(fieldNames);
        log.debug("Constructed facetFields with " + fieldNames.size()
                  + " fields");
    }
    /**
     * Extracts tags from the given document and inserts them in the FacetMap.
     * @param ir    a reader for a Lucene index.
     * @param docID the Lucene-id for the document to insert.
     * @throws java.io.IOException if the tag-info could not be extracted.
     */
    private void buildDocToTerms(IndexReader ir, int docID) throws IOException {
        if (ir.isDeleted(docID)) {
            log.trace("The docID " + docID + " refered to a deleted document");
            return;
        }
        checkFacetFields();
        Map<String, List<String>> facetTags =
                new HashMap<String, List<String>>(structure.getFacets().size());
        Document doc = ir.document(docID, facetFields);
        for (Map.Entry<String, FacetStructure> entry:
                structure.getFacets().entrySet()) {
            FacetStructure facet = entry.getValue();
            List<String> tags = new ArrayList<String>(10);
            for (String fieldName: facet.getFields()) {
                String[] terms = doc.getValues(fieldName);
                if (terms == null) {
                    if (!noValuesSet.contains(facet.getName())) {
                        log.debug(String.format(
                                "No term-values found for field %s in document "
                                + "%d. This warning will be supressed for the "
                                + "remainder of the build",
                                fieldName, docID));
                        noValuesSet.add(fieldName);
                    }
                    continue;
                }
                for (String term: terms) {
                    if (log.isTraceEnabled()) {
                        //noinspection DuplicateStringLiteralInspection
                        log.trace("Adding Tag " + term + " to Facet"
                                  + facet.getName() + " for doc " + docID);
                    }
                    tags.add(term);
                }
            }
            if (tags.size() > 0) {
                facetTags.put(facet.getName(), tags);
            }
        }
        facetMap.addToDocument(docID, facetTags);
    }

    private int BUFFER_SIZE = 1000;
    private int[] docBuffer = new int[BUFFER_SIZE];
    private int[] freqBuffer = new int[BUFFER_SIZE];
    // TODO: Only fill fields that has not previously been filled
    private synchronized void buildTermsToDocs(IndexReader ir) throws
                                                                   IOException {
        log.debug("buildTermsToDocs() started");
        long startTime = System.currentTimeMillis();
        for (Map.Entry<String, FacetStructure> entry:
                structure.getFacets().entrySet()) {
            FacetStructure facet = entry.getValue();
            for (String fieldName: facet.getFields()) {
                TermEnum terms = ir.terms(new Term(fieldName, ""));
                if (terms == null) {
                    continue;
                }
                do {
                    Term term = terms.term();
                    if (term == null || !fieldName.equals(term.field())) {
                        break;
                    }
                    TermDocs termDocs = ir.termDocs(term);
                    if (termDocs == null) {
                        break;
                    }
                    int docCount;
                    while ((docCount = termDocs.read(docBuffer, freqBuffer))
                           > 0) {
                        if (log.isTraceEnabled()) {
                            //noinspection DuplicateStringLiteralInspection
                            log.trace("Adding " + docCount + " references to "
                                      + facet.getName() + ":" + term.text());
                        }
                        facetMap.add(docBuffer, docCount, facet.getName(), 
                                     term.text());
                    }
                    /* Mind-numbingly slow due to repeated term lookups
                    while(termDocs.next()) {
                        facetMap.add(termDocs.doc(), facet.getName(),
                                     term.text());
                    }*/
                } while (terms.next());
            }
        }
        log.debug("Finished buildTermsToDocs in "
                  + (System.currentTimeMillis() - startTime) + " ms");
    }

    public boolean update(Payload payload) {
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("update(" + payload + ") called");
        }
        Integer deleteID = payload.getData().getInt(
                LuceneIndexUtils.META_DELETE_DOCID, null);
        Integer addID = payload.getData().getInt(
                LuceneIndexUtils.META_ADD_DOCID, null);
        if (deleteID == null && addID == null) {
            log.warn(String.format(
                    "updateRecord: Neither %s, nor %s was defined for %s",
                    LuceneIndexUtils.META_ADD_DOCID,
                    LuceneIndexUtils.META_DELETE_DOCID,
                    payload));
            return false;
        }
        if (deleteID != null) {
            //noinspection DuplicateStringLiteralInspection
            log.debug("Deleting '" + deleteID + "'");
            facetMap.removeDocument(deleteID);
        }
        if (addID != null) {
            //noinspection DuplicateStringLiteralInspection
            log.debug("Adding '" + addID + "'");
            Object docObject = payload.getData(Payload.LUCENE_DOCUMENT);
            if (docObject == null) {
                log.warn("No Document stored in " + payload);
                return false;
            }
            try {
                addDocument(addID, (Document)docObject);
            } catch (ClassCastException e) {
                log.warn(String.format(
                        "Expected class %s, got %s",
                        Document.class, docObject.getClass()), e);
                return false;
            }
        }
        return true;
    }

    private void addDocument(Integer addID, Document document) {
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("addDocument(" + addID + ", ...) called");
        }
        long startTime = System.nanoTime();
        Map<String, List<String>> facetTags = new HashMap<String,List<String>>(
                structure.getFacets().size());
        for (Map.Entry<String, FacetStructure> entry:
                structure.getFacets().entrySet()) {
            FacetStructure facet = entry.getValue();
            List<String> tags = new ArrayList<String>(50);
            for (String facetField: facet.getFields()) {
                Field[] docFields = document.getFields(facetField);
                if (docFields == null || docFields.length == 0) {
                    if (log.isTraceEnabled()) {
                        log.trace("No " + facetField + " field in doc #"
                                  + addID);
                    }
                    continue;
                }
                for (Field docField: docFields) {
                    if (!docField.isBinary()) {
                        String value = docField.stringValue();
                        if (value != null && !"".equals(value)) {
                            tags.add(value);
                        }
                    }
                }
            }
            if (tags.size() > 0) {
                facetTags.put(facet.getName(), tags);
            }
        }
        if (facetTags.size() == 0) {
            log.trace("No facet-information derived from doc #" + addID);
            return;
        }
        facetMap.addToDocument(addID, facetTags);
        if (log.isTraceEnabled()) {
            log.trace("Extracted and added Facet-information from doc #"
                      + addID + " in "
                      + (System.nanoTime() - startTime) / 1000000.0
                      + " ms");
        }
    }

    /* Package private getters - primarily used for debugging */
    Structure getStructure() {
        return structure;
    }

    CoreMap getCoreMap() {
        return coreMap;
    }
}
