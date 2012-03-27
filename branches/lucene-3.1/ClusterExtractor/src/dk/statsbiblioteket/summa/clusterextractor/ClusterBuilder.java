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
package dk.statsbiblioteket.summa.clusterextractor;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configurable;

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
public interface ClusterBuilder extends Configurable {
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

    // String keys to properties used when creating centroids
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

    public static final String MAX_CLUSTER_SIZE_KEY =
            "clusterbuilder.MaxClusterSize";
    public static final String APPROX_FACTOR_KEY =
            "clusterbuilder.ApproxFactor";

    /**
     * Build centroids based on index specified in configuration.
     * A centroid set is build and saved using the properties set in the known
     * configuration.
     */
    public void buildCentroids();

    //TODO: register merger with builder or?
}




