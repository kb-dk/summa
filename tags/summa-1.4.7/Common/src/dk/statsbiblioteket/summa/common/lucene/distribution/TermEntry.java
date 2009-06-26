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

/**
 * Representation of a persistent term with stats.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermEntry implements Comparable<TermEntry> {
    //private static Log log = LogFactory.getLog(TermEntry.class);

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

    public TermEntry setTerm(String term) {
        this.term = term;
        return this;
    }

    public TermEntry setCount(int count) {
        this.count = count;
        return this;
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
