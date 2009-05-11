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

import dk.statsbiblioteket.summa.common.index.IndexField;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexField;
import org.apache.log4j.Logger;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortComparator;

import java.util.HashMap;
import java.util.Map;

/**
 * Uses the search description to generate a pool of sorters. The pool is lazy,
 * so sorters are only created when requested. If the IndexDescription is
 * changed, a new SortPool should be constructed to reflect the change.
 */
public class SortPool {
    private static final Logger log = Logger.getLogger(SortPool.class);

    /**
     * Maintains cached structures and makes is cheap to construct new sorters.
     */
    private Map<String, SortFactory> sortFactories =
            new HashMap<String, SortFactory>(100);

    /**
     * Comparators does the heavy lifting when sorting. They are shared between
     * sortFactories.
     */
    private Map<String, SortComparator> comparators =
            new HashMap<String, SortComparator>(10);

    private boolean naturalOrder = false;

    /**
     * The constructor steps through all fields in the index descriptor and
     * stores the sort locale for each.
     * @param descriptor a description of the fields in the index.
     */
    public SortPool(LuceneIndexDescriptor descriptor) {
        log.debug("Creating lazy sort pool");
        long startTime = System.currentTimeMillis();
        for (Map.Entry<String, LuceneIndexField> entry:
                descriptor.getFields().entrySet()) {
                updateField(entry.getValue());
        }
        log.debug("Lazy sort pool finished creating in "
                  + (System.currentTimeMillis() - startTime) + "ms");
    }

    private void updateField(IndexField field) {
        if (field.getSortLocale() == null) {
            return;
        }
        if (!sortFactories.containsKey(field.getName())) {
            log.debug("Adding sort locale '" + field.getSortLocale()
                      + "' to Field '" + field.getName() + "'");
            sortFactories.put(field.getName(), new SortFactory(
                    field.getName(), field.getSortLocale(), comparators));
        } else {
            SortFactory oldFactory = sortFactories.get(field.getName());
            if (!oldFactory.getSortLanguage().equals(field.getSortLocale())) {
                log.warn("New sort locale '" + field.getSortLocale()
                         + "' overrides old sort locale '"
                         + oldFactory.getSortLanguage()
                         + "' for Field '" + field.getName() + "'");
            }
        }
    }

    /**
     * Creates a sort for the field or returns an existing one. It is the
     * responsibility of the caller to ensure that a given sort is only used
     * for by one thread at a time.
     * @param field   the field to get a - potentially localized - sort for.
     * @param reverse if true, the sort-order is reversed.
     * @return a Lucene Sort, ready for use.
     */
    public Sort getSort(String field, boolean reverse) {
        log.debug("getSort called for field '" + field
                  + "', reverse " + reverse);
        // TODO: Remove the following 3 lines to enable caching
//        if (field != null) {
//            return new Sort(new SortField(field, new Locale("da"), reverse));
//        }
        if (naturalOrder) {
            log.warn("Returning sort in natural order. This effectively ignores"
                     + " all localization on sort");
            return new Sort(field, reverse);
        }
        if (!sortFactories.containsKey(field)) {
            log.debug("No explicit sort specified for field '" + field
                      + "'. Returning standard sort");
            return new Sort(field, reverse);
        }
        try {
            return sortFactories.get(field).getSort(reverse);
        } catch (Exception e) {
            log.warn("Could not get sorter for '" + field
                     + "', Defaulting to standard sort, without caching", e);
            return new Sort(field, reverse);
        }
    }

    /**
     * Toggles whether to use the localized or the natural order. Setting this
     * to true effectively turns off all extra functionality for the SortPool.
     * Used mainly for testing and debugging.
     * @param useNaturalOrder true if the SortPool should use only the natural
     *                        order for sorting.
     */
    public void setNaturalOrder(boolean useNaturalOrder) {
        log.debug("useNaturalOrder: " + useNaturalOrder);
        naturalOrder = useNaturalOrder;
    }
}
