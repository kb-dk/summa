package dk.statsbiblioteket.summa.support.didyoumean;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaStandardAnalyzer;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.support.api.DidYouMeanKeys;
import dk.statsbiblioteket.summa.support.api.DidYouMeanResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.facade.DirectoryIndexFacade;
import org.apache.lucene.index.facade.IndexFacade;
import org.apache.lucene.search.didyoumean.secondlevel.token.MultiTokenSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.TokenPhraseSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.ngram.NgramTokenSuggester;
import org.apache.lucene.search.didyoumean.secondlevel.token.ngram.TermEnumIterator;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * FIXME: Missing class docs for dk.statsbiblioteket.summa.support.didyoumean.DidYouMeanSearchNode
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since Feb 9, 2010
 */
public class DidYouMeanSearchNode extends SearchNodeImpl {
    private static final Log log = LogFactory.getLog(DidYouMeanSearchNode.class);

    public static final String CONF_APRIORI_FIELD =
                                        "summa.support.didyoumean.apriorifield";
    public static final String DEFAULT_APRIORI_FIELD = "freetext";

    public static final String CONF_DIDYOUMEAN_ANALYZER =
                                        "summa.support.didyoumean.analyzer";

    public static final Class<? extends Analyzer> DEFAULT_DIDYOUMEAN_ANALYZER =
                                            SummaStandardAnalyzer.class;

    private String aprioriField = null;

    private Analyzer analyzer = null;

    private TokenPhraseSuggester phraseSuggester = null;
    private NgramTokenSuggester tokenSuggester = null;
    private IndexReader aprioriIndex = null;

    public DidYouMeanSearchNode(Configuration config) {
        super(config);
        aprioriField = config.getString(
                                    CONF_APRIORI_FIELD, DEFAULT_APRIORI_FIELD);
        Class<? extends Analyzer> analyzerClass = Configuration.getClass(
                                       CONF_DIDYOUMEAN_ANALYZER, Analyzer.class,
                                       DEFAULT_DIDYOUMEAN_ANALYZER, config);
        analyzer = Configuration.create(analyzerClass, config);

        log.debug("Using apriori field '" + aprioriField +"'");
        log.debug("Using analyzer '" + analyzerClass.getName() + "'");
    }

    @Override
    protected void managedWarmup(String request) {

    }

    @Override
    protected void managedOpen(String location) throws RemoteException {
        log.debug("Opening '" + location + "'");
        IndexFacade aprioriIndexFactory = null;

        try {
            aprioriIndexFactory = new DirectoryIndexFacade(
                                            FSDirectory.getDirectory(location));
            aprioriIndex = aprioriIndexFactory.indexReaderFactory();
        } catch (IOException e) {
            throw new RemoteException("IOException when opening Lucene index.", e);
        }

        try {
            log.debug("Building token index");
            IndexFacade ngramIndexFactory = new DirectoryIndexFacade(new RAMDirectory());
            ngramIndexFactory.indexWriterFactory(null, true).close(); // Initialize empty index
            tokenSuggester = new NgramTokenSuggester(ngramIndexFactory);
            tokenSuggester.indexDictionary(new TermEnumIterator(aprioriIndex, aprioriField), 2);
        } catch (IOException e) {
            throw new RemoteException("IOException when creating ngramIndex.", e);
        }


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

    @Override
    protected void managedClose() throws RemoteException {
        log.debug("Close");
        try {
            aprioriIndex.close();
            // TODO tokenSuggester.close();
            // TODO phraseSuggester.close();
        } catch (IOException e) {
            throw new RemoteException("IOException while closing indexes.", e);
        }

    }

    @Override
    protected void managedSearch(Request request, ResponseCollection responses)
                                                        throws RemoteException {
        if(request.containsKey(DidYouMeanKeys.SEARCH_KEY)) {
            String query = request.getString(DidYouMeanKeys.SEARCH_KEY);
            String result = phraseSuggester.didYouMean(query);
            if(result != null) {
                log.debug("Did-you-mean '" + query + "' -> '" + result + "'");
                responses.add(new DidYouMeanResponse(query, result));
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
