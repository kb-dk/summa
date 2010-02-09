package dk.statsbiblioteket.summa.support.api;

import dk.statsbiblioteket.summa.search.api.Response;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;

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

    private String result = null;
    private String query = null;

    public DidYouMeanResponse(String query, String result) {
        this.result = result;
        this.query = query;
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
            writer.writeCharacters("    ");
            writer.writeStartElement(DIDYOUMEAN);
            writer.writeCharacters(result);
            writer.writeEndElement();
            writer.writeCharacters("\n");
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
}
