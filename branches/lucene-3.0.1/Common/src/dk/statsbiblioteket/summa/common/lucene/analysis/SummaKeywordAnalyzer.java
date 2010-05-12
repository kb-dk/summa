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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.Token;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Reader;
import java.io.IOException;
import java.text.*;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.strings.CharSequenceReader;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttributeImpl;

/**
 * This KeywordAnalyzer strips off the _ character, that the QueryParser
 * substitutes with " " before wrapping a KeyWordAnalyzer.
 *
 * @see org.apache.lucene.analysis.KeywordAnalyzer
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class SummaKeywordAnalyzer extends Analyzer {


    private static final Log log = LogFactory.getLog(SummaKeywordAnalyzer.class);

    // Thread local context used for the reusableTokenStream() method
    private static class TokenStreamContext {
        public final SummaStandardAnalyzer summaStandardAnalyzer;
        public final KeywordAnalyzer keywordAnalyzer;
        public final StringBuffer buf;
        public final CharSequenceReader seq;
        public final Token t;

        public TokenStreamContext() {
            summaStandardAnalyzer = new SummaStandardAnalyzer();
            keywordAnalyzer = new KeywordAnalyzer();
            buf = new StringBuffer();
            seq = new CharSequenceReader(buf);
            t = new Token();
        }
    }

    public SummaKeywordAnalyzer() {
        super();
        log.debug("Creating SummaKeywordAnalyzer");
    }

    /**
     * Gets the tokenStream for the field named by fieldName.
     *
     * @param fieldName
     * @param reader
     * @return a KeywordAnalyzer tokenStream
     */
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        StringBuffer buf = new StringBuffer();
        TokenStream ts =
                     new SummaStandardAnalyzer().tokenStream(fieldName, reader);

        TermAttribute term = ts.getAttribute(TermAttribute.class);
        try {
            ts.reset();

            while(ts.incrementToken()) {
                buf.append(term.termBuffer(), 0, term.termLength())
                    .append(' ');
            }
            ts.end();
            ts.close();
            // We have an extra whitespace at the end. Strip it
            buf.setLength(buf.length() == 0 ? 0 : buf.length() - 1);
        } catch (IOException e) {
            log.error("IOException when reading from TokenStream in "
                      + "SummaKeyWordAnalyzer" ,e);
        }
        return new KeywordAnalyzer().tokenStream(fieldName,
                                                 new CharSequenceReader(buf));
    }

    // Version of this.tokenSteam() that does not allocate any new objects
    @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader)
                                                            throws IOException {
        // FIXME: This implementation is basically a big hack

        TokenStreamContext ctx = (TokenStreamContext)getPreviousTokenStream();
        if (ctx == null) {
            // Create a new tokenStream and add it to the thread local storage
            ctx = new TokenStreamContext();
            setPreviousTokenStream(ctx);
        } else {
            ctx.buf.setLength(0); // Reset the StringBuffer
        }

        TokenStream ts =
               ctx.summaStandardAnalyzer.reusableTokenStream(fieldName, reader);
        ts.reset();

        // FIXME: Here we are buffering the whole stream. Insane.
        TermAttribute term = ts.getAttribute(TermAttribute.class);

        try {
            while (ts.incrementToken()){
                ctx.buf.append(term.termBuffer(), 0, term.termLength())
                       .append(' ');
            }
            // We have an extra whitespace at the end. Strip it
            ts.end();
            ts.close();
            ctx.buf.setLength(ctx.buf.length() == 0 ? 0 : ctx.buf.length() - 1);
        } catch (IOException e) {
            log.error("Error reading next token from TokenStream: "
                      + e.getMessage(), e);
        }

        return ctx.keywordAnalyzer.reusableTokenStream(fieldName,
                                                       ctx.seq.reset(ctx.buf));
    }

    /*public static void main(String[] args) {
        Analyzer a = new SummaKeywordAnalyzer();
        
        try {
            a.reusableTokenStream("field", new StringReader("foobar"));
            a.reusableTokenStream("field", new StringReader(""));
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }

    }*/
}




