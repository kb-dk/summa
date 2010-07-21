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
package dk.statsbiblioteket.summa.search.document;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.OpenBitSet;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * A reusable BitCollector for Lucene searches. After use, {@link #close} /must/
 * be called. This clears the collector and puts it back into the queue of
 * collectors. Failure to call close will result in starvation of collectors.
 * The close is called automatically by SummaSearcherImpl.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DocIDCollector extends Collector {
    private static Log log = LogFactory.getLog(DocIDCollector.class);

    private ArrayBlockingQueue<DocIDCollector> queue = null;
    /*
     * The Lucene OpenBitSet should be significantly faster than Java's BitSet:
* http://lucene.apache.org/java/2_4_0/api/org/apache/lucene/util/OpenBitSet.html
     */
    private OpenBitSet bits = new OpenBitSet(100000);
    private int docCount = 0;
    private int docBase = 0;

    /**
     * Constructs a collector and adds it to the given queue.
     * @param queue the queue that this collector is added to.
     */
    public DocIDCollector(ArrayBlockingQueue<DocIDCollector> queue) {
        this.queue = queue;
        while (true) {
            try {
                queue.put(this);
                break;
            } catch (InterruptedException e) {
                log.debug("Interrupted while putting DicIDCollector. Retrying");
            }
        }
    }

    /**
     * Constructs a collector. Normally the other constructor will be used, in
     * order to re-use the collector.
     * @see #DocIDCollector(ArrayBlockingQueue)
     */
    public DocIDCollector() {
    }

    @Override
    public void setScorer(Scorer scorer) throws IOException {
        // ignored
    }

    @Override
    public void collect(int doc) {
        bits.set(docBase + doc);
        docCount++;
    }

    @Override
    public void setNextReader(IndexReader indexReader, int i)
            throws IOException {
        this.docBase = i;
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
        return false;
    }

    /**
     * Clears the collector and returns it to the queue for reuse.
     * Important: This must be called after use.
     */
    public void close() {
        bits.clear(0, Integer.MAX_VALUE);
        docCount = 0;
        if (queue == null) {
            log.debug("No queue present, so the collector is not put back");
        } else {
            while (true) {
                try {
                    queue.put(this);
                    break;
                } catch (InterruptedException e) {
                    log.debug("Interrupted while putting "
                               + "DocIDCollector. Retrying");
                }
            }
        }
    }

    /**
     * @return the inner BitSet for the collector.
     *         This should only be queried outside of the DicIDCollector-class.
     */
    public OpenBitSet getBits() {
        return bits;
    }

    /**
     * @return the number of collected documents.
     */
    public int getDocCount() {
        return docCount;
    }

    @Override
    public String toString() {
        int MAX_BITS = 10;
        StringWriter sw = new StringWriter(300);
        sw.append("DocIDCollector ").append(Integer.toString(getDocCount()));
        sw.append("(");
        int foundCount = 0;
        int id = 0;
        while ((id = bits.nextSetBit(id))!= -1 && foundCount++ < MAX_BITS) {
            sw.append(Integer.toString(id++)).append(" ");
        }
        sw.append(")");
        return sw.toString();
    }
}




