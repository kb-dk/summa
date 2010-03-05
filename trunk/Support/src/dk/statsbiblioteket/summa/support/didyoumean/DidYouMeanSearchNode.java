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
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaStandardAnalyzer;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.support.api.DidYouMeanKeys;
import dk.statsbiblioteket.summa.support.api.DidYouMeanResponse;
import dk.statsbiblioteket.util.qa.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.facade.DirectoryIndexFacade;
import org.apache.lucene.index.facade.IndexFacade;
import org.apache.lucene.search.didyoumean.Suggestion;
import org.apache.lucene.search.didyoumean.SuggestionPriorityQueue;
import org.apache.lucene.search.didyoumean.secondlevel.token.MultiTokenSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.TokenPhraseSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.ngram.NgramTokenSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.ngram.TermEnumIterator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * DidYouMeanSearchNode creating the index used for the did-you-mean query and
 * doing the actual search. 
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since Feb 9, 2010
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hbk")
public class DidYouMeanSearchNode extends SearchNodeImpl {
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
    public static final String DEFAULT_DIDYOUMEAN_APRIORI_FIELD = "freetext";

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
    public static final Class<? extends Analyzer> DEFAULT_DIDYOUMEAN_ANALYZER =
                                            SummaStandardAnalyzer.class;
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
    public static final String DEFAULT_DIDYOUMEAN_DIRECTORY = "fsDirectory";

    /**
     * The configuration field in configuration file for the Did-you-mean
     * FSDirectory directory, in case
     * {@link DidYouMeanSearchNode#CONF_DIDYOUMEAN_FSDIRECTORY} is set to
     * default value.
     */
    public static final String CONF_DIDYOUMEAN_FSDIRECTORY =
                                         "summa.support.didyoumean.fsdirectory";
    public static final String DEFAULT_DIDYOUMEAN_FSDIRECTORY = "FSDirectory";

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
     * Local variable pointing to FS directory.
     */
    private String fsDirectory = null;

    /**
     * Constructor for DidYouMeanSearchNode. Get needed configuration values.
     *
     * @param config The configuration for this instance.
     * @throws IOException if error opening FSDirectory or value of
     *  {@link DidYouMeanSearchNode#CONF_DIDYOUMEAN_DIRECTORY} isn't valid.
     */
    public DidYouMeanSearchNode(Configuration config) throws IOException {
        super(config);
        aprioriField = config.getString(CONF_DIDYOUMEAN_APRIORI_FIELD,
                                              DEFAULT_DIDYOUMEAN_APRIORI_FIELD);
        Class<? extends Analyzer> analyzerClass = Configuration.getClass(
                                       CONF_DIDYOUMEAN_ANALYZER, Analyzer.class,
                                       DEFAULT_DIDYOUMEAN_ANALYZER, config);
        analyzer = Configuration.create(analyzerClass, config);
        String directoryType = config.getString(CONF_DIDYOUMEAN_DIRECTORY,
                                                  DEFAULT_DIDYOUMEAN_DIRECTORY);
        DIRECTORYTYPE type = DIRECTORYTYPE.valueOf(directoryType);

        fsDirectory = config.getString(CONF_DIDYOUMEAN_FSDIRECTORY,
                                                DEFAULT_DIDYOUMEAN_FSDIRECTORY);

        switch(type) {
            case ramDirectory:
                directory = new RAMDirectory();
                break;
            case fsDirectory:
                directory = FSDirectory.getDirectory(fsDirectory);
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
     * @param request as specified in
     *       {@link dk.statsbiblioteket.summa.search.SearchNode#warmup(String)}.
     */
    @Override
    protected void managedWarmup(String request) {
        // Not needed.
    }

    /**
     * Create needed locale: AprioriIndex, TokenSuggester and PhraseSuggester.
     *
     * @param location as specified in
     * {@link dk.statsbiblioteket.summa.search.SearchNode#open(String)}.
     * @throws RemoteException is thrown if an IOException is cast during
     * creation of a local datastructure.
     */
    @Override
    protected void managedOpen(String location) throws RemoteException {
        log.debug("Opening '" + location + "'");
        IndexFacade aprioriIndexFactory;

        // Setup AprioriIndex
        try {
            aprioriIndexFactory = new DirectoryIndexFacade(
                                            FSDirectory.getDirectory(location));
            aprioriIndex = aprioriIndexFactory.indexReaderFactory();
        } catch (IOException e) {
            throw new RemoteException("IOException when opening Lucene index.",
                                                                             e);
        }

        // Setup TokenSuggester
        NgramTokenSuggester tokenSuggester;
        try {
            log.debug("Building token index");
            IndexFacade ngramIndexFactory = new DirectoryIndexFacade(directory);
            // Initialize empty index
            ngramIndexFactory.indexWriterFactory(null, true).close();
            tokenSuggester = new NgramTokenSuggester(ngramIndexFactory);
            tokenSuggester.indexDictionary(
                           new TermEnumIterator(aprioriIndex, aprioriField), 2);
        } catch (IOException e) {
            throw new RemoteException("IOException when creating ngramIndex.",
                                                                             e);
        }

        // Setup PhraceSuggester
        try {
            //phraseSuggester = new TokenPhraseSuggesterImpl(tokenSuggester,
            //                  aprioriField, false, 3, analyzer, aprioriIndex);
            phraseSuggester = new MultiTokenSuggester(tokenSuggester,
                         aprioriField, false, 3, analyzer, aprioriIndexFactory);
        } catch (IOException e) {
            throw new RemoteException(
                            "IOException while creating phraseSuggester", e);
        }
        
    }

    /**
     * Closes open indexes.
     * @throws RemoteException if IOException is catched when closing the
     * indexes.
     */
    @Override
    protected void managedClose() throws RemoteException {
        log.debug("Close");
        try {
            aprioriIndex.close();
            // TODO tokenSuggester.close();
            // TODO phraseSuggester.close();
            // TODO now sure if this can be closed after usage in managedOpen.
            directory.close();
        } catch (IOException e) {
            throw new RemoteException("IOException while closing indexes.", e);
        }

    }

    /**
     * Manage the search, by giving the local phraseSuggester the 'query' and
     * 'number of suggestion'.
     * Note:
     * <ul>
     *  <li>'number of suggestion' can be overidden in 'request' contains
     *      {@link DidYouMeanKeys#SEARCH_MAX_RESULTS} is set.</li>
     *  <li>'query' is found in the 'request' key
     *      {@link DidYouMeanKeys#SEARCH_QUERY}.</li>
     * </ul>
     * SIDEEFFECT:
     * <ul>
     *  <li>The resulting XML is added to the 'response' parameter.</li>
     * </ul>
     *  
     * @param request   as specified in
     *        {@link dk.statsbiblioteket.summa.search.SearchNode#search(Request,
     *                 dk.statsbiblioteket.summa.search.api.ResponseCollection)}
     * @param responses as specified in
     *        {@link dk.statsbiblioteket.summa.search.SearchNode#search(Request,
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
            SuggestionPriorityQueue spq =
                                phraseSuggester.suggest(query, numSuggestions);
            time = System.currentTimeMillis() - time;

            DidYouMeanResponse response = new DidYouMeanResponse(query, time);
            responses.add(response);
            if(spq.size() > 0) {
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
