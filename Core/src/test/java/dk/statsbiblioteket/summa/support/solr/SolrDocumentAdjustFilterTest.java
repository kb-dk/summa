package dk.statsbiblioteket.summa.support.solr;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import junit.framework.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;

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
public class SolrDocumentAdjustFilterTest {
    public static final String SOLR1_IN =
            "<doc>\n" +
            "<field name=\"foo\">bar</field>\n" +
            "<field name=\"ts_nochange\">2017-05-31T10:15:00.123Z</field>\n" +
            "<field name=\"ts_nochange\">2017-04-31T10:15:00.123Z</field>\n" +
            "<field name=\"ts_change\">2017-05-31T10:15:00.123Z</field>\n" +
            "<field name=\"ts_change\">2017-04-31T10:15:00.123Z</field>\n" +
            "<field name=\"ts_change\">2000-02-29T10:15:00.123Z</field>\n" +
            "<field name=\"ts_change\">2001-02-29T10:15:00.123Z</field>\n" +
            "<field name=\"ts_change\">2004-02-29T10:15:00.123Z</field>\n" +
            "</doc>";
    public static final String SOLR1_EXPECTED =
            "<doc>\n" +
            "<field name=\"foo\">bar</field>\n" +
            "<field name=\"ts_nochange\">2017-05-31T10:15:00.123Z</field>\n" +
            "<field name=\"ts_nochange\">2017-04-31T10:15:00.123Z</field>\n" +
            "<field name=\"ts_change\">2017-05-31T10:15:00.123Z</field>\n" +
            "<field name=\"ts_change\">2017-05-01T10:15:00.123Z</field>\n" +
            "<field name=\"ts_change\">2000-02-29T10:15:00.123Z</field>\n" +
            "<field name=\"ts_change\">2001-03-01T10:15:00.123Z</field>\n" +
            "<field name=\"ts_change\">2004-02-29T10:15:00.123Z</field>\n" +
            "</doc>";

    @Test
    public void testLenientDates() throws UnsupportedEncodingException {
        SolrDocumentAdjustFilter adjuster = new SolrDocumentAdjustFilter(Configuration.newMemoryBased(
                SolrDocumentAdjustFilter.CONF_ADJUSTMENTS, SolrDocumentAdjustFilter.ADJUSTMENT.lenient_dates,
                SolrLenientTimestamp.CONF_FIELDS, "ts_change"
        ));
        adjuster.setSource(PayloadFeederHelper.createHelper(Collections.singletonList(
                new Record("dummy", "dummy", SOLR1_IN.getBytes("utf-8")))));
        final String adjusted = adjuster.next().getRecord().getContentAsUTF8();
        Assert.assertEquals("The timestamps for the faulty selected fields should be corrected",
                            SOLR1_EXPECTED, adjusted);
        adjuster.close(true);
    }
}