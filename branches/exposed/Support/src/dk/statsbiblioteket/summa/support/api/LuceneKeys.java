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
package dk.statsbiblioteket.summa.support.api;

import dk.statsbiblioteket.summa.support.lucene.search.LuceneSearchNode;

/**
 * Interface collecting the search keys used for interacting with a
 * {@link LuceneSearchNode}.
 */
public interface LuceneKeys {

    /**
     * If present, normal search will be skipped an a MoreLikeThis-search will
     * be performed. The recordid is verbatim for the record (document) that
     * should be used as base for the MoreLikethis-functionality.
     * </p><p>
     * Optional. If no value is present, MoreLikeThis will not be active.
     * @see dk.statsbiblioteket.summa.search.api.document.DocumentKeys#SEARCH_START_INDEX
     * @see dk.statsbiblioteket.summa.search.api.document.DocumentKeys#SEARCH_MAX_RECORDS
     */
    String SEARCH_MORELIKETHIS_RECORDID =
            "search.document.lucene.morelikethis.recordid";
}

