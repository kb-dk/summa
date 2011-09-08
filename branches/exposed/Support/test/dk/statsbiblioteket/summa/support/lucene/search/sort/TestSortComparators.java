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

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Helper class for testing SortComparator implementations.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TestSortComparators extends TestCase {
    private static Log log = LogFactory.getLog(TestSortComparators.class);

    static SortHelper.SortFactory getLuceneFactory(final String field) {
        log.warn("Due to deprecation of search time locale based sorting, the "
                 + "Lucene sort factory returns unicode order sort");
        return new SortHelper.SortFactory() {
            @Override
            Sort getSort(IndexReader reader) throws IOException {
                return new Sort(new SortField(field, SortField.Type.STRING));
            }
        };
    }

    static SortHelper.SortFactory getExposedFactory(final String field) {
        return new SortHelper.SortFactory() {
            IndexReader reader = null;
            @Override
            Sort getSort(IndexReader reader) throws IOException {
                ExposedComparator exposed = new ExposedComparator("da");
                exposed.indexChanged(reader);
                return new Sort(new SortField(field, exposed));
            }

            @Override
            void indexChanged(IndexReader reader) throws IOException {
                this.reader = reader;
            }
        };
    }

    public static String[] makeTerms(int count, int maxLength) {
        final String BASE = "abcdefghijklmnopqrstuvwxyzæøå 1234567890 "
                            + "ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ !%/()";
        Random random = new Random(87);
        String[] result = new String[count];
        StringBuilder sb = new StringBuilder(maxLength);
        for (int i = 0 ; i < count ; i++) {
            sb.setLength(0);
            int length = random.nextInt(maxLength + 1);
            for (int j = 0 ; j < length ; j++) {
                sb.append(BASE.charAt(random.nextInt(BASE.length())));
            }
            result[i] = sb.toString();
        }
        return result;
    }

    public void testCompareMemoryConsumption() throws Exception {
        int TERM_COUNT = 10000;
        int TERM_MAX_LENGTH = 20;
        final int SORT_BUFFER = 2 * 1024 * 1024;

        File index = SortHelper.createIndex(
                makeTerms(TERM_COUNT, TERM_MAX_LENGTH));
        System.gc();
        Thread.sleep(200);
        System.gc();
        Thread.sleep(200);
        long initialMem = Runtime.getRuntime().totalMemory()
                   - Runtime.getRuntime().freeMemory();
        log.info(String.format(
                "Measuring memory usage for different sorters. Term count = %d,"
                + " max term length = %d, sort buffer (for multipass) = %d KB. "
                + "Initial memory usage: %d KB",
                TERM_COUNT, TERM_MAX_LENGTH, SORT_BUFFER / 1024,
                initialMem / 1024));
        // TODO assert
        long lucene = SortHelper.performSortedSearch(
            index, "all:all", 10, getLuceneFactory(
                SortHelper.SORT_FIELD));
        long exposed = SortHelper.performSortedSearch(
            index, "all:all", 10, getExposedFactory(
                SortHelper.SORT_FIELD));
        System.out.println(String.format(
            "initial = %d KB, lucene overhead = %d KB, "
            + "exposed overhead = %d KB",
            initialMem / 1024,
            (lucene - initialMem) / 1024,
            (exposed - lucene) / 1024));
    }
}