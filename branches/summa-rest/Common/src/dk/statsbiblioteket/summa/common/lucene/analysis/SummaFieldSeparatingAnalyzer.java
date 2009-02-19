/* $Id: SummaFieldSeparatingAnalyzer.java,v 1.3 2007/10/04 13:28:17 te Exp $
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

import java.io.IOException;
import java.io.Reader;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * You can wrap any other analyzer within this Analyzer. This Analyzer moderates
 * the default appending behavior of Lucene analyzers to separate each fieldable
 * so that phrase searches can not match across field boundaries.
 *
 * Consider the following pseudo-code:
 * <pre>
 * doc.addFeild("author", "Hans Christian Andersen");
 * doc.addField("author", "Pedersen, Peter");
 * writer.addDoc(doc)
 * </pre>
 *
 * Using an analyzer that tokenize input, a query:
 * {@code author:"Christian Andersen Pedersen"}
 * will match the given document. Wrapping the analyzer in the RepeatAnalyzer
 * will change the behavior around the added boundaries so that no prase query
 * can match across multiple additions of fields to the document.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal",
        comment = "Method Javadoc needs updating")
public class SummaFieldSeparatingAnalyzer extends Analyzer {

    Analyzer _underlyingAnalyzer;

    /**
     * Makes an RepeatAnalyzer that wraps another analyzer
     *
     * @param analyzer this analyzer will be wrapped
     */
    public SummaFieldSeparatingAnalyzer(Analyzer analyzer){
        _underlyingAnalyzer = analyzer;
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader){ 
        return _underlyingAnalyzer.tokenStream(fieldName, reader);
    }

    @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader)
                                                            throws IOException {
        return _underlyingAnalyzer.reusableTokenStream(fieldName, reader);
    }

    // This method is where the magic happens. It says that there is a distance
    // of 100 between each fieldable instance. This prevents that phrase
    // searches can match
    @Override
    public int getPositionIncrementGap(String fieldName){
        return 100;
    }


}



