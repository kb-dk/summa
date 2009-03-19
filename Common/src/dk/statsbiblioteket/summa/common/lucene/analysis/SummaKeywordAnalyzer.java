/* $Id: SummaKeywordAnalyzer.java,v 1.3 2007/10/04 13:28:17 te Exp $
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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.Token;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.strings.CharSequenceReader;

/**
 * This KeywordAnalyzer strips off the _ character, that the Queryparser
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

    /**
     * Gets the tokenStream for the field named by fieldName.
     *
     * @param fieldName
     * @param reader
     * @return a KeywordAnalyzer tokenStream
     */
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader){
        StringBuffer buf = new StringBuffer();
        TokenStream ts =
                new SummaStandardAnalyzer().tokenStream(fieldName, reader);

        Token t = new Token();
        try {
            while ((t = ts.next(t)) != null){
                buf.append(t.termBuffer(), 0, t.termLength())
                    .append(' ');
            }

            // We have an extra whitespace at the end. Strip it
            buf.setLength(buf.length() == 0 ? 0 : buf.length() - 1);
        } catch (IOException e) {
            log.error("",e);
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

        TokenStream ts = ctx.summaStandardAnalyzer.reusableTokenStream(fieldName,
                                                                       reader);

        // FIXME: Here we are buffering the whole stream. Insane.
        Token t = ctx.t;
        try {
            while ((t = ts.next(t)) != null){
                ctx.buf.append(t.termBuffer(), 0, t.termLength())
                       .append(' ');
            }

            // We have an extra whitespace at the end. Strip it
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



