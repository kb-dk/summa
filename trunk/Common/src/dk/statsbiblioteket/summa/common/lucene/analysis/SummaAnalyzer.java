/* $Id: SummaAnalyzer.java,v 1.3 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:17 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.common.lucene.analysis;

import org.apache.lucene.analysis.*;

import java.io.Reader;
import java.io.IOException;
import java.util.List;
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
     *                                   a {@link TransliterationFilter}.
     * @param keepDefaultTransliterations if true the transliterationRules are
     *                                   added to the defined in
     *                                   {@link TransliterationFilter#ALL_TRANSLITERATIONS}
     * @param tokenRules                 transliteration rules passed to a
     *                                   {@link TokenReplaceFilter}
     * @param keepDefaultTokenRules      if true the tokenRules are added to
     *                                   the default rules defined in
     *                                   {@link TokenReplaceFilter#DEFAULT_REPLACE_RULES}
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
            ctx.filters.add(new StringReplaceReader(ctx.filters.getLast(),
                                                    tokenRules,
                                                    keepDefaultTokenRules));
        } else {
            ctx.filters.add(new StringReplaceReader(reader,
                                                    tokenRules,
                                                    keepDefaultTokenRules));
        }

        ctx.filters.add(new TransliteratingReader(ctx.filters.getLast(),
                                                  transliterationRules,
                                                  keepDefaultTransliterations));

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



