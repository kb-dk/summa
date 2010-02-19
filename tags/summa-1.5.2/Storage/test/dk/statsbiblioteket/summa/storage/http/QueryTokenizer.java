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
package dk.statsbiblioteket.summa.storage.http;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Utility class to tokenize URI query parts like {@code arg1=foo&arg2=bar}
 * into a sequence of {@link QueryToken}s.
 * <p/>
 * For maximum efficiency you can reuse instances of {@code QueryTokenizer}s
 * without allocating new memory by calling {@link #reset(CharSequence)}.
 *
 * @author mke
 * @since Sep 10, 2009
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class QueryTokenizer implements Iterator<QueryToken> {
    private StringBuilder builder;
    private CharSequence seq;
    private int pos;

    /**
     * Create a new tokenizer on the given character sequence. Note that
     * character sequences can be both Strings, StringBuilders, and a host of
     * other native Java types.
     * <p/>
     * You may also pass {@code null} as an argument in which case
     * @param seq
     */
    public QueryTokenizer(CharSequence seq) {
        this.seq = seq;
        pos = 0;
        builder = new StringBuilder();
    }

    public QueryTokenizer reset() {
        pos = 0;
        builder.setLength(0);
        return this;
    }

    public QueryTokenizer reset(CharSequence seq) {
        this.seq = seq;
        pos = 0;
        builder.setLength(0);
        return this;
    }

    public boolean hasNext() {
        return seq != null && pos < seq.length();
    }

    public QueryToken next() {
        if (seq == null || pos >= seq.length()) {
            throw new NoSuchElementException();
        }

        // Clear the builder without freeing memory
        builder.setLength(0);

        QueryToken tok = new QueryToken("", "");
        boolean foundKey = false;
        char head = '\0';
        while (pos < seq.length()){
            head = seq.charAt(pos);
            if (head == '&') {
                pos++;
                break;
            } else if (head == '=' && !foundKey) {
                tok.setKey(builder.toString());
                builder.setLength(0);
                foundKey = true;
                pos++;
                continue;
            }
            builder.append(head);
            ++pos;
        }

        if (foundKey) {
            tok.setValue(builder.toString());
        } else {
            tok.setKey(builder.toString());
        }

        return tok;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        // We need to clone this tokenizer in order not to disrupt
        // any ongoing iteration
        StringBuilder result = new StringBuilder();
        QueryTokenizer toks = new QueryTokenizer(seq);
        while (toks.hasNext()) {
            result.append(toks.next().toString());
        }

        return result.toString();
    }
}

