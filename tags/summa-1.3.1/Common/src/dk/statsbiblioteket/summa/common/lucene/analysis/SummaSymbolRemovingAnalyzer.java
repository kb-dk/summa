/* $Id: SummaSymbolRemovingAnalyzer.java,v 1.3 2007/10/04 13:28:17 te Exp $
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

import org.apache.commons.logging.Log;       import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Analyzer;

import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.strings.CharSequenceReader;

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
            simpleAnalyzer = new SimpleAnalyzer();
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
        return new SimpleAnalyzer().tokenStream(fieldName,
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



