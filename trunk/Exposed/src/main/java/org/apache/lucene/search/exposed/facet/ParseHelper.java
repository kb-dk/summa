package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.search.exposed.ExposedComparators;
import org.apache.lucene.search.exposed.facet.request.FacetRequest;
import org.apache.lucene.util.BytesRef;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import com.ibm.icu.text.Collator;
import java.util.Comparator;
import java.util.Locale;

public class ParseHelper {
  public static int getInteger(String request, String attribute, String value)
                                                     throws XMLStreamException {
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
    if (locale == null || "".equals(locale)) {
      return ExposedComparators.collatorToBytesRef(null);
    }
    return ExposedComparators.collatorToBytesRef(
        Collator.getInstance(new Locale(locale)));
  }
}
