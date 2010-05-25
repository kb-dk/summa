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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: MemoryStringPool.java,v 1.3 2007/10/04 13:28:21 te Exp $
 */
package dk.statsbiblioteket.summa.common.pool;

import java.text.Collator;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.pool.StringConverter;

/**
 * Simple implementation of Strings with MemoryPool.
 * The persistent files used by this implementation are compatible with those
 * from {@link DiskStringPool}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class MemoryStringPool extends MemoryPool<String> implements
                                                          CollatorSortedPool {
//    private Log log = LogFactory.getLog(MemoryStringPool.class);

    private Collator collator = null;

    public MemoryStringPool(Collator collator) {
        super(new StringConverter(), collator);
        this.collator = collator;
    }

    /* Mutators */

    public Collator getCollator() {
        return collator;
    }
    public void setCollator(Collator collator) {
        this.collator = collator;
    }
}




