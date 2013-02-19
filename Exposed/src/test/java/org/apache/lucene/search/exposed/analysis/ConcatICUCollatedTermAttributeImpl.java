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
package org.apache.lucene.search.exposed.analysis;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RawCollationKey;
import org.apache.lucene.analysis.tokenattributes.CharTermAttributeImpl;
import org.apache.lucene.util.BytesRef;

import java.io.UnsupportedEncodingException;

/**
 * Derivative of {@link org.apache.lucene.collation.tokenattributes.ICUCollatedTermAttributeImpl} that adds the original
 * term to the collation key for future extraction. The delimiter for the key and the original String is 0, which is
 * added automatically by the ICU Collator.
 */
public class ConcatICUCollatedTermAttributeImpl extends CharTermAttributeImpl {
  private final Collator collator;
  private final RawCollationKey key = new RawCollationKey();

  /**
   * Create a new ICUCollatedTermAttributeImpl
   * @param collator Collation key generator
   */
  public ConcatICUCollatedTermAttributeImpl(Collator collator) {
    // clone the collator: see http://userguide.icu-project.org/collation/architecture
    try {
      this.collator = (Collator) collator.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] buffer = new byte[100];

  @Override
  public int fillBytesRef() {
    final String str = toString();
    collator.getRawCollationKey(str, key);
    byte[] strBytes;
    try {
      strBytes = str.getBytes("utf-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("utf-8 must be supported", e);
    }
    final int bufferSize = strBytes.length +  key.bytes.length;
    if (buffer.length < bufferSize) {
      buffer = new byte[bufferSize];
    }
    System.arraycopy(key.bytes, 0, buffer, 0, key.size);
    System.arraycopy(strBytes, 0, buffer, key.size, strBytes.length);

    BytesRef result = getBytesRef();
    result.bytes = buffer;
    result.offset = 0;
    result.length = bufferSize;
    return result.hashCode();
  }

  public static BytesRef getOriginalString(final BytesRef concat, BytesRef reuse) {
    for (int i = 0 ; i < concat.length ; i++) {
      if (concat.bytes[concat.offset+i] == 0) {
        if (reuse == null) {
          reuse = new BytesRef(new byte[concat.length-i+1]); // +1 to handle empty String
        } else if (reuse.bytes.length < concat.length-i+1) {
          reuse.bytes = new byte[concat.length-i+1];
        }
        System.arraycopy(concat.bytes, concat.offset+i+1, reuse.bytes, 0, concat.length-i-1);
        reuse.offset = 0;
        reuse.length = concat.length-i-1;
        return reuse;
      }
    }
    return concat; // Input did not contain a 0
  }
}
