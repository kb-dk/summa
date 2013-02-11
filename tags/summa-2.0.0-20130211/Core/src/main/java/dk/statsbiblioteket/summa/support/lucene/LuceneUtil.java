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

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LuceneUtil {


    /**
     * Flattens the given reader down to AtomicReaders.
     * A simple wrapper for {@link org.apache.lucene.index.IndexReader#leaves()}.
     * @param reader the IndexReader to flatten.
     * @return a list of AtomicReaders
     */
    public static List<AtomicReader> gatherSubReaders(IndexReader reader) {
        List<AtomicReader> readers = new ArrayList<AtomicReader>();
        for (AtomicReaderContext context: reader.leaves()) {
            readers.add(context.reader());
        }
        return readers;
    }
}
