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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.collation.ICUCollationAttributeFactory;
import org.apache.lucene.util.BytesRef;

import java.io.Reader;

/**
 * Derivative of {@link org.apache.lucene.collation.ICUCollationKeyAnalyzer} that adds the original term to the
 * collation key for future extraction.
 * </p><p>
 * Note: The input is not otherwise processes and will retain casing, diacritics etc,
 */
public class ConcatICUCollationAnalyzer extends Analyzer {
    private final Collator collator;
    private final ConcatICUCollationAttributeFactory factory;

    /**
     * Create a new ICUCollationKeyAnalyzer, using the specified collator.
     *
     * @param collator CollationKey generator
     */
    public ConcatICUCollationAnalyzer(Collator collator) {
      this.collator = collator;
      this.factory = new ConcatICUCollationAttributeFactory(collator);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        KeywordTokenizer tokenizer = new KeywordTokenizer(factory, reader, KeywordTokenizer.DEFAULT_BUFFER_SIZE);
        return new TokenStreamComponents(tokenizer, tokenizer);
    }

    // Delegation to {@link ConcatICUCollatedTermAttributeImpl#getOriginalString}.
    public static BytesRef getOriginalString(final BytesRef concat, BytesRef reuse) {
        return ConcatICUCollatedTermAttributeImpl.getOriginalString(concat, reuse);
    }

    public Collator getCollator() {
        return collator;
    }
}
