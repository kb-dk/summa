/* $Id: ClusterExtractorWorkflow.java,v 1.1 2007/12/03 08:51:20 bam Exp $
 * $Revision: 1.1 $
 * $Date: 2007/12/03 08:51:20 $
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
package dk.statsbiblioteket.summa.clusterextractor;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * ClusterExtractorWorkflow.
 * <p>
 * The work flow looks something like this:
 * <ul>
 * <li> For each index machine:
 *      <ul>
 *      <li> run the ClusterBuilder.buildCentroids method;
 *      <li> copy the new local centroid data structure to a central location.
 *      </ul>
 * <li> Central: Call the ClusterMerger.mergeCentroidSets method.
 * <li> For each index machine:
 *      <ul>
 *      <li> copy the new full centroid datastructure to this machine;
 *      <li> call the local ClusterProvider update method.
 *      </ul>
 * </ul>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class ClusterExtractorWorkflow {
}



