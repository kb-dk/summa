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

import com.ibm.icu.text.Collator;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.reader.ReplaceFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.collation.ICUCollationAttributeFactory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import java.io.Reader;

/**
 * The SummaAnalyzer defines a configurable chain for tokenization.
 *
 * If a Collator is provided, the analyzer generated collation keys with the original String embedded.
 * The original String can be extracted by {@link #getOriginalString}.
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

    private Collator collator = null;
    private ICUCollationAttributeFactory factory = null;

    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        //return new TokenStreamComponents(new WhitespaceTokenizer(Version.LUCENE_40, reader));
        if (collator == null) {
            return new TokenStreamComponents(new WhitespaceTokenizer(Version.LUCENE_40, reader));
        }
        KeywordTokenizer tokenizer = new KeywordTokenizer(factory, reader, KeywordTokenizer.DEFAULT_BUFFER_SIZE);
        return new TokenStreamComponents(tokenizer, tokenizer);
    }

    @Override
    protected Reader initReader(String field, Reader reader) {
        return wrap(reader);
    }

    public SummaAnalyzer(String transliterationRules, boolean keepDefaultTransliterations, String tokenRules,
                         boolean keepDefaultTokenRules, boolean ignoreCase){
        this(transliterationRules, keepDefaultTransliterations, tokenRules, keepDefaultTokenRules, ignoreCase, null);
    }
    /**
     * Makes a SummaAnalyzer.
     *
     * @param transliterationRules       the transliteration rules are parsed to a {@link RuleParser} and fed to a
     *                                   {@link ReplaceFactory}.
     * @param keepDefaultTransliterations if true the transliterationRules are added to the ones defined in
     *                                   {@link Rules#ALL_TRANSLITERATIONS}
     * @param tokenRules                 transliteration rules passed to a {@link RuleParser} and fed to a
     *                                   {@link ReplaceFactory}
     * @param keepDefaultTokenRules      if true the tokenRules are added to the default rules defined in
     *                                   {@link Rules#DEFAULT_REPLACE_RULES}
     * @param ignoreCase                 if true everything will be converted to lower case
     */
    public SummaAnalyzer(String transliterationRules, boolean keepDefaultTransliterations, String tokenRules,
                         boolean keepDefaultTokenRules, boolean ignoreCase, Collator collator){
        super();
        this.transliterationRules = transliterationRules;
        this.keepDefaultTransliterations = keepDefaultTransliterations;
        this.tokenRules = tokenRules;
        this.keepDefaultTokenRules = keepDefaultTokenRules;
        this.ignoreCase = ignoreCase;
        this.collator = collator;
        if (collator != null) {
            this.factory = new ICUCollationAttributeFactory(collator);
        }

        transliteratorFactory = new ReplaceFactory(RuleParser.parse(RuleParser.sanitize(
            transliterationRules, keepDefaultTransliterations, Rules.ALL_TRANSLITERATIONS)));
        tokenReplacerFactory = new ReplaceFactory(RuleParser.parse(RuleParser.sanitize(
            tokenRules, keepDefaultTokenRules, Rules.DEFAULT_REPLACE_RULES)));
    }

    private Reader wrap(Reader reader) {
        if (ignoreCase) {
            reader = new LowerCasingReader(reader);
        }
        reader = tokenReplacerFactory.getReplacer(reader);
        reader = transliteratorFactory.getReplacer(reader);
        return reader;
    }

    // Delegation to {@link SummaICUCollatedTermAttributeImpl#getOriginalString}.
    public static BytesRef getOriginalString(final BytesRef concat, BytesRef reuse) {
        return SummaICUCollatedTermAttributeImpl.getOriginalString(concat, reuse);
    }
}
