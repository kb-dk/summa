/* $Id: SummaAnalyzer.java,v 1.3 2007/10/04 13:28:17 te Exp $
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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

import java.io.Reader;
import java.io.IOException;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The SummaAnalyzer defines a configurable chain for tokenization.
 * The chain contains first a TokenMasker the a TransliteratorTokenizer
 *
 * @see dk.statsbiblioteket.summa.common.lucene.analysis.TokenMasker
 * @see dk.statsbiblioteket.summa.common.lucene.analysis.TransliteratorTokenizer
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal",
        comment = "Methods needs Javadoc")
public class SummaAnalyzer extends Analyzer {

    String transliterationRules;
    boolean keepDefaultTransliterations;

    String maskingRules;
    boolean keepDefaultMasking;
    boolean ignoreCase;

    /**
     * Creates an analyzer on the basis of information in the configuration.
     * @param configuration contains all rules for the analyzer.
     */
    public SummaAnalyzer(Configuration configuration) {

    }

    /**
     * Makes a SummaAnalyzer.
     *
     * @param transliterationRules       the transliteration rules are parsed to
     *                                   the TransliteratorTokenizer.
     * @param keepDefaultTransliterations if true the transliterationRules are
     *                                   added to the existing rules defined in
     *                                   the TransliteratorTokenizer.
     * @param maskingRules               the maskingRules are parsed to the
     *                                   TokenMasker
     * @param keepDefaultMasking         if true the maskingRules are added to
     *                                   the default rules defined in the
     *                                   TokenMasker
     * @param ignoreCase                 if true masking will ignore case.
     */
    public SummaAnalyzer(String transliterationRules,
                         boolean keepDefaultTransliterations,
                         String maskingRules,
                         boolean keepDefaultMasking,
                         boolean ignoreCase){
         super();
         this.transliterationRules = transliterationRules;
         this.keepDefaultTransliterations = keepDefaultTransliterations;
         this.maskingRules = maskingRules;
         this.keepDefaultMasking = keepDefaultMasking;
         this.ignoreCase = ignoreCase;
     }

    /**
     * The TokenStream returned is a TransliteratorTokenizer where input have
     * parsed through the TokenMasker.
     *
     * @see org.apache.lucene.analysis.Analyzer#tokenStream(String,java.io.Reader)
     * @param fieldName - name of the field
     * @param reader - containin the text
     * @return a TransliteratorTokenizer tokenStream filtered by a TokenMasker.
     */
     public TokenStream tokenStream(String fieldName, Reader reader){
         try {
             return new TransliteratorTokenizer(
                     new TokenMasker(reader, maskingRules, keepDefaultMasking,
                                     ignoreCase), transliterationRules,
                                     keepDefaultTransliterations);
         } catch (IOException e) {
             return null;
         }
     }



}



