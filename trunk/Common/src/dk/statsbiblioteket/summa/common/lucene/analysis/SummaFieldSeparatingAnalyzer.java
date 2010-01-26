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
 * doc.addField("author", "Hans Christian Andersen");
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
     * Makes an SummaFieldSeparatingAnalyzer that wraps another analyzer
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

    @Override
    public String toString() {
        return "SummaFieldSeparatingAnalyzer(" + _underlyingAnalyzer + ")";
    }
}




