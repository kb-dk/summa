/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.support.lucene.search.sort;

import dk.statsbiblioteket.util.Profiler;
import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FieldComparator;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * This comparator maintains a cache of sort-positions.
 * </p><p>
 * The comparator has high memory-usage for the first sort for a given field,
 * but after that only the relative positions of the documents are stored,
 * taking up #documents * 4 bytes.
 */
public class LocalStaticSortComparator extends ReusableSortComparator {
    private static final Logger log =
            Logger.getLogger(LocalStaticSortComparator.class);

    /**
     * The cache of orders for fields. The cache is cleared when the index
     * changes.
     */
    private Map<String, int[]> orders = new HashMap<String, int[]>(10);

    /**
     * Create a comparator based on the sorting rules for the given language.
     * 
     * @param language A two-letter ISO-639 language code. A list is located at
     *                http://www.loc.gov/standards/iso639-2/php/English_list.php
     */
    public LocalStaticSortComparator(String language) {
        super(language);
    }

    // inherit javadocs
    @Override
    public FieldComparator newComparator(
            String fieldname, int numHits, int sortPos, boolean reversed)
            throws IOException {
            final boolean r = reversed;
            final int[] slots = new int[numHits];
            final String fieldName = fieldname;
        //final int[] order = getOrder(reader, fieldname);

        return new FieldComparator() {
            IndexReader reader = null;
            int docbase = 0;
            int bottom;


            /*
            public int compare (ScoreDoc i, ScoreDoc j) {
                return order[i.doc] - order[j.doc];
            }

            public Comparable sortValue (ScoreDoc i) {
                return order[i.doc];
            }

            public int sortType(){
                return SortField.CUSTOM;
            } */

            @Override
            public int compare(int i, int i1) {
                return (!r) ? i - i1 : i1 - i;
            }

            @Override
            public void setBottom(int i) {
                bottom = i;
            }

            @Override
            public int compareBottom(int i) throws IOException {
                return (!r) ? i - bottom : bottom - i; 
            }

            @Override
            public void copy(int i, int i1) throws IOException {
                slots[i] = i1;
            }

            @Override
            public void setNextReader(IndexReader indexReader, int i)
                    throws IOException {
                //slots = getOrder(reader, fieldName);
                docbase = i;
            }

            @Override
            public Comparable value(int i) {
                return slots[i];
            }
        };
    }

    /**
     * Returns an array of term-positions in sorted order. The array is specific
     * for the given field in the given index and is cached. The getOrder keeps
     * track of versions, so it should be safe to call with different readers.
     *
     * @param reader     The reader to use as basis for the order.
     * @param fieldName  The field name to use for ordering.
     * @return positions To the terms in the field in order.
     * @throws java.io.IOException if the field in the reader could not be
     * sorted.
     */
    protected synchronized int[] getOrder(
            final IndexReader reader, String fieldName) throws IOException {
        fieldName = fieldName.intern();
        checkCacheConsistency(reader);

        int maxDoc = reader.maxDoc();
        log.trace("Checking cache for '" + fieldName + "'");
        if (orders.containsKey(fieldName)) {
            return orders.get(fieldName);
        }

        log.debug("Building new cache for field '" + fieldName + "'");
        Profiler profiler = new Profiler();
        // Build a list of pairs
        Pair[] sorted = getPairs(reader, fieldName);
        // Sort the list

        log.trace("Sorting positions for '" + fieldName + "'");
        Arrays.sort(sorted, new Comparator<Pair>() {
            public int compare(Pair o1, Pair o2) {
                if (o1 == null) {
                    return o2 == null ? 0 : 1;
                } else if (o2 == null) {
                    return -1;
                } else if ("".equals(o1.term)) {
                    return "".equals(o2.term) ? 0 : 1;
                } else if ("".equals(o2.term)) {
                    return -1;
                }
                return o1.compareTo(o2);
            }
        });

        provideFeedback(sorted);

        // Convert to position-list
        log.trace("Converting positions for '" + fieldName
                  + "' to compact form");
        int[] positions = new int[maxDoc];
        int position = 1;
        for (int i = 0 ; i < maxDoc ; i++) {
            if (sorted[i] == null) { // Nulls are last
                break;
            }
            positions[sorted[i].docID] = position++;
        }
        // Fill all non-filled in position list
        for (int i = 0 ; i < maxDoc ; i++) {
            if (positions[i] == 0) {
                positions[i] = position++;
            }
        }

        orders.put(fieldName, positions);
        log.debug(String.format("Created cache for '%s' for %d documents in %s",
                                fieldName, maxDoc, profiler.getSpendTime()));
        return positions;
    }

    @Override
    public void indexChanged(IndexReader reader) {
        log.debug("Index has changed. Dropping caches");
        orders.clear();
    }

    protected Pair[] getPairs(final IndexReader reader, String fieldname)
                                                            throws IOException {
        Pair[] pairs = new Pair[reader.maxDoc()];
        TermDocs termDocs = reader.termDocs();

        TermEnum termEnum = reader.terms(new Term(fieldname, ""));
        int counter = 0;
        try {
            do {
                Term term = termEnum.term();
                //noinspection StringEquality
                if (term==null || term.field() != fieldname) {
                    break;
                }
                String termText = term.text();
                termDocs.seek (termEnum);
                while (termDocs.next()) {
                    if (log.isTraceEnabled() && counter++ % 100000 == 0) {
                        log.trace("Building term positions for term #" +
                                  counter);
                    }
                    pairs[termDocs.doc()] =
                            new Pair(termDocs.doc(), termText);
                }
            } while (termEnum.next());
        } finally {
            termDocs.close();
            termEnum.close();
        }
        return pairs;
    }

    protected class Pair implements Comparable<Pair> {
        int docID;
        String term;

        public Pair(int docID, String term) {
            this.docID = docID;
            this.term = term;
        }

        public int compareTo(Pair o) {
            if (term == null) {
                return o.term == null ? 0 : 1;
            } else if (o.term == null) {
                return -1;
            }
            return collator.compare(term, o.term);
        }
    }

    protected void provideFeedback(Pair[] sorted) {
        if (log.isTraceEnabled()) {
            int SAMPLES = 10;
            StringWriter top = new StringWriter(SAMPLES*100);
            int counter = 0;
            for (Pair p: sorted) {
                if (p != null && p.term != null) {
                    top.append(p.term).append(" ");
                    if (counter++ == SAMPLES) {
                        break;
                    }
                }
            }
            StringWriter bottom = new StringWriter(SAMPLES*100);
            counter = 0;
            for (int i = sorted.length-1 ; i >= 0 ; i--) {
                Pair p = sorted[i];
                if (p != null && p.term != null) {
                    bottom.append(p.term).append(" ");
                    if (counter++ == SAMPLES) {
                        break;
                    }
                }
            }
            log.trace(String.format("The first %d/%d sorted terms: %s",
                                    SAMPLES, sorted.length, top));
            log.trace(String.format("The last %d/%d sorted terms: %s",
                                    SAMPLES, sorted.length, bottom));
        }
    }

}


