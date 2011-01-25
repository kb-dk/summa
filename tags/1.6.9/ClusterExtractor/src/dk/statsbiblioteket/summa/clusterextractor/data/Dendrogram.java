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

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The Dendrogram is a tree (or forest) of centroids or cluster representations.
 * The Dendrogram is build of {@link DendrogramNode}s.
 * Each leaf node in the Dendrogram represents a centroid of a cluster
 * extracted from the current index.
 * Each internal node (or root node) represents a 'combination centroid', i.e.
 * a centroid which is created by joining all of it's child centroid nodes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class Dendrogram implements Serializable {
    protected static final Log log = LogFactory.getLog(Dendrogram.class);
    /** {@link Set} of root DendrogramNodes of this Dendrogram. */
    private HashSet<DendrogramNode> roots;

    /**
     * Construct empty Dendrogram.
     */
    public Dendrogram() {
        this.roots = new HashSet<DendrogramNode>();
    }

    /**
     * Construct Dendrogram with given root node.
     * @param root1 root DendrogramNode
     */
    public Dendrogram(DendrogramNode root1) {
        this();
        this.roots.add(root1);
    }

    /**
     * Get the set of roots of this Dendrogram.
     * @return root set
     */
    public Set<DendrogramNode> getRoots() {
        return roots;
    }

    /**
     * Add given root DendrogramNode to root set of this Dendrogram.
     * @param root root DendrogramNode
     * @return true if the set did not already contain the specified root
     */
    public boolean addRoot(DendrogramNode root) {
        return roots.add(root);
    }

    /**
     * Save this Dendrogram in given file.
     * @param file file to save in
     */
    public void save(File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
            oos.close();
        } catch (FileNotFoundException e) {
            log.error("FileNotFoundException in Dendrogram.save(). " +
                    "The dendrogram cannot be saved.", e);
        } catch (IOException e) {
            log.error("IOException in Dendrogram.save(). " +
                    "The dendrogram cannot be saved.", e);
        }
    }

    /**
     * Load Dendrogram from given file.
     * @param file file to load from
     * @return loaded Dendrogram
     */
    public static Dendrogram load(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object fileContent = ois.readObject();
            ois.close();
            if (fileContent instanceof Dendrogram) {
                return (Dendrogram) fileContent;
            } else {
                log.warn("Dendrogram.load() content of file = " + file +
                        "NOT instanceof Dendrogram.");
            }
        } catch (FileNotFoundException e) {
            log.warn("Dendrogram.load() " +
                    "FileNotFoundException; file = " + file +
                    ". Dendrogram NOT loaded.", e);
        } catch (IOException e) {
            log.warn("Dendrogram.load() " +
                    "IOException; file = " + file +
                    ". Dendrogram NOT loaded.", e);
        } catch (ClassNotFoundException e) {
            log.warn("Dendrogram.load() " +
                    "ClassNotFoundException; file = " + file +
                    ". Dendrogram NOT loaded.", e);
        }
        return null;
    }
    
    /**
     * Returns a textual representation of the roots of this Dendrogram.
     * @return a string representation of this Dendrogram
     */
    @Override
    public String toString() {
        String result = "Dendrogram:\n";
        for (DendrogramNode root: getRoots()) {
            result += root.toString();
        }
        return result;
    }
}




