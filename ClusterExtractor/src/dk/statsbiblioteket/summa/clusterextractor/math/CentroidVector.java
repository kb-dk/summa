/* $Id: CentroidVector.java,v 1.8 2007/12/04 10:26:43 bam Exp $
 * $Revision: 1.8 $
 * $Date: 2007/12/04 10:26:43 $
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
package dk.statsbiblioteket.summa.clusterextractor.math;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.Map;

/**
 * CentroidVector is a SparseVectorMapImpl with a name and a number of points.
 * It is possible to add a diameter to a centroid vector.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class CentroidVector extends SparseVectorMapImpl {

    /** Name of the cluster, which this is a centroid for. */
    private String clusterName;
    /** Number of points in the cluster, which this is a centroid for. */
    private int numberOfPointsInBuild;

    /** Is a diameter defined for the cluster, which this is a centroid for? */
    private boolean diameterDefined = false;
    /** Diameter for the cluster, which this is a centroid for. */
    private double diameter;

    /** Is a similarity threshold defined for this centroid vector? */
    private boolean similarityThresholdDefined = false;
    /** Similarity threshold for the cluster, which this is a centroid for. */
    private double similarityThreshold;

    /** Expected size of cluster with this centroid. */
    private int expectedSize;

    /**
     * Construct a CentroidVector with this clusterName, numberOfPoints and coordinates.
     * @param clusterName name of cluster with this centroid
     * @param numberOfPoints number of points used to calculate this centroid
     * @param coordinates Map from String to Number with coordinates
     */
    public CentroidVector(String clusterName, int numberOfPoints,
                          Map<String, Number> coordinates) {
        super(coordinates);
        this.clusterName = clusterName;
        this.numberOfPointsInBuild = numberOfPoints;
    }

    /**
     * Get the name of the cluster with this centroid vector.
     * @return cluster name
     */
    public String getClusterName() {
        return clusterName;
    }

    /**
     * Get the number of points used to calculate this centroid.
     * @return number of points used to calculate this centroid
     */
    public int getNumberOfPointsInBuild() {
        return numberOfPointsInBuild;
    }

    /**
     * Is a diameter defined for the cluster with this centroid vector?.
     * @return true if a diameter is defined
     */
    public boolean isDiameterDefined() {
        return diameterDefined;
    }

    /**
     * Get the diameter defined for the cluster with this centroid vector.
     * @return diameter
     */
    public double getDiameter() {
        return diameter;
    }

    /**
     * Set the diameter for the cluster with this centroid vector.
     * @param diameter diameter
     */
    public void setDiameter(double diameter) {
        this.diameter = diameter;
        diameterDefined = true;
    }

    /**
     * Is a similarity threshold defined for this centroid vector?.
     * @return true if a similarity threshold is defined
     */
    public boolean isSimilarityThresholdDefined() {
        return similarityThresholdDefined;
    }

    /**
     * Get the similarity threshold defined for the cluster with this centroid.
     * @return similarity threshold
     */
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    /**
     * Set the similarity threshold for the cluster with this centroid vector.
     * @param threshold similarity threshold
     */
    public void setSimilarityThreshold(double threshold) {
        this.similarityThreshold = threshold;
        similarityThresholdDefined = true;
    }

    /**
     * Decide whether this CentroidVector is equal to the argument CentroidVector.
     * @param o - the reference object with which to compare.
     * @return true if this object is the same as the argument; false otherwise
     */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof SparseVector) || !super.equals(o)) {
            return false;
        }
        CentroidVector otherCentroid = (CentroidVector) o;
        return !(!(this.clusterName.equals(otherCentroid.getClusterName())) ||
                !(this.numberOfPointsInBuild == otherCentroid.getNumberOfPointsInBuild())) &&
                !(this.diameterDefined != otherCentroid.isDiameterDefined() ||
                (this.diameterDefined && otherCentroid.isDiameterDefined() &&
                        this.diameter != otherCentroid.getDiameter())) &&
                !(this.similarityThresholdDefined != otherCentroid.isSimilarityThresholdDefined() ||
                (this.similarityThresholdDefined && otherCentroid.isSimilarityThresholdDefined() &&
                        this.similarityThreshold != otherCentroid.getSimilarityThreshold())) &&
                this.expectedSize == otherCentroid.getExpectedSize();
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 83*result + this.clusterName.hashCode();
        result = 83*result + this.numberOfPointsInBuild;
        result = 83*result + new Double(this.diameter).hashCode();
        result = 83*result + new Double(this.similarityThreshold).hashCode();
        result = 83*result + this.expectedSize;
        return result;
    }

    /**
     * Return a string representation of this CentroidVector.
     * @return String representation of this CentroidVector
     */
    public String toString() {
        return clusterName + ": " + super.toString();
    }

    /**
     * Set expected size of cluster with this centroid.
     * @param expectedSize expected size of cluster
     */
    public void setExpectedSize(int expectedSize) {
        this.expectedSize = expectedSize;
    }

    /**
     * Get expected size of cluster with this centroid.
     * @return expected size of cluster
     */
    public int getExpectedSize() {
        return expectedSize;
    }

    /**
     * Set name of cluster with this centroid.
     * @param name name of cluster
     */
    public void setName(String name) {
        this.clusterName = name;
    }
}
