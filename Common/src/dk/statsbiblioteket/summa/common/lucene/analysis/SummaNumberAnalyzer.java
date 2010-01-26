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
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.strings.CharSequenceReader;

/**
 * This Analyzer wraps a StandardAnalyzer after stripping off typical seprator
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
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class SummaNumberAnalyzer extends Analyzer {


    private static final Log log = LogFactory.getLog(SummaNumberAnalyzer.class);

    private static final char[] removable = new char[]{'-', '_',':',
                                                       '/' , '\\'};

    // Thread local context used for the reusableTokenStream() method
    private static class TokenStreamContext {
        public final StringBuffer buf;
        public final StandardAnalyzer standardAnalyzer;
        public final CharSequenceReader seq;

        public TokenStreamContext() {
            buf = new StringBuffer();
            standardAnalyzer = new StandardAnalyzer();
            seq = new CharSequenceReader(buf);
        }
    }

    /**
     * @see org.apache.lucene.analysis.Analyzer#tokenStream(String, java.io.Reader)
     *
     * @param fieldName name of the Indexfield.
     * @param reader  the reader containing the data.
     * @return  A StandardAnalyser tokenStream.
     */
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader){
        char c;
        int i;
        StringBuffer b = new StringBuffer();
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
        return new StandardAnalyzer(
                new String[]{}).tokenStream(fieldName,
                                            new StringReader(b.toString()));
    }

    /**
     * A version of {@link #tokenStream(String, java.io.Reader)} that doesn't
     * allocate any new objects
     *
     * @param fieldName
     * @param reader
     * @return a KeywordAnalyzer tokenStream
     */
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
        return ctx.standardAnalyzer.reusableTokenStream(fieldName,
                                                        ctx.seq.reset(ctx.buf));
    }

}




