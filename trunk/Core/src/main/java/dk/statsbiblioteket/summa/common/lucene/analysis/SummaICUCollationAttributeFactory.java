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
package dk.statsbiblioteket.summa.common.lucene.analysis;

import com.ibm.icu.text.Collator;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.collation.tokenattributes.ICUCollatedTermAttributeImpl;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeSource;

/**
 * Derivative of {@link org.apache.lucene.collation.ICUCollationAttributeFactory} that adds the original term to the
 * collation key for future extraction.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummaICUCollationAttributeFactory extends AttributeSource.AttributeFactory {

    private final Collator collator;
    private final AttributeSource.AttributeFactory delegate;

    /**
     * Create an ICUCollationAttributeFactory, using
     * {@link org.apache.lucene.util.AttributeSource.AttributeFactory#DEFAULT_ATTRIBUTE_FACTORY} as the
     * factory for all other attributes.
     * @param collator CollationKey generator
     */
    public SummaICUCollationAttributeFactory(Collator collator) {
      this(AttributeSource.AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY, collator);
    }

    /**
     * Create an ICUCollationAttributeFactory, using the supplied Attribute
     * Factory as the factory for all other attributes.
     * @param delegate Attribute Factory
     * @param collator CollationKey generator
     */
    public SummaICUCollationAttributeFactory(AttributeSource.AttributeFactory delegate, Collator collator) {
      this.delegate = delegate;
      this.collator = collator;
    }

    @Override
    public AttributeImpl createAttributeInstance(Class<? extends Attribute> attClass) {
      return attClass.isAssignableFrom(ICUCollatedTermAttributeImpl.class)
        ? new ICUCollatedTermAttributeImpl(collator)
        : delegate.createAttributeInstance(attClass);
    }

}
