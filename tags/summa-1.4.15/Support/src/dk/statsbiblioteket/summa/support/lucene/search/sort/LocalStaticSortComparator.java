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
package dk.statsbiblioteket.summa.support.lucene.search.sort;

import dk.statsbiblioteket.util.CachedCollator;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Streams;
import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortComparator;
import org.apache.lucene.search.SortField;

import java.io.*;
import java.net.URL;
import java.text.Collator;
import java.util.*;

/**
 * A localizable comparator for doing sorted searches in Lucene. The
 * comparator maintains a cache of sort-values. Changes to the Lucene
 * index are detected by using reader.getVersion + reader.maxDoc.
 * </p><p>
 * The comparator looks for the files {@code charstats.<languagecode>.dat} and
 * {@code charstats.dat} in that order. If no char statistics are found,
 * it defaults to {@link #summaChars}. The languagecode is a two-letter language
 * code from ISO 639-2. The charset is UTF-8.
 * </p><p>
 * The charstats contains the characters that are safe for comparing
 * char-to-char. If the two Strings to compare only consists of those chars,
 * they can be compared with array-lookups instead of the more costly Collator
 * from Java.
 * </p><p>
 * In order to use the caching, an instance of the comparator should be kept
 * alive between searches.
 * </p><p>
 * Usage:
 * <pre>
 * LocalSortComparator myCompare = new LocalSortComparator("da");
 * SortField titleSort           = new SortField("title", myCompare);
 * SortField titleSortReverse    = new SortField("title", myCompare, true);
 * ...
 * Hits hits = searcher.search(query, Sort(titleSort));
 * </pre>
 */
public class LocalStaticSortComparator extends SortComparator {
    private static final Logger log =
            Logger.getLogger(LocalStaticSortComparator.class);
    private String language;
    private Collator collator;

    // Field name, document position
    private Map<String, int[]> orders = new HashMap<String, int[]>(10);
    // The index-version used for building the orders.
    private String indexVersion = null;

    private static final String summaChars =
            "eaoi 0ntr1s24cl93857hd6pgum.bfv:xwykj_z/-qASPCXIUø"
            + "NEGæ$>é#Väåö&ü^áāLó~–íãT*@ıç%čâèBM|š—FYêDúàūžñRð"
            + "·Oć−ôë,łβα°±HşīîJõKZQēśδ†ṣōïěğăńýřûė→ì";
// Unsafe:            "þ×µμγ§ßο∼"
//            + "£ò▿ưκđσơλùειżτę­νπąρœ¤őηǩĸºφ≥ςĭωί³⋅≤иũňţθό∞ή™υź"
//            + "еаέ…²ªW€≈ψ¢нт•↑ľ¾ύχ₂ώр‰űάÿ¹о½ẽ‐ųζů;л'‡ξĩ√⁰¼ﬁĝȩ←";


    /**
     * Create a comparator based on the sorting rules for the given language.
     * @param language a two-letter ISO-639 language code. A list is located at
     *                http://www.loc.gov/standards/iso639-2/php/English_list.php
     */
    public LocalStaticSortComparator(String language) {
        this.language = language;
        Locale locale = new Locale(language);
        collator = createCollator(locale);
    }

    /**
     * Attempts to load char statistics and create a {@link CachedCollator} from
     * that. If no statistics can be loaded, the CachedCollator uses the default
     * statistics.
     * @param locale the Locale to use for the Collator.
     * @return a collator, preferably based on char statistics.
     */
    protected Collator createCollator(Locale locale) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            String first = "charstats." + locale.getLanguage() + ".dat";
            String second = "charstats.dat";

            URL url = loader.getResource(first);
            if (url == null) {
                //noinspection DuplicateStringLiteralInspection
                log.debug("Could not locate " + first + ", trying " + second);
                url = loader.getResource(second);
                if (url == null) {
                    //noinspection DuplicateStringLiteralInspection
                    log.debug("Could not locate " + second + ". Defaulting to "
                              + "hardcoded Summa char statistics");
                    return new CachedCollator(locale, summaChars, true);
                }
            }
            InputStream is = url.openStream();
            assert is != null : "The InputStream is should be defined, as we "
                                + "know the URL '" + url + "'";
            ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
            Streams.pipe(is, out);
            Collator collator =
                    new CachedCollator(locale, out.toString("utf-8"), true);
            log.debug("Created CachedCollator based on stored char statistics "
                      + "from '" + url + "'");
            return collator;
        } catch(UnsupportedEncodingException e) {
            log.error("Exception converting to UTF-8, defaulting to hardcoded "
                      + "defaults", e);
            return new CachedCollator(locale, summaChars, true);
        } catch(IOException e) {
            log.error("IOException getting statistics for collator, defaulting "
                      + "to hardcoded defaults", e);
            return new CachedCollator(locale, summaChars, true);
        } catch(Exception e) {
            log.error("Exception getting statistics for collator, defaulting to"
                      + " hardcoded defaults", e);
            return new CachedCollator(locale, summaChars, true);
        }
    }

    // inherit javadocs
    @Override
    public ScoreDocComparator newComparator(
            final IndexReader reader, final String fieldname)
            throws IOException {

        final int[] order = getOrder(reader, fieldname);
        return new ScoreDocComparator() {
            public int compare (ScoreDoc i, ScoreDoc j) {
                return order[i.doc] - order[j.doc];
            }

            public Comparable sortValue (ScoreDoc i) {
                return order[i.doc];
            }

            public int sortType(){
                return SortField.CUSTOM;
            }
        };
    }

    /**
     * Returns an array of term-positions in sorted order. The array is specific
     * for the given field in the given index and is cached. The getOrder keeps
     * track of versions, so it should be safe to call with different readers.
     * @param reader     the reader to use as basis for the order.
     * @param fieldname  the field name to use for ordering.
     * @return positions to the terms in the field in order.
     * @throws IOException if the field in the reader could not be sorted.
     */
    protected synchronized int[] getOrder(
            final IndexReader reader, String fieldname) throws IOException {
        fieldname = fieldname.intern();
        checkCacheConsistency(reader);

        int maxDoc = reader.maxDoc();
        log.trace("Checking cache for '" + fieldname + "'");
        if (orders.containsKey(fieldname)) {
            if (orders.get(fieldname).length == maxDoc) {
                log.debug("The cache for '" + fieldname + "' is up to date");
                return orders.get(fieldname);
            } else {
                orders.remove(fieldname);
            }
        }
        log.debug("Building new cache for field '" + fieldname + "'");
        Profiler profiler = new Profiler();
        // Build a list of pairs
        Pair[] sorted = getPairs(reader, fieldname);
        // Sort the list
        log.trace("Sorting positions for '" + fieldname + "'");
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
        log.trace("Converting positions for '" + fieldname
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

        orders.put(fieldname, positions);
        log.debug(String.format("Created cache for '%s' for %d documents in %s",
                                fieldname, maxDoc, profiler.getSpendTime()));
        return positions;
    }

    private synchronized void checkCacheConsistency(IndexReader reader) {
        String indexVersion = "Reader version: " + reader.getVersion()
                               + ", maxDoc: " + reader.maxDoc();
        if (this.indexVersion != null
            && this.indexVersion.equals(indexVersion)) {
            return;
        }
        orders.clear();
        this.indexVersion = indexVersion;
    }

    private void provideFeedback(Pair[] sorted) {
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

    @Override
    protected Comparable getComparable(String termtext) {
        throw new UnsupportedOperationException(
                "Not implemented as it should not be called");
    }

    /* Accessors */

    public String getLanguage() {
        return language;
    }
}

