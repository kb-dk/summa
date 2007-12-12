/* $Id: ClusterSet.java,v 1.3 2007/12/03 11:40:01 bam Exp $
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
public class ClusterSet extends HashSet<Cluster> {
    protected static final Log log = LogFactory.getLog(ClusterSet.class);
    private int builderId;

    /**
     * Construct an empty centroid set.
     * @param builderId id of the builder building this centroid set
     */
    public ClusterSet(int builderId) {
        this.builderId = builderId;
    }

    /**
     * Construct an empty centroid set.
     */
    public ClusterSet() {
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
