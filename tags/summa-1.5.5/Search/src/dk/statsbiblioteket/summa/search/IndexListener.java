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
package dk.statsbiblioteket.summa.search;

import java.io.File;

/**
 * Listens for changes to indexes.
 * @see {link IndexWatcher}.
 */
public interface IndexListener {
    /**
     * Called when the index has changed. This can be either an update or a
     * changed position of the index.
     * @param indexFolder the location of the index. This can be null, if no
     *                    index is available.
     */
    public void indexChanged(File indexFolder);
}




