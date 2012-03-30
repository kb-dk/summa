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
package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Assert;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class HarmoniseTestHelper {
    private static Log log = LogFactory.getLog(HarmoniseTestHelper.class);

    public static void compareHits(String query, ResponseCollection rc1, ResponseCollection rc2) {
        String rs1 = Strings.join(getResultIDs(rc1), ", ");
        Assert.assertEquals("Expected equality for query '" + query + "'",
                            rs1, Strings.join(getResultIDs(rc2), ", "));
        Assert.assertFalse("There was no result for '" + query + "'", "".equals(rs1));
    }

    public static void compareHits(String query, boolean shouldMatch, ResponseCollection rc1, ResponseCollection rc2) {
        if (shouldMatch) {
            Assert.assertEquals("Expected equality for query '" + query + "'",
                                Strings.join(getResultIDs(rc1), ", "), Strings.join(getResultIDs(rc2), ", "));
        } else {
            String qr1 = Strings.join(getResultIDs(rc1), ", ");
            String qr2 = Strings.join(getResultIDs(rc2), ", ");
            if (qr1.equals(qr2)) {
                Assert.fail("Expected non-equality for query '" + query + "' with result '" + qr1 + "'");
            }
        }
    }

    public static List<String> getResultIDs(ResponseCollection responses) {
        for (Response response : responses) {
            if (response instanceof DocumentResponse) {
                DocumentResponse dr = (DocumentResponse)response;
                ArrayList<String> results = new ArrayList<String>((int)dr.getHitCount());
                for (DocumentResponse.Record record: dr.getRecords()) {
                    results.add(record.getId());
                }
                return results;
            }
        }
        return null;
    }

    public static int countResults(ResponseCollection responses) {
        for (Response response : responses) {
            if (response instanceof DocumentResponse) {
                return (int) ((DocumentResponse) response).getHitCount();
            }
        }
        throw new IllegalArgumentException(
                "No documentResponse in ResponseCollection");
    }
}
