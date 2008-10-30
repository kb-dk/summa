/* $Id: DumpIndex.java,v 1.3 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/05 10:20:23 $
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
 * CVS:  $Id: DumpIndex.java,v 1.3 2007/10/05 10:20:23 te Exp $
 */
package dk.statsbiblioteket.summa.tools.utils;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Simpe debug-thingie that dumps the fields/Terms pairs of an index.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DumpIndex {
    public static void main(String args[]) throws IOException {
         new DumpIndex().dumpIndex(args[0]);
    }

    public void dumpIndex(String index) throws IOException {
        IndexReader reader = new IndexSearcher(index).getIndexReader();
        TermEnum terms = reader.terms();
        System.out.println("Dumping index " + index);
        int counter = 0;
        while (terms.next()) {
            System.out.println(terms.term().field() + ": "
                               + terms.term().text());
            counter++;
        }
        System.out.println("Total: " + counter + " Field/Term pairs");
    }
}



