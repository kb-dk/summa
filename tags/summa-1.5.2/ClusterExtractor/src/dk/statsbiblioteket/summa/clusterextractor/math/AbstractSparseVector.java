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




