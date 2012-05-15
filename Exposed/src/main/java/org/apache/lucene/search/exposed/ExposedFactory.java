package org.apache.lucene.search.exposed;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.CompositeReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// TODO: Is this used anymore?
public class ExposedFactory {

  // TODO: Split this so that group is explicit
  public static TermProvider createProvider(
      IndexReader reader, String groupName, List<String> fieldNames,
      Comparator<BytesRef> comparator, boolean reverse, String comparatorID)
                                                            throws IOException {
    if (fieldNames.size() == 0) {
      throw new IllegalArgumentException("There must be at least 1 field name");
    }
    if (reader instanceof AtomicReader && fieldNames.size() == 1) {
      // Segment-level single field
      ExposedRequest.Field request = new ExposedRequest.Field(
          fieldNames.get(0), comparator, reverse, comparatorID);
      return new FieldTermProvider(reader, 0, request, true);
    }

    if (reader instanceof AtomicReader && fieldNames.size() >= 1) {
      // Segment-level multi field
      List<TermProvider> fieldProviders =
          new ArrayList<TermProvider>(fieldNames.size());
      List<ExposedRequest.Field> fieldRequests =
          new ArrayList<ExposedRequest.Field>(fieldNames.size());
      for (String fieldName: fieldNames) {
        ExposedRequest.Field fieldRequest = new ExposedRequest.Field(
            fieldName, comparator, reverse, comparatorID);
        fieldRequests.add(fieldRequest);
        fieldProviders.add(new FieldTermProvider(
            reader, 0, fieldRequest, false));
      }
      ExposedRequest.Group groupRequest = new ExposedRequest.Group(
          groupName, fieldRequests, comparator, reverse, comparatorID);
      return new GroupTermProvider(
          reader.hashCode(), fieldProviders, groupRequest, true);
    }
    if (reader instanceof CompositeReader && fieldNames.size() == 1) {
      // Index-level single field
      IndexReader[] subs = ((CompositeReader)reader).getSequentialSubReaders();
        // TODO: Consider using ReaderUtil.gathersubReaders
      List<TermProvider> fieldProviders =
          new ArrayList<TermProvider>(subs.length);
      List<ExposedRequest.Field> fieldRequests =
          new ArrayList<ExposedRequest.Field>(subs.length);
      int docBase = 0;
      for (IndexReader sub: subs) {
        ExposedRequest.Field fieldRequest = new ExposedRequest.Field(
            fieldNames.get(0), comparator, reverse, comparatorID);
        fieldRequests.add(fieldRequest);
        fieldProviders.add(new FieldTermProvider(
            sub, docBase, fieldRequest, false));
        //sub.getTopReaderContext().docBaseInParent
        docBase += sub.maxDoc();
      }
      ExposedRequest.Group groupRequest = new ExposedRequest.Group(
          groupName, fieldRequests, comparator, reverse, comparatorID);
      return new GroupTermProvider(
          reader.hashCode(), fieldProviders, groupRequest, true);
    }
    // Index-level multi field
    if (!(reader instanceof CompositeReader)) {
      throw new IllegalStateException(
          "Code logic error: Expected CompositeReader, got " + reader.getClass());
    }
    IndexReader[] subs = ((CompositeReader)reader).getSequentialSubReaders();
    List<TermProvider> fieldProviders =
        new ArrayList<TermProvider>(subs.length * fieldNames.size());
    List<ExposedRequest.Field> fieldRequests =
        new ArrayList<ExposedRequest.Field>(subs.length * fieldNames.size());
    int docBase = 0;
    for (IndexReader sub: subs) {
      for (String fieldName: fieldNames) {
        ExposedRequest.Field fieldRequest = new ExposedRequest.Field(
            fieldName, comparator, reverse, comparatorID);
        fieldRequests.add(fieldRequest);
        fieldProviders.add(new FieldTermProvider(
            sub, docBase, fieldRequest, false));
      }
      //sub.getTopReaderContext().docBaseInParent
      docBase += sub.maxDoc();
    }
    ExposedRequest.Group groupRequest = new ExposedRequest.Group(
        groupName, fieldRequests, comparator, reverse, comparatorID);
    return new GroupTermProvider(
        reader.hashCode(), fieldProviders, groupRequest, true);
  }
}
