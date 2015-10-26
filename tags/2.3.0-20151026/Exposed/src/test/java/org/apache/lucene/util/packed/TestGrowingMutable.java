package org.apache.lucene.util.packed;

import junit.framework.TestCase;

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

public class TestGrowingMutable extends TestCase {

  public void testMiscGrowth() throws Exception {
    GrowingMutable g = new GrowingMutable();

    g.set(100, 1000);
    assertEquals("The value at index 100 should be correct", 1000, g.get(100));
    assertEquals("The size should be correct", 1, g.size());

    g.set(98, 5000);
    assertEquals("The value at index 98 should still be correct",
                 5000, g.get(98));
    assertEquals("The value at index 100 should still be correct",
                 1000, g.get(100));
    assertEquals("The size should be extended", 3, g.size());

    g.set(-123, -12345);
    assertEquals("The value at index 98 should still be correct",
                 5000, g.get(98));
    assertEquals("The value at index 100 should still be correct",
                 1000, g.get(100));
    assertEquals("The value at -123 should be as defined", 
                 -12345, g.get(-123));
  }

}
