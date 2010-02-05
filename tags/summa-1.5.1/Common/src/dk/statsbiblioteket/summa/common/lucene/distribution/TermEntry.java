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

