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
package dk.statsbiblioteket.summa.index;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collection;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.Payload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;

/**
 * Maps from RecordIDs to LuceneIDs for a given Lucene Index. The RecordIDs must
 * be indexed in a Field named "RecordID" in the index. One and only one
 * RecordID for each Document in the index.
 * </p><p>
 * The IDMapper extracts IDs from the given Directory upon startup. After that,
 * there are no connection to the Directory.
 * </p><p>
 * It is the responsibility of the caller to ensure that changes to the
 * underlying index triggers an update of the IDMapper.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IDMapper implements Map<String, Integer> {
    private static Log log = LogFactory.getLog(IDMapper.class);

    private Map<String, Integer> recordIDs;

    /**
     * Creates an empty mapper, ready for input.
     */
    public IDMapper() {
        log.debug("Creating empty IDMapper");
        recordIDs = new HashMap<String, Integer>(1000);
    }

    /**
     * Extracts RecordIDs from the given directory.
     * @param directory where to get the initial ID's.
     * @throws IOException if the IDs could not be extracted from the index at
     *                     directory.
     */
    @QAInfo(level = QAInfo.Level.PEDANTIC,
            state = QAInfo.State.IN_DEVELOPMENT,
            author = "te",
            comment="Check if termEnum skips the first term")
    public IDMapper(Directory directory) throws IOException {
        log.debug("Constructing IDMapper for '" + directory + "'");
        long startTime = System.currentTimeMillis();
        IndexReader reader = IndexReader.open(directory);
        recordIDs = new HashMap<String, Integer>((int)(reader.maxDoc() * 1.2));
        TermEnum termEnum = reader.terms(new Term(Payload.RECORD_FIELD, ""));
        while (termEnum.next()) {
            if (!termEnum.term().field().equals(Payload.RECORD_FIELD)) {
                break;
            }
            TermDocs termDocs =
                    reader.termDocs(new Term(Payload.RECORD_FIELD, ""));
            String termString = termEnum.term().text();
            boolean found = false;
            while (termDocs.next()) {
                found = true;
                if (log.isTraceEnabled()) {
                    log.trace("Got RecordID(" + termString + ") => LuceneID("
                              + termDocs.doc() + ")");
                }
                if (recordIDs.containsKey(termString)) {
                    log.warn("A LuceneID (" + recordIDs.get(termString)
                             + ") already exists for field "
                             + Payload.RECORD_FIELD
                             + " (" + termDocs.doc() + ")");
                }
                recordIDs.put(termString, termDocs.doc());
            }
            if (!found) {
                log.warn("No RecordID found in field " + Payload.RECORD_FIELD);
            }
        }
        if (recordIDs.size() != reader.maxDoc()) {
            log.warn("The number of extracted RecordIDs (" + recordIDs.size()
                     + ") does not match the number of Documents ("
                     + reader.maxDoc() + ") for index at '" + directory + "'");
        }
        reader.close();
        //noinspection DuplicateStringLiteralInspection
        log.debug("Extracted " + recordIDs.size() + " RecordIDs for "
                  + reader.maxDoc() + " documents in "
                  + (System.currentTimeMillis()-startTime) + " ms");
    }

    // TODO: Validate ont-to-one correspondance

    /* Map interface methods */

    public int size() {
        return recordIDs.size();
    }
    public boolean isEmpty() {
        return recordIDs.isEmpty();
    }
    public boolean containsKey(Object key) {
        return recordIDs.containsKey(key);
    }
    public boolean containsValue(Object value) {
        return recordIDs.containsValue(value);
    }
    public Integer get(Object key) {
        return recordIDs.get(key);
    }
    public Integer put(String key, Integer value) {
        return recordIDs.put(key, value);
    }
    public Integer remove(Object key) {
        return recordIDs.remove(key);
    }
    public void putAll(Map<? extends String, ? extends Integer> m) {
        recordIDs.putAll(m);
    }
    public void clear() {
        recordIDs.clear();
    }
    public Set<String> keySet() {
        return recordIDs.keySet();
    }
    public Collection<Integer> values() {
        return recordIDs.values();
    }
    public Set<Entry<String, Integer>> entrySet() {
        return recordIDs.entrySet();
    }
}
