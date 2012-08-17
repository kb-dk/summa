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

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.reader.ReplaceFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordTokenizer;

import java.io.Reader;
import java.util.Map;

/**
 * This Analyzer wraps a StandardAnalyzer after stripping off typical separator
 * chars used in many ID schemes.
 * The list of removed chars is:
 * {'-', '_',':', '/' , '\'}
 *
 * 
 * @see org.apache.lucene.analysis.standard.StandardAnalyzer
 * @author Hans Lund, State and University Library - Aarhus, Denmark
 * @version $Id: SummaNumberAnalyzer.java,v 1.3 2007/10/04 13:28:17 te Exp $
 *
 */
// TODO: This passes "123 456" as "123 456". Wouldn't it be better to pass "123456"?
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class SummaNumberAnalyzer extends Analyzer {
//    private static final Log log = LogFactory.getLog(SummaNumberAnalyzer.class);

    public static final Map<String, String> RULES = RuleParser.parse(
            "'-' > '';"
            + "'_' > '';"
            + "':' > '';"
            + "'/' > '';"
            + "'\\' > ''"
        );
    private static final ReplaceFactory replaceFactory = new ReplaceFactory(RULES);

//    private static final char[] removable = new char[]{'-', '_',':', '/' , '\\'};

    // Thread local context used for the reusableTokenStream() method
/*    private static class TokenStreamContext {
        public final StringBuffer buf;
        public final StandardAnalyzer standardAnalyzer;
        public final CharSequenceReader seq;

        public TokenStreamContext() {
            buf = new StringBuffer();
            standardAnalyzer = new StandardAnalyzer(Version.LUCENE_40);
            seq = new CharSequenceReader(buf);
        }
    }
  */
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        return new TokenStreamComponents(new KeywordTokenizer(reader));
    }

    @Override
    protected Reader initReader(String fieldName, Reader reader) {
        return replaceFactory.getReplacer(reader);
    }

    /**
     * @see org.apache.lucene.analysis.Analyzer#tokenStream(String, java.io.Reader)
     *
     * @param fieldName name of the Indexfield.
     * @param reader  the reader containing the data.
     * @return  A StandardAnalyser tokenStream.
     */
/*    @Override
    public final TokenStream tokenStream(String fieldName, Reader reader){
        char c;
        int i;
        StringBuilder b = new StringBuilder();
        try {
            while ((i = reader.read()) != -1) {
                c = (char)i;
                boolean add = true;
                for (char aRemovable : removable) {
                   if (aRemovable == c){
                       add = false;
                       break;
                   }
                }
                if (add) {
                    b.append(c);
                }
            }
        } catch (IOException e) {
            log.error("", e);
        }
        return new StandardAnalyzer(Version.LUCENE_40).tokenStream(fieldName, new StringReader(b.toString()));
    }
  */
    /**
     * A version of {@link #tokenStream(String, java.io.Reader)} that doesn't
     * allocate any new objects
     *
     * @param fieldName
     * @param reader
     * @return a KeywordAnalyzer tokenStream
     */
/*    public final TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
        // FIXME: This implementation is basically a big hack

        TokenStreamContext ctx = (TokenStreamContext)getPreviousTokenStream();
        if (ctx == null) {
            // Create a new tokenStream and add it to the thread local storage
            ctx = new TokenStreamContext();
            setPreviousTokenStream(ctx);
        } else {
            ctx.buf.setLength(0); // Reset the StringBuffer
        }

        char c;
        int i;
        try {
            while ((i = reader.read()) != -1) {
                c = (char)i;
                boolean add = true;
                for (char aRemovable : removable) {
                   if (aRemovable == c){
                       add = false;
                       break;
                   }
                }
                if (add) {
                    ctx.buf.append(c);
                }
            }
        } catch (IOException e) {
            log.error("", e);
        }
        return ctx.standardAnalyzer.reusableTokenStream(fieldName, ctx.seq.reset(ctx.buf));
    }
*/
}
