package dk.statsbiblioteket.summa.common.lucene.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.Token;

import java.io.IOException;

/**
 *
 */
public class TransliterationFilter extends TokenFilter {

    TokenFilter transliteratorImpl;

    public void close() throws IOException {
        transliteratorImpl.close();
    }

    public void reset() throws IOException {
        transliteratorImpl.reset();
    }

    public Token next(Token reusableToken) throws IOException {
        return transliteratorImpl.next(reusableToken);
    }

    /**
     * Construct a token stream filtering the given input.
     */
    protected TransliterationFilter(TokenStream input) {
        super(input);

        transliteratorImpl = new ASCIIFoldingFilter(input);
    }
}
