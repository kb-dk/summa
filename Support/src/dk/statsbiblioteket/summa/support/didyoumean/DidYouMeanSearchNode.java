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
package dk.statsbiblioteket.summa.support.didyoumean;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaStandardAnalyzer;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.support.api.DidYouMeanKeys;
import dk.statsbiblioteket.summa.support.api.DidYouMeanResponse;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.facade.DirectoryIndexFacade;
import org.apache.lucene.index.facade.IndexFacade;
import org.apache.lucene.search.didyoumean.Suggestion;
import org.apache.lucene.search.didyoumean.SuggestionPriorityQueue;
import org.apache.lucene.search.didyoumean.secondlevel.token.SpanNearTokenPhraseSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.TokenPhraseSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.ngram.NgramTokenSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.ngram.TermEnumIterator;
import org.apache.lucene.store.*;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

/**
 * DidYouMeanSearchNode creating the index used for the did-you-mean query and
 * doing the actual search. 
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @author Henrik Bitsch Kirk <mailto:hbk@statsbiblioteket.dk>
 * @since Feb 9, 2010
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke, hbk")
public class DidYouMeanSearchNode extends SearchNodeImpl {
    /**
         * Enum type for Lucene directory.
         */
    private static enum DIRECTORYTYPE {
        fsDirectory,
        ramDirectory
    }

    /**
         * Log factory.
         */
    private static final Log log =
                                  LogFactory.getLog(DidYouMeanSearchNode.class);

    /**
         * The configuration field in configuration file for the apriorifield.
         */
    public static final String CONF_DIDYOUMEAN_APRIORI_FIELD =
                                        "summa.support.didyoumean.apriorifield";
    /**
         * The default value for
         * {@link DidYouMeanSearchNode#CONF_DIDYOUMEAN_APRIORI_FIELD}.
         * The value is 'freetext'.
         */
    public static final String DEFAULT_DIDYOUMEAN_APRIORI_FIELD
                                                                   = "freetext";

    /**
         * The configuration file in the configuration file. This is a boolean which
         * in case of true, throws a fatel exception in managedOpen, if no index
         * exists.
         */
    public static final String
                      CONF_DIDYOMEAN_CLOSE_ON_NON_EXISTING_INDEX
                           = "summa.support.didyoumean.closeonnonexistingindex";
    /**
         * Default value for
         * {@link #CONF_DIDYOMEAN_CLOSE_ON_NON_EXISTING_INDEX}.
         */
    public static final boolean
                   DEFAULT_DIDYOMEAN_CLOSE_ON_NON_EXISTING_INDEX = true;

    /**
         * The configuration field in configuration file for the Did-You-Mean
         * analyzer.
         */
    public static final String CONF_DIDYOUMEAN_ANALYZER =
                                        "summa.support.didyoumean.analyzer";
    /**
         * Default value of {@link DidYouMeanSearchNode#CONF_DIDYOUMEAN_ANALYZER},
         * the value is {@link SummaStandardAnalyzer}
         */
    public static final Class<? extends Analyzer>
                DEFAULT_DIDYOUMEAN_ANALYZER = SummaStandardAnalyzer.class;
    
    /**
         * The configuration field in configuration file for the Did-You-Mean
         * directory type.
         * Possibilities:
         * <ul>
         *  <li>fsDirectory</li>
         *  <li>ramDirectory</li>
         * </ul>
         */
    public static final String CONF_DIDYOUMEAN_DIRECTORY =
                                           "summa.support.didyoumean.directory";
    /**
         * Default value for {@link #CONF_DIDYOUMEAN_DIRECTORY}.
         */
    public static final String DEFAULT_DIDYOUMEAN_DIRECTORY =
                                                                  "fsDirectory";

    /**
         * Where to place the Did-You-Mean index in the persistant storage.
         * Note: only used in combination with fsDirectory.
         */
    public static final String CONF_DIDYOUMEAN_LOCATION =
                                            "summa.support.didyoumean.location";
    /**
         * Default value for {@link #CONF_DIDYOUMEAN_LOCATION}.
         */
    public static final String DEFAULT_DIDYOUMEAN_LOCATION = "didyoumean";

    /**
         * Local directory version.
         */
    private Directory directory = null;

    /**
         * Local variable for apriori field.
         */
    private String aprioriField = null;

    /**
         * Local value for Analyzer.
         */
    private Analyzer analyzer = null;

    /**
         * Local TokenPhraseSuggester
         */
    private TokenPhraseSuggester phraseSuggester = null;
    
    /**
         * Local IndexReader.
         */
    private IndexReader aprioriIndex = null;

    /**
         * Private copy of the local location of the didyoumean index on disc. Not
         * used when using RAMDirectory.
         */
    private File didyoumeanIndex;

    /**
         * True if we are creating index, false otherwise. Used to stop searching
         * when creating index.
         */
    private boolean creatingIndex = true;

    /**
         * If true, we close when we find no index.
         */
    private boolean closeOnNonExistingIndex;
    NgramTokenSuggester tokenSuggester;
    IndexFacade aprioriIndexFactory;
    /**
         * Constructor for DidYouMeanSearchNode. Get needed configuration values.
         *
         * @param config The configuration for this instance.
         * @throws IOException if error opening FSDirectory or value of
         *  {@link DidYouMeanSearchNode#CONF_DIDYOUMEAN_DIRECTORY} isn't valid.
         */
    public DidYouMeanSearchNode(Configuration config) throws IOException {
        super(config);

        closeOnNonExistingIndex =
           config.getBoolean(CONF_DIDYOMEAN_CLOSE_ON_NON_EXISTING_INDEX,
                         DEFAULT_DIDYOMEAN_CLOSE_ON_NON_EXISTING_INDEX);

        // Get AprioriField.
        aprioriField = config.getString(CONF_DIDYOUMEAN_APRIORI_FIELD,
                                         DEFAULT_DIDYOUMEAN_APRIORI_FIELD);

        // Get Anaylyzer class.
        Class<? extends Analyzer> analyzerClass = Configuration.getClass(
                                 CONF_DIDYOUMEAN_ANALYZER, Analyzer.class,
                                     DEFAULT_DIDYOUMEAN_ANALYZER, config);
        analyzer = Configuration.create(analyzerClass, config);

        // Get directory class.
        String directoryType = config.getString(CONF_DIDYOUMEAN_DIRECTORY,
                                            DEFAULT_DIDYOUMEAN_DIRECTORY);
        DIRECTORYTYPE type = DIRECTORYTYPE.valueOf(directoryType);

        // determining the placement for the Did-You-Mean index.
        String placement = config.getString(CONF_DIDYOUMEAN_LOCATION,
                                            DEFAULT_DIDYOUMEAN_LOCATION);
        didyoumeanIndex = Resolver.getPersistentFile(new File(placement));

        switch(type) {
            case ramDirectory:
                directory = new RAMDirectory();
                break;
            case fsDirectory:
                directory = new NIOFSDirectory(didyoumeanIndex);
                break;
            default:
                String error = "Directory '" + directoryType
                        + "' didn't correspond to a known type";
                log.error(error);
                throw new IOException(error);
        }
        log.debug("Using Directory '" + directoryType + "'.");
        log.debug("Using apriori field '" + aprioriField +"'.");
        log.debug("Using analyzer '" + analyzerClass.getName() + "'.");
    }

    /**
         * Nothing is done at warmup.
         *
         * @param request As specified in
         * {@link dk.statsbiblioteket.summa.search.SearchNode#warmup(String)}.
         */
    @Override
    protected void managedWarmup(String request) {
        // Not needed.
    }

    /**
         * Create needed locale indexes: AprioriIndex, TokenSuggester and
         * PhraseSuggester. If index is already created we uses old index.
         *
         * @param location As specified in
         * {@link dk.statsbiblioteket.summa.search.SearchNode#open(String)}.
         * Note: not used, we uses {@link Resolver#getPersistentFile}.
         * @throws RemoteException is thrown if an IOException is cast during
         * creation of a local data structure.
         */
    @Override
    protected void managedOpen(String location) throws RemoteException {
        location = location.concat(File.separator + "lucene"  + File.separator);
        log.debug("Opening Lucene index at '" + location + "'");

        IndexFacade ngramIndexFactory;
        // Setup AprioriIndex
        try {
            aprioriIndexFactory = new DirectoryIndexFacade(
                                     new NIOFSDirectory(new File(location)));
            //aprioriIndex = aprioriIndexFactory.indexReaderFactory();
        } catch (IOException e) {
            throw new RemoteException(
                                   "IOException when opening Lucene index", e);
        }

        // create DirectoryIndexFacede
        if(directory instanceof FSDirectory && didyoumeanIndex.exists()) {
            log.info("Using existing DidYouMean index in '"
                                    + didyoumeanIndex.getAbsolutePath() + "'.");
            try {
                ngramIndexFactory = new DirectoryIndexFacade(directory);
                // opening index
                ngramIndexFactory.indexWriterFactory(null, false).close();
            } catch(IOException e) {
                throw new RemoteException(
                            "IOException when opening directoryIndexFaced", e);
            }
        } else {
            if(closeOnNonExistingIndex) {
                String error =
                        "There does not exists an DidYouMean-index in given "
                              + "location '" + didyoumeanIndex.getAbsolutePath() 
                              + "'";
                log.fatal(error);
                closeIndexes();
                throw new RemoteException(error);
            }
            log.info("Creating new Did-You-Mean index");
            try {
                ngramIndexFactory = new DirectoryIndexFacade(directory);
                // Initialize empty index
                ngramIndexFactory.indexWriterFactory(null, true).close();
            } catch(IOException e) {
                throw new RemoteException(
                           "IOException when creating directoryIndexFaced", e);
            }
        }

        // Setup TokenSuggester
        try {
            log.debug("Building NgramTokenSuggester index");            
            tokenSuggester = new NgramTokenSuggester(ngramIndexFactory);
            aprioriIndex = aprioriIndexFactory.indexReaderFactory();
            tokenSuggester.indexDictionary(
                       new TermEnumIterator(aprioriIndex, aprioriField), 2);

        } catch(IOException e) {
            throw new RemoteException(
                                    "IOException when creating ngramIndex", e);
        }

        // Setup PhraseSuggester
        try {
            phraseSuggester = new SpanNearTokenPhraseSuggester(tokenSuggester,
                         aprioriField, false, 3, analyzer, aprioriIndexFactory);
            //phraseSuggester = new MultiTokenSuggester(tokenSuggester,
            //             aprioriField, false, 3, analyzer, aprioriIndexFactory);
        } catch (IOException e) {
            throw new RemoteException(
                            "IOException while creating phraseSuggester", e);
        }
        creatingIndex = false;
    }

    /**
        * Closes open indexes.
        * @throws RemoteException if IOException is catched when closing the
        * indexes.
        */
    @Override
    protected void managedClose() throws RemoteException {
        log.debug("Closing DidYouMean");
        closeIndexes();
    }

    /**
         * Private helper method to close indexes.
         *
         * @throws RemoteException if IOException is encountered doing close.
         */
    private void closeIndexes() throws RemoteException {
        try {
            aprioriIndex.close();
            tokenSuggester.close();
            phraseSuggester.close();
            aprioriIndexFactory.close();
            // TODO directory.close() should be handled by DirectoryIndexFacede
            //directory.close();
        } catch (NullPointerException e) {
          log.info("DidYouMean index not opened");
        } catch (IOException e) {
          throw new RemoteException("IOException while closing DidYouMean index",
                                    e);
        }
    }

    /**
         * Manage the search, by giving the local phraseSuggester the 'query' and
         * 'number of suggestion'. When creating or opening we return an empty
         * response.
         * Note:
         * <ul>
         *  <li>'number of suggestion' can be overidden in 'request' contains
         *      {@link DidYouMeanKeys#SEARCH_MAX_RESULTS} is set.</li>
         *  <li>'query' is found in the 'request' key
         *      {@link DidYouMeanKeys#SEARCH_QUERY}.</li>
         * </ul>
         * Side-effect:
         * <ul>
         *  <li>The resulting XML is added to the 'response' parameter.</li>
         * </ul>
         *
         * @param request As specified in
         * {@link dk.statsbiblioteket.summa.search.SearchNode#search(Request,
         *                 dk.statsbiblioteket.summa.search.api.ResponseCollection)}
         * @param responses As specified in
         * {@link dk.statsbiblioteket.summa.search.SearchNode#search(Request,
         *                                                      ResponseCollection)}
         * @throws RemoteException dictated by overriding method.
         */
    @Override
    protected void managedSearch(Request request, ResponseCollection responses)
                                                        throws RemoteException {
        int numSuggestions = 1;
        if (request.containsKey(DidYouMeanKeys.SEARCH_MAX_RESULTS)) {
            numSuggestions = request.getInt(DidYouMeanKeys.SEARCH_MAX_RESULTS);
        }

        if(request.containsKey(DidYouMeanKeys.SEARCH_QUERY)) {
            String query = request.getString(DidYouMeanKeys.SEARCH_QUERY);
            long time = System.currentTimeMillis();
            SuggestionPriorityQueue spq = null;
            // if creating index then don't ask phraseSuggester
            if(!creatingIndex) {
                spq = phraseSuggester.suggest(query, numSuggestions);
            } else {
                log.debug("Creating/opening index.");
            }
            time = System.currentTimeMillis() - time;

            DidYouMeanResponse response = new DidYouMeanResponse(query, time);
            responses.add(response);
            if(spq != null && spq.size() > 0) {
                log.debug("Did-you-mean '" + query + "' returned '" + spq.size()
                          + "' results.");
                for(Suggestion suggestion: spq.toArray()) {
                    response.addResult(suggestion.getSuggested(),
                                       suggestion.getScore(),
                                       suggestion.getCorpusQueryResults());
                }
            } else {
                log.debug("No did-you-mean result for '" + query + "'");
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Ignoring request, no known search keys set");
            }
        }
    }
}
