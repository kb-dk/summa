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
package dk.statsbiblioteket.summa.common.lucene.index;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * An IndexChanger notifies listeners when the underlying Lucene index changes.
 * This is a classical implementation of the observer pattern.
 * @see IndexChangeListener
 * @see IndexChangeEvent
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface IndexChanger {
    /**
     * Add a listener to this observable. If a change happens to the Lucene
     * index, the listernes will be notified.
     * @param listener a listener for index events.
     */
    public void addListener(IndexChangeListener listener);

    /**
     * Remove a listener from this observable.
     * @param listener a listener for index events.
     */
    public void removeListener(IndexChangeListener listener);

}




