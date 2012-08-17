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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.CompositeReader;
import org.apache.lucene.index.IndexReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LuceneUtil {
    private static Log log = LogFactory.getLog(LuceneUtil.class);


    public static List<AtomicReader> gatherSubReaders(IndexReader root) {
        if (root instanceof AtomicReader) {
            return Arrays.asList((AtomicReader)root);
        }
        if (!(root instanceof CompositeReader)) {
            throw new UnsupportedOperationException(
                "Expected either AtomicReader or Composite reader. Unknown reader encountered: " + root.getClass());
        }
        List<AtomicReader> readers = new ArrayList<AtomicReader>();
        for (IndexReader ir: ((CompositeReader)root).getSequentialSubReaders()) {
            readers.addAll(gatherSubReaders(ir));
        }
        return readers;
    }
}
