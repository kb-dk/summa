package org.apache.lucene.util;

import junit.framework.TestCase;
import org.apache.lucene.util.packed.GrowingMutable;

import java.util.Arrays;

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

public class TestDoubleIntArrayList extends TestCase {

  public void testGrowth() throws Exception {
    DoubleIntArrayList list = new DoubleIntArrayList(0);
    list.add(1, 2);
    list.add(3, 4);
    assertEquals(new int[]{1, 3}, list.getPrimaries());
    assertEquals(new int[]{2, 4}, list.getSecondaries());
  }

  public void testSet() throws Exception {
    DoubleIntArrayList list = new DoubleIntArrayList(0);
    list.add(1, 2);
    list.set(1000, 3, 4);
  }

  public void testSortPrimary() {
    DoubleIntArrayList list = new DoubleIntArrayList(0);
    list.add(1, 4);
    list.add(3, 2);
    list.add(2, 3);

    list.sortByPrimaries();
    assertEquals(new int[]{1, 2, 3}, list.getPrimaries());
    assertEquals(new int[]{4, 3, 2}, list.getSecondaries());

/*    list.sortBySecondaries();
    assertEquals(new int[]{2, 3, 4}, list.getSecondaries());
    assertEquals(new int[]{3, 2, 1}, list.getPrimaries());*/
  }

  private void assertEquals(int[] expected, int[] actual) {
    if (expected.length != actual.length) {
      fail("Expected array of length " + expected.length + " got one of length " + actual.length);
    }
    for (int i = 0 ; i < expected.length ; i++) {
      if (expected[i] != actual[i]) {
        fail("The element at index " + i + " was expected to be " + expected[i] + " but was " + actual[i]);
      }
    }
  }

}
