/* $Id: AbstractSparseVector.java,v 1.5 2007/12/04 10:26:43 bam Exp $
 * $Revision: 1.5 $
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

/**
 * AbstractSparseVector is a sparse vector with a norm method.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public abstract class AbstractSparseVector implements SparseVector{

    /** Has the sum of squares been calculated? */
    protected boolean sumOfSquaresSet;
    /** The sum of squares of coordinates. */
    protected double sumOfSquares;
    /** Has the norm been calculated? */
    protected boolean normSet;
    /** The Euclidian norm, i.e. the squareroot of the sum of squares. */
    protected double norm;
    /** Is this vector normalised? */
    protected boolean normalised;

    /** Initialise fields. */
    protected void init() {
        normSet = false;
        sumOfSquaresSet = false;
        normalised = false;
    }

    /**
     * Get the Euclidian norm (length) of this vector.
     * @return the norm of this vector
     */
    public double norm() {
        if (normalised) {return 1;}

        if (!normSet) {
            norm = Math.sqrt(normSquared());
            normSet = true;
        }

        return norm;
    }

    /**
     * Get the squared norm of this vector.
     * @return the squared norm of this vector
     */
    public abstract double normSquared();

    /**
     * Get the dotproduct between this and the specified sparse vector.
     * @param o specified sparse vector
     * @return the dotproduct between this and the specified vector
     */
    protected abstract double dotProduct(SparseVector o);

}
