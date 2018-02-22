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
package dk.statsbiblioteket.summa.support.solr;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for {@link SolrManipulator} that optionally checks incoming Solr documents for invalid timestamps
 * and tries to correct them by using lenient timestamp parsing instead of strict. Reason for creation was
 * newspaper scans with dates such as 1868-04-31, which seems like an oversight and should be rolled to 1868-05-01.
 * </p><p>
 * This helper uses simple String manipulation instead of solid XML parse/write for speed. No actual
 * measurements had been performed to verify if this alleged speed-up is substantial or not. For the same
 * reason is skips proper date-parsing and uses custom code instead. The date-math should only be considered stable
 * for years 1583-9999.
 * </p><p>
 * Handled dates are field content that starts with YYYY-MM-DD. Everything following this is left unchanged.
 * </p><p>
 * This class is not thread safe inside of the same instantiation. Using multiple instantiations in separate threads
 * will work.
 */
public class SolrLenientTimestamp implements SolrDocumentAdjustFilter.Adjuster {
    private static Log log = LogFactory.getLog(SolrLenientTimestamp.class);

    /**
     * The fields to check for dates.
     */
    public static final String CONF_FIELDS = "solrlenienttimestamp.fields";
    public static final List<String> DEFAULT_FIELDS = Collections.emptyList();

    private static final int[] NON_LEAP = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final int[] LEAP =     {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    private final List<String> fields;
    private final Matcher fieldMatcher; // Switch to Pattern with Matcher instantiation if the class must be thread-safe
    private static AtomicLong correctedRecords = new AtomicLong(0);

    public SolrLenientTimestamp(Configuration conf) {
        fields = conf.getStrings(CONF_FIELDS, DEFAULT_FIELDS);
        fieldMatcher = Pattern.compile(
                "(<field [^>]*name=\")(" + Strings.join(fields, "|", fields.size()) + ")(\"[^>]*>)([^<]+)"
        ).matcher("");
        log.info("Created " + this);
    }

    @Override
    public boolean adjust(Payload payload) {
        if (payload.getRecord() == null) {
            return false;
        }
        final Record record = payload.getRecord();

        if (fields.isEmpty()) {
            return false;
        }
        final String oldContent = RecordUtil.getString(record, RecordUtil.PART.content);
        if (oldContent == null) {
            return false;
        }

        fieldMatcher.reset(oldContent);
        enriched.setLength(0);

        boolean adjusted = false;
        int lastEnd = 0;
        while (fieldMatcher.find()) {
            enriched.append(oldContent.substring(lastEnd, fieldMatcher.start()));
            enriched.append(fieldMatcher.group(1)).append(fieldMatcher.group(2)).append(fieldMatcher.group(3));
            final String cDate = correctDate(record, fieldMatcher.group(4));
            enriched.append(cDate);
            lastEnd = fieldMatcher.end();
            if (!cDate.equals(fieldMatcher.group(4))) {
                adjusted = true;
                Logging.logProcess("SolrLenientTimestamp",
                                   "Leniency-correcting timestamp '" + fieldMatcher.group(4) + " to " + cDate +
                                   ". This was correction #" + correctedRecords.get() +
                                   ", counted across all instances in this run",
                                   Logging.LogLevel.WARN, record.getId());
            }
        }
        if (adjusted) {
            correctedRecords.incrementAndGet();
            enriched.append(oldContent.substring(lastEnd));
            try {
                record.setContent(enriched.toString().getBytes("utf-8"), false);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8 should be available everywhere", e);
            }
        }
        return adjusted;
    }

    // 2017-05-30T16:22:17.123Z
    // 1868-04-31T00:00:00.000Z
    // 1868-04-31
    private String correctDate(Record record, String timestamp) {
        if (timestamp.length() < 10) {
            Logging.logProcess("SolrLenientTimestamp", "Timestamp must be at least 10 chars, but was '" + timestamp + "'",
                               Logging.LogLevel.WARN, record.getId());
            return timestamp;
        }
        int year = Integer.parseInt(timestamp.substring(0, 4));
        int month = Integer.parseInt(timestamp.substring(5, 7));
        int day = Integer.parseInt(timestamp.substring(8, 10));
        boolean leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0) ;
        int[] monthLengths = leap ? LEAP : NON_LEAP;

        if (day <= monthLengths[month-1]) {
            return timestamp;
        }

        // Homemade date roller
        while (day > monthLengths[month-1]) {
            day -= monthLengths[month-1];
            if (month++ == 12) {
                year++;
                leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0) ;
                monthLengths = leap ? LEAP : NON_LEAP;
            }
        }
        // 1868-05-01T00:00:00.000Z
        return String.format("%04d-%02d-%02d%s", year, month, day, timestamp.substring(10));
    }
    private final StringBuilder enriched = new StringBuilder();

    @Override
    public String toString() {
        return "SolrLenientTimestamp(fields=[" + Strings.join(fields) + "], " +
               "JVM-global correctedRecords=" + correctedRecords + ")";
    }
}
