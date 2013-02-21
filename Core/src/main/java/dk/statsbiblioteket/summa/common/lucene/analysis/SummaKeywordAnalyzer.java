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
import dk.statsbiblioteket.summa.common.strings.CharSequenceReader;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.reader.ReplaceFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.search.exposed.analysis.ConcatICUCollationAttributeFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/**
 * This KeywordAnalyzer strips off the _ character, that the QueryParser
 * substitutes with " " before wrapping a KeyWordAnalyzer.
 *
 * @see org.apache.lucene.analysis.core.KeywordAnalyzer
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, hal",
        comment = "This is a hellish hack that should be re-written from scratch (te)")
public class SummaKeywordAnalyzer extends Analyzer {
    public static final Map<String, String> RULES = RuleParser.parse("'_' > ' ';");

    private SummaStandardAnalyzer standard = new SummaStandardAnalyzer();
    private ReplaceFactory replaceFactory = new ReplaceFactory(RULES);

    private Collator collator = null;
    private ConcatICUCollationAttributeFactory factory = null;

    public SummaKeywordAnalyzer() {
        this(null);
    }

    public SummaKeywordAnalyzer(Collator collator) {
        if (collator != null) {
            factory = new ConcatICUCollationAttributeFactory(collator);
        }
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        if (collator == null) {
            return new TokenStreamComponents(new KeywordTokenizer(reader));
        }
        KeywordTokenizer tokenizer = new KeywordTokenizer(factory, reader, KeywordTokenizer.DEFAULT_BUFFER_SIZE);
        return new TokenStreamComponents(tokenizer, tokenizer);
    }

    @Override
    protected Reader initReader(String fieldName, Reader input) {
        Reader reader = replaceFactory.getReplacer(input);

        TokenStream ts;
        try {
            // TODO: What does this extra call do and can we leave it out?
            ts = standard.createComponents("ss", reader).getTokenStream();
            ts = standard.tokenStream("dummy", reader);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get tokenStream from SummaStandardAnalyzer", e);
        }

        StringBuffer buf = new StringBuffer();
        CharTermAttribute term = ts.getAttribute(CharTermAttribute.class);
        try {
            ts.reset();
            boolean first = true;
            while (ts.incrementToken()) {
                if (first) {
                    first = false;
                } else {
                    buf.append(" ");
                }
//                buf.append(term.termBuffer(), 0, term.termLength())
                buf.append(term.toString());
            }
            ts.end();
            ts.close();
        } catch (IOException e) {
            throw new RuntimeException("IOException when reading from TokenStream" ,e);
        }
        return new CharSequenceReader(buf);
    }
}




