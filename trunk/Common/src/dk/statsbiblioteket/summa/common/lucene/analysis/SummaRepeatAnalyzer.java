/* $Id: SummaRepeatAnalyzer.java,v 1.3 2007/10/04 13:28:17 te Exp $
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
 * You can wrap any other analyzer within this Analyzer. This Analyzer moderates the default appendable
 * behavior of Lucene analyzers to be additative. This can be illustrated by this example:
 *
 * given the following pseudo-code
 * doc.addFeild("author", "Hans Christian Andersen"); doc.addField("author", "Pedersen, Peter");
 * writer.addDoc(doc)
 *
 * using a analyzer that tokenize input a query: author:"Christian Andersen Pedersen"
 * will match the given document. Wrapping a analyzer in the RepeatAnalyzer will change the behavior around
 * the added boundaries so that no prase query can match across multiple additions of fields to the document.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal",
        comment = "Method Javadoc needs updating")
public class SummaRepeatAnalyzer extends Analyzer {

    Analyzer _underlyingAnalyzer;

    /**
     * Makes an RepeatAnalyzer that wraps another analyzer
     *
     * @param analyzer this analyzer will be wrapped
     */
    public SummaRepeatAnalyzer(Analyzer analyzer){
        _underlyingAnalyzer = analyzer;
    }

    /**
     * @see org.apache.lucene.analysis.Analyzer#tokenStream(String, java.io.Reader) 
     * @param fieldName
     * @param reader
     * @return
     */
    public TokenStream tokenStream(String fieldName, Reader reader){ 
        return _underlyingAnalyzer.tokenStream(fieldName, reader);
    }

    public int getPositionIncrementGap(String fieldName){
        return 100;
    }


}



