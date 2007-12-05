/* $Id: ClusterMerger.java,v 1.2 2007/12/03 09:24:19 bam Exp $
 * $Revision: 1.2 $
 * $Date: 2007/12/03 09:24:19 $
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

import dk.statsbiblioteket.summa.clusterextractor.data.CentroidSet;
import dk.statsbiblioteket.summa.clusterextractor.data.Dendrogram;
import dk.statsbiblioteket.summa.clusterextractor.data.Vocabulary;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * ClusterMerger is responsible for merging vocabularies and centroid sets.
 *
 * This interface defines two upload methods and two merge methods that should
 * be implemented to be a ClusterMerger. The upload methods are meant to be
 * called by local builders and provide new material. The merge methods are
 * meant to merge the already uploaded material and push the merged data to
 * the local providers.
 *
 * A number of names to merger configuration values are also defined.
 *
 * TODO: how do we get the workflow working?
 * TODO: who tells the merger that it's time to merge?
 * TODO: should the merger know all the providers?
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public interface ClusterMerger {
    /** The String key to the incoming vocabularies path property. */
    public static final String IN_VOCAB_PATH_KEY = "clustermerger.invocabpath";
    /** The String key to the output vocabulary path property. */
    public static final String OUT_VOCAB_PATH_KEY = "clustermerger.outvocabpath";
    /** The String key to the centroid sets path property. */
    public static final String CENTROID_SETS_PATH_KEY = "clustermerger.centroidsetspath";
    /** The String key to the dendogram path property. */
    public static final String DENDROGRAM_PATH_KEY = "clustermerger.dendrogrampath";

    /** The String key to the join centroids similarity threshold. */
    public static final String JOIN_SIMILARITY_THRESHOLD_KEY = "clustermerger.joinSimilarityThreshold";
    /** The String key to the join similarity threshold for centroids with the same name. */
    public static final String JOIN_SIMILARITY_THRESHOLD_SAME_NAME_KEY = "clustermerger.joinSimilarityThresholdSameName";

    /*
     * Upload new locally build vocabulary from given machine, handle, file info.
     * @param machineId the Id of the machine, which has build this vocabulary
     * @param handle the handle to upload the file
     * @param vocab the vocabulary TODO remove vocabulary parameter
     */
    public void uploadVocabulary(String machineId, long handle, Vocabulary vocab);

    /**
     * Merge all known vocabularies to one vocabulary and push to local providers.
     */
    public void mergeVocabularies();

    /**
     * Get the merged vocabulary.
     * @return merged vocabulary
     */
    public Vocabulary getNewVocabulary();
    //TODO: public long getNewVocabulary(); return handle

    /*
     * Upload new locally build centroid set from given machine, handle, file info.
     * @param machineId the Id of the machine, which has build this vocabulary
     * @param handle the handle to upload the file
     * @param centroidSet the centroid set TODO remove centroid set parameter
     */
    public void uploadCentroidSet(String machineId, long handle, CentroidSet centroidSet);

    /**
     * Merge all known centroid sets to one dendrogram and push to local providers.
     */
    public void mergeCentroidSets();
    public Dendrogram getNewDendrogram();
    //TODO public long getNewDendrogram(int handle); ???
}
