package dk.statsbiblioteket.summa.storage.http;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Utility class to tokenize
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

    public QueryTokenizer(CharSequence seq) {
        this.seq = seq;
        pos = 0;
        builder = new StringBuilder();
    }

    public QueryTokenizer reset(CharSequence seq) {
        this.seq = seq;
        pos = 0;
        builder.setLength(0);
        return this;
    }

    public boolean hasNext() {
        return pos < seq.length();
    }

    public QueryToken next() {
        if (pos >= seq.length()) {
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
