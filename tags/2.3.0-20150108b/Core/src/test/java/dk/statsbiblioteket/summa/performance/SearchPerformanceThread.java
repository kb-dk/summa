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
package dk.statsbiblioteket.summa.performance;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.TokenMgrError;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            Logging.fatal(log, "SearchPerformanceThread.run", message, e);
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
        if (log.isTraceEnabled()) {
            log.trace("Searching for '" + query + "'");
        }
        if ("".equals(query)) {
            return 0;
        }
        try {
            Query parsedQuery = mediator.parser.parse(query);
            if (mediator.simulate) {
                return 0;
            }
            TopDocs topDocs = searcher.search(parsedQuery, mediator.maxHits);
            //noinspection DuplicateStringLiteralInspection
            Set<String> selector = new HashSet<>(Arrays.asList("shortformat"));
            for (int i = 0 ; i < Math.min(topDocs.scoreDocs.length, mediator.maxHits) ; i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                Document doc = searcher.getIndexReader().document(scoreDoc.doc, selector);
                for (String field: mediator.fields) {
                    IndexableField iField = doc.getField(field);
                    if (log.isTraceEnabled()) {
                        log.trace("Query(" + query + ") hit(" + i + ") field '" + field
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
            String message ="Exception parsing '" + query + "': "
                            + e.getMessage();
            System.err.println(message);
            if (log.isDebugEnabled()) {
                log.debug(message, e);
            }
            return 0;
        }
    }
}
