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

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * ClusterExtractorWorkflow.
 * <p>
 * The work flow looks something like this:
 * <ul>
 * <li> For each index machine:
 *      <ul>
 *      <li> run the ClusterBuilder.buildCentroids method;
 *      <li> copy the new local centroid data structure to a central location.
 *      </ul>
 * <li> Central: Call the ClusterMerger.mergeCentroidSets method.
 * <li> For each index machine:
 *      <ul>
 *      <li> copy the new full centroid datastructure to this machine;
 *      <li> call the local ClusterProvider update method.
 *      </ul>
 * </ul>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class ClusterExtractorWorkflow {
}




