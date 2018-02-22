package dk.statsbiblioteket.summa.support.enrich;


import com.google.common.collect.ImmutableList;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.List;

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
public class SRTProcessorTest {
    private static Log log = LogFactory.getLog(SRTProcessorTest.class);

    public static final String ENTRY =
            "168\n" +
            "00:20:41,150 --> 00:20:45,109\n" +
            "- How did he do that?\n" +
            "- Made him an offer he couldn't refuse.\n";
    public static final String XML_ENTRY =
            "<content><srt>" + escape(ENTRY) + "</srt></content>";
    public static final String XML_ENTRIES =
            "<content><srt>" + escape(ENTRY) + "\n" + escape(ENTRY) + "</srt></content>";
    public static final String META = "sub002041150e002045109i168";

    @Test
    public void testSingleEntry() throws UnsupportedEncodingException {
        List<String> srts = getProcessedSRT("content/srt", XML_ENTRY);
        assertEquals("The number of entries should be as expected", 1, srts.size());
        String srt = srts.get(0);

        String EXPECTED = "sub002041150e002045109i168";
        assertTrue("The result should contain '" + EXPECTED + ", but was\n" + srt, srt.contains(EXPECTED));
        log.debug("Got SRT entry\n" + srt);
    }

    @Test
    public void testMultiEntry() throws UnsupportedEncodingException {
        List<String> srts = getProcessedSRT("content/srt", XML_ENTRIES);
        assertEquals("The number of entries should be as expected", 2, srts.size());
        for (int i = 0 ; i < srts.size() ; i++) {
            String srt = srts.get(i);

            assertTrue("The entry at index " + i + " should contain '" + META + "\n" + srt, srt.contains(META));
            log.debug("Got SRT entry\n" + srt);
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getProcessedSRT(String xpath, String xml) throws UnsupportedEncodingException {

        PayloadFeederHelper feeder = new PayloadFeederHelper(ImmutableList.of(new Payload(
                new Record("dummyID", "dummyBase", xml.getBytes("utf-8")))));
        SRTProcessor srtProcessor = new SRTProcessor(Configuration.newMemoryBased(
                SRTProcessor.CONF_SRT_FAKE_XPATH, xpath
        ));
        srtProcessor.setSource(feeder);
        assertTrue("The processor should contain at least 1 Payload", srtProcessor.hasNext());
        Payload payload = srtProcessor.next();
        return (List<String>)payload.getData(SRTProcessor.SRT_KEY);
    }

    public static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}