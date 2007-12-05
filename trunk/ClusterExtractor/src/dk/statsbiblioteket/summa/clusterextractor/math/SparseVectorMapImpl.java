/* $Id: SparseVectorMapImpl.java,v 1.7 2007/12/04 10:26:43 bam Exp $
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * SparseVectorMapImpl implements {@link SparseVector} using a {@link HashMap}.
 * I.e. the coordinates are saved in a map from named dimensions (Strings) to
 * numbers ({@link Number}).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class SparseVectorMapImpl extends AbstractSparseVector {
    protected static final int EXPECTEDCOORDINATECOUNT = 20;
    protected static final Log log = LogFactory.getLog(SparseVectorMapImpl.class);

    /** Map from named dimensions to non-zero coordinates. */
    protected Map<String, Number> coordinates;

    // Lazily initialized, cached hashCode
    private volatile int hashCode = 0;

    /**
     * Constructor for SparseVectorMapImpl.
     * Stopwords are assumed removed, and dimension names are assumed lower case!
     * I.e. stopwords are not removed and names not changed by this constructor.
     * @param entries map from named dimensions to non-zero coordinates
     */
    public SparseVectorMapImpl(Map<String, Number> entries) {
        init();
        if (entries!=null) {
            this.coordinates.putAll(entries);
        }
    }
    /** Initialise private fields. */
    protected void init() {
        super.init();
        this.coordinates = new HashMap<String, Number>(EXPECTEDCOORDINATECOUNT);
    }

    /**
     * Get the similarity between this and the specified sparse vector.
     * The similarity measure used by SparseVectorMapImpl is the cosine
     * similarity measure, i.e. the cosine of the angle between the vectors.
     *
     * @param o specified sparse vector
     * @return the similarity between this and the specified vector
     */
    public double similarity(SparseVector o) {
        if (o.nonZeroEntries()<this.nonZeroEntries()) {
            return o.similarity(this);
        }
        return this.cosineToAngle(o);
    }

    /**
     * Get a map from named dimensions to non-zero coordinates.
     * @return map from named dimensions to non-zero coordinates
     */
    public Map<String, Number> getCoordinates() {
        return coordinates;
    }

    /**
     * Get the squared norm of this vector.
     * @return the squared norm of this vector
     */
    public double normSquared() {
        if (normalised) {return 1;}

        if (!sumOfSquaresSet) {
            if (coordinates ==null) {return 0;}

            double sum = 0;
            Iterator<Map.Entry<String, Number>> entrySetIterator =
                    coordinates.entrySet().iterator();
            while (entrySetIterator.hasNext()) {
                Map.Entry<String, Number> entry = entrySetIterator.next();
                double value = entry.getValue().doubleValue();
                if (value==0) {
                    entrySetIterator.remove();
                } else {
                    sum = sum + (value*value);
                }
            }
            sumOfSquares = sum;
            sumOfSquaresSet = true;
        }

        return sumOfSquares;
    }

    /**
     * Get the angle between this and the specified sparse vector.
     * @param o specified sparse vector
     * @return angle between this and the specified vector
     */
    public double angle(SparseVector o) {
        if (o.nonZeroEntries()<this.nonZeroEntries()) {
            return o.angle(this);
        }
        return Math.acos(this.cosineToAngle(o));
    }
    /**
     * Get the cosine to the angle between this and the specified vector.
     * @param o specified sparse vector
     * @return cosine to the angle between this and the specified vector
     */
    protected double cosineToAngle(SparseVector o) {
        if (o==null || this.coordinates ==null) {return -1;}

        double dotProduct = this.dotProduct(o);
        double denominator = this.norm() * o.norm();
        if (denominator==0) {return -1;}

        //rounding to avoid later NaN scenarios
        //(cosine to an angle must be between -1 and 1)
        double cosineToAngle = dotProduct / denominator;
        if (cosineToAngle>1) {
            cosineToAngle=1;
        }
        if (cosineToAngle<-1) {
            cosineToAngle=-1;
        }
        return cosineToAngle;
    }

    /**
     * Get the dotproduct between this and the specified sparse vector.
     * @param o specified sparse vector
     * @return the dotproduct between this and the specified vector
     */
    protected double dotProduct(SparseVector o) {
        double dotProduct = 0;
        if (sumOfSquaresSet) {
            for (Map.Entry<String, Number> entry: coordinates.entrySet()) {
                double valueThis = entry.getValue().doubleValue();
                double valueSpec = o.getValue(entry.getKey());
                dotProduct = dotProduct + (valueThis * valueSpec);
            }

        } else {
            double sum = 0;
            Iterator<Map.Entry<String, Number>> entrySetIterator =
                    coordinates.entrySet().iterator();
            while (entrySetIterator.hasNext()) {
                Map.Entry<String, Number> entry = entrySetIterator.next();
                double valueThis = entry.getValue().doubleValue();
                double valueSpec = o.getValue(entry.getKey());
                if (valueThis ==0) {
                    entrySetIterator.remove();
                } else {
                    dotProduct = dotProduct + (valueThis * valueSpec);
                    sum = sum + (valueThis *valueThis);
                }
            }
            sumOfSquares = sum;
            sumOfSquaresSet = true;
        }
        return dotProduct;
    }

    /**
     * Get the number of non-zero coordinates.
     * @return number of non-zero coordinates
     */
    public int nonZeroEntries() {
        if (coordinates ==null) {return 0;}
        normSquared();//make sure that all non-zero coordinates are removed
        return coordinates.size();
    }

    /**
     * Get the value of this vector in the named dimension.
     * @param namedDimension a named dimension
     * @return the value of this vector in the named dimension
     */
    public double getValue(String namedDimension) {
        if (namedDimension==null) {return 0;}
        Number entry = coordinates.get(namedDimension);
        if (entry==null) {return 0;}
        return entry.doubleValue();
    }

    /**
     * Sort the content in descending order, basen on Number and trim to the
     * first maxElements elements.
     * @param maxElements the maximum number of elements
     */
    public void reduceTo(int maxElements) {
        log.debug("reduceTo probably expensive...");
        if (this.coordinates == null || this.coordinates.size()<=maxElements) {return;}

        //sort the coordinates by size (ascending)
        List<Map.Entry<String, Number>> entryList =
                new ArrayList<Map.Entry<String, Number>>(coordinates.entrySet());
        Collections.sort(entryList, new CoordinateComparator());

        for (int index = 0; index<entryList.size()-maxElements; index++) {
            coordinates.remove(entryList.get(index).getKey());
        }
    }

    /**
     * Normalise this vector, i.e. transform to a parallel unit vector.
     * This is done by dividing the given vector by its norm.
     */
    public void normalise() {
        if (!normalised) {
            double norm = norm();
            for (Map.Entry<String, Number> entry: coordinates.entrySet()) {
                coordinates.put(entry.getKey(), entry.getValue().doubleValue() / norm);
            }
            normalised = true;
            this.norm = 1;
            normSet = true;
            this.sumOfSquares = 1;
            sumOfSquaresSet = true;
        }
    }

    /**
     * Decide whether this SparseVector is equal to the argument SparseVector.
     * @param o - the reference object with which to compare.
     * @return true if this object is the same as the argument; false otherwise
     */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof SparseVector)) {
            return false;
        }
        SparseVector other = (SparseVector) o;
        for (Map.Entry<String, Number> entryThis: coordinates.entrySet()) {
            double coordinateThis = entryThis.getValue().doubleValue();
            double coordinateOther = other.getValue(entryThis.getKey());
            if (coordinateThis!=coordinateOther) {
                return false;
            }
        }
        Map<String, Number> coordinatesOther = other.getCoordinates();
        for (Map.Entry<String, Number> entryOther: coordinatesOther.entrySet()) {
            if (!coordinates.containsKey(entryOther.getKey()) &&
                    entryOther.getValue().doubleValue()!=0) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 7913;
            for (Map.Entry<String, Number> entry: coordinates.entrySet()) {
                result = 83*result + entry.getKey().hashCode();
                result = 83*result + entry.getValue().hashCode();
            }
            hashCode = result;
        }
        return hashCode;
    }

    /**
     * Return a string representation of this SparseVector.
     * @return String representation of this SparseVector
     */
    public String toString() {
        if (this.coordinates.isEmpty()) {return "[0]";}

        //sort the coordinates descending
        List<Map.Entry<String, Number>> entryList =
                new ArrayList<Map.Entry<String, Number>>(coordinates.entrySet());
        Collections.sort(entryList, Collections.reverseOrder(new CoordinateComparator()));

        return entryList.toString();
    }

    /**
     * Get the Euclidian distance between this and the specified sparse vector.
     * The Euclidian distance is also known as 2-norm distance.
     * Note: Euclidian distance is computationally expensive.
     * @param o specified sparse vector
     * @return the Euclidian distance between this and the specified vector
     */
    public double dist(SparseVector o) {
        if (o==null) {return -1;}
        if (o.nonZeroEntries()<this.nonZeroEntries()) {return o.dist(this);}
        if (this.coordinates ==null) {return -1;}

        double distSum = 0;
        if (sumOfSquaresSet) {
            for (Map.Entry<String, Number> entry: coordinates.entrySet()) {
                double valueThis = entry.getValue().doubleValue();
                double valueSpec = o.getValue(entry.getKey());
                distSum = distSum + ((valueThis - valueSpec) * (valueThis - valueSpec));
            }
            for (Map.Entry<String, Number> entry: o.getCoordinates().entrySet()) {
                if (!this.coordinates.containsKey(entry.getKey())) {
                    double valueSpec = entry.getValue().doubleValue();
                    distSum = distSum + (valueSpec * valueSpec);
                }
            }
        } else {
            double squareSum = 0;
            Iterator<Map.Entry<String, Number>> entrySetIterator =
                    coordinates.entrySet().iterator();
            while (entrySetIterator.hasNext()) {
                Map.Entry<String, Number> entry = entrySetIterator.next();
                double valueThis = entry.getValue().doubleValue();
                double valueSpec = o.getValue(entry.getKey());
                if (valueThis ==0) {
                    entrySetIterator.remove();
                } else {
                    distSum = distSum + ((valueThis - valueSpec) * (valueThis - valueSpec));
                    squareSum = squareSum + (valueThis * valueThis);
                }
            }
            for (Map.Entry<String, Number> entry: o.getCoordinates().entrySet()) {
                if (!this.coordinates.containsKey(entry.getKey())) {
                    double valueSpec = entry.getValue().doubleValue();
                    distSum = distSum + (valueSpec * valueSpec);
                }
            }
            sumOfSquares = squareSum;
            sumOfSquaresSet = true;
        }

        return Math.sqrt(distSum);
    }

    /**
     * Get the Chebyshev distance between this and the specified sparse vector.
     * The Chebyshev distance is also known as the infinity norm distance.
     * Note: Chebyshev distance is computationally cheaper than Euclidian.
     * @param o specified sparse vector
     * @return the Chebyshev distance between this and the specified vector
     */
    public double chebyshevDistance(SparseVectorMapImpl o) {
        if (o==null) {return -1;}
        if (o.nonZeroEntries()<this.nonZeroEntries()) {return o.chebyshevDistance(this);}
        if (this.coordinates ==null) {return -1;}

        double maxDist = 0;
        for (Map.Entry<String, Number> entry: coordinates.entrySet()) {
            double valueThis = entry.getValue().doubleValue();
            double valueSpec = o.getValue(entry.getKey());
            if (valueThis - valueSpec > maxDist) {
                maxDist = (valueThis - valueSpec);
            }
        }
        for (Map.Entry<String, Number> entry: o.getCoordinates().entrySet()) {
            if (!this.coordinates.containsKey(entry.getKey())) {
                double valueSpec = entry.getValue().doubleValue();
                if (valueSpec > maxDist) {
                    maxDist = valueSpec;
                }
            }
        }
        return maxDist;
    }
    /**
     * Get the Manhattan distance between this and the specified sparse vector.
     * Note: Manhattan distance is computationally cheaper than Euclidian.
     * @param o specified sparse vector
     * @return the Chebyshev distance between this and the specified vector
     */
    public double manhattanDistance(SparseVectorMapImpl o) {
        if (o==null) {return -1;}
        if (o.nonZeroEntries()<this.nonZeroEntries()) {return o.manhattanDistance(this);}
        if (this.coordinates ==null) {return -1;}

        double distSum = 0;
        for (Map.Entry<String, Number> entry: coordinates.entrySet()) {
            double valueThis = entry.getValue().doubleValue();
            double valueSpec = o.getValue(entry.getKey());
            distSum = distSum + (valueThis - valueSpec);
        }
        for (Map.Entry<String, Number> entry: o.getCoordinates().entrySet()) {
            if (!this.coordinates.containsKey(entry.getKey())) {
                double valueSpec = entry.getValue().doubleValue();
                distSum = distSum + valueSpec;
            }
        }
        return distSum;
    }
}
