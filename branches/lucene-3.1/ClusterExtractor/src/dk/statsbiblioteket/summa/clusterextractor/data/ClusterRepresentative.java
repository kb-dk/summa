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
package dk.statsbiblioteket.summa.clusterextractor.data;

import dk.statsbiblioteket.summa.clusterextractor.math.SparseVector;

import java.io.Serializable;

/**
 * ClusterRepresentative represents a cluster and defines a getCentroid method.
 */
public interface ClusterRepresentative extends Serializable {
    /**
     * Get name of this Cluster.
     * @return name
     */
    public String getName();
    /**
     * Get centroid of this ClusterRepresentative.
     * @return centroid vector
     */
    public SparseVector getCentroid();
}




