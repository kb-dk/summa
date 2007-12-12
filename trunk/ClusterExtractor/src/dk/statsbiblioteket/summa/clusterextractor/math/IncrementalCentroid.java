/* $Id: IncrementalCentroid.java,v 1.7 2007/12/04 10:26:43 bam Exp $
 * $Revision: 1.7 $
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
import dk.statsbiblioteket.summa.clusterextractor.data.Cluster;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * IncrementalCentroid builds a centroid of a set of points (a cluster) incrementally.
 * All points are added one by one. When all points have been added,
 * the centroid can be calculated and returned as a SparseVector.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class IncrementalCentroid {
    private static final int EXPECTEDCOORDINATECOUNT = 50000;
    private static final Log log =
            LogFactory.getLog(IncrementalCentroid.class);

    /** The number of non-zero dimensions which the centroid is reduced to in optimise. */
    private int maxNonZeroDimensionsDuringReduce = Integer.MAX_VALUE;
    /** The number off points added between each optimise. */
    private int optimiseEveryXPoints = Integer.MAX_VALUE;
    /** Should the new points be normalised before added to the centroid? */
    private boolean normaliseWhenAdding = false;

    /** Name of the cluster, which this is a centroid for. */
    private String name = null;
    /** Number of points so far in the cluster, which this is a centroid for. */
    private int numberOfPoints = 0;
    /** Map from named dimensions to sums of coordinates from all added points. */
    private Map<String, Number> coordinates =
            new HashMap<String, Number>(EXPECTEDCOORDINATECOUNT);

    /**
     * Construct incremental centroid of a cluster with the given name.
     * @param name name of cluster
     */
    public IncrementalCentroid(String name) {
        this.name = name;
    }

    /**
     * Get the name of the cluster around this centroid.
     * @return name of cluster
     */
    public String getName() {
        return name;
    }

    /**
     * Set the number of dimensions, which the centroid will be cut down to by optimise.
     * @param maxNonZeroDimensionsDuringReduce number of dimensions to cut down to
     */
    public void setMaxSizeAfterOptimise(int maxNonZeroDimensionsDuringReduce) {
        this.maxNonZeroDimensionsDuringReduce = maxNonZeroDimensionsDuringReduce;
    }
    /**
     * Set the frequency of the optimise operation.
     * @param optimiseEveryXPoints frequency of optimise operation
     */
    public void setOptimiseEveryXPoints(int optimiseEveryXPoints) {
        this.optimiseEveryXPoints = optimiseEveryXPoints;
    }

    /**
     * Set 'normalise when adding boolean'.
     * @param normaliseWhenAdding normalise when adding boolean
     */
    public void setNormaliseWhenAdding(boolean normaliseWhenAdding) {
        this.normaliseWhenAdding = normaliseWhenAdding;
    }

    /**
     * Add the point specified by the given vector to the cluster around this centroid.
     * Stopwords are assumed removed.
     * Dimension names are assumed to be lower case.
     * If 'normaliseWhenAdding', the point is normalised before adding.
     * @param vec a sparse vector specifying the point
     */
    public void addPoint(SparseVector vec) {
        if (vec==null) {return;}

        if (numberOfPoints != 0 &&
                numberOfPoints%optimiseEveryXPoints == 0) {optimise();}

        if (normaliseWhenAdding) {vec.normalise();}

        Map<String, Number> vecCoordinates = vec.getCoordinates();

        for (Map.Entry<String, Number> entry: vecCoordinates.entrySet()) {
            String dimName = entry.getKey();
            double value = 0;
            if (coordinates.containsKey(dimName)) {
                value = coordinates.get(dimName).doubleValue();
            }
            coordinates.put(dimName, value + entry.getValue().doubleValue());
        }

        numberOfPoints++;
    }

    /**
     * Optimise the incremental centroid.
     * Optimise removes all entries with value 'only' one;
     * if we do not normalise when adding (this is one reason for different results).
     * Optimise cuts the number of entries to 'maxNonZeroDimensionsDuringReduce'.
     * Optimise keeps the entries with the highest value.
     */
    private void optimise() {
        double tooSmall = 1;
        if (normaliseWhenAdding) {tooSmall = 0;}

        Map<String, Number> keeperCoordinates =
                new HashMap<String, Number>(EXPECTEDCOORDINATECOUNT);
        List<Map.Entry<String, Number>> entryList =
                new ArrayList<Map.Entry<String, Number>>(coordinates.entrySet());
        //sort ascending!
        Collections.sort(entryList, new CoordinateComparator());

        log.trace("optimise start: coordinates.size()="+coordinates.size());
        for (int index = entryList.size()-1; index>=0; index--) {
            Map.Entry<String, Number> entry = entryList.get(index);
            if (index>entryList.size()-maxNonZeroDimensionsDuringReduce &&
                    entry.getValue().doubleValue()>tooSmall) {
                keeperCoordinates.put(entry.getKey(), entry.getValue());
            }
        }

        coordinates = keeperCoordinates;
        log.trace("optimise end: coordinates.size()="+coordinates.size());
    }

    /**
     * Get this centroid as a SparseVector.
     * @return CentroidVector
     */
    public Cluster getCluster() {
        for (Map.Entry<String, Number> entry : coordinates.entrySet()) {
            double sum = entry.getValue().doubleValue();
            entry.setValue(sum / numberOfPoints);
        }
        return new Cluster(name, new SparseVectorMapImpl(coordinates), numberOfPoints);
    }

    /**
     * Cut off this centroid as specified and return it as a SparseVector.
     * Cut off to at most maxSize entries each of at least minValue.
     * If normalise, normalise the centroid vector after cutting.
     * Note: If the centroid is not cut "along the way",
     * this method becomes very expensive.
     * Note also that minValue should be very different depending on
     * whether the points were cut before adding - and well depending
     * on the added points...
     * @param maxSize max number of entries
     * @param minValue min value of entry
     * @param normalise normalise if true
     * @return CentroidVector
     */
    public Cluster getCutCentroidCluster(int maxSize, double minValue,
                                             boolean normalise) {
        int denominator = numberOfPoints;
        if (normaliseWhenAdding) {denominator = 1;}

        Map<String, Number> keeperCoordinates =
                new HashMap<String, Number>(maxSize);
        List<Map.Entry<String, Number>> entryList =
               new ArrayList<Map.Entry<String, Number>>(coordinates.entrySet());
        //sort ascending!
        Collections.sort(entryList, new CoordinateComparator());

        //Cut off using maxSize and minValue
        //Calculate norm while cutting
        double sumOfSquares = 0;
        for (int index = entryList.size()-1; index>-1 &&
                keeperCoordinates.size()<maxSize; index--) {
            Map.Entry<String, Number> entry = entryList.get(index);
            double sum = entry.getValue().doubleValue();
            double value = sum / denominator;
            if (value>=minValue) {
                keeperCoordinates.put(entry.getKey(), value);
                sumOfSquares = sumOfSquares + (value*value);
            }
        }

        if (normalise) {
            double norm = Math.sqrt(sumOfSquares);
            for (Map.Entry<String, Number> entry: keeperCoordinates.entrySet()) {
                keeperCoordinates.put(entry.getKey(), entry.getValue().doubleValue()/norm);
            }
        }
        return new Cluster(name, new SparseVectorMapImpl(keeperCoordinates), numberOfPoints);
    }

    /**
     * Cut off this centroid as specified and return it as a SparseVector.
     * This is the simple version of getCutCentroidVector.
     * Cut off to at most maxSize non-zero entries.
     * @param maxSize maximum number of non-zero entries
     * @return this incremental centroid as a centroid vector cut to at
     *         most maxSize non-zero entries
     */
    public Cluster getCutCentroidCluster(int maxSize) {

        Map<String, Number> keeperCoordinates =
                new HashMap<String, Number>(maxSize);
        List<Map.Entry<String, Number>> entryList =
               new ArrayList<Map.Entry<String, Number>>(coordinates.entrySet());
        //sort descending!
        Collections.sort(entryList, Collections.reverseOrder(new CoordinateComparator()));

        //Cut off using maxSize
        for (int index = 0; index<entryList.size() &&
                keeperCoordinates.size()<maxSize; index++) {
            Map.Entry<String, Number> entry = entryList.get(index);
            keeperCoordinates.put(entry.getKey(), entry.getValue());
        }

        return new Cluster(name, new SparseVectorMapImpl(keeperCoordinates), numberOfPoints);
    }

}
