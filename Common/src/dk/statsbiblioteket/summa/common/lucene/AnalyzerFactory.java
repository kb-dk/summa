/* $Id: AnalyzerFactory.java,v 1.2 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:20 $
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
 * CVS:  $Id: AnalyzerFactory.java,v 1.2 2007/10/04 13:28:20 te Exp $
 */
package dk.statsbiblioteket.summa.common.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.analysis.FreeTextAnalyzer;
import dk.statsbiblioteket.summa.common.lucene.index.OldIndexField;
import dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Builds Analyzers for Summa.
 * @deprecated in favor og {@link LuceneIndexDescriptor}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AnalyzerFactory {
    private static final Log log = LogFactory.getLog(AnalyzerFactory.class);

    /**
     * Fetches a search-descriptor, as specified in the configuration, and
     * builds a tree of Analyzer, based on said descriptor.
     * @param configuration contains the location of the search descriptor.
     * @return a proper Analyzer for the setup specified in the configuration.
     */
    public static Analyzer buildAnalyzer(Configuration configuration) {
        SearchDescriptor descriptor = new SearchDescriptor(configuration);
        return buildAnalyzer(descriptor);
    }

    /**
     * Builds a tree of Analyzers based on the structure defined in the
     * SearchDescriptor.
     * @param descriptor a description of Fields and Groups in the index.
     * @return a proper Analyzer based on the search description.
     */
    public static Analyzer buildAnalyzer(SearchDescriptor descriptor) {
        PerFieldAnalyzerWrapper analyzer =
                new PerFieldAnalyzerWrapper(new FreeTextAnalyzer());
        for (OldIndexField field : descriptor.getSingleFields()) {
            analyzer.addAnalyzer(field.getName(),
                                 field.getType().getAnalyzer());
        }
        for (SearchDescriptor.Group g : descriptor.getGroups().values()) {
            for (OldIndexField field : g.getFields()) {
                analyzer.addAnalyzer(field.getName(),
                                     field.getType().getAnalyzer());
            }
        }
        return analyzer;
    }
}
