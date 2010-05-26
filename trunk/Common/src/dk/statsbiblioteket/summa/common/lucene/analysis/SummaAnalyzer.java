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
package dk.statsbiblioteket.summa.common.lucene.analysis;

import org.apache.lucene.analysis.*;

import java.io.Reader;
import java.io.IOException;
import java.util.LinkedList;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.reader.ReplaceReader;
import dk.statsbiblioteket.util.reader.ReplaceFactory;

/**
 * The SummaAnalyzer defines a configurable chain for tokenization.
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke, hal",
        comment = "Methods needs Javadoc")
public class SummaAnalyzer extends Analyzer {

    String transliterationRules;
    boolean keepDefaultTransliterations;

    String tokenRules;
    boolean keepDefaultTokenRules;
    boolean ignoreCase;

    private ReplaceFactory transliteratorFactory;
    private ReplaceFactory tokenReplacerFactory;

    /**
     * Encapsulation of a TokenStream and it data source (a Tokenizer)
     */
    private static class TokenStreamContext {

        /**
                 * The topmost tokenstream tokens should be read from.
                 */
        public TokenStream tokenStream;

        /**
         * The tokenSource is the bottom most Tokenizer in the chain of
         * tokenstreams. We need a handle to this to be able to reset the
         * underlying Reader
         */
        public LinkedList<ReplaceReader> filters =
                new LinkedList<ReplaceReader>();
    }

    /**
     * Makes a SummaAnalyzer.
     *
     * @param transliterationRules       the transliteration rules are parsed to
     *                                   a {@link RuleParser} and fed to a
     *                                   {@link ReplaceFactory}.
     * @param keepDefaultTransliterations if true the transliterationRules are
     *                                   added to the defined in
     *                                   {@link Rules#ALL_TRANSLITERATIONS}
     * @param tokenRules                 transliteration rules passed to a
     *                                   {@link RuleParser} and fed to a
     *                                   {@link ReplaceFactory}
     * @param keepDefaultTokenRules      if true the tokenRules are added to
     *                                   the default rules defined in
     *                                   {@link Rules#DEFAULT_REPLACE_RULES}
     * @param ignoreCase                 if true everything will be converted to
     *                                   lower case
     */
    public SummaAnalyzer(String transliterationRules,
                         boolean keepDefaultTransliterations,
                         String tokenRules,
                         boolean keepDefaultTokenRules,
                         boolean ignoreCase){
        super();
        this.transliterationRules = transliterationRules;
        this.keepDefaultTransliterations = keepDefaultTransliterations;
        this.tokenRules = tokenRules;
        this.keepDefaultTokenRules = keepDefaultTokenRules;
        this.ignoreCase = ignoreCase;

        transliteratorFactory =
                new ReplaceFactory(RuleParser.parse(RuleParser.sanitize(
                        transliterationRules,
                        keepDefaultTransliterations,
                        Rules.ALL_TRANSLITERATIONS)));
        tokenReplacerFactory =
                new ReplaceFactory(RuleParser.parse(RuleParser.sanitize(
                        tokenRules,
                        keepDefaultTokenRules,
                        Rules.DEFAULT_REPLACE_RULES)));
    }

    /**
     * The TokenStream returned is a TransliteratorTokenizer where input have
     * parsed through the TokenMasker.
     *
     * @see org.apache.lucene.analysis.Analyzer#tokenStream(String,java.io.Reader)
     * @param fieldName - name of the field
     * @param reader - containin the text
     * @return a TransliteratorTokenizer tokenStream filtered by a TokenMasker.
     */
    public TokenStream tokenStream(String fieldName, Reader reader) {

        TokenStreamContext ctx = prepareReusableTokenStream(fieldName,
                reader);
        return ctx.tokenStream;
    }

    private TokenStreamContext prepareReusableTokenStream (String fieldName,
                                                           Reader reader) {
        TokenStreamContext ctx = new TokenStreamContext();

        if (ignoreCase) {
            ctx.filters.add(new LowerCasingReader(reader));
            ctx.filters.add(tokenReplacerFactory.getReplacer(
                    ctx.filters.getLast()));
        } else {
            ctx.filters.add(tokenReplacerFactory.getReplacer(reader));
        }

        ctx.filters.add(transliteratorFactory.getReplacer(
                ctx.filters.getLast()));

        ctx.tokenStream = new WhitespaceTokenizer(ctx.filters.getLast());

        return ctx;
    }

    @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader)
            throws IOException {
        // This method fetches a stored *thread local* TokenStreamContext
        // this means that we have one unique token stream per thread

        TokenStreamContext ctx = (TokenStreamContext)getPreviousTokenStream();
        if (ctx == null) {
            // Create a new tokenStream and add it to the thread local storage
            ctx = prepareReusableTokenStream(fieldName, reader);
            setPreviousTokenStream(ctx);
        } else {
            // Reset all readers in the filter chain
            ReplaceReader prev = null;
            for (ReplaceReader r : ctx.filters) {
                if (prev == null) {
                    r.setSource(reader);
                } else {
                    r.setSource(prev);
                }

                prev = r;
            }
        }

        return ctx.tokenStream;
    }


}




