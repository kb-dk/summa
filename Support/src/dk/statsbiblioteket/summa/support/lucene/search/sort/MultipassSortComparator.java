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

import dk.statsbiblioteket.summa.common.util.*;
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
import org.apache.lucene.search.FieldCache;

import java.io.IOException;
import java.text.Collator;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;

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
 * </p><p>
 * Important: This is experimental and will not work with multi searchers, as
 * no global sortValue can be derived from the internal order structure.
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

        final BitsArray order = getOrderFast(reader, fieldname);
//        System.out.println("Multi: " + Logs.expand(order, 20));
        return new ScoreDocComparator() {
            public int compare (ScoreDoc i, ScoreDoc j) {
                try {
                    return order.getAtomic(i.doc) - order.getAtomic(j.doc);
                } catch (ArrayIndexOutOfBoundsException e) {
                    log.error(String.format(
                            "ScoreDocComparator got exception while comparing "
                            + "orders for docID %d with %d. The number or "
                            + "orders is %d. This should not happen as orders "
                            + "should contain all docIDs",
                            i.doc, j.doc, order.size()));
                    return 0;
                }
            }

            public Comparable sortValue (ScoreDoc i) {
                return order.getAtomic(i.doc);
            }

            public int sortType(){
                return SortField.CUSTOM;
            }
        };
    }

    /**
     * Calculate the order of the documents in the reader, sorted by fieldname
     * and the language or collator specified at construction time.
     * This calculator is optimized towards low memory impact at the cost of
     * (very) high processing time.
     * @param reader    the reader with the terms.
     * @param fieldname the fieldname to get the order for.
     * @return the order for the documents handled by the reader.
     * @throws IOException if the index could not be accessed.
     */
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
                reader.maxDoc(), profiler.getSpendTime(), loopCount-2));
        return positions;
    }

    // Meant for testing
    private FieldCache.StringIndex getStringIndex(
            IndexReader reader, String field) {
        log.info("Requesting StringIndex for field " + field);
        Profiler profiler = new Profiler();
        FieldCache.StringIndex stringIndex;
        try {
            stringIndex = FieldCache.DEFAULT.getStringIndex(reader, field);
        } catch (IOException e) {
            log.error("Could not retrieve StringIndex", e);
            return null;
        }
        log.info("Got StringIndex of length " + stringIndex.order.length
                 + " in " + profiler.getSpendTime());
        return stringIndex;
    }

    /*
     * IntArray2D t2d: termPos -> docID[].
     * PriorityQueue<orderedString(term, termpos)>> slider: .
     * BitsArray docOrder: docID -> docPos.
     */
    /**
     * Calculate the order of the documents in the reader, sorted by fieldname
     * and the language or collator specified at construction time.
     * This calculator is optimized towards a mix of low memory impact an fast
     * processing.
     * @param reader    the reader with the terms.
     * @param fieldname the fieldname to get the order for.
     * @return the order for the documents handled by the reader.
     * @throws IOException if the index could not be accessed.
     */
    private synchronized BitsArray getOrderFast(
            IndexReader reader, String fieldname) throws IOException {
        fieldname = fieldname.intern();
        checkCacheConsistency(reader);
        if (orders.containsKey(fieldname)) {
            return orders.get(fieldname);
        }

        // getStringIndex(reader, fieldname); // Performance testing

        Profiler profiler = new Profiler();
        profiler.setBpsSpan(10);
        log.debug(String.format(
                "Calculating order for all the terms in the field %s using "
                + "language %s with Collator %s for %d documents",
                fieldname, language, collator, reader.maxDoc()));
        final TermDocs termDocs = reader.termDocs();

        // 1. Create a heap for Strings. The heap has a maximum size in bytes.
        final ResourceTracker<OrderedString> tracker = new OrderedStringTracker(
                Integer.MAX_VALUE, sortBufferSize);
        OrderedString base = null;
        WindowQueue<OrderedString> slider =
                new WindowQueue<OrderedString>(
                new OrderedStringComparator(collator), base, null, tracker);
        IntArray2D t2d = new IntArray2D(reader.maxDoc(), 1.1D);

        // 2. Set the empty String as base, set the logicalPos to 1.
        int logicalPos = 1;
        // 3. Create a BitsArray docOrders of length maxDocs.
        BitsArray docOrders = BitsArrayFactory.createArray(
                reader.maxDoc()+1, 1, BitsArray.PRIORITY.mix);

        int loopCount = 1;
        int termPos;
        while (true) {
            log.trace("Starting sort-loop #" + loopCount
                      + " with lower bound '" + base + "'");
            // 4. Iterate through all terms for the given field
            long enumLoopStart = System.currentTimeMillis();
            termPos = 0;
            final TermEnum termEnum = reader.terms(new Term(fieldname, ""));
            try {
                do {
                    final Term term = termEnum.term();
                    if (term==null || !term.field().equals(fieldname)) {
                        break;
                    }
                    if (loopCount == 1) { // Collect docIDs for terms
                        termDocs.seek(termEnum);
                        while (termDocs.next()) {
                            t2d.append(termPos, termDocs.doc());
                        }
                    }
                    // TODO: Consider first-letter check optimization
                    if (!t2d.isCleared(termPos)) { // Insert if unhandled
                        slider.insert(new OrderedString(term.text(), termPos));
                    }
                    termPos++;
                } while (termEnum.next());
            } finally {
                termEnum.close();
            }
            log.trace("Finished sort-loop #" + loopCount
                      + (loopCount == 1 ? " with docID collection" : "")
                      + " in "
                      + (System.currentTimeMillis() - enumLoopStart) / 1000
                      + " seconds. Got " + slider.getSize() + "/" + termPos
                      + " terms");

            // 5. If the heap is empty, goto 9.<br />
            if (slider.getSize() == 0) {
                break;
            }

            // Assign orders from the slider to docOrders

            int sliderSize = slider.getSize();
            log.trace("Assigning docIDs for " + sliderSize
                      + " terms above base '" + base + "'");
            OrderedString oldBase = base;
            base = slider.getMin();
            logicalPos += slider.getSize(); // For later
            int reverseLogicalPos = logicalPos-1;
            long collectorStart = System.currentTimeMillis();
            // 6. For each terms on the heap (sorted order)
            while (slider.getSize() > 0) {
                final OrderedString term = slider.removeMin();
                int[] docIDs = t2d.get(term.getOrder());
                for (int docID: docIDs) {
                    docOrders.set(docID, reverseLogicalPos);
                }
                // 6.2 increment logicalPos.
                // As the slider delivers in reverse order, we reverse too
                reverseLogicalPos--;
            }
            log.trace("DocID assignment for " + sliderSize
                      + " terms above base '" + oldBase + "' took "
                      + (System.currentTimeMillis() - collectorStart) / 1000
                      + " seconds");

            // 7. Set base to the last extracted term from the heap.
            slider.setLowerBound(base);
            profiler.beat();
            // 8. Goto 4.
            loopCount++;
        }

        // 9. docOrders now contain relative docOrders for all documents in the
        // index. A position of 0 means that there was no term for the given
        // document.

        // The array should contain entries for _all_ docIDs
        if (docOrders.size() < reader.maxDoc()) {
            docOrders.set(reader.maxDoc()-1, 0);
        }

        // Depending on wanted behaviour, these can be left or set to
        // logicalPos+1.
        if (nullComesLast) {
            logicalPos++;
            for (int i = 0 ; i < docOrders.size() ; i++) {
                if (docOrders.get(i) == 0) {
                    docOrders.set(i, logicalPos);
                }
            }
        }
        termDocs.close();

        orders.put(fieldname, docOrders);
        log.debug(String.format(
                "Got %d unique positions in the order-array for %d terms in "
                + "the field %s using language %s with Collator %s for %d "
                + "documents in %s performing %d loops through the terms",
                logicalPos-1, termPos, fieldname, language, collator,
                reader.maxDoc(), profiler.getSpendTime(), loopCount-2));
        return docOrders;
    }

    public static class OrderedString implements Comparable<OrderedString> {
        private String s;
        private int order;
        public OrderedString(String s, int order) {
            this.s = s;
            this.order = order;
        }

        public int compareTo(OrderedString o) {
            return s.compareTo(o.getString());
        }

        public String getString() {
            return s;
        }
        public int getOrder() {
            return order;
        }
        @Override
        public String toString() {
            return s + "(" + order + ")";
        }
    }
    
    private static class OrderedStringComparator implements
                                                 Comparator<OrderedString> {
        private Collator collator;

        private OrderedStringComparator(Collator collator) {
            this.collator = collator;
        }

        public int compare(OrderedString o1, OrderedString o2) {
            return collator.compare(o1.getString(), o2.getString());
        }
    }

    private class OrderedStringTracker extends
                                            ResourceTrackerImpl<OrderedString> {
        // 38 == String, 8 == ref to String, 8 == Object, 4 == order(int)
        private static final int SINGLE_ENTRY_OVERHEAD = 38 + 8 + 8 + 4; // 32 bit

        public OrderedStringTracker(long maxCountLimit, long memLimit) {
            super(1, maxCountLimit, memLimit);
        }

        @Override
        public long calculateBytes(OrderedString element) {
            return element == null ? 0
                   : element.getString().length() * 2 + SINGLE_ENTRY_OVERHEAD;
        }

    }
}
