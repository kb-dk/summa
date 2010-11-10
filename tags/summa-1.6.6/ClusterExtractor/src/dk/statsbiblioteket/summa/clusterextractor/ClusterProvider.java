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
package dk.statsbiblioteket.summa.clusterextractor;

import org.apache.lucene.document.Document;
import dk.statsbiblioteket.summa.common.lucene.index.FieldType;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * ClusterProvider provides clusters for given records.
 *
 * The enrich method takes a Lucene Document parameter. If any appropriate
 * clusters can be found, the document is enriched and returned (if no
 * clusters are found, the document is simply returned without alterations).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public interface ClusterProvider extends Configurable {
    /** The String key to the dendogram path property. */
    public static final String DENDROGRAM_PATH_KEY = "clusterprovider.dendrogrampath";

    /** Cluster field name. */
    public static final String CLUSTER_FIELD_NAME = "cluster";
    /** Cluster field type. */
    public static final FieldType CLUSTER_FIELD_TYPE = FieldType.keyWord;
    /** Cluster field alias; danish. */
    public static final String CLUSTER_FIELD_ALIAS_DANISH = "klynge";

    /**
     * Enrich given document with clusters.
     * If this document can be determined to belong to any clusters,
     * enrich the document with these cluster.
     * @param document the document to be enriched
     * @return the enriched document
     */
    public Document enrich(Document document);

    /**
     * Reload data structure(s) from disc.
     */
    public void reload();

    //TODO: register merger with provider or?
}




