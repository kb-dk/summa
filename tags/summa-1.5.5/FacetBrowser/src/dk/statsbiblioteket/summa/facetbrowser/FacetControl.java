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
package dk.statsbiblioteket.summa.facetbrowser;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.facetbrowser.browse.Browser;
import dk.statsbiblioteket.summa.facetbrowser.build.Builder;
import dk.statsbiblioteket.summa.facetbrowser.core.FacetCore;

/**
 * Faceting is presentations of Tags grouped together in Facets. A Facet might
 * be "City" and a Tag might be "Copenhagen". The Tags are derived from
 * the result of a search in an index. A search for "H. C. Andersen" might
 * give a result like {@code
 * Author
   - H.C. Andersen (230)
   - Hans Christian Andersen (123)
 * Title
   - Den grimme ælling (12)
   - Kejserens nye klæder (3)
 * City
   - Odense (102)
   - Copenhagen (10)
 }
 * The exact presentation is up to the presentation layer.
 * </p><p>
 * Updating of the Facet structure will normally be part of indexing, in order
 * to keep the structure in sync with a search index such as Lucene. In case
 * of major clean-up or other position-changing events, a full rebuild of
 * the structure can be triggered.
 * </p><p>
 * FacetControl provides both updating and querying of Facets and Tags.
 * The design goal of Facets in Summa is to provide full faceting, using
 * best-effort calculations. This means that all documents from a search
 * will be used to calculate the Facet-Tag presentation, but that the
 * result is not guaranteed to be 100% correct in terms of tag-counts.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface FacetControl extends Browser, Builder {

}




