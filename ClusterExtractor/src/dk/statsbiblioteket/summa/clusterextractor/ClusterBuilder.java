/* $Id: ClusterBuilder.java,v 1.1 2007/12/03 08:51:20 bam Exp $
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
 * ClusterBuilder is responsible for building local centroid sets.
 *
 * This interface defines two methods that should be implemented to be a
 * ClusterBuilder, and a number of names (keys) to useful configuration
 * values (properties).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public interface ClusterBuilder {
    /** The String key to the local work index path property. */
    public static final String LOCAL_INDEX_PATH_KEY = "search.index.main";

    /** The String key to the id for this machine property. */
    public static final String LOCAL_MACHINE_ID_KEY = "clusterbuilder.machineid";

    /** The String key to the local builder cluster set path property. */
    public static final String LOCAL_CLUSTER_SET_PATH_KEY = "clusterbuilder.localclustersetpath";

    // String keys to properties used when finding candidate terms
    public static final String FIELDS_IN_INIT_KEY =
            "clusterbuilder.FieldsUsedInInit";
    public static final String NEG_FIELDS_IN_INIT_KEY =
            "clusterbuilder.NegativeFieldsInInit";
    public static final String TERM_TEXT_KEY =
            "clusterbuilder.Terms";
    public static final String NEG_TERM_TEXT_KEY =
            "clusterbuilder.NegativeTerms";
    public static final String MIN_TERM_SUM_KEY =
            "clusterbuilder.MinTermSum";
    public static final String MIN_TERM_LOCAL_SUM_KEY =
            "clusterbuilder.MinTermLocalSum";
    public static final String MAX_TERM_SUM_KEY =
            "clusterbuilder.MaxTermSum";
    public static final String FIELDS_IN_VECTORS_KEY =
            "clusterbuilder.FieldsUsedInVectors";
    public static final String NEG_FIELDS_IN_VECTORS_KEY =
            "clusterbuilder.NegativeFieldsInVectors";
    public static final String MIN_NUMBER_OF_FIELDS_KEY =
            "clusterbuilder.MinNumberOfFields";

    // String keys to properties used when creating centroids (from queries)
    public static final String NORMALISE_WHEN_ADDING_KEY =
            "clusterbuilder.NormaliseWhenAdding";
    public static final String OPTIMISE_EVERY_X_POINTS_KEY =
            "clusterbuilder.OptimiseEveryXPoints";
    public static final String MAX_SIZE_AFTER_OPTIMISE_KEY =
            "clusterbuilder.MaxSizeAfterOptimise";
    public static final String MAX_POINTS_TO_BUILD_KEY =
            "clusterbuilder.MaxPointsToBuild";
    public static final String MAX_FINAL_SIZE_KEY =
            "clusterbuilder.MaxFinalSize";
    public static final String SIMILARITY_THRESHOLD_KEY =
            "clusterbuilder.SimilarityThresholdFraction";

    /**
     * Build centroids based on index specified in configuration.
     * A centroid set is build and saved using the properties set in the known
     * configuration.
     */
    public void buildCentroids();

    /**
     * Register the merger to send the build data to.
     * TODO: register merger with builder or?
     * @param merger ClusterMerger to send data to
     */
    public void registerMerger(ClusterMerger merger);
}
