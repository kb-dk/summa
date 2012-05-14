package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.search.exposed.facet.request.FacetRequest;
import org.apache.lucene.search.exposed.facet.request.FacetRequestGroup;
import org.apache.lucene.search.exposed.facet.request.SubtagsConstraints;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.List;

public class FacetResponse {
  public static final String NAMESPACE =
      "http://lucene.apache.org/exposed/facet/response/1.0";

  private static final XMLOutputFactory xmlOutFactory;
  static {
    xmlOutFactory = XMLOutputFactory.newInstance();
  }

  private final FacetRequest request;
  private final List<Group> groups;
  private long hits;
  private long countingTime = -1; // ms: just counting the references
  private long totalTime = -1; // ms
  private boolean countCached = false;

  public FacetResponse(FacetRequest request, List<Group> groups, long hits) {
    this.request = request;
    this.groups = groups;
    this.hits = hits;
  }

  public void setTotalTime(long totalTime) {
    this.totalTime =  totalTime;
  }

  public void setCountingTime(long countingTime) {
    this.countingTime = countingTime;
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
    out.writeStartElement("facetresponse");
    out.writeDefaultNamespace(NAMESPACE);
    out.writeAttribute("query", request.getQuery());
    writeIfDefined(out, "hits", hits);
    writeIfDefined(out, "countms", countingTime);
    out.writeAttribute("countcached", countCached ? "true" : "false");
    writeIfDefined(out, "totalms", totalTime);
    out.writeCharacters("\n");
    for (Group group: groups) {
      group.toXML(out);
    }
    out.writeEndElement(); // </facets>
    out.writeCharacters("\n");
  }

  public FacetRequest getRequest() {
    return request;
  }

  public List<Group> getGroups() {
    return groups;
  }

  public long getHits() {
    return hits;
  }

  public long getCountingTime() {
    return countingTime;
  }

  public long getTotalTime() {
    return totalTime;
  }

  public boolean isCountCached() {
    return countCached;
  }

  public static class Group {
    private final FacetRequestGroup request;
    private TagCollection tags;
    private long extractionTime = -1; // ms

    public Group(FacetRequestGroup request, List<Tag> tags) {
      this.request = request;
      this.tags = new TagCollection();
      this.tags.setTags(tags);
    }

    public Group(FacetRequestGroup request, TagCollection tags) {
      this.request = request;
      this.tags = tags;
    }

    public void toXML(XMLStreamWriter out) throws XMLStreamException {
      out.writeCharacters("  ");
      out.writeStartElement("facet");
      out.writeAttribute("name", request.getGroup().getName());
      out.writeAttribute("fields", getFieldsStr());
      out.writeAttribute("order", request.getOrder().toString());
      if (request.getLocale() != null) {
        out.writeAttribute("locale", request.getLocale());
      }
      out.writeAttribute("maxtags",  Integer.toString(
          request.getMaxTags() == Integer.MAX_VALUE ? -1 :
              request.getMaxTags()));
      writeIfDefined(out, "mincount", request.getMinCount());
      writeIfDefined(out, "offset", request.getOffset());
      if (request.isHierarchical()) {
        out.writeAttribute("hierarchical", "true");
      }
      // TODO: Write hierarchical attributes
      writeIfDefined(out, "prefix", request.getPrefix());
      writeIfDefined(out, "extractionms", extractionTime);

      tags.toXML(out, "    ");
/*      writeIfDefined(out, "potentialtags", tags.getPotentialTags());
      writeIfDefined(out, "usedreferences", tags.getCount());
      writeIfDefined(out, "validtags", tags.getTotalTags());
      out.writeCharacters("\n");

      for (Tag tag: tags.getTags()) {
        tag.toXML(out);
      }
                                                            */
      out.writeCharacters("  ");
      out.writeEndElement(); // </facet>
      out.writeCharacters("\n");
    }

    private StringBuffer sb = new StringBuffer();
    public synchronized String getFieldsStr() {
      sb.setLength(0);
      List<String> fieldNames = request.getGroup().getFieldNames();
      for (int i = 0 ; i < fieldNames.size() ; i++) {
        if (i != 0) {
          sb.append(", ");
        }
        sb.append(fieldNames.get(i));
      }
      return sb.toString();
    }

    public void setPotentialTags(long potentialTags) {
      tags.setPotentialTags(potentialTags);
    }
    public void setTotalReferences(long usedReferences) {
      tags.setCount(usedReferences);
    }
    public void setValidTags(long validTags) {
      tags.setTotalTags(validTags);
    }
    public void setExtractionTime(long extractionTime) {
      this.extractionTime = extractionTime;
    }

    public FacetRequestGroup getRequest() {
      return request;
    }

    public TagCollection getTags() {
      return tags;
    }

    public long getExtractionTime() {
      return extractionTime;
    }

    public boolean isHierarchical() {
      return request.isHierarchical();
    }
  }

  public static class TagCollection {
    private SubtagsConstraints constraints;
    private List<Tag> tags;
    private long potentialTags = -1;
    private long count = -1;
    private long totalCount = -1;
    private long totalTags = -1;

    public SubtagsConstraints getDefiner() {
      return constraints;
    }
    public void setDefiner(SubtagsConstraints constraints) {
      this.constraints = constraints;
    }
    public List<Tag> getTags() {
      return tags;
    }
    public void setTags(List<Tag> tags) {
      this.tags = tags;
    }
    public long getPotentialTags() {
      return potentialTags;
    }
    public void setPotentialTags(long potentialTags) {
      this.potentialTags = potentialTags;
    }
    public long getCount() {
      return count;
    }
    public void setCount(long count) {
      this.count = count;
    }
    public long getTotalCount() {
      return totalCount;
    }
    public void setTotalCount(long totalcount) {
      this.totalCount = totalcount;
    }
    public long getTotalTags() {
      return totalTags;
    }
    public void setTotalTags(long totalTags) {
      this.totalTags = totalTags;
    }

    public SubtagsConstraints getConstraints() {
      return constraints;
    }

    public void toXML(XMLStreamWriter out, String prefix)
                                                     throws XMLStreamException {
      writeIfDefined(out, "potentialtags", getPotentialTags());
      writeIfDefined(out, "count", getCount());
      writeIfDefined(out, "totalCount", getTotalCount());
      writeIfDefined(out, "totaltags", getTotalTags());
      if (getTags().size() > 0) {
        out.writeCharacters("\n");
      }
      for (Tag tag: getTags()) {
        tag.toXML(out, prefix + "  ");
      }
    }
  }

  public static class Tag {
    private TagCollection subTags;
    private final String term;
    private final int count;
    private final int totalCount;

    public Tag(String term, int count) {
      this.term = term;
      this.count = count;
      this.totalCount = -1;
    }

    public Tag(String term, int count, int totalCount) {
      this.term = term;
      this.count = count;
      this.totalCount = totalCount;
    }

    public void toXML(XMLStreamWriter out, String prefix)
                                                     throws XMLStreamException {
      out.writeCharacters(prefix);
      out.writeStartElement("tag");
      out.writeAttribute("count", Integer.toString(count));
      if (totalCount != -1) {
        out.writeAttribute("totalcount", Integer.toString(totalCount));
      }
      out.writeAttribute("term", term);
      if (subTags != null) {
        out.writeCharacters("\n");
        out.writeCharacters(prefix);
        out.writeCharacters("  ");
        out.writeStartElement("subtags");
        subTags.toXML(out, prefix + "  ");
        out.writeCharacters(prefix);
        out.writeCharacters("  ");
        out.writeEndElement(); // </subtags>
        out.writeCharacters("\n");
        out.writeCharacters(prefix);
      }
      out.writeEndElement(); // </tag>
      out.writeCharacters("\n");
    }

    public TagCollection getSubTags() {
      return subTags;
    }

    public void setSubTags(TagCollection subTags) {
      this.subTags = subTags;
    }

    public String getTerm() {
      return term;
    }

    public int getCount() {
      return count;
    }

    public int getTotalCount() {
      return totalCount;
    }
  }

  private static void writeIfDefined(XMLStreamWriter out, String attributeName,
                                     long value) throws XMLStreamException {
    if (value == -1) {
      return;
    }
    out.writeAttribute(attributeName, Long.toString(value));
  }

  private static void writeIfDefined(XMLStreamWriter out, String attributeName,
                                     String value) throws XMLStreamException {
    if (value == null || "".equals(value)) {
      return;
    }
    out.writeAttribute(attributeName, value);
  }

  /**
   * @param wasCached true if the counting part of the facet generation was
   * cached.
   */
  public void setCountingCached(boolean wasCached) {
    this.countCached = wasCached;
  }

}
