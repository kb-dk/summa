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
package dk.statsbiblioteket.summa.clusterextractor.data;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.HashSet;

/**
 * ClusterSet is an unordered set of {@link Cluster}s.
 * The ClusterBuilder builds ClusterSets, and the ClusterSets are
 * merged into a Dendrogram by the ClusterMerger.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class ClusterSet extends HashSet<ClusterRepresentative> {
    protected static final Log log = LogFactory.getLog(ClusterSet.class);
    private String builderId;

    /**
     * Construct an empty centroid set with specified initial capacity.
     * @param builderId id of the builder building this centroid set
     * @param initialCapacity the initial capacity of the hash table
     */
    public ClusterSet(String builderId, int initialCapacity) {
        super(initialCapacity);
        this.builderId = builderId;
    }

    /**
     * Construct an empty centroid set with specified initial capacity.
     * @param initialCapacity the initial capacity of the hash table
     */
    public ClusterSet(int initialCapacity) {
        this("NO_ID", initialCapacity);
    }

    /**
     * Construct an empty centroid set.
     * @param builderId id of the builder building this centroid set
     */
    public ClusterSet(String builderId) {
        super();
        this.builderId = builderId;
    }

    /**
     * Construct an empty centroid set.
     */
    public ClusterSet() {
        this("NO_ID");
    }

    /**
     * Get the id of the builder building this centroid set.
     * @return builder id
     */
    public String getBuilderId() {
        return builderId;
    }

    /**
     * Save this ClusterSet in the given {@link File}.
     * @param file file to save in
     */
    public void save(File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
            oos.close();
        } catch (FileNotFoundException e) {
            log.error("FileNotFoundException in ClusterSet.save. " +
                    "The centroids cannot be saved.", e);
        } catch (IOException e) {
            log.error("IOException in ClusterSet.save. " +
                    "The centroids cannot be saved.", e);
        }
    }

    /**
     * Load ClusterSet from file.
     * @param file file to load from
     * @return loaded ClusterSet
     */
    public static ClusterSet load(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object fileContent = ois.readObject();
            ois.close();
            if (fileContent instanceof ClusterSet) {
                return (ClusterSet) fileContent;
            } else {
                log.error("ClusterSet.load; content of file = " + file +
                        " is not a ClusterSet.");
            }
        } catch (FileNotFoundException e) {
            log.error("ClusterSet.load " +
                    "FileNotFoundException; file = " + file, e);
        } catch (IOException e) {
            log.error("ClusterSet.load " +
                    "IOException; file = " + file, e);
        } catch (ClassNotFoundException e) {
            log.error("ClusterSet.load " +
                    "ClassNotFoundException; file = " + file, e);
        }
        return null;
    }
    /**
     * Returns a textual representation of this ClusterSet.
     * @return a string representation of this ClusterSet
     */
    @Override
    public String toString() {
        return "ClusterSet: " + super.toString();
    }

}




