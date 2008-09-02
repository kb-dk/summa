/* $Id: SummaNumberAnalyzer.java,v 1.3 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:17 $
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
package dk.statsbiblioteket.summa.common.lucene.analysis;

import org.apache.commons.logging.Log;       import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * This Analyzer wraps a StandardAnalyzer after stripping off typical seprator
 * chars used in many ID schemes.
 * The list of removed chars is:
 * {'-', '_',':', '/' , '\'}
 *
 * 
 * @see org.apache.lucene.analysis.standard.StandardAnalyzer
 * @author Hans Lund, State and University Library - Aarhus, Denmark
 * @version $Id: SummaNumberAnalyzer.java,v 1.3 2007/10/04 13:28:17 te Exp $
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class SummaNumberAnalyzer extends Analyzer {


    private static final Log log = LogFactory.getLog(SummaNumberAnalyzer.class);

    private static final char[] removable = new char[]{'-', '_',':',
                                                       '/' , '\\'};

    /**
     * @see org.apache.lucene.analysis.Analyzer#tokenStream(String, java.io.Reader)
     *
     * @param fieldName name of the Indexfield.
     * @param reader  the reader containing the data.
     * @return  A StandardAnalyser tokenStream.
     */
    public TokenStream tokenStream(String fieldName, Reader reader){
        char c;
        int i;
        StringBuffer b = new StringBuffer();
        try {
            while ((i = reader.read()) != -1) {
                c = (char)i;
                boolean add = true;
                for (char aRemovable : removable) {
                   if (aRemovable == c){
                       add = false;
                       break;
                   }
                }
                if (add) { b.append(c); }
            }
        } catch (IOException e) {
            log.error("", e);
        }
        return new StandardAnalyzer(
                new String[]{}).tokenStream(fieldName,
                                            new StringReader(b.toString()));
    }



}
