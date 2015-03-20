package org.apache.lucene.search.exposed;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


// TODO: Reimplement this codec
/** Adapted from Default codec (VariableGap). */
public class FixedGapCodec { //extends Codec {

  public FixedGapCodec() {
  //  super("FixedGap");
  }
    /*
  @Override
  public FieldsConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
    PostingsWriterBase docs = new StandardPostingsWriter(state);

    TermsIndexWriterBase indexWriter;
    boolean success = false;
    try {
      indexWriter = new FixedGapTermsIndexWriter(state);
      success = true;
    } finally {
      if (!success) {
        docs.close();
      }
    }

    success = false;
    try {
      FieldsConsumer ret = new BlockTermsWriter(indexWriter, state, docs);
      success = true;
      return ret;
    } finally {
      if (!success) {
        try {
          docs.close();
        } finally {
          indexWriter.close();
        }
      }
    }
  }

  public final static int TERMS_CACHE_SIZE = 1024;

  @Override
  public FieldsProducer fieldsProducer(SegmentReadState state) throws IOException {
    PostingsReaderBase postings = new StandardPostingsReader(state.dir, state.segmentInfo, state.context, state.codecId);
    TermsIndexReaderBase indexReader;

    boolean success = false;
    try {
      indexReader = new FixedGapTermsIndexReader(
          state.dir, state.fieldInfos, state.segmentInfo.name,
          state.termsIndexDivisor, BytesRef.getUTF8SortedAsUnicodeComparator(),
          state.codecId, state.context);
      success = true;
    } finally {
      if (!success) {
        postings.close();
      }
    }

    success = false;
    try {
      FieldsProducer ret = new BlockTermsReader(
          indexReader, state.dir, state.fieldInfos,
          state.segmentInfo.name, postings,
          state.context, TERMS_CACHE_SIZE,
          state.codecId);
      success = true;
      return ret;
    } finally {
      if (!success) {
        try {
          postings.close();
        } finally {
          indexReader.close();
        }
      }
    }
  }
*/
  /** Extension of freq postings file */
  static final String FREQ_EXTENSION = "frq";

  /** Extension of prox postings file */
  static final String PROX_EXTENSION = "prx";
  /*
  @Override
  public void files(Directory dir, SegmentInfo segmentInfo, int id, Set<String> files) throws IOException {
    StandardPostingsReader.files(dir, segmentInfo, id, files);
    BlockTermsReader.files(dir, segmentInfo, id, files);
    FixedGapTermsIndexReader.files(dir, segmentInfo, id, files);
  }

  @Override
  public void getExtensions(Set<String> extensions) {
    getStandardExtensions(extensions);
  }

  public static void getStandardExtensions(Set<String> extensions) {
    extensions.add(FREQ_EXTENSION);
    extensions.add(PROX_EXTENSION);
    BlockTermsReader.getExtensions(extensions);
    FixedGapTermsIndexReader.getIndexExtensions(extensions);
  }

  @Override
  public PerDocConsumer docsConsumer(PerDocWriteState state) throws IOException {
    return new DefaultDocValuesConsumer(
         // TODO: Check compound option
        state, BytesRef.getUTF8SortedAsUnicodeComparator(), true);
  }

  @Override
  public PerDocValues docsProducer(SegmentReadState state) throws IOException {
         // TODO: Check compound option
    return new DefaultDocValuesProducer(
        state.segmentInfo, state.dir, state.fieldInfos, state.codecId, true,
        BytesRef.getUTF8SortedAsUnicodeComparator(), state.context);
  }
  */
}
