/* $Id: Tools.java,v 1.4 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.4 $
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
package dk.statsbiblioteket.summa.tools.utils;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Converts an index to compound form (the separate segments are merged into
 * one).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, te")
public class Tools {
   private static Analyzer a = new StandardAnalyzer();

    /**
     * Usage: Tools indexlocation(dir) compound(boolean).
     * @param args first argument is the index location, second argument is a
     *             boolean that specifies whether the index should be converted
     *             to compound form.
     * @throws IOException if the index could not be accessed.
     */
    public static void main(String args[]) throws IOException {
         convertIndexStructure( args[0], Boolean.parseBoolean(args[1]) );
    }

    private static void convertIndexStructure(String indexDir, boolean compound)
            throws IOException {
        IndexWriter w = new IndexWriter(indexDir, a, false);
        w.setUseCompoundFile(compound);
        w.optimize();
        w.close();
    }
}
