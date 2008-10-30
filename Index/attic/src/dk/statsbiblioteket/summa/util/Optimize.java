/* $Id: Optimize.java,v 1.3 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/05 10:20:22 $
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
package dk.statsbiblioteket.summa.util;

import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaStandardAnalyzer;
import dk.statsbiblioteket.util.qa.QAInfo;


import java.io.IOException;

import org.apache.lucene.index.IndexWriter;

/**
@deprecated optimization is performed during consolidation of indexes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class Optimize {

    static IndexWriter writer;

    public static void main(String args[]) throws IOException, IndexServiceException {
        writer = new IndexWriter(args[0], new SummaStandardAnalyzer(), false );
        optimizeAll();
    }

    private static  synchronized void optimizeAll() throws IndexServiceException {
        if (writer != null) {
            try {
                writer.optimize();
            } catch (IOException e) {
                throw new IndexServiceException("Unable to optimize:" + writer.getInfoStream(), e);
            }
            try {

                writer.close();
            } catch (IOException e) {
                throw new IndexServiceException("Unable to close:" + writer.getInfoStream(), e);
            }
            writer = null;
        }
    }


}



