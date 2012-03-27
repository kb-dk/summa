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
package dk.statsbiblioteket.summa.facetbrowser.core;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;
import java.io.IOException;

/**
 * The core is responsible for loading existing Facet-structures and for
 * handling Facet setup in the form of configuration.
 * </p><p>
 * @see dk.statsbiblioteket.summa.facetbrowser.Structure for Configuration parameters.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface FacetCore extends Configurable {
    /**
     * The folder containing all persistent facet-information.
     */
    String FACET_FOLDER = "facet";

    /**
     * Open a Facet structure at the given location. If there is no existing
     * structure, .
     * @param directory the location of the data.
     * @throws IOException if the data could not be read from the file system.
     */
    public void open(File directory) throws IOException;

    /**
     * Closes any connections to underlying persistent data and clears
     * structures in memory.
     * </p><p>
     * Note: This does not synchronize content in memory to storage.
     * @throws IOException if opened persistent files could not be closed.
     */
    public void close() throws IOException;
}




