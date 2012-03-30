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
package dk.statsbiblioteket.summa.support.lucene;

import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A wrapped Lucene IndexReader that connects to a pool containing term
 * statistics extracted from a distributed environment. The reader does not
 * have a hard dependency on the stats and will fall back to the stats from
 * the current index if they are not available.
 * </p><p>
 * Using this reader ensures that ranking is coordinated between shards of the
 * whole conceptual index, making it possible for distribution to be
 * functionally equivalent with a single big index.
 * </p><p>
 * Note that it is acceptable for the TermProvider to return the docCount for
 * terms with only one instance as non-existing. This will result in a request
 * to the underlying IndexReader
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, hal")
public class SummaIndexReader extends FilterIndexReader {
    private final Log log = LogFactory.getLog(SummaIndexReader.class);

    // Don't close the provider on reader close
    private TermProvider termProvider;

    public SummaIndexReader(IndexReader indexReader,
                            TermProvider termProvider) {
        super(indexReader);
        this.termProvider = termProvider;
    }

    @Override
    public int numDocs(){
        int num = termProvider.numDocs();
        return num == -1 ? super.numDocs() : num;
    }

    /**
     * Note: The explain and the JavaDocs for Lucene states that numDocs is
     *       used for valculating idf. In reality, maxDoc is called.
     * @return numDocs + 1 from termProvider or maxDoc is no term stats are
     *         present.
     */
    @Override
    public int maxDoc() {
        int num = termProvider.numDocs() + 1;
        return num == 0 ? super.maxDoc() : num;
    }

    /**
     * Resolve the number of documents containing the term across the
     * distributed index. If the underlying term-provider is unable to provide
     * a document count, the wrapped Indexreader  is used for resolving.
     * @param t the term to get the document count for.
     * @return the document count for the term.
     * @throws IOException if an error happened during resolving.
     */
    @Override
    public int docFreq(Term t) throws IOException {
        int freq;
        try {
            freq = termProvider.docFreq(t.field() + ":" + t.text());
        } catch (Exception e) {
            try {
                log.warn(String.format(
                        "Unexpected exception getting docFreq for term '%s:%s'",
                        t.field(), t.text()), e);
            } catch (Exception e2) {
                log.warn("Exception while extracting field and text from term "
                         + "to create error message. The term was '" + t + "'",
                         e2);
            }
            return atLeastOne(super.docFreq(t));
        }
        return freq == -1 ? atLeastOne(super.docFreq(t)) : freq;
    }

    private int atLeastOne(int value) {
        return value <= 0 ? 1 : value;
    }
}

