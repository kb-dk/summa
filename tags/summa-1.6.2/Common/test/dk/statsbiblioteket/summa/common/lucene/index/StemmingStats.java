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
 * CVS:  $Id: StemmingStats.java,v 1.2 2007/10/04 13:28:21 te Exp $
 */
package dk.statsbiblioteket.summa.common.lucene.index;

import java.io.*;
import java.util.Map;
import java.util.HashMap;

import junit.framework.TestCase;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.store.*;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StemmingStats extends TestCase {
    private static final String INDEX_LOCATION =
            "/space/full_index";

    public void countTermsInFields(String termPrefix) throws Exception {
        int MAX_DUMP = 10;
        IndexReader ir = new IndexSearcher(new NIOFSDirectory(new File(INDEX_LOCATION))).getIndexReader();
        int all = 0;
        int termCount = 0;
        int totalTermCount = 0;
        String lastField = null;
        TermEnum terms = ir.terms();
        while(terms.next()) {
            all++;
            Term term = terms.term();
            if (!term.field().equals(lastField)) {
                if (lastField != null) {
                    totalTermCount += termCount;
                    if (termCount > MAX_DUMP) {
                        System.out.println("Total term-matches: " + termCount);
                    }
                }
                lastField = term.field();
                termCount = 0;
                System.out.println("Looking for '" + termPrefix
                                   + "' in field " + term.field());
            }
            if (term.text().startsWith(termPrefix)) {
                if (termCount++ < MAX_DUMP) {
                    System.out.println(term.text());
                }
            }
        }
        System.out.println("Total term-matches: " + totalTermCount + "/" + all);
    }

    public void countDenmarks() throws Exception {
        countTermsInFields("danmarks");
    }
    public void countMiddelalder() throws Exception {
        countTermsInFields("middel");
    }
}




