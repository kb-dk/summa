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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

/**
 * Lazy container with sorters for fields.
 */
public class SortFactory {
    private static final Logger log = Logger.getLogger(SortFactory.class);

    private Map<String, ReusableSortComparator> comparators =
            new HashMap<String, ReusableSortComparator>(10);
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
        lucene, exposed;
        public static COMPARATOR parse(String value) {
            if (value == null) {
                return DEFAULT_COMPARATOR;
            }
            if (value.equalsIgnoreCase(lucene.toString())) {
                return lucene;
            }
            if (value.equalsIgnoreCase(exposed.toString())) {
                return exposed;
            }
            return DEFAULT_COMPARATOR;
        }
    }

    /** The Default comparator. */
    public static final COMPARATOR DEFAULT_COMPARATOR = COMPARATOR.exposed;

    /** Default buffer size is 100MB. */
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
     *                     Currently not used by any comparator.
     * @param field        the field to perform sorting on.
     * @param sortLanguage the language for sorting.
     * @param comparators  a map of existing comparators for fields.
     */
    public SortFactory(COMPARATOR comparator, int buffer,
                       String field, String sortLanguage,
                      Map<String, ReusableSortComparator> comparators) {
        this.field = field;
        this.sortLanguage = sortLanguage;
        this.comparators = comparators;
        this.comparator = comparator;
        this.buffer = buffer;
    }

    /**
     * Return the sort for the field, given a language.
     * @param reverse True if the sort should be reversed.
     * @return The sort for the field. Returns a default sorter if sort language
     * is undefined or there is an error creating the {@link Sort} object.
     */
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

    /**
     * Create default sorter.
     * @param reverse Reverse the default sorter.
     * @return A default sorter.
     */
    private Sort makeDefaultSorters(boolean reverse) {
        normalSort = new Sort(new SortField(field, Locale.getDefault(), false));
        reverseSort = new Sort(new SortField(field, Locale.getDefault(), true));
        return reverse ? reverseSort : normalSort;
    }

    /**
     * Return the sort field.
     * @param reverse True if the sort field be reversed.
     * @return A {@link SortField} for the field, with the sort language locale,
     * if defined.
     */
    private SortField getSortField(boolean reverse) {
        if (comparator == COMPARATOR.lucene) {
            return new SortField(field, new Locale(sortLanguage), reverse);
        }
        return new SortField(field, getComparator(), reverse);
    }

    /**
     * Used to get the right comparator for the field, given a field and sort
     * language.                      ??
     * @return Field comparator source, which is a
     * {@link org.apache.lucene.search.FieldComparator} for a custom field
     * sorting.
     */
    private FieldComparatorSource getComparator() {
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
                    case exposed: {
                        comparators.put(
                                sortLanguage,
                                new ExposedComparator(sortLanguage));
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

    /**
     * Return the sort language, which defines the sort locale used by this
     * sort factory.
     * @return The sort language, which defines the sort locale used by this
     * sort factory.
     */
    public String getSortLanguage() {
        return sortLanguage;
    }

    /**
     * Must be called before any sort comparators can be returned and must be
     * called whenever the underlying index changes.
     * @param reader the new reader to use for sorting.
     */
    public void indexChanged(IndexReader reader) {
        for (Map.Entry<String, ReusableSortComparator> source:
            comparators.entrySet()) {
            source.getValue().indexChanged(reader);
        }
    }
}