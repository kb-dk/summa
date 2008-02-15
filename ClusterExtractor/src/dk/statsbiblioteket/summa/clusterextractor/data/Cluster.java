/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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

import dk.statsbiblioteket.summa.clusterextractor.math.SparseVector;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.Serializable;
import java.util.Map;

/**
 * A Cluster object represents a cluster with a name and a centroid vector.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class Cluster implements Serializable {

    /** Name of this cluster. */
    private String name;
    /** Centroid of this cluster as a {@link SparseVector}. */
    private SparseVector centroidVector;
    /** Number of points used to build the centroid of this cluster. */
    private int numberOfPointsInBuild;

    /**
     * Similarity threshold for this cluster.
     * I.e. how similar to the centroid should a given vector be to be
     * considered part of this cluster?
     */
    private double similarityThreshold;

    /** Expected size of this cluster. */
    private int expectedSize;

    /** Set of 'core documents' in this cluster (map from vectors to id's). */
    private transient Map<SparseVector, String> corePoints;

    /**
     * Construct a Cluster with the given name, centroid and # points in build.
     * @param name name of cluster
     * @param centroidVector centroid of cluster
     * @param numberOfPoints number of points used to calculate the centroid
     */
    public Cluster(String name, SparseVector centroidVector, int numberOfPoints) {
        this.name = name;
        this.centroidVector = centroidVector;
        this.numberOfPointsInBuild = numberOfPoints;
        this.corePoints = null;
    }

    /**
     * Get name of this Cluster.
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Set name of this Cluster.
     * @param name new name of this Cluster
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get centroid of this Cluster.
     * @return centroid vector
     */
    public SparseVector getCentroid() {
        return centroidVector;
    }

    /**
     * Get number of points used to build the centroid of this Cluster.
     * @return number of points used to build centroid
     */
    public int getNumberOfPointsInBuild() {
        return numberOfPointsInBuild;
    }

    /**
     * Get similarity threshold of this cluster.
     * I.e. how similar to the centroid should a given vector be to be
     * considered part of this cluster?
     * @return similarity threshold
     */
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    /**
     * Set similarity threshold of this cluster.
     * I.e. how similar to the centroid should a given vector be to be
     * considered part of this cluster?
     * @param similarityThreshold similarity threshold
     */
    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * Get expected size of this cluster.
     * @return expected size
     */
    public int getExpectedSize() {
        return expectedSize;
    }

    /**
     * Set expected size of this cluster.
     * @param expectedSize expected size
     */
    public void setExpectedSize(int expectedSize) {
        this.expectedSize = expectedSize;
    }

    /**
     * Decide whether this Cluster is equal to the argument Cluster.
     * @param o - the reference object with which to compare.
     * @return true if this object is the same as the argument; false otherwise
     */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof Cluster)) {
            return false;
        }
        Cluster other = (Cluster) o;
        return this.getCentroid().equals(other.getCentroid()) &&
                this.getName().equals(other.getName()) &&
                this.getSimilarityThreshold() == other.getSimilarityThreshold()
                && this.getExpectedSize() == other.getExpectedSize();
    }

    public int hashCode() {
        int result = this.getCentroid().hashCode();
        result = 83*result + this.getName().hashCode();
        result = 83*result + new Double(this.getSimilarityThreshold()).hashCode();
        result = 83*result + this.getExpectedSize();
        return result;
    }

    /**
     * Return a textual representation of this Cluster.
     * @return String representation of this Cluster
     */
    public String toString() {
        return this.getName() + "; Centroid: " + this.getCentroid().toString();
    }

    /**
     * Get the set of representatives or core points of this cluster.
     * @return map from vectors to id's
     */
    public Map<SparseVector, String> getCorePoints() {
        return corePoints;
    }

    /**
     * Set the set of representatives or core points of this cluster.
     * @param corePoints map from vectors to id's
     */
    public void setCorePoints(Map<SparseVector, String> corePoints) {
        this.corePoints = corePoints;
    }

    /* TODO: can we calculate 'within-cluster variation' based upon a small
     * TODO: set of core points, and do we need it?
     * Calculate 'within-cluster variation' of this cluster.
     * The within-cluster variation is a cluster quality measure, which should
     * be minimised to optimise clusters...
     * TODO: look up within-cluster variation formulas and test
     * @return within-cluster variation
    public double calculateWithinClusterVariation() {
        if (content == null) {
            return -1;
        }
        double sum = 0;
        for (SparseVector x: content.keySet()) {
            sum = sum + (1 - centroidVector.similarity(x));
        }
        return sum / content.size();
    }
     */

}
