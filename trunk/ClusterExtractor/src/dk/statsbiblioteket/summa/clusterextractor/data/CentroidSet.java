/* $Id: CentroidSet.java,v 1.3 2007/12/03 11:40:01 bam Exp $
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

import dk.statsbiblioteket.summa.clusterextractor.math.CentroidVector;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.HashSet;

/**
 * CentroidSet is an unordered set of CentroidVectors.
 * The ClusterBuilder builds CentroidSets, and the CentroidSets are
 * merged into a Dendrogram by the ClusterMerger.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class CentroidSet extends HashSet<CentroidVector> {
    private int builderId;

    /**
     * Construct an empty centroid set.
     * @param builderId id of the builder building this centroid set
     */
    public CentroidSet(int builderId) {
        this.builderId = builderId;
    }

    /**
     * Construct an empty centroid set.
     */
    public CentroidSet() {
        this(0);
    }

    /**
     * Get the id of the builder building this centroid set.
     * @return builder id
     */
    public int getBuilderId() {
        return builderId;
    }

    /**
     * Returns a textual representation of this centroid set.
     * @return a string representation of this centroid set
     */
    @Override
    public String toString() {
        return "CentroidSet: " + super.toString();
    }

}
