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

import java.io.IOException;

/**
 * Provides distributed term statistics.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface TermProvider {
    /**
     * @return the total number of documents is the distributes index.
     */
    int numDocs();

    /**
     * The sum of the document frequencies from all the index shards. 
     * @param term the term to look up. This will normally be
     *        {@code field:value}.
     * @return the document frequency for the term or -1 if it could not be
     *         resolved.
     * @throws java.io.IOException if an error happened during resolving.
     */
    int docFreq(String term) throws IOException;
}

