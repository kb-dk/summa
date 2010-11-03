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
package dk.statsbiblioteket.summa.clusterextractor.math;

import java.util.Map;
import java.io.Serializable;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * SparseVector represents a vector with few non-zero entries.
 *
 * The sparse vector is small even in high-dimensional space, but has no
 * knowledge of the dimensions in which it is zero. This means that it is up
 * to you not to mix spaces...
 *
 * The sparse vector requires both Euclidean distance and norm as well as
 * some similarity measure. It is up to the implementation to choose a
 * similarity function, and to make sure not to mix vectors using different
 * similarity functions.
 *
 * We could loosen the definition and rather than insisting on Euclidean
 * distance, we could require some metric (distance function), and some norm
 * to use for normalisation...
 *
 * The SparseVector interface extends Serializable. 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public interface SparseVector extends Serializable {

    /**
     * Get the similarity between this and the specified sparse vector.
     * The choice of similarity is up to the implementation.
     * @param o specified sparse vector
     * @return the similarity between this and the specified vector
     */
    public double similarity(SparseVector o);

    /**
     * Get the Euclidean distance between this and the specified sparse vector.
     * @param o specified sparse vector
     * @return the Euclidean distance between this and the specified vector
     */
    public double dist(SparseVector o);

    /**
     * Get the Euclidean norm (length (magnitude)) of this vector.
     * @return the norm of this vector
     */
    public double norm();

    /**
     * Get the angle between this and the specified sparse vector.
     * @param o specified sparse vector
     * @return angle between this and the specified vector
     */
    public double angle(SparseVector o);

    /**
     * Get a map from named dimensions to non-zero coordinates.
     * @return map from named dimensions to non-zero coordinates
     */
    public Map<String, Number> getCoordinates();

    /**
     * Get the number of non-zero coordinates.
     * @return number of non-zero coordinates
     */
    public int nonZeroEntries();

    /**
     * Get the coordinate of this vector in the named dimension.
     * @param namedDimension a named dimension
     * @return the value of this vector in the named dimension
     */
    public double getValue(String namedDimension);

    /**
     * Sort coordinates in descending order and trim to the first maxElements elements.
     * @param maxElements the maximum number of elements 
     */
    public void reduceTo(int maxElements);

    /**
     * Normalise this vector, i.e. transform to a parallel unit vector.
     * This is done by dividing the given vector by its norm.
     */
    public void normalise();
}




