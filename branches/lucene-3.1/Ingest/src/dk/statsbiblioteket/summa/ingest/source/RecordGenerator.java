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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Generates semi-random Records, usable for testing performance and
 * scalability.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment = "Needs Javadoc")
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
            "dummy_$TIMESTAMP[ms]_$INCREMENTAL_NUMBER[id]";

    /**
     * If true, the pseudo-random Records generated will occur with
     * deterministic content (e.g. two runs will yield the same Records).
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_DETERMINISTIC_RANDOM =
            "summa.ingest.generator.random.deterministic";
    public static final boolean DEFAULT_DETERMINISTIC_RANDOM = true;

    /**
     * All occurences of "$INCREMENTAL_NUMBER[key]" will be replaced by an
     * integer starting at 0 and incremented by 1 for each use. The integer is
     * referenced by key, making it possible to use distinct counters.
     * Key is a word made up of the letters a-z.
     */
    public static final String CONTENT_INCREMENTAL_NUMBER =
            "$INCREMENTAL_NUMBER";
    private static Pattern PATTERN_INCREMENTAL_NUMBER =
            Pattern.compile("(\\$INCREMENTAL_NUMBER\\[(\\w+)\\])",
                            Pattern.DOTALL);
    private Map<String, Integer> incrementalNumbers =
            new HashMap<String, Integer>(10);

    /**
     * All occurences of "$TIMESTAMP[unit]" will be replaced by the current time
     * as dictated by unit. Valid units are<br />
     * ms: System.currentTimeMillis.<br />
     * iso: YYYYMMDD-HHmmSS.ms (example: 20081108-103945.123).<br />
     * Note that the granularity of ms will often be too large for creating
     * uniqueue ids, so creating an {@link #CONF_ID_TEMPLATE} of
     * "myPrefix_$TIMESTAMP[ms]" is normally a bad idea. Use
     * "myPrefix_$TIMESTAMP[ms]$INCREMENTAL_COUNTER[idCounter]" instead.
     * </p><p>
     * Note: It takes considerable power to format the timestamp as iso.
     * If the goal is to produce thousands of test-records on 2008-level
     * hardware, it might wise to avoid this unit.
     */
    public static final String CONTENT_TIMESTAMP =
            "$TIMESTAMP";
    private static Pattern PATTERN_TIMESTAMP =
            Pattern.compile(".*?(\\$TIMESTAMP\\[(\\w+)\\]).*", Pattern.DOTALL);

    /**
     * All occurences of "$RANDOM_NUMBER[min, max]" will be replaced by a random
     * integer from min to max (both inclusive).
     */
    public static final String CONTENT_RANDOM_NUMBER = "$RANDOM_NUMBER";
    private static Pattern PATTERN_RANDOM_NUMBER =
            Pattern.compile(".*?(\\$RANDOM_NUMBER\\[(\\d+), *(\\d+)\\]).*",
                            Pattern.DOTALL);

    /**
     * All occurences of "$RANDOM_CHARS[min, max, onlyletters]" will be replaced
     * by a random number of random chars, where the number of chars go from min
     * to max. If onlyletters is true, only a-z will be generated.
     */
    public static final String CONTENT_RANDOM_CHARS = "$RANDOM_CHARS";
    private static Pattern PATTERN_RANDOM_CHARS =
            Pattern.compile(
                    ".*?(\\$RANDOM_CHARS\\[(\\d+), *(\\d+), *(\\w+)\\]).*",
                    Pattern.DOTALL);

    /**
     * All occurences of
     * "$RANDOM_WORDS[min, max, minlength, maxlength, onlyLetters]" will be
     * replaced by a random number of words, where the number of words go from
     * min to max, where each word will have a length going from minlength to
     * maxlength. If onlyLetters is true, only a-z will be generated.
     */
    public static final String CONTENT_RANDOM_WORDS = "$RANDOM_WORDS";
    private static Pattern PATTERN_RANDOM_WORDS =
            Pattern.compile(
       ".*?(\\$RANDOM_WORDS\\[(\\d+), *(\\d+), *(\\d+), *(\\d+), *(\\w+)\\]).*",
       Pattern.DOTALL);

    /**
     * All occurences of "$WORD_LIST[min, max, listname]" will be replaced by
     * a number of words taken randomly from the list stored in the
     * configuration under the key "listname". The numbers of words go from
     * min to max. The word delimiter is space.
     */
    public static final String CONTENT_WORD_LIST = "$WORD_LIST";
    private static Pattern PATTERN_WORD_LIST =
            Pattern.compile(".*?(\\$WORD_LIST\\[(\\d+), *(\\d+), *(\\w+)\\]).*",
                            Pattern.DOTALL);

    private Configuration conf;
    private int maxRecords = DEFAULT_RECORDS;
    private int minDelay = DEFAULT_MINDELAY;
    private List<RecordToken> idTokens;
    private List<RecordToken> baseTokens;
    private List<RecordToken> contentTokens;
    private boolean deterministic = DEFAULT_DETERMINISTIC_RANDOM;

    private int generatedRecords = 0;
    private long lastGeneration = 0;
    private Profiler profiler;
    private Random random;

    /**
     * Extracts templates for id, base and content and parses them into tokens.
     * The generator is ready for use after this.
     * @param conf the configuration for the generator.
     * @throws ConfigurationException if the configuration contained errors.
     */
    public RecordGenerator(Configuration conf) throws ConfigurationException {
        this.conf = conf;
        String contentTemplate;
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
        String idTemplate = conf.getString(CONF_ID_TEMPLATE,
                                           DEFAULT_ID_TEMPLATE);
        String baseTemplate = conf.getString(CONF_BASE_TEMPLATE, DEFAULT_BASE);
        maxRecords = conf.getInt(CONF_RECORDS, maxRecords);
        minDelay = conf.getInt(CONF_MINDELAY, minDelay);
        idTokens = parseTemplate(idTemplate);
        baseTokens = parseTemplate(baseTemplate);
        contentTokens = parseTemplate(contentTemplate);
        deterministic = conf.getBoolean(CONF_DETERMINISTIC_RANDOM,
                                        deterministic);

        profiler = new Profiler();
        profiler.setExpectedTotal(maxRecords);
        profiler.setBpsSpan(Math.max(3, Math.min(1000, maxRecords / 100)));
        random = deterministic ? new Random(87) : new Random();
    }

    private Pattern PATTERN_GENERIC =
            Pattern.compile("(\\$([A-Z]|_)+?\\[.*?\\])", Pattern.DOTALL);
    private List<RecordToken> parseTemplate(String template) {
        int lastEnd = 0;
        List<RecordToken> tokens = new ArrayList<RecordToken>(100);
        List<RecordToken> factories = new ArrayList<RecordToken>(10);
        factories.add(new IncrementalNumberToken());
        factories.add(new TimestampToken());
        factories.add(new RandomIntToken());
        factories.add(new RandomCharsToken());
        factories.add(new RandomWordsToken());
        factories.add(new WordListToken());
        factories.add(new LiteralToken());

        Matcher matcher = PATTERN_GENERIC.matcher(template);
        while (matcher.find()) {
            if (log.isTraceEnabled()) {
                log.trace(String.format(
                        "Parser matched generic pattern at pos(%d, %d) with "
                          + "content '%s'",
                        matcher.start(1), matcher.end(1), matcher.group(1)));
            }
            if (matcher.start(1) > lastEnd) {
                tokens.add(new LiteralToken(
                        template.substring(lastEnd, matcher.start(1))));
            }
            for (RecordToken tokenFactory: factories) {
                RecordToken token =
                        tokenFactory.createTokenIfMatch(matcher.group(1));
                if (token != null) {
                    tokens.add(token);
                    break;
                }
            }
            lastEnd = matcher.end(1);
        }
        if (lastEnd != template.length()) {
            tokens.add(new LiteralToken(
                    template.substring(lastEnd, template.length())));
        }
        log.debug("Constructed " + tokens.size() + " tokens");
        return tokens;
    }

    static interface RecordToken {
        public String getContent();
        public RecordToken createTokenIfMatch(String template);
    }

    class IncrementalNumberToken implements RecordToken {
        private String key;
        public IncrementalNumberToken() { }
        public IncrementalNumberToken(String key) {
            log.trace("Creating IncrementalNumberToken(" + key + ")");
            this.key = key;
            if (!incrementalNumbers.containsKey(key)) {
                incrementalNumbers.put(key, 0);
            }
        }
        public String getContent() {
            int counter = incrementalNumbers.get(key);
            incrementalNumbers.put(key, counter+1);
            return Integer.toString(counter);
        }
        public RecordToken createTokenIfMatch(String template) {
            Matcher incrementalNumber =
                    PATTERN_INCREMENTAL_NUMBER.matcher(template);
            if (!incrementalNumber.matches()) {
                 return null;
            }
            return new IncrementalNumberToken(incrementalNumber.group(2));
        }
    }

    class TimestampToken implements RecordToken {
        private String unit;

        public TimestampToken() { }
        public TimestampToken(String unit) {
            log.trace("Creating TimestampToken(" + unit + ")");
            this.unit = unit;
        }
        public String getContent() {
            if ("ms".equals(unit)) {
                return Long.toString(System.currentTimeMillis());
            } else if ("iso".equals(unit)) {
                return String.format("%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS.%1$tL",
                                      System.currentTimeMillis());
            } else {
                throw new IllegalArgumentException(
                        "The unit '" + unit + "' for " + CONTENT_TIMESTAMP
                        + " is not supported");
            }
        }
        public TimestampToken createTokenIfMatch(String template) {
            Matcher matcher = PATTERN_TIMESTAMP.matcher(template);
            if (!matcher.matches()) {
                 return null;
            }
            return new TimestampToken(matcher.group(2).toLowerCase());
        }
    }

    class RandomIntToken implements RecordToken {
        private int min;
        private int max;
        public RandomIntToken() { }
        public RandomIntToken(int min, int max) {
            log.trace("Creating RandomIntToken(" + min + ", " + max + ")");
            this.min = min;
            this.max = max;
        }
        public String getContent() {
            return Integer.toString(getRandomInt(min, max));
        }
        public RecordToken createTokenIfMatch(String template) {
            Matcher matcher =
                    PATTERN_RANDOM_NUMBER.matcher(template);
            if (!matcher.matches()) {
                 return null;
            }
            int min = Integer.parseInt(matcher.group(2));
            int max = Integer.parseInt(matcher.group(3));
            return new RandomIntToken(min, max);
        }
    }

    class RandomCharsToken implements RecordToken {
        private int min;
        private int max;
        private boolean onlyLetters;
        public RandomCharsToken() { }
        public RandomCharsToken(int min, int max, boolean onlyLetters) {
            log.trace("Creating RandomCharsToken(" + min + ", " + max + ", "
                      + onlyLetters + ")");
            this.min = min;
            this.max = max;
            this.onlyLetters = onlyLetters;
        }
        public String getContent() {
            return randomChars(min, max, onlyLetters);
        }
        public RecordToken createTokenIfMatch(String template) {
            Matcher matcher =
                    PATTERN_RANDOM_CHARS.matcher(template);
            if (!matcher.matches()) {
                 return null;
            }
            int min = Integer.parseInt(matcher.group(2));
            int max = Integer.parseInt(matcher.group(3));
            boolean onlyLetters = Boolean.parseBoolean(matcher.group(4));
            return new RandomCharsToken(min, max, onlyLetters);
        }
    }

    class RandomWordsToken implements RecordToken {
        private int min;
        private int max;
        private int minLength;
        private int maxLength;
        boolean onlyLetters;
        public RandomWordsToken() { }
        public RandomWordsToken(int min, int max, int minLength, int maxLength,
                                boolean onlyLetters) {
            log.trace("Creating RandomWordsToken(" + min + ", " + max + ", "
                      + minLength + ", " + maxLength + ", " + onlyLetters
                      + ")");
            this.min = min;
            this.max = max;
            this.minLength = minLength;
            this.maxLength = maxLength;
            this.onlyLetters = onlyLetters;
        }
        public String getContent() {
            int wordCount = getRandomInt(min, max);
            StringWriter sw = new StringWriter(wordCount * maxLength);
            for (int i = 0 ; i < wordCount ; i++) {
                int length = getRandomInt(minLength, maxLength);
                randomChars(sw, length, onlyLetters);
                if (i < wordCount-1) {
                    sw.append(" ");
                }
            }
            return sw.toString();
        }
        public RecordToken createTokenIfMatch(String template) {
            Matcher matcher =
                    PATTERN_RANDOM_WORDS.matcher(template);
            if (!matcher.matches()) {
                 return null;
            }
            int min = Integer.parseInt(matcher.group(2));
            int max = Integer.parseInt(matcher.group(3));
            int minLength = Integer.parseInt(matcher.group(4));
            int maxLength = Integer.parseInt(matcher.group(5));
            boolean onlyLetters = Boolean.parseBoolean(matcher.group(6));
            return new RandomWordsToken(min, max, minLength, maxLength,
                                        onlyLetters);
        }
    }

    class WordListToken implements RecordToken {
        private int min;
        private int max;
        private List<String> words;
        public WordListToken() { }
        public WordListToken(int min, int max, String listName) {
            log.trace("Creating WordListToken(" + min + ", " + max + ", "
                      + listName + ")");
            this.min = min;
            this.max = max;
            words = getWords(listName);
        }
        public String getContent() {
            int wordCount = getRandomInt(min, max);
            StringWriter sw = new StringWriter(wordCount * 20);
            for (int i = 0 ; i < wordCount ; i++) {
                sw.append(words.get(random.nextInt(words.size())));
                if (i < wordCount-1) {
                    sw.append(" ");
                }
            }
            return sw.toString();
        }
        public RecordToken createTokenIfMatch(String template) {
            Matcher matcher =
                    PATTERN_WORD_LIST.matcher(template);
            if (!matcher.matches()) {
                 return null;
            }
            int min = Integer.parseInt(matcher.group(2));
            int max = Integer.parseInt(matcher.group(3));
            String listName = matcher.group(4);
            return new WordListToken(min, max, listName);
        }
    }

    class LiteralToken implements RecordToken {
        private String literal;
        public LiteralToken() { }
        public LiteralToken(String literal) {
            log.trace("Creating LiteralToken(" + literal + ")");
            this.literal = literal;
        }
        public String getContent() {
            return literal;
        }
        public RecordToken createTokenIfMatch(String template) {
            if (template.length() == 0) {
                return null;
            }
            return new LiteralToken(template);
        }
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
        } else if ((log.isDebugEnabled() &&
                    // +1 to avoud division by zero
                    generatedRecords % (profiler.getBpsSpan() / 10 + 1) == 0)
                   ||
                   (log.isInfoEnabled() &&
                    generatedRecords % profiler.getBpsSpan() == 0)) {
            //noinspection DuplicateStringLiteralInspection
            String message = "Generated Payload" + generatedRecords + "/"
                      + maxRecords + ", average speed is "
                      + profiler.getBps(true) + " Records/sec. ETA: "
                      + profiler.getTimeLeftAsString(true);
            if (log.isDebugEnabled()) {
                log.debug(message);
            } else {
                log.info(message);
            }
        }
        return payload;
    }

    private Payload generatePayload() {
        String id = expand(idTokens);
        String base = expand(baseTokens);
        String content = expand(contentTokens);
        try {
            return new Payload(new Record(id, base, content.getBytes("utf-8")));
        } catch (UnsupportedEncodingException e) {
            //noinspection DuplicateStringLiteralInspection
            throw new RuntimeException("utf-8 encoding not supported", e);
        }
    }

    /**
     * Creates a a String based on a parsed template.
     * @param tokens a template parsed to
     * {@link dk.statsbiblioteket.summa.ingest.source.RecordGenerator.RecordToken}s.
     * @return pseudo-random content.
     */
    public String expand(List<RecordToken> tokens) {
        StringWriter sw = new StringWriter(5000);
        for (RecordToken token: tokens) {
            sw.append(token.getContent());
        }
        return sw.toString();
    }

    /**
     * Parses the template into tokens and calls {@link #expand(java.util.List)}
     * with the tokens. This should only be used for one-time calls, such as
     * testing.
     * @param template the template for the content.
     * @return pseudo-random content based on the given template.
     */
    public String expand(String template) {
        return expand(parseTemplate(template));
    }

    private int getRandomInt(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    private String randomChars(int min, int max, boolean onlyLetters) {
        int length = getRandomInt(min, max);
        StringWriter sw = new StringWriter(length);
        randomChars(sw, length, onlyLetters);
        return sw.toString();
    }
    private void randomChars(StringWriter sw, int length, boolean onlyLetters) {
        for (int i = 0 ; i < length ; i++) {
            if (onlyLetters) {
                sw.append((char)getRandomInt(97, 122));
            } else {
                sw.append((char)getRandomInt(33, 127));
            }
        }
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

