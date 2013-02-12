package org.apache.lucene.util.packed;

import junit.framework.TestCase;
import org.apache.lucene.search.exposed.ExposedSettings;

import java.util.Random;

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

public class TestMonotonicReaderFactory extends TestCase {

  public void testBasicReduce() throws Exception {
    final int VALUES = 10000;
    final PackedInts.Mutable original = PackedInts.getMutable(
        VALUES, PackedInts.bitsRequired(VALUES), 0);
    ExposedSettings.debug = true;
    for (int i = 0 ; i < VALUES ; i++) {
      original.set(i, i);
    }
    PackedInts.Reader reduced = MonotonicReaderFactory.reduce(original);
    assertEquals(original, reduced);
  }

  public void testMonkey() throws Exception {
    Random random = new Random();
    final int VALUES = 10000;
    final int MAX_DELTA = 100;
    final PackedInts.Mutable original = PackedInts.getMutable(
        VALUES, PackedInts.bitsRequired(VALUES*MAX_DELTA), 0);
    ExposedSettings.debug = true;
    long val = 0;
    for (int i = 0 ; i < VALUES ; i++) {
      val += random.nextInt(MAX_DELTA);
      original.set(i, val);
    }
    PackedInts.Reader reduced = MonotonicReaderFactory.reduce(original);
    assertEquals(original, reduced);
  }

  private void assertEquals(
      PackedInts.Reader expected, PackedInts.Reader actual) {
    assertEquals("Sizes should match",
                 expected.size(), actual.size());
    assertEquals("BPV should match",
                 expected.getBitsPerValue(), actual.getBitsPerValue());
    for (int i = 0 ; i < expected.size() ; i++) {
      assertEquals("Entries at index " + i + " should match",
                   expected.get(i), actual.get(i));
    }
  }

}
