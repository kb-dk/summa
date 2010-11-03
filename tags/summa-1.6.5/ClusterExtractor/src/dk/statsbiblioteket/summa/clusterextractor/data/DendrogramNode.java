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

import dk.statsbiblioteket.summa.clusterextractor.ClusterProvider;
import dk.statsbiblioteket.summa.clusterextractor.math.SparseVector;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.document.Field;

import java.util.HashSet;
import java.util.Set;

/**
 * A representation of a dendogram node; extends {@link Cluster}.
 * A {@link Dendrogram} is a tree (or forest) of centroids or cluster
 * representations. The dendogram node contains a CentroidVector and a
 * set of children. The node has no knowledge of its parent.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class DendrogramNode extends Cluster {
    /** {@link Set} of DendrogramNode children of this DendrogramNode. */
    private HashSet<DendrogramNode> children;

    /**
     * A {@link Field} for the cluster of this DendrogramNode.
     * The field has value 'cluster name of the cluster of this DendrogramNode'.
     * The name and properties is specified by the {@link ClusterProvider}
     * interface. The field can be added directly to a
     * {@link org.apache.lucene.document.Document}.
     */
    private transient Field clusterField = null;

    /**
     * Construct DendrogramNode from the given cluster (no children).
     * @param cluster Cluster of this DendrogramNode
     */
    public DendrogramNode(ClusterRepresentative cluster) {
        this(cluster.getName(), cluster.getCentroid());

    }

    /**
     * Construct DendrogramNode from the given cluster (no children).
     * The cluster is represented by name, centroid and # points in build.
     *
     * @param name           name of cluster
     * @param centroidVector centroid of cluster
     */
    public DendrogramNode(String name, SparseVector centroidVector) {
        super(name, centroidVector);
        this.children = new HashSet<DendrogramNode>();
    }

    /**
     * Add given child to this DendrogramNode.
     * @param child DendrogramNode child
     * @return true if given node was not already a child of this node
     */
    public boolean addChild(DendrogramNode child) {
        return children.add(child);
    }

    /**
     * Get Set of children of this node.
     * @return Set of child DendrogramNodes
     */
    public Set<DendrogramNode> getChildren() {
        return children;
    }

    /**
     * Get a {@link Field} for the cluster of this DendrogramNode.
     * The field has value 'cluster name of the cluster of this DendrogramNode'.
     * The name and properties is specified by the {@link ClusterProvider}
     * interface. The field can be added directly to a
     * {@link org.apache.lucene.document.Document}.
     * @return Field for the cluster of this DendrogramNode
     */
    public Field getClusterField() {
        if (clusterField==null && children.isEmpty()) {//this is a leaf node
            clusterField =
                    new Field(ClusterProvider.CLUSTER_FIELD_NAME,
                            this.getName(),
                            ClusterProvider.CLUSTER_FIELD_TYPE.getStore(),
                            ClusterProvider.CLUSTER_FIELD_TYPE.getIndex(),
                            ClusterProvider.CLUSTER_FIELD_TYPE.getVector());
        }
        return clusterField;
    }

    /**
     * Returns a textual representation of this DendrogramNode.
     * The representation includes the names of children, but it is not a
     * recursive representaton.
     * @return a string representation of this DendrogramNode
     */
    @Override
    public String toString() {
        String result = "DendrogramNode: " + super.toString();
        if (children.isEmpty()) {
            result = result + "\n(Leaf node.)";
        } else {
            result = result + "\nChildren:";
            for (DendrogramNode child: getChildren()) {
                result += " " + child.getName();
            }
        }
        return result;
    }
}




