/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2009  The State and University Library
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
package dk.statsbiblioteket.summa.support.lucene.search.sort;

import dk.statsbiblioteket.summa.common.util.CollatorFactory;
import dk.statsbiblioteket.summa.common.util.ResourceTracker;
import dk.statsbiblioteket.summa.common.util.StringTracker;
import dk.statsbiblioteket.summa.common.util.bits.BitsArray;
import dk.statsbiblioteket.summa.common.util.bits.BitsArrayFactory;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortField;

import java.io.IOException;
import java.text.Collator;
import java.util.HashMap;
import java.util.Map;

/**
 * A SortComparator that utilized multipass-scans of the corpus in order to
 * provide sorting on many terms without the usual huge memory overhead.
 * Other than that, it behaves logically as a standard Lucene String sort.
 * The first search with sort for a given index takes longer than a standard
 * search with sort, but subsequent searches are noticeable faster.
 * </p><p>
 * A significant downside to this implementation is that the cost of
 * initialization must be paid in full when an index is re-opened.
 * This makes it a poor fit for realtime search.
 * </p><p>
 * The algorithm for the comparator is as follows:<br />
 * 1. Create a heap for Strings. The heap has a maximum size in bytes.<br />
 * 2. Set the empty String as base, set the logicalPos to 1.<br />
 * 3. Create a BitsArray positions of length maxDocs.<br />
 * 4. Iterate through all terms for the given field<br />
 * 4.1 if the current term is ordered after the base (typically order is
 *     provided by a Collator), add it to the heap.
 *     Note that the heap is limited in size and thus containg top-X after base.
 *     X is determined by byte size and thus is not a fixed number.<br />
 * 5. If the heap is empty, goto 9.<br />
 * 6. For each terms on the heap (sorted order)<br />
 * 6.1 for each docID for the term<br />
 * 6.1.1 assign with {@code positions.set(docID, logicalPos)}<br />
 * 6.2 increment logicalPos.<br />
 * 7. Set base to the last extracted term from the heap.<br />
 * 8. Goto 4.<br />
 * 9. positions now contain relative positions for all documents in the index.
 *    A position of 0 means that there was no term for the given document.
 *    Depending on wanted behaviour, these can be left or set to logicalPos+1.
 * </p><p>
 * When a MultipassSortComparator is created, a buffer size is specified. If
 * there are X characters in the terms that are to be sorted, they will be
 * counted as {@code (C + X * 2)} bytes (a character in Java is 2 bytes) in the
 * buffer, where C is 50 and account (roughly) for the memory usage of pointers
 * and structure data for a String.
 * </p><p>
 * With a total term-size of X1 bytes and a buffer size of Y1 bytes, the
 * number of loops through the term-list will be X1/Y1. As each loop takes
 * roughly the same amount of time, the trade-off between memory usage and
 * speed is simple to adjust.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MultipassSortComparator extends ReusableSortComparator {
    private static Log log = LogFactory.getLog(MultipassSortComparator.class);

    // The number of bytes for the buffer
    private int sortBufferSize;

    /**
     * Determines whether documents without the given field are sorted last.
     */
    private boolean nullComesLast = true;

    private Map<String, BitsArray> orders = new HashMap<String, BitsArray>(10);

    /**
     * Create multipass sorter for the given language with the given buffer
     * size - see the class doc for details.
     * @param language       the language to use for sorting.
     * @param sortBufferSize the buffer-size in bytes.
     */
    public MultipassSortComparator(String language, int sortBufferSize) {
        super(language);
        this.sortBufferSize = sortBufferSize;
    }

    /**
     * Create multipass sorter for the given language with the given buffer
     * size - see the class doc for details.
     * @param collator       the collator to use for sorting.
     * @param sortBufferSize the buffer-size in bytes.
     */
    public MultipassSortComparator(Collator collator, int sortBufferSize) {
        super(collator);
        this.sortBufferSize = sortBufferSize;
    }

    @Override
    protected void indexChanged() {
        log.debug("Index changed. Clearing sort order caches");
        orders.clear();
    }

    // inherit javadocs
    @Override
    public ScoreDocComparator newComparator(
            final IndexReader reader, final String fieldname)
            throws IOException {

        final BitsArray order = getOrder(reader, fieldname);
//        System.out.println("Multi: " + Logs.expand(order, 20));
        return new ScoreDocComparator() {
            public int compare (ScoreDoc i, ScoreDoc j) {
                return order.getAtomic(i.doc) - order.getAtomic(j.doc);
            }

            public Comparable sortValue (ScoreDoc i) {
                return order.getAtomic(i.doc);
            }

            public int sortType(){
                return SortField.CUSTOM;
            }
        };
    }

    private synchronized BitsArray getOrder(
            IndexReader reader, String fieldname) throws IOException {
        fieldname = fieldname.intern();
        checkCacheConsistency(reader);

        if (orders.containsKey(fieldname)) {
            return orders.get(fieldname);
        }

        Profiler profiler = new Profiler();
        profiler.setBpsSpan(10);
        log.debug(String.format(
                "Calculating order for all the terms in the field %s using "
                + "language %s with Collator %s for %d documents",
                fieldname, language, collator, reader.maxDoc()));
        final TermDocs termDocs = reader.termDocs();

        // 1. Create a heap for Strings. The heap has a maximum size in bytes.
        final ResourceTracker<String> tracker = new StringTracker(
                1, Integer.MAX_VALUE, sortBufferSize);
        String base = null;
        WindowQueue<String> collector = new WindowQueue<String>(
                CollatorFactory.wrapCollator(collator), base, null, tracker);

        // 2. Set the empty String as base, set the logicalPos to 1.
        int logicalPos = 1;
        // 3. Create a BitsArray positions of length maxDocs.
        BitsArray positions = BitsArrayFactory.createArray(
                reader.maxDoc(), 1, BitsArray.PRIORITY.mix);

        int loopCount = 1;
        long termCount = 0;
        while (true) {
            log.trace("Starting sort-loop #" + loopCount++
                      + " with lower bound '" + base + "'");
            // 4. Iterate through all terms for the given field
            long enumLoopStart = System.currentTimeMillis();
            termCount = 0;
            final TermEnum termEnum = reader.terms(new Term(fieldname, ""));
            try {
                do {
                    final Term term = termEnum.term();
                    if (term==null || !term.field().equals(fieldname)) {
                        break;
                    }
                    termCount++;
                    // 4.1 if the current term is ordered after the base, add
                    //     it to the heap.
                    collector.insert(term.text());
                } while (termEnum.next());
            } finally {
                termEnum.close();
            }
            log.trace("Finished term-sort in "
                      + (System.currentTimeMillis() - enumLoopStart) / 1000
                      + " seconds");

            // 5. If the heap is empty, goto 9.<br />
            if (collector.getSize() == 0) {
                break;
            }

            log.trace("Extracting docIDs for " + collector.getSize()
                      + " terms above base '" + base + "'");
            base = collector.getMin();
            logicalPos += collector.getSize(); // For later
            int reverseLogicalPos = logicalPos-1;
            long collectorStart = System.currentTimeMillis();
            // 6. For each terms on the heap (sorted order)
            while (collector.getSize() > 0) {
                final String term = collector.removeMin();
//                System.out.println(" * " + term + " has position " + reverseLogicalPos);

                // TODO: Can we optimize this for termEnum?
                // Preferably without doing unneccesary requests in the previous
                // loop
                termDocs.seek(new Term(fieldname, term));
                // 6.1 for each docID for the term
                while (termDocs.next()) {
                    // 6.1.1 assign with positions.set(docID, logicalPos)
                    positions.set(termDocs.doc(), reverseLogicalPos);
                }

                // 6.2 increment logicalPos.
                // As the collector delivers in reverse order, we reverse too
                reverseLogicalPos--;
            }
            log.trace("DocID-extraction took " 
                      + (System.currentTimeMillis() - collectorStart) / 1000
                      + " seconds");

            // 7. Set base to the last extracted term from the heap.
            collector.setLowerBound(base);
            profiler.beat();
            // 8. Goto 4.
        }

        // 9. positions now contain relative positions for all documents in the
        // index. A position of 0 means that there was no term for the given
        // document.

        // Depending on wanted behaviour, these can be left or set to
        // logicalPos+1.
        if (nullComesLast) {
            logicalPos++;
            for (int i = 0 ; i < positions.size() ; i++) {
                if (positions.get(i) == 0) {
                    positions.set(i, logicalPos);
                }
            }
        }
        termDocs.close();

        orders.put(fieldname, positions);
        log.debug(String.format(
                "Got %d unique positions in the order-array for %d terms in "
                + "the field %s using language %s with Collator %s for %d "
                + "documents in %s performing %d loops through the terms",
                logicalPos-1, termCount, fieldname, language, collator,
                reader.maxDoc(), profiler.getSpendTime(), loopCount-1));
        return positions;
    }
}
