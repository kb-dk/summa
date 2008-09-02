/* $Id: SparseVector.java,v 1.6 2007/10/04 09:48:13 te Exp $
 * $Revision: 1.6 $
 * $Date: 2007/10/04 09:48:13 $
 * $Author: te $
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
