/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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

import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ExposedFieldComparatorSource;
import org.apache.lucene.search.FieldComparator;

import java.io.IOException;
import java.util.Locale;

/**
 * Low memory, fast sort, slow startup SortComparator that uses
 * https://issues.apache.org/jira/browse/LUCENE-2369 for the heavy lifting.
 * </p><p>
 * From LUCENE-2369:
 * 40GB index, 5 segments, 7.5M documents, 5.5M unique sort terms,
 * 87M terms total, sort locale da, top-20 displayed with fillFields=true.
 * Just opening the index without any Sort requires 140MB.<br/>
 * Standard sorter: -Xmx1800m, 26 seconds for first search.<br/>
 * Exposed sorter: -Xmx350m, 7 minutes for first search
 * (~4½ minutes for segment sorting, ~2½ minutes for merging).
 * </p><p>
 * Fully warmed searches, approximate mean:<br/>
 * 6.5M hits: standard 2500 ms, exposed 240 ms<br/>
 * 4.1M hits: standard 1600 ms, exposed 190 ms<br/>
 * 2.1M hits: standard 900 ms, exposed 90 ms<br/>
 * 1.2M hits: standard 500 ms, exposed 45 ms<br/>
 * 0.5M hits: standard 220 ms, exposed 40 ms<br/>
 * 0.1M hits: standard 80 ms, exposed 6 ms<br/>
 * 1.7K hits: standard 3 ms, exposed <1 ms<br/>
 * </p><p>
 * 2.5GB index, 4 segments, 420K documents, 240K unique sort terms,
 * 11M terms total, sort locale da, top-20 displayed with fillFields=true.
 * Just opening the index without any Sort requires 18MB.<br/>
 * Standard sorter: -Xmx120m, 2 seconds for first search<br/>
 * Exposed sorter: -Xmx50m, 14 seconds for first search
 * (9 seconds for segment sorting, 5 seconds for merging).
 * </p><p>
 * Fully warmed searches, approximate mean:<br/>
 * 420K hits: standard 170 ms, exposed 15 ms<br/>
 * 200K hits: standard 85 ms, exposed 9 ms<br/>
 * 100K hits: standard 50 ms, exposed 8 ms<br/>
 * 10K hits: standard 6 ms, exposed 0-1 ms<br/>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ExposedSortComparator extends ReusableSortComparator {
    private static final long serialVersionUID = 79546L;
    private static Log log = LogFactory.getLog(ExposedSortComparator.class);

    private String language;
    private ExposedFieldComparatorSource efcSource = null;

    public ExposedSortComparator(String language) {
        super(language);
        this.language = language;
        log.debug("Created comparator for language '" + language + "'");
    }

    @Override
    public FieldComparator newComparator(
        String fieldname, int numHits, int sortPos, boolean reversed)
                                                            throws IOException {
        log.debug(String.format("Creating comparator for fieldName=%s, "
                                + "numHits=%d, sortPos=%d, reversed=%b",
                                fieldname, numHits, sortPos, reversed));
        if (efcSource == null) {
            throw new IllegalStateException(
                "No reader defined. Call indexChanged(reader) before "
                + "requesting a new comparator");
        }
        Profiler profiler = new Profiler();
        FieldComparator comparator = efcSource.newComparator(
            fieldname, numHits, sortPos, reversed);
        log.debug(String.format(
            "Finished creating comparator for fieldName=%s,"
            + " numHits=%d, sortPos=%d, reversed=%b in %s",
            fieldname, numHits, sortPos, reversed, profiler.getSpendTime()));
        return comparator;
    }

    @Override
    public void indexChanged(IndexReader reader) {
        efcSource = new ExposedFieldComparatorSource(
            reader, new Locale(language));
        log.debug("IndexChanged called, caches flushed");
    }
}
