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

import dk.statsbiblioteket.summa.clusterextractor.data.CentroidSet;
import dk.statsbiblioteket.summa.clusterextractor.data.Vocabulary;
import dk.statsbiblioteket.summa.clusterextractor.data.Word;
import dk.statsbiblioteket.summa.clusterextractor.math.CentroidVector;
import dk.statsbiblioteket.summa.clusterextractor.math.IncrementalCentroid;
import dk.statsbiblioteket.summa.clusterextractor.math.SparseVector;
import dk.statsbiblioteket.summa.clusterextractor.math.SparseVectorMapImpl;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.regex.Pattern;

/**
 * ClusterBuilderImpl builds local vocabulary and centroids.
 * ClusterBuilderImpl implements ClusterBuilder and Remote.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam")
public class ClusterBuilderImpl extends UnicastRemoteObject implements ClusterBuilder {
    protected static final Log log = LogFactory.getLog(ClusterBuilderImpl.class);
    /** Configurations. */
    protected Configuration conf;
    /** Index reader for the work index. */
    protected IndexReader ir;
    /** Machine id for this provider. */
    protected String id;
    /** The merger to upload data to. */
    private ClusterMerger merger;

    /** The vocabulary build and used by this cluster builder. */
    protected Vocabulary vocab = null;

    /** Fields in which to look for candidate terms initially. */
    protected List<String> fieldsUsedInInit;
    /** Fields to use in vectors and queries. */
    protected List<String> fieldsUsedInVectors;

    /** Positive pattern used for filtering the text of the terms. */
    protected Pattern pTerms;
    /** Negative pattern used for filtering the text of the terms. */
    protected Pattern pNTerms;

    /** The zero centroid vector. */
    public static final CentroidVector zero =
            new CentroidVector("zero-vec", 0, null);

    /**
     * Construct cluster builder using given configurations.
     * @param conf configurations
     * @throws java.rmi.RemoteException if failed to export object
     */
    public ClusterBuilderImpl(Configuration conf) throws RemoteException {
        super();
        this.conf = conf;
        this.openIndexReader();
        this.id = conf.getString(LOCAL_MACHINE_ID_KEY);
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

    /**
     * Open {@link IndexReader} to index specified in known {@link Configuration}.
     * @return true if successfull, false otherwise
     */
    private boolean openIndexReader() {
        String index_location = conf.getString(LOCAL_INDEX_PATH_KEY);
        try {
            this.ir = IndexReader.open(index_location);
        } catch (IOException e) {
            log.error("ClusterBuilderImpl constructor not able to open " +
                    "IndexReader; index_location = " + index_location +
                    "; the cluster builder does not work without " +
                    "access to an index.", e);
            return false;
        }
        return true;
    }

    public void buildVocabulary() {
        if (log.isTraceEnabled()) {
            log.trace("ClusterBuilderImpl.buildVocabulary start.");
        }

        if (ir==null && !openIndexReader()) {
            log.error("Vocabulary cannot be build without access to an index.");
            return;
        }
        Vocabulary vocabulary = new Vocabulary();
        Word word;
        TermEnum allTerms;
        try {
            allTerms = ir.terms();
        } catch (IOException e) {
            log.error("IndexReader/TermEnum IOException in " +
                    "ClusterBuilderImpl.buildVocabulary. No vocabulary is built.", e);
            return;
        }
        try {
            // look at all terms (in all fields) in index
            while (allTerms.next()) {
                Term term = allTerms.term();
                int docFreq = allTerms.docFreq();

                // if the term text is 'allowed' and not unique,
                // save word in vocabulary
                if (termTextOk(term.text()) && docFreq>1) {
                    word = new Word(term.text(), docFreq, 1);
                    //TODO: boost factor
                    vocabulary.put(word);
                }
            }
        } catch (IOException e) {
            log.warn("IndexReader/TermEnum IOException in " +
                    "ClusterBuilderImpl.buildVocabulary. Skipping term.", e);
            return;
        }

        File file = saveVocabulary(vocabulary);
        //TODO: look up merger, get handle and move new vocabulary to merger?
        if (file != null && this.merger!=null) {
            this.merger.uploadVocabulary(this.id, -1, vocabulary);
        }

        if (log.isTraceEnabled()) {
            log.trace("ClusterBuilderImpl.buildVocabulay end. " +
                    "Size of voacabulary: " + vocabulary.size());
        }

    }

    /**
     * Save given {@link Vocabulary} in directory specified in {@link Configuration}.
     * @param vocabulary Vocabulary to save
     * @return the file the vocabulary was saved in
     */
    private File saveVocabulary(Vocabulary vocabulary) {
        String localVocabularyPath = conf.getString(LOCAL_VOCAB_PATH_KEY);
        File localVocabularyDirectory = new File(localVocabularyPath);
        if (!localVocabularyDirectory.exists()) {
            localVocabularyDirectory.mkdir();
        }
        long timeStamp = System.currentTimeMillis();
        File file = new File(localVocabularyPath+timeStamp+"vocab.obj");
        vocabulary.save(file);
        return file;
    }

    public void buildCentroids() {
        if (ir==null && !openIndexReader()) {
            log.error("Centroids cannot be build without access to an index.");
            return;
        }
        //TODO: if ??? get new vocab from merger
        if (this.merger!=null) {
            this.vocab = this.merger.getNewVocabulary();
        }

        Set<String> candidateTerms = getCandidateTerms();
        Map<String, Query> queries =
                candidateTermsToQueries
                        (candidateTerms, fieldsUsedInInit);

        CentroidSet centroids = queriesToCentroids(queries);

        File file = saveCentroids(centroids);
        //TODO: look up merger, get handle and move new centroid set to merger?
        if (file != null && this.merger!=null) {
            this.merger.uploadCentroidSet(this.id, -1, centroids);
        }
    }

    /**
     * Save given {@link CentroidSet} in directory specified in {@link Configuration}.
     * @param centroids CentroidSet to save
     * @return the file the centroid set was saved in
     */
    private File saveCentroids(CentroidSet centroids) {
        String localCentroidSetPath = conf.getString(LOCAL_CENTROID_SET_PATH_KEY);
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
     * @return set of centroids calculated from initial search based clusters
     */
    private CentroidSet queriesToCentroids(Map<String, Query> namesToQueries) {
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

        //create centroid set
        int size = namesToQueries.size();
        CentroidSet centroids = new CentroidSet();

        //Array used for statistics on number of non-zero dimensions in doc vectors
        int[] numberOfVectorsWithThisNumberOfDiffTerms = new int[100];

        //iterate through all names and query entries; use queries to get hits
        //and use hits to create centroids
        int clusterCounter = 0;
        Profiler feedback = new Profiler();
        feedback.setExpectedTotal(size);
        for (Map.Entry<String, Query> entry: namesToQueries.entrySet()) {
            feedback.beat();

            Hits hits;
            IndexSearcher is = new IndexSearcher(ir);
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
                    SparseVector vec = getVec(hits.id(hitsIndex), hits.score(hitsIndex));
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
            CentroidVector centroid = incCentroid.getCutCentroidVector(maxFinalSize);
            centroid.normalise();
            centroid.setExpectedSize(hitsIndex-1);
            centroid = calculateSimilarityThreshold(centroid, initialCluster);
            if (log.isTraceEnabled()) {
                log.trace("Similarity threshold: " + centroid.getSimilarityThreshold());
            }

            //put new centroid in centroid set
            centroids.add(centroid);
        }
        if (log.isTraceEnabled()) {
            log.trace("ClusterBuilderImpl.queriesToCentroids end. Statistics: " +
                    "numberOfVectorsWithThisNumberOfDiffTerms = " +
                    Arrays.toString(numberOfVectorsWithThisNumberOfDiffTerms));
        }
        return centroids;
    }

    /**
     * Calculate similarity threshold for the given cluster and centroid.
     * WARNING: this is an expensive method
     * @return the centroid with the threshold set
     * @param centroid the centroid vector for the cluster
     * @param initialCluster the initial cluster is the document vectors used
     *                       to build the centroid
     */
    private CentroidVector calculateSimilarityThreshold(CentroidVector centroid,
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
            centroid.setSimilarityThreshold(similarities[index]);
        }
        return centroid;
    }

    /**
     * Get the vector for the document with this document id.
     * Any stopwords are removed and all dimension names are converted to lowercase.
     * @param docId a document id
     * @return the sparse vector for the document with the given doc id
     */
    public SparseVector getVec(int docId) {
        return getVec(docId, 1);
    }
    /**
     * Get the vector for the document with this document id.
     * The vector entries are weighted with the given score.
     * Any stopwords are removed and all dimension names are converted to lowercase.
     * @param docId a document id
     * @param score the score of this document relative to some Hits result
     * @return the sparse vector for the document with the given doc id
     *         weighted with the given score
     */
    public SparseVector getVec(int docId, float score) {
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
        if (vocab!=null) {
            return vocab.contains(text);
        } else {
            return (pTerms == null || pTerms.matcher(text).matches()) &&
                    (pNTerms == null || !pNTerms.matcher(text).matches());
        }
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
     * @param positiveFieldsKey String key to positive fields property
     * @param negativeFieldsKey String key to negative fields property
     * @return list of filtered fieldnames
     */
    private List<String> getFilteredFields(String positiveFieldsKey, String negativeFieldsKey) {
        String fields = conf.getString(positiveFieldsKey);
        Pattern pFields = fields == null || fields.equals("") ? null : Pattern.compile(fields);
        String nFields = conf.getString(negativeFieldsKey);
        Pattern pNFields = nFields == null || nFields.equals("") ? null : Pattern.compile(nFields);

        List<String> fieldsToUseInInit = new LinkedList<String>();
        Collection fieldNames = ir.getFieldNames(IndexReader.FieldOption.INDEXED_WITH_TERMVECTOR);
        for (Object fieldObj: fieldNames) {
            if (fieldObj instanceof String) {
                String field = (String) fieldObj;
                if ((pFields==null ||
                        pFields.matcher(field).matches()) &&
                        (pNFields==null ||
                                !pNFields.matcher(field).matches())) {
                    fieldsToUseInInit.add(field);
                }
            }
        }
        return fieldsToUseInInit;
    }

    public void registerMerger(ClusterMerger merger) {
        this.merger = merger;
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
