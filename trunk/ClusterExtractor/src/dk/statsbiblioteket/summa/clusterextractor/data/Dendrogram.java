/* $Id: Dendrogram.java,v 1.3 2007/12/03 11:40:01 bam Exp $
 * $Revision: 1.3 $
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

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * The Dendrogram is a tree (or forest) of centroids or cluster representations.
 * The Dendrogram is build of {@link DendrogramNode}s.
 * Each leaf node in the Dendrogram represents a centroid of a cluster
 * extracted from the current index.
 * Each internal node (or root node) represents a 'combination centroid', i.e.
 * a centroid which is created by joining all of it's child centroid nodes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class Dendrogram implements Serializable {
    /** {@link Set} of root DendrogramNodes of this Dendrogram. */
    private HashSet<DendrogramNode> roots;

    /**
     * Construct empty Dendrogram.
     */
    public Dendrogram() {
        this.roots = new HashSet<DendrogramNode>();
    }

    /**
     * Construct Dendrogram with given root node.
     * @param root1 root DendrogramNode
     */
    public Dendrogram(DendrogramNode root1) {
        this();
        this.roots.add(root1);
    }

    /**
     * Get the set of roots of this Dendrogram.
     * @return root set
     */
    public Set<DendrogramNode> getRoots() {
        return roots;
    }

    /**
     * Add given root DendrogramNode to root set of this Dendrogram.
     * @param root root DendrogramNode
     * @return true if the set did not already contain the specified root
     */
    public boolean addRoot(DendrogramNode root) {
        return roots.add(root);
    }

    /**
     * Returns a textual representation of the roots of this Dendrogram.
     * @return a string representation of this Dendrogram
     */
    @Override
    public String toString() {
        String result = "Dendrogram:\n";
        for (DendrogramNode root: getRoots()) {
            result += root.toString();
        }
        return result;
    }
}
