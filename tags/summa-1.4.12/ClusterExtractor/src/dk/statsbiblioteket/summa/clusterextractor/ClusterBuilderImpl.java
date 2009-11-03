/* $Id: ClusterBuilderImpl.java,v 1.3 2007/12/04 10:26:43 bam Exp $
 * $Revision: 1.3 $
 * $Date: 2007/12/04 10:26:43 $
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

import dk.statsbiblioteket.summa.clusterextractor.data.Cluster;
import dk.statsbiblioteket.summa.clusterextractor.data.ClusterSet;
import dk.statsbiblioteket.summa.clusterextractor.math.IncrementalCentroid;
import dk.statsbiblioteket.summa.clusterextractor.math.SparseVector;
import dk.statsbiblioteket.summa.clusterextractor.math.SparseVectorMapImpl;
import dk.statsbiblioteket.summa.clusterextractor.math.CoordinateComparator;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.regex.Pattern;

/**
 * ClusterBuilderImpl builds local centroid sets.
 * ClusterBuilderImpl implements ClusterBuilder and Remote.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class ClusterBuilderImpl extends UnicastRemoteObject implements ClusterBuilder {
    protected static final Log log = LogFactory.getLog(ClusterBuilderImpl.class);
    /** Configurations. */
    protected Configuration conf;
    /** Machine id for this provider. */
    protected String id;
    /** Storage and Index Access. */
    private StorageAndIndexAccess access;
    /** Fields in which to look for candidate terms initially. */
    protected List<String> fieldsUsedInInit;
    /** Fields to use in vectors and queries. */
    protected List<String> fieldsUsedInVectors;

    /** Positive pattern used for filtering the text of the terms. */
    protected Pattern pTerms;
    /** Negative pattern used for filtering the text of the terms. */
    protected Pattern pNTerms;

    /** The zero vector. */
    public static final SparseVector zero = new SparseVectorMapImpl(null);

    /**
     * Construct cluster builder using given configurations.
     * @param conf configurations
     * @throws java.rmi.RemoteException if failed to export object
     */
    public ClusterBuilderImpl(Configuration conf) throws RemoteException {
        super();
        this.conf = conf;
        this.id = conf.getString(LOCAL_MACHINE_ID_KEY);
        this.access = new StorageAndIndexAccess(conf.getString(LOCAL_INDEX_PATH_KEY));
        fieldsUsedInInit = getFieldsToUseInInit();
        fieldsUsedInVectors = getFieldsToUseInVectors();
        String terms = conf.getString(TERM_TEXT_KEY);
        pTerms = terms == null || terms.equals("") ? null : Pattern.compile(terms);
        String nTerms = conf.getString(NEG_TERM_TEXT_KEY);
        pNTerms = nTerms == null || nTerms.equals("") ? null : Pattern.compile(nTerms);
        if (log.isTraceEnabled()) {
            log.trace("\nfieldsUsedInInit = " + fieldsUsedInInit +
                    "\nfieldsUsedInVectors = " + fieldsUsedInVectors +
                    "\nnTerms = " + nTerms);
        }

        exportRemoteInterfaces();
    }

    public void buildCentroids() {
        Set<String> candidateTerms = getCandidateTerms();
        ClusterSet clusterSet = buildCentroidsUsingSearch(candidateTerms);

        saveCentroids(clusterSet);
    }

    private ClusterSet buildCentroidsUsingSearch(Set<String> candidateTerms) {
        Map<String, Query> queries =
                candidateTermsToQueries
                        (candidateTerms, fieldsUsedInInit);
        return queriesToCentroids(queries);
    }

    public void buildCentroids2() {
        Set<String> candidateTerms = getCandidateTerms();

        int maxClusterSize = conf.getInt(MAX_CLUSTER_SIZE_KEY);
        int approximationFactor = conf.getInt(APPROX_FACTOR_KEY);
        ClusterSet clusterSet = buildCentroidsFromStorage(candidateTerms,
                maxClusterSize, approximationFactor);

        saveCentroids(clusterSet);
    }

    /**
     * Build Centroids by looping through all records from storage.
     * If a candidateterm is in the record, the record is used in a
     * corresponding centroid. I think a split method will be necessary.
     * @param candidateTerms terms occurring often, but not too often in index
     * @param maxClusterSize the maximum expected cluster size
     * @param approximationFactor use one out of approx factor docs in build
     * @return ClusterSet
     */
    public ClusterSet buildCentroidsFromStorage(Set<String> candidateTerms,
                                                int maxClusterSize, int approximationFactor) {
        long startTime = System.currentTimeMillis();
        Map<String, IncrementalCentroid> centroids =
                new HashMap<String, IncrementalCentroid>(candidateTerms.size());
        Map<String, List<Integer>> idSets =
                new HashMap<String, List<Integer>>(candidateTerms.size());
        //Note this Map to id sets is potentially huge...

        ClusterSet resultSet = new ClusterSet(this.id, candidateTerms.size());

        //20071212: We loop through index for now (we want storage access based on the indexer)
        //get index reader
        IndexReader ir = access.getIndexReader();
        //TODO: remove index reader from method
        int maxDoc = ir.maxDoc();
        if (log.isTraceEnabled()) {
            log.trace("ClusterBuilderImpl.buildCentroidsFromStorage: maxDoc = " + maxDoc);
        }
        Profiler feedback = new Profiler();
        feedback.setExpectedTotal(maxDoc);

        SparseVector vec;
        IncrementalCentroid incCen;
        List<Integer> idSet;
        //This method could be threaded, but we may not be able to so
        //when changing to storage/index access provided by storage/index
        for (int index=0; index<maxDoc; index = index + approximationFactor) {
            vec = getVec(ir, index);

            feedback.beat();
            if (log.isTraceEnabled() && index%10000==0) {
                log.trace("ClusterBuilderImpl.buildCentroidsFromStorage ("
                        + index + "/" + maxDoc + "); ETA: " 
                        + feedback.getETAAsString(false) + "; vec = "
                        + vec.toString());
            }

            for (String dim: vec.getCoordinates().keySet()) {
                if (centroids.containsKey(dim)) {
                    incCen = centroids.get(dim);
                    incCen.addPoint(vec);
                    idSet = idSets.get(dim);
                    idSet.add(index);
                } else {
                    if (candidateTerms.contains(dim)) {
                        incCen = new IncrementalCentroid(dim);
                        incCen.addPoint(vec);
                        centroids.put(dim, incCen);
                        idSet = new ArrayList<Integer>();
                        idSet.add(index);
                        idSets.put(dim, idSet);
                    }
                }
                //maybe we should normalise the vectors and only look at
                //dimensions over a certain threshold
            }

        }

        feedback.reset();
        feedback.setExpectedTotal(centroids.size());
        int count = 0;

        for (Map.Entry<String, IncrementalCentroid> entry: centroids.entrySet()) {
            incCen = entry.getValue();
            feedback.beat();
            count++;
            if (incCen.getNumberOfPoints()*approximationFactor>maxClusterSize) {
                if (log.isTraceEnabled()) {
                    log.trace("ClusterBuilderImpl.buildCentroidsFromStorage SPLIT ("
                            + count + "/" + centroids.size() + "); ETA: "
                            + feedback.getETAAsString(false));
                }

                ClusterSet splitCentroids =
                        split(incCen, idSets.get(entry.getKey()), maxClusterSize);
                    resultSet.addAll(splitCentroids);
            } else {
                resultSet.add(incCen.getCluster());
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("ClusterBuilderImpl.buildCentroidsFromStorage: time = "
                    + (System.currentTimeMillis()-startTime)/1000/60 + " min");
        }
        return resultSet;
    }

    /**
     * Split the cluster given as an id list into smaller clusters.
     * Note this is a very expensive method!
     *
     * @param bigCen centroid of the cluster given as an id list
     * @param idList list of ids of documents in a cluster
     * @param maxClusterSize maximum size of clusters after split
     * @return set of clusters generated from the given id list
     */
    private ClusterSet split(IncrementalCentroid bigCen,List<Integer> idList,
                             int maxClusterSize) {
        if (log.isTraceEnabled()) {
            log.trace("ClusterBuilderImpl.split: split " + bigCen.getName() +
                    " centroid (idList.size() = "+idList.size()+").");
        }

        //get index reader
        IndexReader ir = access.getIndexReader();
        //TODO: remove index reader from method

        //first pick random seeds
        int numberOfSeeds = idList.size()/maxClusterSize + 1;
        int seedId;
        SparseVector vec;
        IncrementalCentroid seedCentroid;
        IncrementalCentroid[] centroidArray = new IncrementalCentroid[numberOfSeeds];
        for (int count = 0; count < numberOfSeeds; count++) {
            seedId = idList.remove((int) Math.random()*idList.size());
            vec = getVec(ir, seedId);
            seedCentroid = new IncrementalCentroid(Integer.toString(seedId));
            seedCentroid.addPoint(vec);
            centroidArray[count] = seedCentroid;
        }

        //next apply cluster subroutine
        SparseVector centroidVec;

        double similarity;
        double maxSim;
        int maxSimIndex;

        Profiler feedback = new Profiler();
        feedback.setExpectedTotal(idList.size());
        int count = 0;

        for (Integer id : idList) {

            feedback.beat();
            count++;
            if (log.isTraceEnabled() && count%1000==0) {
                log.trace("ClusterBuilderImpl.split ("
                        + count + "/" + idList.size() + "); ETA: "
                        + feedback.getETAAsString(false));
            }

            vec = getVec(ir, id);
            maxSim = 0;
            maxSimIndex = 0;
            for (int centroidArrayIndex = 0; centroidArrayIndex < numberOfSeeds; centroidArrayIndex++) {
                centroidVec = centroidArray[centroidArrayIndex].getVector();
                similarity = vec.similarity(centroidVec);
                if (similarity > maxSim) {
                    maxSim = similarity;
                    maxSimIndex = centroidArrayIndex;
                }
            }
            centroidArray[maxSimIndex].addPoint(vec);
        }
        //and create reasonable names
        ClusterSet resultSet = new ClusterSet(numberOfSeeds);
        Cluster cluster;
        List<Map.Entry<String, Number>> coordsEntryList;
        for (IncrementalCentroid incCen: centroidArray) {
            cluster = incCen.getCluster();
            //sort the coordinates descending
            coordsEntryList = new ArrayList<Map.Entry<String, Number>>
                    (incCen.getVector().getCoordinates().entrySet());
            Collections.sort(coordsEntryList, Collections.reverseOrder(new CoordinateComparator()));

            cluster.setName(bigCen.getName() + "; " + coordsEntryList.get(0).getKey());
            if (log.isTraceEnabled()) {
                log.trace("ClusterBuilderImpl.split: created " + cluster.getName() + " centroid.");
            }
            resultSet.add(cluster);
        }

        return resultSet;
    }

    /**
     * Save given {@link ClusterSet} in directory specified in {@link Configuration}.
     * @param centroids ClusterSet to save
     * @return the file the centroid set was saved in
     */
    private File saveCentroids(ClusterSet centroids) {
        String localCentroidSetPath = conf.getString(LOCAL_CLUSTER_SET_PATH_KEY);
        File localCentroidSetDirectory = new File(localCentroidSetPath);
        if (!localCentroidSetDirectory.exists()) {
            localCentroidSetDirectory.mkdir();
        }
        long timeStamp = System.currentTimeMillis();
        File file = new File(localCentroidSetPath+timeStamp+"centroids.set");
        centroids.save(file);
        return file;
    }

    /**
     * Given initial queries, get initial search based clusters and calculate centroid.
     * @param namesToQueries map from cluster names to 'cluster queries'
     * @return set of clusters calculated from initial search based clusters
     */
    private ClusterSet queriesToCentroids(Map<String, Query> namesToQueries) {
        if (log.isTraceEnabled()) {
            log.trace("ClusterBuilderImpl.queriesToCentroids start.");
        }
        if (namesToQueries == null) {
            if (log.isTraceEnabled()) {
                log.trace("ClusterBuilderImpl.queriesToCentroids: " +
                        "query map null; returning null.");
            }
            return null;
        }

        //get centroid settings from properties
        boolean normaliseWhenAdding = conf.getBoolean(NORMALISE_WHEN_ADDING_KEY);
        int optimiseEveryXPoints = conf.getInt(OPTIMISE_EVERY_X_POINTS_KEY);
        int maxSizeAfterOptimise = conf.getInt(MAX_SIZE_AFTER_OPTIMISE_KEY);
        int maxPointsToBuild = conf.getInt(MAX_POINTS_TO_BUILD_KEY);
        int maxFinalSize = conf.getInt(MAX_FINAL_SIZE_KEY);

        //create cluster set
        int size = namesToQueries.size();
        ClusterSet clusters = new ClusterSet();

        //Array used for statistics on number of non-zero dimensions in doc vectors
        int[] numberOfVectorsWithThisNumberOfDiffTerms = new int[100];

        //get index reader
        IndexReader ir = access.getIndexReader();
        //TODO: remove index reader from method
        IndexSearcher is = new IndexSearcher(ir);
        Hits hits;

        //iterate through all names and query entries; use queries to get hits
        //and use hits to create centroids
        int clusterCounter = 0;
        Profiler feedback = new Profiler();
        feedback.setExpectedTotal(size);
        for (Map.Entry<String, Query> entry: namesToQueries.entrySet()) {
            feedback.beat();

            try {
                hits = is.search(entry.getValue());
            } catch (IOException e) {
                log.warn("ClusterBuilderImpl.queriesToCentroids; " +
                        "IndexSearcher IOException; name = " + entry.getKey() +
                        "; query = " + entry.getValue().toString(), e);
                continue;
            }
            clusterCounter++;
            if (log.isTraceEnabled()) {
                log.trace(entry.getKey() + "; " + clusterCounter + "/" + size +
                        "; Hits: " + hits.length() + "; ETA: " +
                        feedback.getETAAsString(false));
            }

            IncrementalCentroid incCentroid =
                    new IncrementalCentroid(entry.getKey());
            incCentroid.setNormaliseWhenAdding(normaliseWhenAdding);
            incCentroid.setOptimiseEveryXPoints(optimiseEveryXPoints);
            incCentroid.setMaxSizeAfterOptimise(maxSizeAfterOptimise);

            Set<SparseVector> initialCluster = new HashSet<SparseVector>();
            int hitsIndex;
            for (hitsIndex = 0; hitsIndex < hits.length() &&
                    hitsIndex < maxPointsToBuild; hitsIndex++) {
                try {
                    SparseVector vec = getVec(ir, hits.id(hitsIndex), hits.score(hitsIndex));
                    //if not zero-vector, add to initial cluster and centroid
                    if (vec!=null && !vec.equals(zero)) {
                        initialCluster.add(vec);
                        incCentroid.addPoint(vec);
                    }
                    //statistics
                    if (vec!=null && vec.nonZeroEntries()<
                            numberOfVectorsWithThisNumberOfDiffTerms.length) {
                        numberOfVectorsWithThisNumberOfDiffTerms[vec.nonZeroEntries()]++;
                    }
                } catch (IOException e) {
                    log.warn("ClusterBuilderImpl.queriesToCentroids; " +
                            "Hits.id or Hits.score IOException; query = " +
                            entry.getValue().toString() + "' hitsIndex = " +
                            hitsIndex, e);
                }
            }

            

            //cut and normalise centroid vector, set properties
            Cluster cluster = incCentroid.getCutCentroidCluster(maxFinalSize);
            cluster.getCentroid().normalise();
            cluster.setExpectedSize(hitsIndex-1);
            cluster.setSimilarityThreshold(
                    calculateSimilarityThreshold(cluster.getCentroid(), initialCluster));
            if (log.isTraceEnabled()) {
                log.trace("Similarity threshold: " + cluster.getSimilarityThreshold());
            }

            //put new cluster in cluster set
            clusters.add(cluster);
        }
        if (log.isTraceEnabled()) {
            log.trace("ClusterBuilderImpl.queriesToCentroids end. Statistics: " +
                    "numberOfVectorsWithThisNumberOfDiffTerms = " +
                    Arrays.toString(numberOfVectorsWithThisNumberOfDiffTerms));
        }
        return clusters;
    }

    /**
     * Calculate similarity threshold for the given cluster and centroid.
     * WARNING: this is an expensive method
     * @return the centroid with the threshold set
     * @param centroid the centroid vector for the cluster
     * @param initialCluster the initial cluster is the document vectors used
     *                       to build the centroid
     */
    private double calculateSimilarityThreshold(SparseVector centroid,
                                                        Set<SparseVector> initialCluster) {
        //calculate all similarities and sort (ascending)
        SparseVector[] points = new SparseVector[initialCluster.size()];
        initialCluster.toArray(points);
        double[] similarities = new double[points.length];
        for (int pointIndex=0; pointIndex<points.length; pointIndex++) {
            similarities[pointIndex] = centroid.similarity(points[pointIndex]);
        }
        Arrays.sort(similarities);

        //read similarity threshold fraction from properties
        double similarityThresholdFraction;

        try {
            similarityThresholdFraction =
                    Double.parseDouble(conf.getString(SIMILARITY_THRESHOLD_KEY));
        } catch (NumberFormatException e) {
            log.warn("ClusterBuilderImpl.calculateSimilarityThreshold: The " +
                    "double property with key " + SIMILARITY_THRESHOLD_KEY +
                    "could not be parsed.", e);
            similarityThresholdFraction = 1.0;
        }

        if (log.isTraceEnabled()) {
            log.trace("Similarity array: " + Arrays.toString(similarities));
            log.trace("Similarity threshold fraction: " + similarityThresholdFraction);
        }

        //set cluster similarity threshold on this centroid
        int index = (int) ((1.0-similarityThresholdFraction)*similarities.length);
        if (index>=0 && index<similarities.length) {
            return similarities[index];
        }
        return 0;
    }

    /**
     * Get the vector for the document with this document id.
     * Any stopwords are removed and all dimension names are converted to lowercase.
     * @param ir index reader
     * @param docId a document id
     * @return the sparse vector for the document with the given doc id
     */
    public SparseVector getVec(IndexReader ir, int docId) {
        return getVec(ir, docId, 1);
    }
    /**
     * Get the vector for the document with this document id.
     * The vector entries are weighted with the given score.
     * Any stopwords are removed and all dimension names are converted to lowercase.
     * @param ir index reader
     * @param docId a document id
     * @param score the score of this document relative to some Hits result
     * @return the sparse vector for the document with the given doc id
     *         weighted with the given score
     */
    public SparseVector getVec(IndexReader ir, int docId, float score) {
        //TODO: remove index reader from method
        HashMap<String, Number> coordinates = new HashMap<String, Number>();
        for (String field : fieldsUsedInVectors) {
            TermFreqVector tfv = null;
            try {
                tfv = ir.getTermFreqVector(docId, field);
            } catch (IOException e) {
                log.error("ir.getTermFreqVector IOException; docId = "+
                          docId+"; field = "+field, e);
            }
            if (tfv==null) {continue;}
            String[] terms = tfv.getTerms();
            int[] frequencies = tfv.getTermFrequencies();
            for (int i=0; i < terms.length; ++i) {
                String dim = terms[i].toLowerCase();
                if (termTextOk(dim)) {
                    int freq = coordinates.containsKey(dim) ?
                            coordinates.get(dim).intValue() : 0;
                    freq += score*frequencies[i];
                    coordinates.put(dim, freq);
                }
            }
        }
        return new SparseVectorMapImpl(coordinates);
    }

    /**
     * Convert candidate terms to candidate queries.
     * The queries build are for the given candidate terms in the given default fields.
     * Note that the cluster name is extracted from the term
     * removing numbers in parentheses and converting to lower case.
     * If this makes two cluster names identical
     * the cluster queries are simply combined.
     * @param candidateTerms set of candidate terms
     * @param defaultFields list of default field names; if null,
     *                      fields used for vectors (see properties) are also used here
     * @return map from cluster names to queries
     */
    public Map<String,Query> candidateTermsToQueries(
            Set<String> candidateTerms, List<String> defaultFields) {
        if (log.isTraceEnabled()) {
            log.trace("ClusterBuilderImpl.candidateTermsToQueries start.");
        }
        if (candidateTerms == null) {
            if (log.isTraceEnabled()) {
                log.trace("ClusterBuilderImpl.candidateTermsToQueries: " +
                        "candidate terms null; returning null.");
            }
            return null;
        }

        String regexIntsInPars = "[(][0-9]+[.]?-?[0-9]+[)]|[(][0-9][)]";
        if (defaultFields==null) {defaultFields = fieldsUsedInInit;}

        Map<String, Query> queries = new HashMap<String, Query>(candidateTerms.size());
        for (String term: candidateTerms) {
            BooleanQuery q = new BooleanQuery();
            for (String field: defaultFields) {
                q.add(new TermQuery(new Term(field, term)), BooleanClause.Occur.SHOULD);
            }
            String termLowerCaseNoIntsInPars =
                    term.replaceAll(regexIntsInPars,"").trim().toLowerCase();
            if (!term.equalsIgnoreCase(termLowerCaseNoIntsInPars)) {
                for (String field: defaultFields) {
                    q.add(new TermQuery(new Term(field, termLowerCaseNoIntsInPars)),
                            BooleanClause.Occur.SHOULD);
                }
            }
            if (queries.containsKey(termLowerCaseNoIntsInPars)) {
                q.add(queries.get(termLowerCaseNoIntsInPars), BooleanClause.Occur.SHOULD);
            }
            queries.put(termLowerCaseNoIntsInPars, q);
        }
        if (log.isTraceEnabled()) {
            log.trace("ClusterBuilderImpl.candidateTermsToQueries end.");
        }
        return queries;
    }

    /**
     * Get candidate terms for cluster initialisation (independant of fields).
     * @return set of terms, which can be used to create search-based clusters
     */
    public Set<String> getCandidateTerms() {
        if (log.isTraceEnabled()) {
            log.trace("ClusterBuilder.getCandidateTerms start.");
        }

        //get minimum and maximum number of times that the Term has to
        //occur in the index, in order to be tagged as valid
        int minTermSum = conf.getInt(MIN_TERM_SUM_KEY);
        int minTermLocalSum = conf.getInt(MIN_TERM_LOCAL_SUM_KEY);
        int maxTermSum = conf.getInt(MAX_TERM_SUM_KEY);

        //get index reader
        IndexReader ir = access.getIndexReader();
        //TODO: remove index reader from method

        Map<String, Integer> resultMap = new HashMap<String, Integer>();
        try {
            // look at all terms in index
            TermEnum allTerms = ir.terms();
            while (allTerms.next()) {
                Term current = allTerms.term();
                int docFreq = allTerms.docFreq();

                // if the field of the term is to be used in init and the text is 'allowed'
                // and the term occurs often enough and not too often, save the term text
                if (fieldsUsedInInit.contains(current.field()) &&
                        termTextOk(current.text()) &&
                        docFreq>minTermLocalSum && docFreq<maxTermSum) {

                    String termText = current.text();
                    int sum = 0;
                    if (resultMap.containsKey(termText)) {
                        sum = resultMap.get(termText);
                    }
                    resultMap.put(termText, sum+docFreq);
                }
            }
        } catch (IOException e) {
            log.error("IndexReader/TermEnum IOException in " +
                    "ClusterBuilder.getCandidateTerms. No candidate terms are " +
                    "returned and no centroids can be built.", e);
            return null;
        }

        //get minimum number of fields, in which term text must occur
        int minNumberOfFields = conf.getInt(MIN_NUMBER_OF_FIELDS_KEY);
        Set<String> resultSet = new HashSet<String>();
        int tooFewFieldsCounter = 0; //statistics
        for (Map.Entry<String, Integer> entry: resultMap.entrySet()) {
            // if this term text (map entry key) occurs often enough and not
            // too often in index (that is in the fields used in init)
            // AND it occurs in at least the given minimum of the fields used
            // in vectors, use this term as a query candidate
            if (entry.getValue()>minTermSum && entry.getValue()<maxTermSum) {
                int numberOfFields = 0;
                for (String field: fieldsUsedInVectors) {
                    try {
                        TermDocs td = ir.termDocs(new Term(field, entry.getKey()));
                        if (td != null && td.next()) {
                            numberOfFields++;
                        }
                    } catch (IOException e) {
                        log.warn("ClusterBuilder.getCandidateTerms IOException " +
                                "ir.termDocs(new Term(" + field + ", " +
                                entry.getKey() + ")). The method will continue.", e);
                    }
                }
                if (numberOfFields>=minNumberOfFields) {
                    resultSet.add(entry.getKey());
                } else {
                    tooFewFieldsCounter++;
                }
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("ClusterBuilder.getCandidateTerms end. " +
                    "Number of candidates: resultSet.size() = "+resultSet.size() +
                    ". Number of terms thrown away on account of occurring " +
                    "in too few fields: " + tooFewFieldsCounter);
        }
        return resultSet;
    }

    /**
     * Is the given text acceptable as a term in cluster extraction?.
     * If the builder knows a vocabulary, the text is acceptable as term if
     * in vocabulary, otherwise the text is accebtable if it matches the
     * positive term pattern and does <b>not</b> match the negative term
     * pattern given in the known {@link Configuration}.
     * @param text String to check if acceptable as term
     * @return true if text acceptable as a term, false otherwise
     */
    private boolean termTextOk(String text) {
        return (pTerms == null || pTerms.matcher(text).matches()) &&
                (pNTerms == null || !pNTerms.matcher(text).matches());
    }

    /**
     * Get (from properties) a list of the fields used when initialising clusters.
     * @return list of fields (fieldnames), which should be used when searching for 'start terms'
     */
    private List<String> getFieldsToUseInInit() {
        return getFilteredFields(FIELDS_IN_INIT_KEY, NEG_FIELDS_IN_INIT_KEY);
    }
    /**
     * Get (from properties) a list of the fields used when 'building' document vectors.
     * @return list of fields (fieldnames), which should be used when building document vectors
     */
    private List<String> getFieldsToUseInVectors() {
        return getFilteredFields(FIELDS_IN_VECTORS_KEY, NEG_FIELDS_IN_VECTORS_KEY);
    }
    /**
     * Get a list of index fields filtered according to named properties.
     * If the positive fields list is non-empty, only fields both in index and
     * in this list, are returned.
     * If the negative fields list is non-empty, only fields in index and NOT
     * in this list, are returned.
     * If a list of fields in index is not accessible, the result is the fields
     * in the positive list.
     * @param positiveFieldsKey String key to positive fields property
     * @param negativeFieldsKey String key to negative fields property
     * @return list of filtered fieldnames
     */
    private List<String> getFilteredFields(String positiveFieldsKey, String negativeFieldsKey) {
        String fields = conf.getString(positiveFieldsKey);
        Pattern pFields = fields == null || fields.equals("") ? null : Pattern.compile(fields);
        String nFields = conf.getString(negativeFieldsKey);
        Pattern pNFields = nFields == null || nFields.equals("") ? null : Pattern.compile(nFields);

        List<String> fieldsToUse = new LinkedList<String>();
        Collection fieldNames = access.getFieldNames();
        if (fieldNames!=null) {
            for (Object fieldObj: fieldNames) {
                if (fieldObj instanceof String) {
                    String field = (String) fieldObj;
                    if ((pFields==null ||
                            pFields.matcher(field).matches()) &&
                            (pNFields==null ||
                                    !pNFields.matcher(field).matches())) {
                        fieldsToUse.add(field);
                    }
                }
            }
        } else {
            if (fields != null) {
                fieldsToUse.addAll(Arrays.asList(fields.split("|")));
            }
        }
        return fieldsToUse;
    }

    /**
     * Expose the Builder as a remote service over rmi.
     * See dk.statsbiblioteket.summa.score.client.Client for implementation.
     * @throws RemoteException if failed to export remote interfaces
     */
    private void exportRemoteInterfaces() throws RemoteException {
        //TODO implement exportRemoteInterfaces
    }
}



