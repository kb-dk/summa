package org.apache.lucene.search.exposed.facet.request;

import org.apache.lucene.search.exposed.compare.NamedComparator;
import org.apache.lucene.search.exposed.facet.ParseHelper;

import javax.xml.stream.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * A specifications of groups of fields with sort order, max tags etc.
 * Used for specifying how to generate the internal facet structure as well as
 * the concrete user-request. The vocabulary is taken primarily from
 * http://wiki.apache.org/solr/SimpleFacetParameters
 * </p><p>
 *comparatorId See FacetRequest.xsd and FacetRequest.xml for syntax.
 * </p><p>
 * The facet request works with overriding params. FacetRequest is top level,
 * then follows FacetRequestGroups.
 */
public class FacetRequest {
  public static final String NAMESPACE =
      "http://lucene.apache.org/exposed/facet/request/1.0";

  public static final NamedComparator.ORDER DEFAULT_GROUP_ORDER =
    NamedComparator.ORDER.count;
  public static final boolean DEFAULT_REVERSE = false;
  public static final int    DEFAULT_MAXTAGS = 20;
  public static final int    DEFAULT_MINCOUNT = 0;
  public static final int    DEFAULT_OFFSET = 0;
  public static final String DEFAULT_PREFIX = "";
  private static final boolean DEFAULT_HIERARCHICAL = false;

  private static final XMLInputFactory xmlFactory;
  static {
    xmlFactory = XMLInputFactory.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
  }
  private static final XMLOutputFactory xmlOutFactory;
  static {
    xmlOutFactory = XMLOutputFactory.newInstance();
  }

  // groups: group*
  // group: fieldName* sort offset limit mincount prefix 
  // sort: count, index, custom (comparator)
  private String stringQuery;
  private final List<FacetRequestGroup> groups;

  // Defaults
  private NamedComparator.ORDER order = DEFAULT_GROUP_ORDER;
  private boolean reverse = DEFAULT_REVERSE;
  private String locale = null;
  private int maxTags =   DEFAULT_MAXTAGS;
  private int minCount =  DEFAULT_MINCOUNT;
  private int offset =    DEFAULT_OFFSET;
  private String prefix = DEFAULT_PREFIX;
  private boolean hierarchical = DEFAULT_HIERARCHICAL;
  private String delimiter =     FacetRequestGroup.DEFAULT_DELIMITER;
  private int levels =           FacetRequestGroup.DEFAULT_LEVELS;
  private String startPath =     null;

  public FacetRequest(String stringQuery, List<FacetRequestGroup> groups) {
    this.stringQuery = stringQuery;
    this.groups = groups;
  }

  public FacetRequest(String stringQuery) {
    this.stringQuery = stringQuery;
    this.groups = new ArrayList<FacetRequestGroup>();
  }

  /**
   * @param request A facet request in XML as specified in FacetRequest.xsd.
   * @return a facet request in Object-form.
   * @throws XMLStreamException if the request could not be parsed.
   */
  public static FacetRequest parseXML(String request)
                                                     throws XMLStreamException {
    XMLStreamReader reader =
        xmlFactory.createXMLStreamReader(new StringReader(request));
    List<FacetRequestGroup> groups = null;

    String stringQuery = null;
    NamedComparator.ORDER order = DEFAULT_GROUP_ORDER;
    boolean reverse = DEFAULT_REVERSE;
    String locale = null;
    int maxTags =   DEFAULT_MAXTAGS;
    int minCount =  DEFAULT_MINCOUNT;
    int offset =    DEFAULT_OFFSET;
    String prefix = DEFAULT_PREFIX;
    boolean hierarchical = DEFAULT_HIERARCHICAL;
    int levels = FacetRequestGroup.DEFAULT_LEVELS;
    String delimiter = FacetRequestGroup.DEFAULT_DELIMITER;
    String startPath = null;

    reader.nextTag();
    if (!ParseHelper.atStart(reader, "facetrequest")) {
      throw new XMLStreamException(
          "Could not locate start tag <facetrequest...> in " + request);
    }
    for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
      String attribute = reader.getAttributeLocalName(i);
      String value = reader.getAttributeValue(i);
      if ("order".equals(attribute)) {
        order = NamedComparator.ORDER.fromString(value);
      } else if ("reverse".equals(attribute)) {
        reverse = Boolean.parseBoolean(value);
      } else if ("locale".equals(attribute)) {
        locale = value;
      } else if ("maxtags".equals(attribute)) {
        maxTags = ParseHelper.getInteger(request, "maxtags", value);
      } else if ("mincount".equals(attribute)) {
        minCount = ParseHelper.getInteger(request, "mincount", value);
      } else if ("offset".equals(attribute)) {
        offset = ParseHelper.getInteger(request, "offset", value);
      } else if ("prefix".equals(attribute)) {
        prefix = value;
      } else if ("hierarchical".equals(attribute)) {
        hierarchical = Boolean.parseBoolean(value);
      } else if ("levels".equals(attribute)) {
        levels = ParseHelper.getInteger(request, "levels", value);
      } else if ("delimiter".equals(attribute)) {
        delimiter = value;
      } else if ("startPath".equals(attribute)) {
        startPath = value;
      }
    }

    while ((stringQuery == null || groups == null)
        && reader.nextTag() != XMLStreamReader.END_DOCUMENT) {
      if (ParseHelper.atStart(reader, "query")) {
        reader.next();
        stringQuery = reader.getText();
        reader.nextTag(); // END_ELEMENT
      } else if (ParseHelper.atStart(reader, "groups")) {
        groups = resolveGroups(reader, request, order, reverse, locale,
            maxTags, minCount, offset, prefix, hierarchical,
            levels, delimiter, startPath);
      } else {
        reader.nextTag(); // Ignore and skip to END_ELEMENT
      }
    }
    FacetRequest fr = new FacetRequest(stringQuery, groups);
    fr.setOrder(order);
    fr.setReverse(reverse);
    fr.setLocale(locale);
    fr.setMaxTags(maxTags);
    fr.setMinCount(minCount);
    fr.setOffset(offset);
    fr.setPrefix(prefix);
    fr.setHierarchical(hierarchical);
    fr.setLevels(levels);
    fr.setDelimiter(delimiter);
    fr.setStartPath(startPath);
    return fr;
  }

  private static List<FacetRequestGroup> resolveGroups(
      XMLStreamReader reader, String request, NamedComparator.ORDER order,
      boolean reverse, String locale, int maxTags, int minCount, int offset,
      String prefix, boolean hierarchical, int levels, String delimiter,
      String startPath) throws XMLStreamException {
    List<FacetRequestGroup> groups = new ArrayList<FacetRequestGroup>();
    while (reader.nextTag() != XMLStreamReader.END_DOCUMENT
        && !ParseHelper.atEnd(reader, "groups")) { // Not END_ELEMENT for groups
      if (ParseHelper.atStart(reader, "group")) {
        groups.add(new FacetRequestGroup(
            reader, request, order, reverse, locale, maxTags, minCount, offset,
            prefix, hierarchical, levels, delimiter, startPath));
      } else {
        reader.nextTag(); // Ignore and skip to END_ELEMENT
      }
    }
    if (groups.isEmpty()) {
      throw new XMLStreamException("No groups defined for " + request);
    }
    return groups;
  }

  // TODO: Consider sorting the groups and fields to ensure compatibility
  // with request where the order is changed
  /**
   * Produces a key created from the build-specific properties of the request.
   * This includes stringQuery, group name, fields and comparator. It does not
   * include extraction-specific things such as sort, mincount, offset and
   * prefix. The key can be used for caching of the counts from a request.
   * @return a key generated from the build-properties.
   */
  public String getBuildKey() {
    StringWriter sw = new StringWriter();
    sw.append("facetrequest(").append("query(").append(stringQuery);
    sw.append("), ");
    writeGroupKey(sw);
    sw.append(")");
    return sw.toString();
  }

  public String toXML() {
    StringWriter sw = new StringWriter();
    try {
      XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(sw);
      out.writeStartDocument("utf-8", "1.0");
      out.writeCharacters("\n");
      toXML(out);
      out.writeEndDocument();
      out.flush();
    } catch (XMLStreamException e) { // Getting here means error in the code
      throw new RuntimeException("Unable to create XML", e);
    }
    return sw.toString();
  }
  public void toXML(XMLStreamWriter out) throws XMLStreamException {
    out.setDefaultNamespace(NAMESPACE);
    out.writeStartElement("facetrequest");
    out.writeDefaultNamespace(NAMESPACE);
    out.writeAttribute("order", order.toString());
    if (locale != null) {
      out.writeAttribute("locale", locale);
    }
    out.writeAttribute("maxtags",  Integer.toString(maxTags));
    out.writeAttribute("mincount", Integer.toString(minCount));
    out.writeAttribute("offset",   Integer.toString(offset));
    out.writeAttribute("prefix",   prefix);
    out.writeAttribute("hierarchical", Boolean.toString(hierarchical));
    out.writeAttribute("levels", Integer.toString(levels));
    out.writeAttribute("delimiter", delimiter);
    if (startPath != null) {
      out.writeAttribute("startPath", startPath);
    }
    out.writeCharacters("\n  ");

    out.writeStartElement("query");
    out.writeCharacters(stringQuery);
    out.writeEndElement();
    out.writeCharacters("\n  ");

    out.writeStartElement("groups");
    out.writeCharacters("\n");
    for (FacetRequestGroup group: groups) {
      group.toXML(out);
    }
    out.writeCharacters("  ");
    out.writeEndElement(); // groups
    out.writeCharacters("\n");

    out.writeEndElement(); // facetrequest
    out.writeCharacters("\n");
    out.flush();
  }

  /**
   * Produces a key generated from the group-specific properties of the request.
   * This includes group name, fields and comparator.
   * The key is normally used for caching TermProviders.
   * @return a key generated from the group-specific properties of the request.
   */
  public String getGroupKey() {
    StringWriter sw = new StringWriter();
    writeGroupKey(sw);
    return sw.toString();
  }

  public List<FacetRequestGroup> getGroups() {
    return groups;
  }

  /**
   * Creates a group based on the default values and adds it to this request.
   * @param groupName the name for the group.
   * @return the newly created group.
   */
  public FacetRequestGroup createGroup(String groupName) {
    FacetRequestGroup group = new FacetRequestGroup(
        groupName, order, reverse, locale, maxTags, minCount, offset, prefix,
        hierarchical, levels, delimiter, startPath);
    groups.add(group);
    return group;
  }

  private void writeGroupKey(StringWriter sw) {
    sw.append("groups(");
    boolean first = true;
    for (FacetRequestGroup group: groups) {
      if (first) {
        first = false;
      } else {
        sw.append(", ");
      }
      sw.append(group.getBuildKey());
    }
    sw.append(")");
  }

  /* Mutators */

  public NamedComparator.ORDER getOrder() {
    return order;
  }

  public void setOrder(NamedComparator.ORDER order) {
    this.order = order;
  }

  public String getLocale() {
    return locale;
  }

  public void setReverse(boolean reverse) {
    this.reverse = reverse;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public int getMaxTags() {
    return maxTags;
  }

  public void setMaxTags(int maxTags) {
    this.maxTags = maxTags;
  }

  public int getMinCount() {
    return minCount;
  }

  public void setMinCount(int minCount) {
    this.minCount = minCount;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public boolean isHierarchical() {
    return hierarchical;
  }

  public void setHierarchical(boolean hierarchical) {
    this.hierarchical = hierarchical;
  }

  public String getDelimiter() {
    return delimiter;
  }

  public void setDelimiter(String delimiter) {
    this.delimiter = delimiter;
  }

  public int getLevels() {
    return levels;
  }

  public void setLevels(int levels) {
    this.levels = levels;
  }

  public String getStartPath() {
    return startPath;
  }

  public void setStartPath(String startPath) {
    this.startPath = startPath;
  }

  public String getQuery() {
    return stringQuery;
  }

  /**
   * Warning: Changing the query is not recommended as it is used af key in
   * cahce pools.
   * @param query the new query.
   */
  public void setQuery(String query) {
    this.stringQuery = query;
  }

}
