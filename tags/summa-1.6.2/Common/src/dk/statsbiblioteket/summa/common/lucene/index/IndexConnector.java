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
package dk.statsbiblioteket.summa.common.lucene.index;

import java.io.IOException;
import java.io.File;
import java.util.List;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.ParallelReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.RAMDirectory;

/**
 * Main entry for accessing indexes. Readers and Searchers can be requested.
 * Both Readers and Searchers are Singletons.
 * </p><p>
 * The format for the configuration is a strict tree, with the index root on
 * top, multiIndex and parallelIndex as nodes and singleIndex as leafs.
 * SingleIndex: Leaf. LINKS should specify the location of the index.
 * MultiIndex:  Node. LINKS should point to other entries in the configuration.
 *              The children of a multi index can be perceived as one big
 *              index with the children in serial.
 * ParallelIndex: As MultiIndex, but the children are accessed in parallel,
 *                which also means that their docID's must match.
 * RAMIndex:    Leaf. LINKS should specify the location of the index.
 *              The index will be loaded into RAM upon connection.
 *
 * An example setup for a parallel index;
 * facetbrowser.indexrootTYPE=ParallelIndex
 * facetbrowser.indexrootLINKS=facetbrowser.mainindex, facetbrowser.clusterindex
 * facetbrowser.mainindexTYPE=SingleIndex
 * facetbrowser.mainindexLINKS=/tmp/mainindexfolder
 * facetbrowser.clusterindexTYPE=SingleIndex
 * facetbrowser.clusterindexLINKS=/home/summa/broadclusters
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment = "Needs JavaDoc")
public class IndexConnector {
    private final Log log = LogFactory.getLog(IndexConnector.class);

    public static enum INDEXTYPE {
        singleIndex { String getPropertyKey() { return "SingleIndex"; } },
        multiIndex { String getPropertyKey() { return "MultiIndex"; } },
        parallelIndex { String getPropertyKey() { return "ParallelIndex"; } },
        ramIndex { String getPropertyKey() { return "RAMIndex"; } };
        String getPropertyValue;
        INDEXTYPE getEnum(String propertyValue) {
            String p = propertyValue.toLowerCase();
            if (p.equals("singleindex")) { return singleIndex; }
            if (p.equals("multiindex")) { return multiIndex; }
            if (p.equals("parallelindex")) { return parallelIndex; }
            if (p.equals("ramindex")) { return ramIndex; }
            throw new IllegalArgumentException("Unknown index type: "
                                               + propertyValue);
        }
    }

    /**
     * The root should be postfixed with TYPE and LINKS. The type specifies an
     * INDEXTYPE, while the inks contains either a directory (when the root type
     * is singleIndex) or a list of property keys. The keys must be prefixes for
     * property entries keynameTYPE and keynameLINKS.
     */
    public static final String INDEXROOT = "facetbrowser.indexroot";
    public static final String TYPE = "TYPE";
    public static final String LINKS = "LINKS";

    private Configuration configuration;
    private IndexReader reader;
    private IndexSearcher searcher;

    /**
     * Create an IndexConnector around a single index at the specified location.
     * This constructor is mainly for testing and it is recommended to use
     * {@link #IndexConnector(Configuration)} in production code, due to the
     * greater flexibility.
     * @param indexLocation a folder with a Lucene index.
     */
    public IndexConnector(File indexLocation) {
        log.info("Creating an IndexConnector with single index at '"
                 + indexLocation + "'");
        configuration = Configuration.newMemoryBased(INDEXROOT + TYPE,
                                         INDEXTYPE.singleIndex.getPropertyValue,
                                         INDEXROOT + LINKS, indexLocation);
    }

    /**
     * Create an IndexConnector based on the given configuration. The index
     * won't be opened before {@link #getReader} or {@link #getSearcher} is
     * called.
     * @param configuration the setup for Indexreader. See the class
     *                      documentation for details.
     */
    public IndexConnector(Configuration configuration) {
        log.debug("Creating a IndexConnector from a configuration");
        this.configuration = configuration;
    }

    public IndexReader getReader() throws IOException {
        if (reader == null) {
            log.debug("Attempting to construct a reader");
            reader = getNewReader(INDEXROOT);
        }
        return reader;
    }

    public IndexReader getNewReader() throws IOException {
        log.debug("Attempting to construct a guaranteed new reader");
        reader = getNewReader(INDEXROOT);
        return reader;
    }

    public synchronized IndexSearcher getSearcher() throws IOException {
        if (searcher == null) {
            log.debug("Attempting to construct a searcher");
            searcher = new IndexSearcher(getReader());
        }
        return searcher;
    }

    // Should we reuse searcher?
    public IndexSearcher getNewSearcher() throws IOException {
        log.debug("Attempting to construct a guaranteed new searcher");
        searcher = new IndexSearcher(getNewReader());
        return searcher;
    }

    private IndexReader getNewReader(String propertyKey) throws IOException {
        String iTypeS = configuration.getString(propertyKey + "TYPE");
        if (iTypeS == null) {
            throw new IllegalAccessError("The property " + propertyKey
                                         + "TYPE could not be retrieved from "
                                         + "the configuration");
        }
        String linksS = configuration.getString(propertyKey + "LINKS");
        if (linksS == null) {
            throw new IllegalAccessError("The property " + propertyKey
                                         + "LINKS could not be retrieved from "
                                         + "the configuration");
        }
        List<String> indexKeys = configuration.getStrings(propertyKey
                                                          + "LINKS");

        switch (INDEXTYPE.multiIndex.getEnum(iTypeS)) {
            case singleIndex:
                log.info("Constructing Summa reader for '" + linksS + "'");
                return IndexReader.open(new NIOFSDirectory(new File(linksS)));
            case multiIndex:
                IndexReader[] subReaders = new IndexReader[indexKeys.size()];
                int pos = 0;
                for (String indexKey: indexKeys) {
                    log.info("Getting sub-reader " + (pos+1) + "/"
                             + indexKeys.size() + " '" + indexKey
                             + "' for MultiReader");
                    subReaders[pos++] = getNewReader(indexKey);
                }
                log.info("Constructing MultiReader based on " + indexKeys.size()
                         + " sub-readers");
                return new MultiReader(subReaders);
            case parallelIndex:
                ParallelReader parallel = new ParallelReader();
                int counter = 1;
                for (String indexKey: indexKeys) {
                    log.info("Getting sub-reader " + counter + "/"
                             + indexKeys.size() + " '" + indexKey
                             + "' for ParallelReader");
                    parallel.add(getNewReader(indexKey));
                }
                log.info("Returning ParallelReader based on " + indexKeys.size()
                         + " sub-readers");
                return parallel;
            case ramIndex:
                log.info("Loading the index at location '" + linksS
                         + "' into RAM...");
                RAMDirectory ramDir = new RAMDirectory(
                                          new NIOFSDirectory(new File(linksS)));
                log.info("The index at location '" + linksS + " was loaded into"
                         + " RAM. Constructing RAM-based reader");
                return IndexReader.open(ramDir);
            default:
                throw new IllegalStateException("Unhandled index type: "
                                       + INDEXTYPE.multiIndex.getEnum(iTypeS));
        }
    }
}




