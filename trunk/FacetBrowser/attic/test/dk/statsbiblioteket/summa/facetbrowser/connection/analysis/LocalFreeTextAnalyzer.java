/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
 * CVS:  $Id$
 */
package dk.statsbiblioteket.summa.facetbrowser.connection.analysis;

import java.io.Reader;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import dk.statsbiblioteket.summa.facetbrowser.connection.analysis.TransliteratorTokenizer;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Free text analyzer lifted from the Commons module. Suitable only for testing!
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, te")
public class LocalFreeTextAnalyzer extends Analyzer {
    String translitterationRules;
    boolean keepDefaultTransliterations;

    String maskingRules;
    boolean keepDefaultMasking;
    boolean ignoreCase;

    public LocalFreeTextAnalyzer() {
        this("",true,"",true,true);
    }

    /**
     * Makes a SummaAnalyzer.
     *
     * @param transliterationRules - the transliteration rules are parsed to the TransliteratorTokenizer.
     * @param keepDefaultTransliteration - if true the transliterationRules are added to the existing rules defined in the TransliteratorTokenizer.
     * @param maskingRules - the maskingRules are parsed to the TokenMasker
     * @param keepDefaultMasking - if true the maskingRules are added to the default rules defined in the TokenMasker
     * @param ignoreCase - if true masking will ignore case.
     */
    public LocalFreeTextAnalyzer(String transliterationRules,
                                 boolean keepDefaultTransliteration,
                                 String maskingRules,
                                 boolean keepDefaultMasking,
                                 boolean ignoreCase){
         super();
         translitterationRules = transliterationRules;
         keepDefaultTransliterations = keepDefaultTransliteration;
         this.maskingRules = maskingRules;
         this.keepDefaultMasking = keepDefaultMasking;
         this.ignoreCase = ignoreCase;
     }

    /**
     * The TokenStream returned is a TransliteratorTokenizer where input have parsed through the TokenMasker.
     *
     * @see org.apache.lucene.analysis.Analyzer#tokenStream(String, java.io.Reader)
     * @param fieldName - name of the field
     * @param reader - containin the text
     * @return a TransliteratorTokenizer tokenStream filtered by a TokenMasker.
     */
     public TokenStream tokenStream(String fieldName, Reader reader){
         try {
             return  new TransliteratorTokenizer(new TokenMasker(reader, maskingRules, keepDefaultMasking, ignoreCase),
                translitterationRules,keepDefaultTransliterations);
         } catch (IOException e) {
             return null;
         }
     }



}
