package dk.statsbiblioteket.summa.support.solr;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.junit.Assert.*;

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
public class SolrDocumentEnrichTest {
    public static final String SIMPLE = "<doc><field name=\"foo\">bar</field></doc>";
    public static final String EXTRA = "<doc boost=\"5\"><field name=\"foo\">bar</field></doc>";
    public static final String FAULTY = "<field name=\"foo\">bar</field>";

    @Test
    public void testNoChange() throws UnsupportedEncodingException {
        SolrDocumentEnrich enricher = new SolrDocumentEnrich(Configuration.newMemoryBased());
        Record record = new Record("dummy", "dummy", SIMPLE.getBytes("utf-8"));
        assertFalse("The enricher should not be active", enricher.enrich(record));
    }

    @Test
    public void testRecordIDSuccess() throws UnsupportedEncodingException {
        SolrDocumentEnrich enricher = new SolrDocumentEnrich(Configuration.newMemoryBased(
                SolrDocumentEnrich.CONF_ELEMENTS, SolrDocumentEnrich.ELEMENTS.recordID.toString()
        ));
        Record record = new Record("dummyID", "dummy", SIMPLE.getBytes("utf-8"));
        assertTrue("The enricher should process the Record", enricher.enrich(record));
        assertTrue("The result should contain the recordID", record.getContentAsUTF8().contains(
                "<doc>\n<field name=\"recordID\">dummyID</field>\n<field name=\"foo\">bar</field></doc>"
        ));
    }

    @Test
    public void testRecordIDWithExtra() throws UnsupportedEncodingException {
        SolrDocumentEnrich enricher = new SolrDocumentEnrich(Configuration.newMemoryBased(
                SolrDocumentEnrich.CONF_ELEMENTS, SolrDocumentEnrich.ELEMENTS.recordID.toString()
        ));
        Record record = new Record("dummyID", "dummy", EXTRA.getBytes("utf-8"));
        assertTrue("The enricher should process the Record", enricher.enrich(record));
        assertTrue("The result should contain the recordID", record.getContentAsUTF8().contains(
                "<doc boost=\"5\">\n<field name=\"recordID\">dummyID</field>\n<field name=\"foo\">bar</field></doc>"
        ));
    }

    @Test
    public void testMTime() throws UnsupportedEncodingException {
        final long now = System.currentTimeMillis();
        SolrDocumentEnrich enricher = new SolrDocumentEnrich(Configuration.newMemoryBased(
                SolrDocumentEnrich.CONF_ELEMENTS, SolrDocumentEnrich.ELEMENTS.mtime.toString()
        ));
        Record record = new Record("dummyID", "dummy", SIMPLE.getBytes("utf-8"));
        record.setModificationTime(now);
        assertTrue("The enricher should process the Record", enricher.enrich(record));
        assertTrue("The result should contain the field mtime", record.getContentAsUTF8().contains(
                "<doc>\n<field name=\"mtime\">"
        ));
        // TODO: Do a proper verification of the time output
        System.out.println(record.getContentAsUTF8());
    }
}