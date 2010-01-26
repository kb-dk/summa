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
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.summa.facetbrowser.core.FacetCore;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResult;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.List;
import java.rmi.RemoteException;


/**
 * This is tightly connected to
 * {@link dk.statsbiblioteket.summa.common.search.Search}
 * as the browser should present the same query possibilities as the search.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface Browser extends FacetCore {
    /**
     * @return a list with the names of all mapped Facets.
     */
    public List<String> getFacetNames();

    /**
     * Generate a facet browser structure and return it in a form suitable for
     * further handling, such as merging, reduction or conversion to XML.
     * </p><p>
     * The parameters of this call are fairly low-level. It is recommended that
     * implementations of Browser also supply a query-based method for
     * requesting facet/tag information, as it eases experiments.
     * @param docIDs the ids for the documents to calculate Tags for.
     * @param facets a comma-separated list with the names and optionally
     *              max tag count of the wanted Facets. See {@link FacetRequest}
     *              for details. If null is specified, the default Facets
     *              are returned.
     * @return FacetResult with a serializable representation of the wanted
     *         facets.
     * @throws RemoteException if the request could not be handled.
     */
    public FacetResult getFacetMap(DocIDCollector docIDs, String facets) throws RemoteException;

    // TODO: Add index-lookup
}




