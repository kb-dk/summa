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

import dk.statsbiblioteket.summa.clusterextractor.data.Dendrogram;
import dk.statsbiblioteket.summa.clusterextractor.data.DendrogramNode;
import dk.statsbiblioteket.summa.clusterextractor.math.SparseVector;
import dk.statsbiblioteket.summa.clusterextractor.math.SparseVectorMapImpl;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ClusterProviderImpl can enrich a given Document with known clusters.
 * The ClusterProviderImpl loads a {@link Dendrogram},
 * and provides an enrich method based on this. The enrich method takes a
 * Lucene @{link Document}, enriches and returns it.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class ClusterProviderImpl extends UnicastRemoteObject implements ClusterProvider {
    protected static final Log log = LogFactory.getLog(ClusterProviderImpl.class);
    /** Configurations. */
    protected Configuration conf;
    /** Dendrogram. */
    protected Dendrogram dendrogram;

    /** Positive Fields Pattern. */
    protected Pattern pFields;
    /** Negative Fields Pattern. */
    protected Pattern pNFields;

    /** Positive Terms Pattern. */
    protected Pattern pTerms;
    /** Negative Terms Pattern. */
    protected Pattern pNTerms;

    /**
     * Construct ClusterProviderImpl with given configuration.
     * @param conf configuration
     * @throws RemoteException if failed to export object
     */
    public ClusterProviderImpl(Configuration conf) throws RemoteException {
        super();
        this.conf = conf;
        reload();
        loadFieldPatterns();
    }

    public Document enrich(Document document) {
        if (dendrogram==null) {
            log.warn("ClusterProviderImpl.enrich; no dendrogram loaded.");
            reload();
            if (dendrogram==null) {
                return document;
            }
        }
        SparseVector documentVector = getVector(document);

        //compare to known centroids
        for (DendrogramNode root: dendrogram.getRoots()) {
            document = traverseDendrogram(root, documentVector, document);
        }

        return document;
    }

    /**
     * Traverse dendrogram and find the clusters that given vector belong to.
     * @param node current node of dendrogram
     * @param vector vector to compare to centroids in dendrogram
     * @param document document to enrich with found clusters
     * @return enriched document
     */
    private Document traverseDendrogram(DendrogramNode node, SparseVector vector, Document document) {
        double similarity = vector.similarity(node.getCentroid());
        if (log.isTraceEnabled()) {
            log.trace("node.getCentroid() = " + node.getCentroid());
            log.trace("node.getSimilarityThreshold() = " + node.getSimilarityThreshold());
            log.trace("vector = " + vector);
            log.trace("similarity = " + similarity);
        }
        if (similarity>node.getSimilarityThreshold()) {
            if (node.getChildren().isEmpty()) {//this is a leaf node
                document.add(node.getClusterField());
            } else {//call method recursively
                for (DendrogramNode child: node.getChildren()) {
                    document = traverseDendrogram(child, vector, document);
                }
            }
        }
        return document;
    }

    /**
     * Get the vector for the given document.
     * Any stopwords are removed and all dimension names are converted to lowercase.
     * @param document a Lucene document
     * @return the sparse vector for the given document
     */
    public SparseVector getVector(Document document) {
        HashMap<String, Number> coordinates = new HashMap<String, Number>();
        List fields = document.getFields();
        for (Object obj: fields) {
            if (obj instanceof Field) {
                Field field = (Field) obj;
                if (fieldOk(field)) {
                    String fieldStringValue = field.stringValue();
                    //TODO: nice tokenisation/parsing...
                    for (String dim: fieldStringValue.split(" ")) {
                        if (termTextOk(dim)) {
                            int freq = coordinates.containsKey(dim) ?
                                    coordinates.get(dim).intValue() : 0;
                            freq ++;
                            if (log.isTraceEnabled()) {
                                log.trace("Adding (dim, freq) = (" + dim + ", " + freq + ") to vector.");
                            }
                            coordinates.put(dim, freq);
                        }
                    }
                }
            }
        }
        return new SparseVectorMapImpl(coordinates);
    }

    /**
     * Is the given term-text/dimension ok?.
     * I.e. is it in the known vocabulary or if no vocabulary is known,
     * is it in agreement with the configuration patterns.
     * @param dim text to test
     * @return true if dim is accepted as term-text/dimension; false otherwise
     */
    private boolean termTextOk(String dim) {
        return ((pTerms==null || pTerms.matcher(dim).matches()) &&
                (pNTerms==null || !pNTerms.matcher(dim).matches()));
    }

    /**
     * Is the given field accepted by the configuration patterns?.
     * @param field field to test
     * @return true if field is accepted by the configuration patterns;
     *         false otherwise
     */
    private boolean fieldOk(Field field) {
        return ((pFields==null || pFields.matcher(field.name()).matches()) &&
                (pNFields==null || !pNFields.matcher(field.name()).matches()));
    }

    /**
     * Load field patterns from configurations.
     */
    private void loadFieldPatterns() {
        String fields = conf.getString(ClusterBuilder.FIELDS_IN_VECTORS_KEY);
        pFields = fields == null || fields.equals("") ? null : Pattern.compile(fields);
        if (log.isTraceEnabled()) {
            log.trace("pFields = " + pFields);
        }
        String nFields = conf.getString(ClusterBuilder.NEG_FIELDS_IN_VECTORS_KEY);
        pNFields = nFields == null || nFields.equals("") ? null : Pattern.compile(nFields);
        if (log.isTraceEnabled()) {
            log.trace("pNFields = " + pNFields);
        }
    }

    /**
     * Load Dendrogram from path provided in configuration.
     */
    private void loadDendrogram() {
        String directoryPath = conf.getString(DENDROGRAM_PATH_KEY);
        File dir = new File(directoryPath);
        if (dir.isDirectory()) {
            File[] dendrogramFileList = dir.listFiles();
            if (dendrogramFileList.length==0) {
                log.warn("ClusterProviderImpl.loadDendrogram; no dendrogram file " +
                        "found; directoryPath = " + directoryPath +
                        ". Dendrogram NOT loaded.");
                return;
            }
            Arrays.sort(dendrogramFileList);
            File dendrogramFile;
            int index = dendrogramFileList.length-1;
            do {
                dendrogramFile = dendrogramFileList[index];
                index--;
            }
            while (!dendrogramFile.isFile() && index>=0);
            if (!dendrogramFile.isFile()) {
                log.warn("ClusterProviderImpl.loadDendrogram; no dendrogram file " +
                        "found; directoryPath = " + directoryPath +
                        ". Dendrogram NOT loaded.");
                return;
            }
            this.dendrogram = Dendrogram.load(dendrogramFile);
        } else {
            log.warn("ClusterProviderImpl.loadDendrogram; dendrogram directory path" +
                    "not a directory; directoryPath = " + directoryPath +
                    ". Dendrogram NOT loaded.");
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace("ClusterProviderImpl.loadDendrogram: Dendrogram loaded.");
        }
    }

    public void reload() {
        //TODO make reload make sense again !!!
        loadDendrogram();
    }
}




