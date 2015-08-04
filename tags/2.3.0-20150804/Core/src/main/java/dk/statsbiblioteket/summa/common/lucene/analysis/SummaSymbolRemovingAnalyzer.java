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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordTokenizer;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * The SummaSymbolRemovingAnalyzer is used to generate uniform sortable fields.
 * In effect all chars where Character.isLetter == false will be removed.
 * This analyzer wraps a SimpleAnalyzer
 *
 * @see org.apache.lucene.analysis.core.SimpleAnalyzer
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummaSymbolRemovingAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName, final Reader reader) {
        return new TokenStreamComponents(new KeywordTokenizer(new FilterReader(reader) {
            @Override
            public int read() throws IOException {
                int c;
                while ((c = reader.read()) != -1) {
                    if (Character.isLetter(c)) {
                        break;
                    }
                }
                return c;
            }
        }));
    }
}
