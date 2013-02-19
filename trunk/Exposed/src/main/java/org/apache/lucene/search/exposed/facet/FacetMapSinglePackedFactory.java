package org.apache.lucene.search.exposed.facet;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.search.exposed.ExposedSettings;
import org.apache.lucene.search.exposed.ExposedTuple;
import org.apache.lucene.search.exposed.GroupTermProvider;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ExpandablePackedPair;
import org.apache.lucene.util.packed.MonotonicReaderFactory;
import org.apache.lucene.util.packed.PackedIntWrapper;
import org.apache.lucene.util.packed.PackedInts;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Single pass builder that collects termID-docID pairs in space optimized
 * PackedInts-structures separated by provider.
 */
public class FacetMapSinglePackedFactory {

  public static FacetMap createMap(int docCount, List<TermProvider> providers)
      throws IOException {
    final long startTime = System.currentTimeMillis();
    if (ExposedSettings.debug) {
      System.out.println(
          "FacetMap: Creating packed single pass map for "
          + providers.size() + " group" + (providers.size() == 1 ? "" : "s")
          + " with " + docCount + " documents)");
    }
    final int[] indirectStarts = new int[providers.size() +1];
    int start = 0;
    long indexExtractTime = -System.currentTimeMillis();
    long refCount = 0;

    List<ProviderData> providerDatas = extractProviderDatas(docCount, providers);
    new ArrayList<ProviderData>(providers.size());

    // Collects unordered docID -> indirect
    List<ExpandablePackedPair> providerMaps =
        new ArrayList<ExpandablePackedPair>(providers.size());
    long totalUniqueTerms = 0;
    for (int i = 0 ; i < providerDatas.size() ; i++) {
      ProviderData providerData = providerDatas.get(i);
      indirectStarts[i] = start;
      final long termOffset = start;
      ExpandablePackedPair providerMap = providerData.providerMap;
      providerMap.setSecondaryOffset(termOffset);
      providerMaps.add(providerMap);
      totalUniqueTerms += providerData.uniqueTerms;
      refCount += providerMap.size();
      start += providerData.uniqueTerms;
    }
    indirectStarts[indirectStarts.length-1] = start;
    indexExtractTime += System.currentTimeMillis();

    if (ExposedSettings.debug) {
      System.out.println(
          "FacetMap: Full index iteration in "
          + (System.currentTimeMillis() - startTime) + "ms. "
          + "Commencing extraction of structures");
    }

    long tagExtractTime = - System.currentTimeMillis();
    Map.Entry<PackedInts.Reader, PackedInts.Reader> pair =
        extractTags(providerMaps, docCount, totalUniqueTerms);
    tagExtractTime += System.currentTimeMillis();
    final PackedInts.Reader doc2ref = pair.getKey();
    final PackedInts.Reader refs = pair.getValue();
    if (ExposedSettings.debug) {
      System.out.println(String.format(
          "FacetMap: docs=%s, terms=%s, refs=%d. Term extraction time=%dms,"
          + " Secondary structure processing time=%dms",
          docCount, totalUniqueTerms, refCount, indexExtractTime,
          tagExtractTime));
    }
    return new FacetMap(providers, indirectStarts, doc2ref, refs);
  }

  private static List<ProviderData> extractProviderDatas(
      int docCount, List<TermProvider> providers) {
    List<ProviderData> providerDatas =
        new ArrayList<ProviderData>(providers.size());
    if (ExposedSettings.threads == 1 || providers.size() == 1) {
      if (ExposedSettings.debug) {
        System.out.println(String.format(
            "FacetMap: Performing single threaded extraction of term " +
            "data from %d providers",
            providers.size()));
      }
      for (TermProvider provider: providers) {
        try {
          providerDatas.add(extractProviderData(docCount, provider));
        } catch (IOException e) {
          throw new RuntimeException(
              "Unable to extract data for " + provider, e);
        }
      }
      return providerDatas;
    }

    if (ExposedSettings.debug) {
      System.out.println(String.format(
          "FacetMap: Starting a maximum of %d threads for extracting term " +
          "data from %d providers",
          ExposedSettings.threads, providers.size()));
    }
    long realTime = -System.currentTimeMillis();
    long summedTime = 0;
    final ExecutorService executor =
        Executors.newFixedThreadPool(ExposedSettings.threads);
    List<Future<ProviderData>> jobs =
        new ArrayList<Future<ProviderData>>(providers.size());
    for (TermProvider provider: providers) {
      jobs.add(executor.submit(new ProviderCallable(docCount, provider)));
    }
    for (Future<ProviderData> job: jobs) {
      try {
        ProviderData pd = job.get();
        providerDatas.add(pd);
        summedTime += pd.processingTime;
      } catch (InterruptedException e) {
        throw new RuntimeException(
            "Interrupted while waiting for data extraction from provider", e);
      } catch (ExecutionException e) {
        throw new RuntimeException(
            "ExecutionException extracting data from provider", e);
      }
    }
    realTime += System.currentTimeMillis();
    if (ExposedSettings.debug) {
      System.out.println(String.format(
          "FacetMap: Finished running max %d threads for %d providers. " +
          "Real time spend: %dms. Summed thread time: %dms",
          ExposedSettings.threads, providers.size(), realTime, summedTime));
    }
    return providerDatas;
  }

  private static class ProviderCallable implements Callable<ProviderData> {
    private final int docCount;
    private final TermProvider provider;

    private ProviderCallable(int docCount, TermProvider provider) {
      this.docCount = docCount;
      this.provider = provider;
    }

    @Override
    public ProviderData call() throws Exception {
      return extractProviderData(docCount, provider);
    }
  }

  private static class ProviderData {
    public final TermProvider provider; // For debug
    public final ExpandablePackedPair providerMap;
    public final int uniqueTerms;
    public final long processingTime;

    public ProviderData(
        TermProvider provider, ExpandablePackedPair providerMap,
        int uniqueTerms, long processingTime) {
      this.provider = provider;
      this.providerMap = providerMap;
      this.uniqueTerms = uniqueTerms;
      this.processingTime = processingTime;
    }
  }

  private static ProviderData extractProviderData(
      int docCount, TermProvider provider) throws IOException {
    long processingTime = -System.currentTimeMillis();
    ExpandablePackedPair providerMap = new ExpandablePackedPair(
        PackedInts.bitsRequired(docCount),
        PackedInts.bitsRequired(provider.getOrdinalTermCount()),
        0); // TODO: Add termOffset later
    Iterator<ExposedTuple> tuples = provider.getIterator(true);
    PackedInts.Mutable i2o =  PackedInts.getMutable(
        (int) provider.getOrdinalTermCount(),
        PackedInts.bitsRequired(provider.getOrdinalTermCount()), 0);
    BytesRef last = null;
    int uniqueTerms = 0;
    while (tuples.hasNext()) {
      ExposedTuple tuple = tuples.next();
//        indirectToOrdinal.add((int) tuple.indirect, (int) tuple.ordinal);
      i2o.set((int) tuple.indirect, tuple.ordinal);
      int docID;
      while ((docID = tuple.docIDs.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
//          System.out.println("*** " + tuple + " docID " + docID);
        providerMap.add((int)(tuple.docIDBase + docID), (int)tuple.indirect);
      }
//      if (last == null || !last.equals(tuple.term)) { // TODO: Remove this check
  //      last = tuple.term;
        uniqueTerms++;
//          System.out.println("FaM got: " + last.utf8ToString());
    //  }
    }

    processingTime += System.currentTimeMillis();
    if (provider instanceof GroupTermProvider) {
      // Not at all OO
      ((GroupTermProvider)provider).setOrderedOrdinals(i2o);
      if (ExposedSettings.debug) {
        System.out.println(String.format(
            "FacetMap: Assigning indirects for %d unique terms, " +
            "%d references, extracted in %d ms, to %s: %s",
            uniqueTerms, providerMap.size(), processingTime,
            ((GroupTermProvider)provider).getRequest().getFieldNames(),
            i2o));
      }
    } else if (ExposedSettings.debug) {
      System.out.println(String.format(
          "FacetMap: Hoped for GroupTermProvider, but got %s. " +
          "Collected ordered ordinals are discarded",
          provider.getClass()));
    }
    return new ProviderData(provider, providerMap, uniqueTerms, processingTime);
  }

  /**
   * Sorts the pairs by docID (the primary in pairs). The order of the indirects
   * (the secondary in pairs) is not guaranteed.
   * </p><p>
   * This operation temporarily allocates a {@code int[docCount]} and runs in
   * <tt>O(n)</tt>, where n is {@code pairs.size()}. In reality, two passes is
   * done on <tt>pairs</tt> where the first one is sequential and the second one
   * is
   * @param d2is
   * @param docCount the number of documents.
   * @param uniqueTerms the number of unique terms (and thus indirects).
   */
  private static Map.Entry<PackedInts.Reader, PackedInts.Reader> extractTags(
      List<ExpandablePackedPair> d2is, int docCount, long uniqueTerms) {
    long startTime = System.currentTimeMillis();

    // Count tags and convert the tagCounts to starting positions
    final int[] starts = new int[docCount+1];

    for (ExpandablePackedPair d2i: d2is) {
      d2i.countUniquePrimaries(starts);
    }
    int index = 0 ;
    for (int i = 0 ; i < starts.length ; i++) {
      final int delta = starts[i];
      starts[i] = index;
      index += delta;
    }
    starts[starts.length-1] = index;

    // The starting positions are used in the final structure, so we absorb them
    // right away. Important: We copy the values (by reducing) as they are
    // changed while constructing the ref-structure.
    PackedInts.Reader direct = new PackedIntWrapper(starts);
    PackedInts.Reader doc2ref = MonotonicReaderFactory.reduce(direct);
    if (direct == doc2ref) {
      int[] startsCopy = new int[starts.length];
      System.arraycopy(starts, 0, startsCopy, 0, starts.length);
      doc2ref = new PackedIntWrapper(startsCopy);
    }
    long countTime = System.currentTimeMillis() - startTime;

    int fullSize = 0;
    for (ExpandablePackedPair d2i: d2is) {
      fullSize += d2i.size();
    }
    final PackedInts.Mutable refs = PackedInts.getMutable(
        fullSize, PackedInts.bitsRequired(uniqueTerms), 0);

    // Iterate pairs and put the pairs at the proper starting positions
    for (ExpandablePackedPair d2i: d2is) {
      d2i.assignSecondaries(starts, refs);
    }
    if (ExposedSettings.debug) {
      System.out.println(String.format(
          "FacetMap: Extracted doc2in and refs %s from %d pairs for %d docIDs "
          + "in %d seconds (%d of these seconds used for counting docID "
          + "frequencies and creating doc2in structure)",
          refs, fullSize, docCount,
          (System.currentTimeMillis() - startTime) / 1000, countTime / 1000));
    }
    return new AbstractMap.SimpleEntry<PackedInts.Reader, PackedInts.Reader>(
        doc2ref, refs);
  }
}
