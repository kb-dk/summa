/* $Id: StemmingStats.java,v 1.2 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:21 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
/*
 * The State and University Library of Denmark
 * CVS:  $Id: StemmingStats.java,v 1.2 2007/10/04 13:28:21 te Exp $
 */
package dk.statsbiblioteket.summa.common.lucene.index;

import java.util.Map;
import java.util.HashMap;

import junit.framework.TestCase;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StemmingStats extends TestCase {
    private static final String INDEX_LOCATION =
            "/space/full_index";

    public void countTermsInFields(String termPrefix) throws Exception {
        int MAX_DUMP = 10;
        IndexReader ir = new IndexSearcher(INDEX_LOCATION).getIndexReader();
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



