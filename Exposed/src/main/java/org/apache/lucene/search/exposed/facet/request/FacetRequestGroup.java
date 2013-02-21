package org.apache.lucene.search.exposed.facet.request;

import org.apache.lucene.search.exposed.ExposedRequest;
import org.apache.lucene.search.exposed.compare.ComparatorFactory;
import org.apache.lucene.search.exposed.compare.NamedComparator;
import org.apache.lucene.search.exposed.facet.ParseHelper;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FacetRequestGroup implements SubtagsConstraints {
  public static final String DEFAULT_DELIMITER = "/";
  public static final int DEFAULT_LEVELS = 5;

  private final ExposedRequest.Group group;
  private final NamedComparator.ORDER order;
  private final boolean reverse;
  private final String locale;
  private final int offset;
  private final int maxTags;
  private final int minCount;
  private final String prefix;
  private final String buildKey;
  private final boolean hierarchical;
  private final String delimiter;
  private final int levels;
  private final String startPath;
  private FacetRequestSubtags subtags = null;

  // Reader must be positioned at group start
  FacetRequestGroup(
      XMLStreamReader reader, String request, NamedComparator.ORDER order,
      boolean defaultReverse, String locale, int maxTags, int minCount,
      int offset, String prefix, boolean defaultHierarchical, int defaultLevels,
      String defaultDelimiter, String defaultStartPath)
                                                     throws XMLStreamException {
    String name = null;
    boolean hierarchical = defaultHierarchical;
    String delimiter = defaultDelimiter;
    int levels = defaultLevels;
    String startPath = defaultStartPath;
    boolean reverse = defaultReverse;

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
        if (maxTags == -1) {
          maxTags = Integer.MAX_VALUE;
        }
      } else if ("mincount".equals(attribute)) {
        minCount = ParseHelper.getInteger(request, "mincount", value);
      } else if ("offset".equals(attribute)) {
        offset = ParseHelper.getInteger(request, "offset", value);
      } else if ("prefix".equals(attribute)) {
        prefix = value;
      } else if ("name".equals(attribute)) {
        name = value;
      } else if ("hierarchical".equals(attribute)) {
        hierarchical = Boolean.parseBoolean(value);
      } else if ("delimiter".equals(attribute)) {
        delimiter = value;
      } else if ("levels".equals(attribute)) {
        levels = ParseHelper.getInteger(request, "levels", value);
      } else if ("startpath".equals(attribute)) {
        startPath = value;
      }
    }
    this.order = order;
    this.reverse = reverse;
    this.locale = locale;
    this.offset = offset;
    this.maxTags = maxTags;
    this.minCount = minCount;
    this.prefix = prefix;

    this.hierarchical = hierarchical;
    this.delimiter = delimiter == null ? "" : delimiter;
    this.levels = levels;
    this.startPath = startPath;

    if (name == null) {
      throw new XMLStreamException(
          "FacetGroup name must be specified with the attribute 'name' in "
              + request);
    }
    List<String> fieldNames = new ArrayList<String>();
    reader.nextTag();
    while (!ParseHelper.atEnd(reader, "group")) {
      if (ParseHelper.atStart(reader, "fields")) {
        try {
          reader.nextTag();
        } catch (Exception e) {
          throw new XMLStreamException(
              "Exception while getting next tag in fields for group '"
                  + name + "'", e);
        }
        while (!ParseHelper.atEnd(reader, "fields")) {
          if (ParseHelper.atStart(reader, "field")) {
            String fieldName = null;
            for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
              if ("name".equals(reader.getAttributeLocalName(i))) {
                fieldName = reader.getAttributeValue(i);
              }
            }
            if (fieldName == null) {
              throw new XMLStreamException(
                  "Unable to determine name for field in group " + name
                      + " in " + request);
            }
            if ("".equals(fieldName)) {
              throw new XMLStreamException(
                  "Encountered empty field in group '" + name + "'");
            }
            fieldNames.add(fieldName);
          }
          reader.nextTag(); // Until /fields
        }
      } else if (ParseHelper.atStart(reader, "subtags")) {
        subtags = new FacetRequestSubtags(reader, this);
      }
      reader.nextTag(); // until /group
    }
    NamedComparator comp = ComparatorFactory.create(
          locale == null ? null : new Locale(locale));
    comp.setReverse(reverse);
    group = createGroup(name, fieldNames, comp);
    buildKey = createBuildKey();
  }

  // Simple non-hierarchical constructor
  public FacetRequestGroup(
      ExposedRequest.Group group, NamedComparator.ORDER order,
      boolean reverse, String locale, int offset, int maxTags, int minCount,
      String prefix) {
    this.group = group;
    this.order = order;
    this.reverse = reverse;
    this.locale = locale;
    this.offset = offset;
    this.maxTags = maxTags;
    this.minCount = minCount;
    this.prefix = prefix;
    hierarchical = false;
    delimiter = null;
    levels = 1;
    startPath = null;

    buildKey = createBuildKey();
  }

  // Creates a group based on a single field
  public FacetRequestGroup(
      String field, NamedComparator.ORDER order, boolean reverse,
      String locale, int maxTags, int minCount, int offset, String prefix,
      boolean hierarchical, int levels, String delimiter, String startPath) {
    NamedComparator comparator = ComparatorFactory.create(
      locale == null ? null : new Locale(locale));
    comparator.setReverse(reverse);
    List<ExposedRequest.Field> fields = new ArrayList<ExposedRequest.Field>(1);
    fields.add(new ExposedRequest.Field(field, comparator));
    group = new ExposedRequest.Group(field, fields, comparator);
    this.order = order;
    this.reverse = reverse;
    this.locale = locale;
    this.offset = offset;
    this.maxTags = maxTags;
    this.minCount = minCount;
    this.prefix = prefix;
    this.hierarchical = hierarchical;
    this.delimiter = delimiter;
    this.levels = levels;
    this.startPath = startPath;

    buildKey = createBuildKey();
  }

  private String createBuildKey() {
    StringWriter sw = new StringWriter();
    sw.append("group(name=").append(group.getName()).append(", order=");
    sw.append(order.toString()).append(", locale=").append(locale);
    sw.append(", fields(");
    boolean first = true;
    for (String field: group.getFieldNames()) {
      if (first) {
        first = false;
      } else {
        sw.append(", ");
      }
      sw.append(field);
    }
    sw.append("), hierarchical=").append(Boolean.toString(hierarchical));
    sw.append(", delimiter=").append(delimiter).append(")");
    return sw.toString();
  }

  public String getBuildKey() {
    return buildKey;
  }

  public ExposedRequest.Group getGroup() {
    return group;
  }

  public void toXML(XMLStreamWriter out) throws XMLStreamException {
    out.writeCharacters("    ");
    out.writeStartElement("group");
    out.writeAttribute("name", group.getName());
    out.writeAttribute("order",    order.toString());
    out.writeAttribute("reverse",  Boolean.toString(reverse));
    if (locale != null) {
      out.writeAttribute("locale", locale);
    }
    out.writeAttribute("maxtags",  Integer.toString(
        maxTags == Integer.MAX_VALUE ? -1 : maxTags));
    out.writeAttribute("mincount", Integer.toString(minCount));
    out.writeAttribute("offset",   Integer.toString(offset));
    out.writeAttribute("prefix",   prefix);
    out.writeAttribute("hierarchical",   Boolean.toString(hierarchical));
    out.writeAttribute("levels",   Integer.toString(levels));
    out.writeAttribute("delimiter", delimiter == null ? "" : delimiter);
    out.writeAttribute("startpath", startPath == null ? "" : startPath);
    out.writeCharacters("\n      ");

    out.writeStartElement("fields");
    out.writeCharacters("\n");
    for (ExposedRequest.Field field: group.getFields()) {
      out.writeCharacters("        ");
      out.writeStartElement("field");
      out.writeAttribute("name", field.getField());
      out.writeEndElement();
      out.writeCharacters("\n");
    }
    out.writeCharacters("      ");
    out.writeEndElement(); // fields
// TODO: Write XML for subtags
    out.writeCharacters("\n    ");
    out.writeEndElement(); // group
    out.writeCharacters("\n");
  }

  public NamedComparator.ORDER getOrder() {
    return order;
  }

  public boolean isReverse() {
    return reverse;
  }

  public String getLocale() {
    return locale;
  }

  public int getOffset() {
    return offset;
  }

  @Override
  public int getMaxTags() {
    return maxTags;
  }

  @Override
  public int getMinCount() {
    return minCount;
  }

  public String getPrefix() {
    return prefix;
  }

  @Override
  public int getMinTotalCount() {
    return minCount;
  }

  @Override
  public SUBTAGS_ORDER getSubtagsOrder() {
    return order == NamedComparator.ORDER.count ?
           SUBTAGS_ORDER.count : SUBTAGS_ORDER.base;
  }

  @Override
  public SubtagsConstraints getDeeperLevel() {
    return subtags == null ? this : subtags;
  }

  public boolean isHierarchical() {
    return hierarchical;
  }

  public String getDelimiter() {
    return delimiter;
  }

  public int getLevels() {
    return levels;
  }

  public String getStartPath() {
    return startPath;
  }

  public static ExposedRequest.Group createGroup(
      String name, List<String> fieldNames, NamedComparator comparator) {
    return createGroup(name, fieldNames, comparator, null);
  }
    public static ExposedRequest.Group createGroup(
        String name, List<String> fieldNames, NamedComparator comparator,
        String concatCollatorID) {
    if (fieldNames.isEmpty()) {
      throw new IllegalArgumentException("There must be at least 1 field name");
    }
    List<ExposedRequest.Field> fieldRequests =
        new ArrayList<ExposedRequest.Field>(fieldNames.size());
    for (String fieldName: fieldNames) {
      fieldRequests.add(new ExposedRequest.Field(
          fieldName, comparator, concatCollatorID));
    }
    return new ExposedRequest.Group(
        name, fieldRequests, comparator, concatCollatorID);
  }
}
