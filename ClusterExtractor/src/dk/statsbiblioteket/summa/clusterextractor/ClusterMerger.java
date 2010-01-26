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

import dk.statsbiblioteket.summa.clusterextractor.data.ClusterSet;
import dk.statsbiblioteket.summa.clusterextractor.data.Dendrogram;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * ClusterMerger is responsible for merging centroid sets.
 *
 * This interface defines an upload method, a merge method and a get method
 * that should be implemented to be a ClusterMerger. The upload method is meant
 * to be called by local builders and provide new material. The merge methods
 * is meant to merge the already uploaded material and push the merged data to
 * the local providers. The get method is meant to return the latest version of
 * the merged dendrogram.
 *
 * A number of names to merger configuration values are also defined.
 *
 * TODO: how do we get the workflow working?
 * TODO: who tells the merger that it's time to merge?
 * TODO: should the merger know all the providers?
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public interface ClusterMerger extends Configurable {
    public static final String CLUSTER_SETS_PATH_KEY = "clustermerger.clustersetspath";
    /** The String key to the dendogram path property. */
    public static final String DENDROGRAM_PATH_KEY = "clustermerger.dendrogrampath";

    /** The String key to the join centroids similarity threshold. */
    public static final String JOIN_SIMILARITY_THRESHOLD_KEY = "clustermerger.joinSimilarityThreshold";
    /** The String key to the join similarity threshold for centroids with the same name. */
    public static final String JOIN_SIMILARITY_THRESHOLD_SAME_NAME_KEY = "clustermerger.joinSimilarityThresholdSameName";

    /**
     * Merge all known centroid sets to one dendrogram and push to local providers.
     */
    public void mergeCentroidSets();
}




