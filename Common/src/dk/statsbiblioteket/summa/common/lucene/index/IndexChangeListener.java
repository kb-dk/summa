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
/**
 * Created: te 2007-08-28 11:23:44
 * CVS:     $Id: IndexChangeListener.java,v 1.2 2007/10/04 13:28:19 te Exp $
 */
package dk.statsbiblioteket.summa.common.lucene.index;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Listens to changes to a Lucene index.
 * @see IndexChanger, IndexChangeEvent
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface IndexChangeListener {
    /**
     * Called every time the index changes.
     * @param event a description of the change.
     */
    public void indexChanged(IndexChangeEvent event);
}




