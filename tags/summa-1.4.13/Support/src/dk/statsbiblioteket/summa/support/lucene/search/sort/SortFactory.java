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

/**
 * Lazy container with sorters for fields.
 */
public class SortFactory {
    private static final Logger log = Logger.getLogger(SortFactory.class);

    private Map<String, SortComparator> comparators =
            new HashMap<String, SortComparator>(10);
    protected static final Object comparatorSync = new Object();

    private String field;
    private String sortLanguage;
    private Sort normalSort;
    private Sort reverseSort;

    public SortFactory(String field, String sortLanguage,
                      Map<String, SortComparator> comparators) {
        this.field = field;
        this.sortLanguage = sortLanguage;
        this.comparators = comparators;
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
            SortField nField = new SortField(field, getComparator());
            normalSort = new Sort(nField);
            SortField rField = new SortField(field, getComparator(), true);
            reverseSort = new Sort(rField);
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

    /**
     * @return a
     */
    private SortComparator getComparator() {
        synchronized (comparatorSync) {
            if (!comparators.containsKey(sortLanguage)) {
                /* Language specified, so create localized sorters */
                log.debug(String.format(
                        "Creating localized comparators for field '%s' with "
                        + "language '%s'", field, sortLanguage));
                    comparators.put(sortLanguage,
                                   new LocalStaticSortComparator(sortLanguage));
            }
            return comparators.get(sortLanguage);
        }
    }

    /* Accessors */

    public String getSortLanguage() {
        return sortLanguage;
    }
}
