/* $Id$
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
package dk.statsbiblioteket.summa.support.lucene.search.sort;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortComparator;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

/**
 * Lazy container with sorters for fields.
 */
public class SortFactory {
    private static final Logger log = Logger.getLogger(SortFactory.class);

    private Map<String, SortComparator> comparators =
            new HashMap<String, SortComparator>(10);
    protected static final Object comparatorSync = new Object();

    /**
     * The different back-ends for comparing Strings.<br />
     * lucene: The build-in Comparator. Loads all terms for the given field into
     *         RAM and creates Collator-keys for them.<br />
     *         Pros: Fast startup-time, efficient on re-open.<br />
     *         Cons: Consumes a lot of memory, so-so fast on actual sort.<br />
     * localstatic: Uses an optimized Collator and creates an array with
     *         sort-order for the terms in the given field.<br />
     *         Pros: Fast startup, best actual sort-performance,<br />
     *         Cons: Temporarily consumes a lot of memory at startup.<br />
     * multipass: Uses an optimized collator and creates a structure with
     *         sort-order for the terms in the given field.<br />
     *         Pros: Customizable memory-usage at the cost of startup time,
     *               faster than build-in sort in actual sort-performance.<br/ >
     *         Cons: Long startup-time if low memory-usage is requested.
     */
    public static enum COMPARATOR {
        lucene, localstatic, multipass;
        public static COMPARATOR parse(String value) {
            if (value == null) {
                return DEFAULT_COMPARATOR;
            }
            if (value.toLowerCase().equals(lucene.toString())) {
                return lucene;
            }
            if (value.toLowerCase().equals(localstatic.toString())) {
                return localstatic;
            }
            if (value.toLowerCase().equals(multipass.toString())) {
                return multipass;
            }
            return DEFAULT_COMPARATOR;
        }
    }

    public static final COMPARATOR DEFAULT_COMPARATOR = COMPARATOR.localstatic;
    public static final int DEFAULT_BUFFER = 100 * 1024 * 1024; // 100MB

    private String field;
    private String sortLanguage;
    private Sort normalSort;
    private Sort reverseSort;
    private COMPARATOR comparator;
    private int buffer;

    /**
     * Create a SortFactory with the given parameters. Note that the absence of
     * a sortLanguage will result in a lucene default sorter, capable of
     * auto-detecting field type (String, integer, float...).
     * @param comparator   the String-comparator implementation to use.
     * @param buffer       comparator-specific buffer-size.
     *                     Currently used by {@link MultipassSortComparator}.
     * @param field        the field to perform sorting on.
     * @param sortLanguage the language for sorting.
     * @param comparators  a map of existing comparators for fields.
     */
    public SortFactory(COMPARATOR comparator, int buffer,
                       String field, String sortLanguage,
                      Map<String, SortComparator> comparators) {
        this.field = field;
        this.sortLanguage = sortLanguage;
        this.comparators = comparators;
        this.comparator = comparator;
        this.buffer = buffer;
    }

    public synchronized Sort getSort(boolean reverse) {
        log.trace("getSort for field '" + field + "' with language '"
                  + sortLanguage + "' called");
        if (normalSort != null) {
            /* Already created, so just return */
            return reverse ? reverseSort : normalSort;
        }
        if (sortLanguage == null || "".equals(sortLanguage)) {
            /* No language, so just create default sorter */
            log.debug("No sort language. Creating non-localized sorts for "
                      + "field '" + field + "'");
            return makeDefaultSorters(reverse);
        }
        try {
            log.debug(String.format(
                    "Creating sorters for field '%s' with language '%s'",
                    field, sortLanguage));
            normalSort = new Sort(getSortField(false));
            reverseSort = new Sort(getSortField(true));
        } catch (Exception e) {
            log.error("Could not create comparator for language code '"
                      + sortLanguage + "'. Defaulting to basic sort");
            return makeDefaultSorters(reverse);
        }
        return reverse ? reverseSort : normalSort;
    }

    private Sort makeDefaultSorters(boolean reverse) {
        normalSort = new Sort(field, false);
        reverseSort = new Sort(field, true);
        return reverse ? reverseSort : normalSort;
    }

    private SortField getSortField(boolean reverse) {
        if (comparator == COMPARATOR.lucene) {
            return new SortField(field, new Locale(sortLanguage), reverse);
        }
        return new SortField(field, getComparator(), reverse);
    }

    private SortComparator getComparator() {
        synchronized (comparatorSync) {
            if (!comparators.containsKey(sortLanguage)) {
                /* Language specified, so create localized sorters */
                log.debug(String.format(
                        "Creating localized comparators for field '%s' with "
                        + "language '%s'", field, sortLanguage));
                switch (comparator) {
                    case lucene: {
                        throw new IllegalStateException(
                                "Lucene sorters should be constructed by "
                                + "getSortField");
                    }
                    case localstatic: {
                        comparators.put(
                                sortLanguage,
                                new LocalStaticSortComparator(sortLanguage));
                        break;
                    }
                    case multipass: {
                        comparators.put(
                                sortLanguage,
                                new MultipassSortComparator(
                                        sortLanguage, buffer));
                        break;
                    }
                    default: {
                        throw new IllegalStateException(
                                "Unknown compatator " + comparator);
                    }
                }
            }
            return comparators.get(sortLanguage);
        }
    }

    /* Accessors */

    public String getSortLanguage() {
        return sortLanguage;
    }
}
