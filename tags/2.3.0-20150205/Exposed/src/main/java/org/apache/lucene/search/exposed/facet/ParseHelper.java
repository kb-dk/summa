package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.search.exposed.compare.ComparatorFactory;
import org.apache.lucene.search.exposed.facet.request.FacetRequest;
import org.apache.lucene.util.BytesRef;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.Comparator;

public class ParseHelper {
  public static int getInteger(String request, String attribute, String value) throws XMLStreamException {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new XMLStreamException("The integer for " + attribute + " was '"
          + value + "' which is not valid. Full request: " + request);
    }
  }

  public static boolean atStart(XMLStreamReader reader, String tag) {
    return reader.getEventType() == XMLStreamReader.START_ELEMENT
        && tag.equals(reader.getLocalName())
        && FacetRequest.NAMESPACE.equals(reader.getNamespaceURI());
  }

  public static boolean atEnd(XMLStreamReader reader, String tag) {
    return reader.getEventType() == XMLStreamReader.END_ELEMENT
        && tag.equals(reader.getLocalName())
        && FacetRequest.NAMESPACE.equals(reader.getNamespaceURI());
  }

  public static Comparator<BytesRef> createComparator(String locale) {
    return ComparatorFactory.create(locale);
  }
}
