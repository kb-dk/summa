/* $Id: DendrogramNode.java,v 1.2 2007/12/03 11:40:01 bam Exp $
 * $Revision: 1.2 $
 * $Date: 2007/12/03 11:40:01 $
 * $Author: bam $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.clusterextractor.data;

import dk.statsbiblioteket.summa.clusterextractor.ClusterProvider;
import dk.statsbiblioteket.summa.clusterextractor.math.CentroidVector;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.document.Field;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A representation of a dendogram node containing a {@link CentroidVector}.
 * A {@link Dendrogram} is a tree (or forest) of centroids or cluster
 * representations. The dendogram node contains a CentroidVector and a
 * set of children. The node has no knowledge of its parent.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class DendrogramNode implements Serializable {
    /** {@link Set} of DendrogramNode children of this DendrogramNode. */
    private HashSet<DendrogramNode> children;
    /** CentroidVector of this DendrogramNode. */
    private CentroidVector centroid;
    /**
     * A {@link Field} for the cluster of this DendrogramNode.
     * The field has value 'cluster name of the cluster of this DendrogramNode'.
     * The name and properties is specified by the {@link ClusterProvider}
     * interface. The field can be added directly to a
     * {@link org.apache.lucene.document.Document}.
     */
    private transient Field clusterField = null;

    /**
     * Construct DendrogramNode with given centroid and no children.
     * @param centroid CentroidVector of this DendroramNode
     */
    public DendrogramNode(CentroidVector centroid) {
        this.centroid = centroid;
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
     * Get CentroidVector of the cluster of this DendrogramNode.
     * @return CentroidVector of this DendrogramNode
     */
    public CentroidVector getCentroid() {
        return centroid;
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
                            centroid.getClusterName(),
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
        String result = "DendrogramNode: " + getCentroid().toString();
        if (children.isEmpty()) {
            result = result + "\n(Leaf node.)";
        } else {
            result = result + "\nChildren:";
            for (DendrogramNode child: getChildren()) {
                result += " " + child.getCentroid().getClusterName();
            }
        }
        return result;
    }
}
