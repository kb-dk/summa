/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.performance;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.TokenMgrError;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.SetBasedFieldSelector;
import org.apache.lucene.document.Field;

import java.util.HashSet;
import java.util.Arrays;
import java.io.*;

/**
 * Iterates through a list of logged queries, performing a search on each.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchPerformanceThread extends Thread {
    private static Log log = LogFactory.getLog(SearchPerformanceThread.class);

    private IndexSearcher searcher;
    private SearchPerformanceMediator mediator;

    public SearchPerformanceThread(SearchPerformanceMediator mediator,
                                   IndexSearcher searcher) {
        this.mediator = mediator;
        this.searcher = searcher;
        log.trace("Thread ready for run");
    }

    @Override
    public void run() {
        try {
            test();
        } catch (Exception e) {
            String message = "Exception running performance thread";
            System.err.println(message);
            log.fatal(message, e);
        }
    }

    public void test() throws IOException {
        String query;
        while ((query = mediator.getNextQuery()) != null) {
            try {
                mediator.ping(test(query));
            } catch(Exception e) {
                System.err.println("Exception doing query '" + query + "'");
                // Yes, we ignore the exception
            }
        }
    }

    public int test(String query) {
        if ("".equals(query)) {
            return 0;
        }
        try {
            Query parsedQuery = mediator.parser.parse(query);
            if (mediator.simulate) {
                return 0;
            }
            TopFieldDocs topDocs = searcher.search(
                    parsedQuery, null, mediator.maxHits, null);
            //noinspection DuplicateStringLiteralInspection
            FieldSelector selector = new SetBasedFieldSelector(
                    new HashSet<String>(Arrays.asList("shortformat")),
                    new HashSet(5));
            for (int i = 0 ;
                 i < Math.min(topDocs.scoreDocs.length, mediator.maxHits) ;
                 i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                Document doc =
                     searcher.getIndexReader().document(scoreDoc.doc, selector);
                for (String field: mediator.fields) {
                    Field iField = doc.getField(field);
                    if (log.isTraceEnabled()) {
                        log.trace("Query(" + query + ") field '" + field
                                  + "'(" + iField.stringValue() + ")");
                    }
                }

            }
            return topDocs.scoreDocs.length;
        } catch(ParseException e) {
            System.err.println("ParseException parsing '" + query + "'");
            return 0;
        } catch (IOException e) {
            System.err.println("IOException handling '" + query + "': "
                               + e.getMessage());
            return 0;
        } catch (TokenMgrError e) {
            System.err.println("TokenMgr error for '" + query + "': "
                               + e.getMessage());
            return 0;
        } catch (Exception e) {
            System.err.println("Exception parsing '" + query + "': "
                               + e.getMessage());
            return 0;
        }
    }
}
