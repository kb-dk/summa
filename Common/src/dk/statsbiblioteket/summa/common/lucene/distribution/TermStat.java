/* $Id:$
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
package dk.statsbiblioteket.summa.common.lucene.distribution;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.pool.SortedPool;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Persistens structure for TermStats from an index.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStat {
    private static Log log = LogFactory.getLog(TermStat.class);

    /**
     * If true, the terms are memory based. If false, the content is stored on
     * storage during operation.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_MEMORYBASED =
            "common.distribution.termstat.memorybased";
    public static final boolean DEFAULT_MEMORYBASED = false;

    private SortedPool<TermEntry> termCounts;
    private boolean memoryBased = DEFAULT_MEMORYBASED;

    public TermStat(Configuration conf) {
        memoryBased = conf.getBoolean(CONF_MEMORYBASED, memoryBased);
        //noinspection DuplicateStringLiteralInspection
        log.debug(String.format("Constructed %s-based TermStat",
                                memoryBased ? "memory" : "storage"));
    }

    public class TermEntry implements Comparable<TermEntry> {
        private String term;
        private int count;

        public TermEntry(String term, int count) {
            if (term == null || "".equals(term)) {
                throw new IllegalArgumentException(String.format(
                        "Term must be defined, but it was '%s'", term));
            }
            this.term = term;
            this.count = count;
        }

        public int compareTo(TermEntry o) {
            return term.compareTo(o.getTerm());
        }

        public String getTerm() {
            return term;
        }

        public int getCount() {
            return count;
        }

        /**
         * Absorb the other entry into this by adding the counts.
         * @param other the term entry to absorb into this.
         */
        public void absorb(TermEntry other) {
            if (!term.equals(other.getTerm())) {
                throw new IllegalArgumentException(String.format(
                        "The term must match. This term was '%s', other "
                        + "term was '%s'", term, other.getTerm()));
            }
            count += other.getCount();
        }
    }
}
