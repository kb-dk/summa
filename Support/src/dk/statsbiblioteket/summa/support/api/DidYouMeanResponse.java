package dk.statsbiblioteket.summa.support.api;

import dk.statsbiblioteket.summa.search.api.Response;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * FIXME: Missing class docs for dk.statsbiblioteket.summa.support.api.DidYouMeanResponse
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since Feb 9, 2010
 */
public class DidYouMeanResponse implements Response {
    public static final String DIDYOUMEANRESPONSE = "DidYouMeanResponse";

    public static final String NAMESPACE =
            "http://statsbiblioteket.dk/summa/2009/DidYouMeanResponse";
    public static final String VERSION = "1.0";

    public static final String DIDYOUMEAN_RESPONSE = "DidYouMeanResponse";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String VERSION_TAG = "version";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String QUERY_TAG = "query";
    public static final String MAXRESULTS_TAG = "maxResults";

    public static final String DIDYOUMEANS = "didyoumeans";

    public static final String DIDYOUMEAN = "didyoumean";
    public static final String HITS_TAG = "hits";
    public static final String QUERYCOUNT_TAG = "queryCount";

    private static XMLOutputFactory xmlOutputFactory =
                                                XMLOutputFactory.newInstance();

    private String query = null;
    private LinkedList<ResultTuple> resultTuples = null;

    public DidYouMeanResponse(String query) {
        this.query = query;
        resultTuples = new LinkedList<ResultTuple>();
    }

    @Override
    public String getName() {
        return DIDYOUMEANRESPONSE;
    }

    @Override
    public void merge(Response other) throws ClassCastException {
        // TODO something nice to other.
    }

    @Override
    public String toXML() {
        StringWriter sw = new StringWriter(2000);
        XMLStreamWriter writer;
        try {
            writer = xmlOutputFactory.createXMLStreamWriter(sw);
        } catch (XMLStreamException e) {
            throw new RuntimeException(
                    "Unable to create XMLStreamWriter from factory", e);
        }
        try {
            writer.setDefaultNamespace(NAMESPACE);
            //writer.writeStartDocument();
            writer.writeStartElement(DIDYOUMEAN_RESPONSE);
            writer.writeDefaultNamespace(NAMESPACE);
            writer.writeAttribute(VERSION_TAG, VERSION);
            writer.writeAttribute(QUERY_TAG, query);
            writer.writeCharacters("\n");
            for(ResultTuple tuple: resultTuples) {
                writer.writeCharacters("    ");
                writer.writeStartElement(DIDYOUMEAN);
                writer.writeAttribute("score", String.valueOf(tuple.getScore()));
                //writer.writeAttribute("corpusqueryresults", String.valueOf(tuple.corpusQueryResults));
                writer.writeCharacters(tuple.getResult());
                writer.writeEndElement();
                writer.writeCharacters("\n");
            }
            writer.writeEndElement();
            writer.writeCharacters("\n");
            writer.writeEndDocument();
            
            writer.flush(); // Just to make sure
        } catch (XMLStreamException e) {
            throw new RuntimeException(
                    "Got XMLStreamException while constructing XML from "
                    + "DidYouMeanResponse", e);
        }
        return sw.toString();
    }

    public void addResult(String result, double score, int corpusQueryResults) {
        resultTuples.addFirst(new ResultTuple(result, score, corpusQueryResults));
    }

    private static class ResultTuple implements Serializable {
        private String result = null;
        private double score = 0d;
        private int corpusQueryResults = 0;

        public ResultTuple(String result, double score, int corpusQueryResults) {
            this.result = result;
            this.score = score;
            this.corpusQueryResults = corpusQueryResults;
        }         

        public String getResult() {
            return result;
        }

        public double getScore() {
            return score;
        }

        public int getCorpusQueryResults() {
            return corpusQueryResults;
        }
    }

}
