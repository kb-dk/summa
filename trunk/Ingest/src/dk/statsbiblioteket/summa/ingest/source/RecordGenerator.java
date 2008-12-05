/* $Id$
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
package dk.statsbiblioteket.summa.ingest.source;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.Record;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.StringWriter;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.List;

/**
 * Generates semi-random Records, usable for testing performance and
 * scalability.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RecordGenerator implements ObjectFilter {
    private static Log log = LogFactory.getLog(RecordGenerator.class);

    /**
     * The location of the template for the generator. This is resolved by
     * {@link dk.statsbiblioteket.summa.common.configuration.Resolver},
     * so URLs, classpaths and more can be used. The format of the template is
     * given in the CONTENT_*-constants in this class.
     * </p><p>
     * Either this or {@link #CONF_CONTENT_TEMPLATE}.
     */
    public static final String CONF_CONTENT_TEMPLATE_LOCATION =
            "summa.ingest.generator.template.location";

    /**
     * The template for content.
     * </p><p>
     * Either this or {@link #CONF_CONTENT_TEMPLATE_LOCATION} must be defined.
     */
    public static final String CONF_CONTENT_TEMPLATE =
            "summa.ingest.generator.template";

    /**
     * The number of Records to generate.
     * </p><p>
     * Optional. Default is Integer.MAX_VALUE.
     */
    public static final String CONF_RECORDS = "summa.ingest.generator.records";
    public static final int DEFAULT_RECORDS = Integer.MAX_VALUE;

    /**
     * The minimal delay in ms between Record generation. If a new Record is
     * requested befor the delay has passed, the generator blocks to ensure
     * the delay.
     * </p><p>
     * Optional. Default is 0 ms.
     */
    public static final String CONF_MINDELAY = "summa.ingest.generator.delay";
    public static final int DEFAULT_MINDELAY = 0;

    /**
     * The base for the generated Records. This is expanded just as
     * {@link #CONF_CONTENT_TEMPLATE_LOCATION}.
     * </p><p>
     * Optional. Default is "dummy".
     */
    public static final String CONF_BASE_TEMPLATE =
            "summa.ingest.generator.base";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String DEFAULT_BASE = "dummy";

    /**
     * The template for the id. This is expanded just as
     * {@link #CONF_CONTENT_TEMPLATE_LOCATION}.
     * </p><p>
     * Optional. Default is "dummy_$INCREMENTAL_NUMBER[id]".
     */
    public static final String CONF_ID_TEMPLATE =
            "summa.ingest.generator.idtemplate";
    public static final String DEFAULT_ID_TEMPLATE =
            "dummy_$INCREMENTAL_NUMBER[id]";

    /**
     * All occurences of "$INCREMENTAL_NUMBER[key]" will be replaced by an
     * integer starting at 0 and incremented by 1 for each use. The integer is
     * referenced by key, making it possible to use distinct counters.
     * Key is a word made up of the letters a-z.
     */
    public static final String CONTENT_INCREMENTAL_NUMBER =
            "$INCREMENTAL_NUMBER";
    private Pattern PATTERN_INCREMENTAL_NUMBER =
            Pattern.compile(".*?(\\$INCREMENTAL_NUMBER\\[(\\w+)\\]).*",
                            Pattern.DOTALL);
    private Map<String, Integer> incrementalNumbers =
            new HashMap<String, Integer>(10);

    /**
     * All occurences of "$RANDOM_NUMBER[min, max]" will be replaced by a random
     * integer from min to max (both inclusive).
     */
    public static final String CONTENT_RANDOM_NUMBER = "$RANDOM_NUMBER";
    private Pattern PATTERN_RANDOM_NUMBER =
            Pattern.compile(".*?(\\$RANDOM_NUMBER\\[(\\d+), *(\\d+)\\]).*",
                            Pattern.DOTALL);

    /**
     * All occurences of "$RANDOM_CHARS[min, max]" will be replaced by a random
     * number of random chars, where the number of chars go from min to max.
     */
    public static final String CONTENT_RANDOM_CHARS = "$RANDOM_CHARS";
    private Pattern PATTERN_RANDOM_CHARS =
            Pattern.compile(".*?(\\$RANDOM_CHARS\\[(\\d+), *(\\d+)\\]).*",
                            Pattern.DOTALL);

    /**
     * All occurences of "$RANDOM_WORDS[min, max, minlength, maxlength]" will be
     * replaced by a random number of words, where the number of words go from
     * min to max, where each word will have a length going from minlength to
     * maxlength.
     */
    public static final String CONTENT_RANDOM_WORDS = "$RANDOM_WORDS";
    private Pattern PATTERN_RANDOM_WORDS =
            Pattern.compile(
                ".*?(\\$RANDOM_WORDS\\[(\\d+), *(\\d+), *(\\d+), *(\\d+)\\]).*",
                   Pattern.DOTALL);

    /**
     * All occurences of "$WORD_LIST[min, max, listname]" will be replaced by
     * a number of words taken randomly from the list stored in the
     * configuration under the key "listname". The numbers of words go from
     * min to max. The word delimiter is space.
     */
    public static final String CONTENT_WORD_LIST = "$WORD_LIST";
    private Pattern PATTERN_WORD_LIST =
            Pattern.compile(".*?(\\$WORD_LIST\\[(\\d+), *(\\d+), *(\\w+)\\]).*",
                            Pattern.DOTALL);

    private Configuration conf;
    private String contentTemplate;
    private String idTemplate = DEFAULT_ID_TEMPLATE;
    private String baseTemplate = DEFAULT_BASE;
    private int maxRecords = DEFAULT_RECORDS;
    private int minDelay = DEFAULT_MINDELAY;

    private int generatedRecords = 0;
    private long lastGeneration = 0;
    private Profiler profiler;
    private Random random = new Random();

    public RecordGenerator(Configuration conf) throws ConfigurationException {
        this.conf = conf;
        try {
            if (conf.valueExists(CONF_CONTENT_TEMPLATE_LOCATION)) {
                contentTemplate = Resolver.getUTF8Content(
                        conf.getString(CONF_CONTENT_TEMPLATE_LOCATION));
            } else {
                contentTemplate = conf.getString(CONF_CONTENT_TEMPLATE);
            }
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Could not resolve template at '"
                    + conf.getString(CONF_CONTENT_TEMPLATE_LOCATION));
        }
        baseTemplate = conf.getString(CONF_BASE_TEMPLATE, baseTemplate);
        idTemplate = conf.getString(CONF_ID_TEMPLATE, idTemplate);
        maxRecords = conf.getInt(CONF_RECORDS, maxRecords);
        minDelay = conf.getInt(CONF_MINDELAY, minDelay);
        profiler = new Profiler();
        profiler.setExpectedTotal(maxRecords);
        profiler.setBpsSpan(Math.max(3, Math.min(1000, maxRecords / 100)));
    }

    /* ObjectFilter interface */

    public boolean hasNext() {
        return generatedRecords < maxRecords;
    }

    public boolean pump() throws IOException {
        if (hasNext()) {
            next();
        }
        return hasNext();
    }

    public void close(boolean success) {
        log.info(String.format(
                "Closing down with success %b, spend %s generating %d Records "
                + "at an average of %s Records/sec in total, %s Records/sec for"
                + " the last %d Records",
                success, profiler.getSpendTime(), generatedRecords,
                profiler.getBps(), profiler.getBps(true),
                profiler.getBpsSpan()));
        generatedRecords = maxRecords;
    }

    public void setSource(Filter filter) {
        //noinspection DuplicateStringLiteralInspection
        throw new UnsupportedOperationException("No source accepted");
    }

    public void remove() {
        throw new UnsupportedOperationException("Removal is not supported");
    }

    public Payload next() {
        long sinceLast = System.currentTimeMillis() - lastGeneration;
        if (sinceLast < minDelay) {
            log.trace("Sleeping " + (minDelay - sinceLast) + " ms");
            try {
                Thread.sleep(minDelay - sinceLast);
            } catch (InterruptedException e) {
                log.warn("Interrupted while sleeping "
                         + (minDelay - sinceLast) + " ms. Continuing");
            }
        }
        Payload payload = generatePayload();
        profiler.beat();
        generatedRecords++;
        lastGeneration = System.currentTimeMillis();
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("Generated " + payload + " (" + generatedRecords + "/"
                      + maxRecords + "), average speed is "
                      + profiler.getBps(true) + " Records/sec");
        } else if (log.isDebugEnabled() ||
                   (log.isInfoEnabled() &&
                    generatedRecords % profiler.getBpsSpan() == 0)) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("Generated Payload" + generatedRecords + "/"
                      + maxRecords + ", average speed is "
                      + profiler.getBps(true) + " Records/sec");
        }
        return payload;
    }

    private Payload generatePayload() {
        String content = expand(contentTemplate);
        String id = expand(idTemplate);
        String base = expand(baseTemplate);
        try {
            return new Payload(new Record(id, base, content.getBytes("utf-8")));
        } catch (UnsupportedEncodingException e) {
            //noinspection DuplicateStringLiteralInspection
            throw new RuntimeException("utf-8 encoding not supported", e);
        }
    }

    public String expand(String template) {
        if (!template.contains("$")) { // Trivial case
            return template;
        }
        template = expandIncremental(template);
        template = expandRandomNumber(template);
        template = expandRandomChars(template);
        template = expandRandomWords(template);
        template = expandWordList(template);
        return template;
    }

    private String expandIncremental(String template) {
        while (true) {
            Matcher incrementalNumber =
                    PATTERN_INCREMENTAL_NUMBER.matcher(template);
            if (!incrementalNumber.matches()) {
                 break;
            }
            String key = incrementalNumber.group(2);
            //log.trace("Got key " + key + " for incremental number");
            Integer counter = incrementalNumbers.get(key);
            if (counter == null) {
                counter = 0;
            }
            incrementalNumbers.put(key, counter+1);
            template = template.substring(0, incrementalNumber.start(1))
                    + counter + template.substring(incrementalNumber.end(1),
                                                   template.length());
        }
        return template;
    }

    private String expandRandomNumber(String template) {
        while (true) {
            Matcher matcher =
                    PATTERN_RANDOM_NUMBER.matcher(template);
            if (!matcher.matches()) {
                 break;
            }
            int min = Integer.parseInt(matcher.group(2));
            int max = Integer.parseInt(matcher.group(3));
            int rand = random.nextInt(max - min) + min;
            template = template.substring(0, matcher.start(1))
                       + rand + template.substring(matcher.end(1),
                                                   template.length());
        }
        return template;
    }

    private String expandRandomChars(String template) {
        while (true) {
            Matcher matcher =
                    PATTERN_RANDOM_CHARS.matcher(template);
            if (!matcher.matches()) {
                 break;
            }
            int min = Integer.parseInt(matcher.group(2));
            int max = Integer.parseInt(matcher.group(3));
            template = template.substring(0, matcher.start(1))
                       + randomChars(min, max)
                       + template.substring(matcher.end(1), template.length());
        }
        return template;
    }
    private String randomChars(int min, int max) {
        int length = random.nextInt(max - min) + min;
        StringWriter sw = new StringWriter(length);
        for (int i = 0 ; i < length ; i++) {
            sw.append((char)(random.nextInt(127 - 33) + 33));
        }
        return sw.toString();
    }

    private String expandRandomWords(String template) {
        while (true) {
            Matcher matcher =
                    PATTERN_RANDOM_WORDS.matcher(template);
            if (!matcher.matches()) {
                 break;
            }
            int min = Integer.parseInt(matcher.group(2));
            int max = Integer.parseInt(matcher.group(3));
            int minLength = Integer.parseInt(matcher.group(4));
            int maxLength = Integer.parseInt(matcher.group(5));

            int wordCount = random.nextInt(max - min) + min;
            StringWriter sw = new StringWriter(wordCount * maxLength);
            for (int i = 0 ; i < wordCount ; i++) {
                int length = random.nextInt(maxLength - minLength) + minLength;
                for (int j = 0 ; j < length ; j++) {
                    sw.append((char)(random.nextInt(127 - 33) + 33));
                }
                if (i < wordCount-1) {
                    sw.append(" ");
                }
            }

            template = template.substring(0, matcher.start(1))
                       + sw.toString()
                       + template.substring(matcher.end(1), template.length());
        }
        return template;
    }

    /**
     * All occurences of "$WORD_LIST[min, max, listname]" will be replaced by
     * a number of words taken randomly from the list stored in the
     * configuration under the key "listname". The numbers of words go from
     * min to max.
     */
    private String expandWordList(String template) {
        while (true) {
            Matcher matcher =
                    PATTERN_WORD_LIST.matcher(template);
            if (!matcher.matches()) {
                 break;
            }
            int min = Integer.parseInt(matcher.group(2));
            int max = Integer.parseInt(matcher.group(3));
            String listName = matcher.group(4);

            List<String> words = getWords(listName);

            int wordCount = random.nextInt(max - min) + min;
            StringWriter sw = new StringWriter(wordCount * 20);
            for (int i = 0 ; i < wordCount ; i++) {
                sw.append(words.get(random.nextInt(words.size())));
                if (i < wordCount-1) {
                    sw.append(" ");
                }
            }

            template = template.substring(0, matcher.start(1))
                       + sw.toString()
                       + template.substring(matcher.end(1), template.length());
        }
        return template;
    }

    private Map<String, List<String>> words =
            new HashMap<String, List<String>>(10);
    private List<String> getWords(String listName) {
        List<String> result = words.get(listName);
        if (result == null) {
            result = conf.getStrings(listName);
            if (result == null) {
                throw new IllegalArgumentException(
                        "The word-list '" + listName + "' was requested by the"
                        + " template, but was not defined in the properties");
            }
            words.put(listName, result);
        }
        return result;
    }
}
