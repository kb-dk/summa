/* $Id:$
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

import dk.statsbiblioteket.summa.common.util.bits.BitsArray;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortField;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A Sortcomparator that utilized multipass-scans of the corpus in order to
 * provide sorting on many terms without the usual huge memory overhead.
 * The first search with sort for a given index takes longer than a standard
 * search with sort, but subsequent searches are noticeable faster.
 * </p><p>
 * Another downside to this implementation is that the cost of initialization
 * must be paid in full even when re-opening an index.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MultipassSortComparator extends ReusableSortComparator {
    private static Log log = LogFactory.getLog(MultipassSortComparator.class);

    private static final int SINGLE_ENTRY_OVERHEAD = 50;
    private int sortBufferSize;

    private Map<String, BitsArray> orders = new HashMap<String, BitsArray>(10);

    /**
     * Create multipass sorter for the given language with the given buffer
     * size. The buffer determines the memory usage and the speed. If there are
     * X characters in the terms that are to be sorted, they will be counted as
     * {@code (C + X * 2)} bytes (a character in Java is 2 bytes), where C is
     * 50 and account (roughly) for the memory usage of pointers and structure
     * data for a String.
     * </p><p>
     * With a total term-size of X1 bytes and a buffer size of Y1 bytes, the
     * number of loops through the term-list will be X1/Y1. As each loop takes
     * roughly the same amount of time, the trade-off between memory usage and
     * speed is simple to adjust. 
     * @param language       the language to use for sorting.
     * @param sortBufferSize the buffer-size in bytes.
     */
    public MultipassSortComparator(String language, int sortBufferSize) {
        super(language);
        this.sortBufferSize = sortBufferSize;
    }

    @Override
    protected void indexChanged() {
        log.debug("Index changed. Clearing sort caches");
        orders.clear();
    }

    // inherit javadocs
    @Override
    public ScoreDocComparator newComparator(
            final IndexReader reader, final String fieldname)
            throws IOException {

        final BitsArray order = getOrder(reader, fieldname);
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

    private BitsArray getOrder(IndexReader reader, String fieldname) {
        fieldname = fieldname.intern();
        checkCacheConsistency(reader);

        throw new UnsupportedOperationException("Not implemented yet");
    }

}
