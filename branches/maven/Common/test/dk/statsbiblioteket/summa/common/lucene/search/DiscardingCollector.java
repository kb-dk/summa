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
package dk.statsbiblioteket.summa.common.lucene.search;

import org.apache.lucene.index.*;
import org.apache.lucene.search.*;

import java.io.*;

/**
 * HitCollector that simply disregards the result. Used to measure performance.
 */
public class DiscardingCollector extends Collector {
    public void collect(int i, float v) {
        // Do nothing
    }

    @Override
    public void setScorer(Scorer scorer) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void collect(int i) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setNextReader(IndexReader indexReader, int i)
            throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}




