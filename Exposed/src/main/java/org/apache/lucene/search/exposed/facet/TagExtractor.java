package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.search.exposed.ExposedPriorityQueue;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.search.exposed.compare.ComparatorFactory;
import org.apache.lucene.search.exposed.compare.NamedComparator;
import org.apache.lucene.search.exposed.facet.request.FacetRequestGroup;
import org.apache.lucene.search.exposed.facet.request.SubtagsConstraints;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.PriorityQueue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Helper class for extracting tags for a faceting structure.
 */
public class TagExtractor {
  final private FacetRequestGroup requestGroup;
  final private Pattern splitPattern; // Defined if hierarchical

  public TagExtractor(FacetRequestGroup requestGroup) {
    this.requestGroup = requestGroup;
    splitPattern = requestGroup.isHierarchical() ?
        Pattern.compile(requestGroup.getDelimiter()) :
        null;
  }

  public FacetResponse.Group extract(
      int groupID, FacetMap map,
      int[] tagCounts, int startPos, int endPos) throws IOException {

    if (requestGroup.isHierarchical()) {
      TermProvider provider = map.getProviders().get(groupID);
      if (!(provider instanceof HierarchicalTermProvider)) {
        throw new IllegalStateException(
            "Internal inconsistency: The provider for "
                + requestGroup.getGroup().getName()
                + " should be hierarchical" +
                " but is " + provider.getClass());
      }
      int delta = -map.getIndirectStarts()[groupID];
      return new FacetResponse.Group(
          requestGroup,
          extractHierarchical(requestGroup.getDeeperLevel(), 1,
              (HierarchicalTermProvider)provider, delta,
              map, tagCounts, startPos, endPos));
    }

    NamedComparator.ORDER order = requestGroup.getOrder();
    if (NamedComparator.ORDER.count == order) {
      return extractCountResult(
          requestGroup, map, tagCounts, startPos, endPos);
    } else if (NamedComparator.ORDER.index == order
        || NamedComparator.ORDER.locale == order) {
      return extractOrderResult(
          groupID, map, tagCounts, startPos, endPos);
    }
    throw new UnsupportedOperationException(
        "The order '" + order + "' is unknown");
  }

  private FacetResponse.TagCollection extractHierarchical(
      final SubtagsConstraints constraints,
      final int currentLevel,
      final HierarchicalTermProvider provider, final int delta,
      final FacetMap map,
      final int[] tagCounts, final int startPos, final int endPos)
                                                            throws IOException {
    if (currentLevel > requestGroup.getLevels()) {
      return null; // Stop descending
    }
    // TODO: Support count, offset, path etc.
    switch( constraints.getSubtagsOrder()) {
      case base: return extractHierarchicalOrder(
          constraints, currentLevel, provider, delta, map,
          tagCounts, startPos, endPos);
     case count: return extractHierarchicalCount(
          constraints, currentLevel, provider, delta, map,
          tagCounts, startPos, endPos);
      default: throw new IllegalArgumentException(
          "The order '" + constraints.getSubtagsOrder() + "' is unknown");
    }
  }

  private class HCElement {
    final int tagStartPos;
    final int tagEndPos;
    final int count;
    final int totalCount;

    private HCElement(int tagStartPos, int tagEndPos, int count, int totalCount) {
      this.tagStartPos = tagStartPos;
      this.tagEndPos = tagEndPos;
      this.count = count;
      this.totalCount = totalCount;
    }
  }

  private class HCEPQ extends PriorityQueue<HCElement> {
    private HCEPQ(int size) {
      super(size, false); // TODO: Consider prepopulating
    }

    @Override
    protected boolean lessThan(HCElement a, HCElement b) {
      return a.totalCount == b.totalCount ?
          a.tagStartPos > b.tagStartPos :
          a.totalCount < b.totalCount;
    }
  }

  private FacetResponse.TagCollection extractHierarchicalCount(
      final SubtagsConstraints constraints, final int level,
      final HierarchicalTermProvider provider, final int delta,
      final FacetMap map,
      final int[] tagCounts, final int startPos, final int endPos)
                                                            throws IOException {
    HCEPQ pq = new HCEPQ(Math.min(constraints.getMaxTags(), endPos - startPos));
    long validTags = 0;
    long totalCount = 0;
    long count = 0;
    { // Find most popular tags
      final TagSumIterator tagIterator = new TagSumIterator(
          provider, constraints,
          tagCounts, startPos, endPos, level, delta);
      while (tagIterator.next()) {
        totalCount += tagIterator.getTotalCount();
        count += tagIterator.getCount();
        final HCElement current = new HCElement(
            tagIterator.tagStartPos, tagIterator.tagEndPos,
            tagIterator.count, tagIterator.totalCount);
        pq.insertWithOverflow(current); // TODO: Consider reusing objects
        validTags++;
      }
    }
    // Build result structure and
    FacetResponse.Tag[] tags = new FacetResponse.Tag[pq.size()];
    for (int i = pq.size()-1 ; i >= 0 ; i--) {
      HCElement element = pq.pop();
      String term = provider.getOrderedTerm(element.tagStartPos + delta).
          utf8ToString();
      FacetResponse.Tag tag = new FacetResponse.Tag(
          getLevelTerm(term, level), element.count, element.totalCount);
      tags[i] = tag;
      if (level < requestGroup.getLevels() &&
          !(provider.getLevel(element.tagStartPos) < level+1 &&
              element.tagStartPos+1 == element.tagEndPos)) {
        tag.setSubTags(extractHierarchical(
            constraints.getDeeperLevel(), level+1, provider, delta, map,
            tagCounts, element.tagStartPos, element.tagEndPos));
      }
      // TODO: State totalcount in tags so they are not forgotten
    }
    FacetResponse.TagCollection collection = new FacetResponse.TagCollection();
    collection.setTags(Arrays.asList(tags));
    collection.setTotalCount(totalCount);
    collection.setTotalTags(validTags);
    collection.setCount(count);
    collection.setDefiner(constraints);
    collection.setPotentialTags(endPos-startPos);
    return collection;
  }

  private FacetResponse.TagCollection extractHierarchicalOrder(
      final SubtagsConstraints constraints,
      final int level,
      final HierarchicalTermProvider provider,
      final int delta, final FacetMap map,
      final int[] tagCounts, final int startTermPos, final int endTermPos)
                                                            throws IOException {
    final TagSumIterator tagIterator = new TagSumIterator(
        provider, constraints,
        tagCounts, startTermPos, endTermPos, level, delta);
    // TODO: Consider optimization by not doing totalTags, count and totalCount
    List<FacetResponse.Tag> tags = new ArrayList<FacetResponse.Tag>(
        Math.max(1, Math.min(constraints.getMaxTags(), 100000)));
    long validTags = 0;
    long totalCount = 0;
    long count = 0;
    while (tagIterator.next()) {
      totalCount += tagIterator.getTotalCount();
      count += tagIterator.getCount();
      if (validTags < constraints.getMaxTags()) {
        String term = provider.getOrderedTerm(tagIterator.tagStartPos + delta).
            utf8ToString();
        FacetResponse.Tag tag =
            new FacetResponse.Tag(getLevelTerm(term, level),
                tagIterator.getCount(), tagIterator.getTotalCount());
        tags.add(tag);
        if (level < requestGroup.getLevels() &&
            !(provider.getLevel(tagIterator.tagStartPos) < level+1 &&
            tagIterator.tagStartPos+1 == tagIterator.tagEndPos)) {
          tag.setSubTags(extractHierarchical(
              constraints.getDeeperLevel(), level+1, provider, delta, map,
              tagCounts, tagIterator.tagStartPos, tagIterator.tagEndPos));
        }
      }
      validTags++;
    }
    FacetResponse.TagCollection collection = new FacetResponse.TagCollection();
    collection.setTags(tags);
    collection.setTotalCount(totalCount);
    collection.setTotalTags(validTags);
    collection.setCount(count);
    collection.setDefiner(constraints);
    collection.setPotentialTags(endTermPos-startTermPos);
    return collection;
  }

  private String getLevelTerm(String term, int level) {
    try {
      return splitPattern.split(term)[level-1];
    } catch (ArrayIndexOutOfBoundsException e) {
      ArrayIndexOutOfBoundsException ex = new ArrayIndexOutOfBoundsException(
          "Unable to split '" + term + "' with delimiter '"
              + splitPattern.pattern() + "' into " + level + " parts or more");
      ex.initCause(e);
      throw ex;
    }
  }

  private FacetResponse.Group extractOrderResult(
      final int groupID, final FacetMap map,
      final int[] tagCounts, final int startTermPos, final int endTermPos)
                                                            throws IOException {
    long extractionTime = System.currentTimeMillis();
    // Locate prefix
    int origo = startTermPos;
    if (requestGroup.getPrefix() != null
        && !"".equals(requestGroup.getPrefix())) {
      int tpOrigo = map.getProviders().get(groupID).getNearestTermIndirect(
          new BytesRef(requestGroup.getPrefix()));
      origo = tpOrigo + map.getIndirectStarts()[groupID];
      //origo = origo < 0 ? (origo + 1) * -1 : origo; // Ignore missing match
    }

    // TODO: Add sort order
    // skip to offset (only valid)
    final int minCount = requestGroup.getMinCount();
    final int direction = requestGroup.getOffset() < 0 ? -1 : 1;
    int delta = Math.abs(requestGroup.getOffset());
    while (delta > 0) {
      origo += direction;
      if (origo < startTermPos || origo >= endTermPos) {
        origo += delta-1;
        break;
      }
      if (tagCounts[origo] >= minCount) {
        delta--;
      }
    }
    // Collect valid IDs
    int collectedTags = 0;
    if (origo < startTermPos) {
      collectedTags = startTermPos - origo;
      origo = startTermPos;
    }
    List<FacetResponse.Tag> tags = new ArrayList<FacetResponse.Tag>(
        Math.max(0, Math.min(requestGroup.getMaxTags(), endTermPos-origo)));
    for (int termPos = origo ;
         termPos < endTermPos & collectedTags < requestGroup.getMaxTags() ;
         termPos++) {
      if (tagCounts[termPos] >= minCount) {
        tags.add(new FacetResponse.Tag(
          map.getOrderedTerm(termPos).utf8ToString(), tagCounts[termPos]));
        collectedTags++;
      }
    }
    FacetResponse.Group responseGroup =
        new FacetResponse.Group(requestGroup, tags);
    extractionTime = System.currentTimeMillis() - extractionTime;
    responseGroup.setExtractionTime(extractionTime);
    responseGroup.setPotentialTags(endTermPos - startTermPos);

    return responseGroup;
  }

  private FacetResponse.Group extractCountResult(
      FacetRequestGroup requestGroup, FacetMap map, final int[] tagCounts,
      final int startTermPos, final int endTermPos) throws IOException {
    long extractionTime = System.currentTimeMillis();
    // Sort tag references by count
    final int maxSize = Math.min(
        endTermPos- startTermPos, requestGroup.getMaxTags());
    final int minCount = requestGroup.getMinCount();
    ExposedPriorityQueue pq = new ExposedPriorityQueue(
        new ComparatorFactory.IndirectComparator(tagCounts), maxSize);
    long totalRefs = 0;
    long totalValidTags = 0;
    for (int termPos = startTermPos ; termPos < endTermPos ; termPos++) {
      totalRefs += tagCounts[termPos];
      if (tagCounts[termPos] >= minCount) {
        pq.insertWithOverflow(termPos);
        totalValidTags++;
      }
    }

    // Extract Tags
    FacetResponse.Tag[] tags = new FacetResponse.Tag[pq.size()];
    int pos = pq.size()-1;
    while (pq.size() > 0) {
      final int termIndex = pq.pop();
      tags[pos--] =  new FacetResponse.Tag(
          map.getOrderedTerm(termIndex).utf8ToString(), tagCounts[termIndex]);
    }

    // Create response
    FacetResponse.Group responseGroup =
        new FacetResponse.Group(requestGroup, Arrays.asList(tags));
    extractionTime = System.currentTimeMillis() - extractionTime;
    responseGroup.setExtractionTime(extractionTime);
    responseGroup.setPotentialTags(endTermPos-startTermPos);
    responseGroup.setTotalReferences(totalRefs);
    responseGroup.setValidTags(totalValidTags);
    return responseGroup;
  }

}
