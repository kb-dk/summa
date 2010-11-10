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

import org.apache.commons.logging.Log;       import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Analyzer;

import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.strings.CharSequenceReader;
import org.apache.lucene.util.Version;

/**
 * The SummaSymbolRemovingAnalyzer is used to generate uniform sortable fields.
 * In effect all chars where Character.isLetter == false will be removed.
 * This analyzer wraps a SimpleAnalyzer
 *
 * @see org.apache.lucene.analysis.SimpleAnalyzer
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal",
        comment="This functionality is not that of an Analyzer, " +
                "but a TokenFilter")
public class SummaSymbolRemovingAnalyzer extends Analyzer {

    private static final Log log = LogFactory.getLog(SummaSymbolRemovingAnalyzer.class);

    private static class TokenStreamContext {
        public final SimpleAnalyzer simpleAnalyzer;
        public final StringBuffer buf;
        public final CharSequenceReader seq;

        public TokenStreamContext() {
            simpleAnalyzer = new SimpleAnalyzer(Version.LUCENE_30);
            buf = new StringBuffer();
            seq = new CharSequenceReader(buf);
        }
    }

    /**
     * The underlying char stream from the reader is filtered so that only
     * letters survive before it is passed to a SimpleAnalyzer and the
     * tokenStream from here is returned.
     *
     * @see org.apache.lucene.analysis.SimpleAnalyzer#tokenStream(String, java.io.Reader)
     * @see org.apache.lucene.analysis.Analyzer
     * @param fieldName the name of the field
     * @param reader the provided reader containing the text.
     * @return a tokenStream with one token containin only lowercase letters.
     */
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader){
        char c;
        int i;
        StringBuffer b = new StringBuffer(200);
        try {
            while ((i = reader.read()) != -1) {
                c = (char)i;
                 if (Character.isLetter(c)){
                     b.append(c);
                 }
            }
        } catch (IOException e) {
            log.error("Error reading data for token stream: " + e.getMessage(),
                      e);
        }
        return new SimpleAnalyzer(Version.LUCENE_30).tokenStream(fieldName,
                                                new StringReader(b.toString()));
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

        char c;
        int i;
        while ((i = reader.read()) != -1) {
            c = (char)i;
            if (Character.isLetter(c)){
                ctx.buf.append(c);
            }
        }
        
        return ctx.simpleAnalyzer.reusableTokenStream(fieldName,
                                                      ctx.seq.reset(ctx.buf));
    }
}




