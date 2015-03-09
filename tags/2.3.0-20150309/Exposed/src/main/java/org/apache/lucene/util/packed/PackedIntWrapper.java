/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.lucene.util.packed;

public class PackedIntWrapper extends PackedInts.ReaderImpl {
  private final int[] values;

  public PackedIntWrapper(int[] values) {
    super(values.length, 32);
    this.values = values;
  }

  public PackedIntWrapper(int[] values, int size) {
    super(size, 32);
    this.values = values;
  }

  @Override
  public long get(int index) {
    return values[index];
  }

  @Override
  public long ramBytesUsed() {
    return values.length*4;
  }

}
