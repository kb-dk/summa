package dk.statsbiblioteket.summa.common.lucene.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;

import java.util.Map;
import java.io.IOException;

/**
 *  A {@code TokenFilter} replacing only entire tokens, not substrings
 * inside tokens.
 * <p/>
 * One point of doing this could be to enforce string representations of
 * characters that might otherwise not be searchable.
 */
public class TokenReplaceFilter extends TokenFilter {

    public static final String DEFAULT_REPLACE_RULES =
            "'c++' > 'cplusplus';"
            + "'c#' > 'csharp';"
            + " 'c* algebra' > 'cstaralgebra';"
            + " 'c*-algebra > 'cstaralgebra'";

    protected Map<String,String> tokenMap;

    /**
     * Construct a token stream filtering the given input.
     */
    public TokenReplaceFilter(TokenStream input,
                              Map<String,String> tokenMap) {
        super(input);
        this.tokenMap = tokenMap;
    }

    /**
     * Create a token replacer with the {@link #DEFAULT_REPLACE_RULES}
     * @param input the token stream to read tokens from
     */
    public TokenReplaceFilter(TokenStream input) {
        this(input, DEFAULT_REPLACE_RULES, false);
    }

    public TokenReplaceFilter(TokenStream input, String rules) {
        this(input, rules, false);
    }

    public TokenReplaceFilter(TokenStream input,
                              String rules, boolean keepDefaultRules) {
        super(input);

        if (keepDefaultRules) {
            if (rules == null || "".equals(rules)) {
                rules = DEFAULT_REPLACE_RULES;
            } else {
                rules = rules + DEFAULT_REPLACE_RULES;
            }
        } else if (rules == null) {
            rules = DEFAULT_REPLACE_RULES;
        }

        this.tokenMap = RuleParser.parse(rules);
    }

    @Override
    public Token next(Token tok) throws IOException {
        tok = input.next(tok);

        if (tok == null) {
            return null;
        }

        String val = tokenMap.get(tok.term());

        if (val != null) {
            tok.setTermBuffer(val);
        }

        return tok;
    }



}
