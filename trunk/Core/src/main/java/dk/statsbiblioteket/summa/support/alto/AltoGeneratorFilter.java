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
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.util.ReaderInputStream;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.xml.stream.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates Alto-files from a sample corpus. Usable for scale testing. The generator should be positioned after a
 * source that delivers ALTO-files and will extract terms and structures from the source.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoGeneratorFilter implements ObjectFilter {
    private static Log log = LogFactory.getLog(AltoGeneratorFilter.class);

    /**
     * The number of sample structures to keep in memory when analyzing source data.
     * </p><p>
     * Optional. Default is 10.
     */
    public static final String CONF_STRUCTURES = "altogenerator.samplestructures";
    public static final int DEFAULT_STRUCTURES = 10;

    /**
     * The chance that a new structure will replace an existing when the maximum amount of structures has been reached.
     * This should ideally be {@code maximum_number_of_structures / total_number_of_structures}.
     * </p><p>
     * Optional. Default is 0.1 (10%).
     */
    public static final String CONF_STRUCTURE_REPLACE_CHANCE = "altogenerator.structure.replace.chance";
    public static final double DEFAULT_STRUCTURE_REPLACE_CHANCE = 0.1d;

    /**
     * The amount of Alto-Records to generate.
     * </p><p>
     * Optional. Default is Integer.MAX_VALUE;
     */
    public static final String CONF_RECORDS = "altogenerator.records";
    public static final int DEFAULT_RECORDS = Integer.MAX_VALUE;

    /**
     * The base for generated Records (only relevant if {@link #CONF_STREAM} is false).
     * </p><p>
     * Optional. Default is "alto".
     */
    public static final String CONF_BASE = "altogenerator.record.base";
    public static final String DEFAULT_BASE = "alto";

    /**
     * If true, Payloads with Streams are generated. If false, Payloads with Records are generated.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_STREAM = "altogenerator.stream";
    public static final boolean DEFAULT_STREAM = true;

    /**
     * Optional seed for random. If specified, the output will be deterministic. If not specified, Random will be
     * seeded randomly.
     * </p><p>
     * Optional. Default is no seed (Random is initialized randomly).
     */
    public static final String CONF_RANDOM_SEED = "altogenerator.random.seed";

    /**
     * The chance that a term will be randomly generated instead of being drawn from {@link #terms}.
     * </p><p>
     * Optional. Default is 0.01 (1% chance).
     */
    public static final String CONF_RANDOM_TERM_CHANCE = "altogenerator.random.term.chance";
    public static final double DEFAULT_RANDOM_TERM_CHANCE = 0.01d;

    private final int maxStructures;
    private final int maxRecords;
    private final long seed;
    private final boolean createStream;
    private final double randomTermChance;
    private final static int randomTermMinLength = 2;
    private final static int randomTermMaxLength = 10;
    private final String base;
    private final Random random = new Random();
    private final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
    private final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
    {
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    }

    private final String id;
    private int delivered = 0;
    private long genTime = 0;
    private ObjectFilter source = null;

    private final RandomList<Template> templates;
    private final RandomList<String> structures;
    // TODO: Consider using a weighted approach: http://stackoverflow.com/questions/6737283/weighted-randomness-in-java
    private final List<String> terms = new ArrayList<String>();

    private final boolean hackedMode;

    public AltoGeneratorFilter(Configuration conf) {
        id = conf.getString(ObjectFilterImpl.CONF_FILTER_NAME, "AltoGenerator");
        maxRecords = conf.getInt(CONF_RECORDS, DEFAULT_RECORDS);
        maxStructures = conf.getInt(CONF_STRUCTURES, DEFAULT_STRUCTURES);
        double structureReplaceChance = conf.getDouble(CONF_STRUCTURE_REPLACE_CHANCE, DEFAULT_STRUCTURE_REPLACE_CHANCE);
        seed = conf.getLong(CONF_RANDOM_SEED, random.nextLong());
        random.setSeed(seed);
        randomTermChance = conf.getDouble(CONF_RANDOM_TERM_CHANCE, DEFAULT_RANDOM_TERM_CHANCE);
        createStream = conf.getBoolean(CONF_STREAM, DEFAULT_STREAM);
        base = conf.getString(CONF_BASE, DEFAULT_BASE);
        structures = new RandomList<String>(maxStructures, structureReplaceChance, random);
        templates = new RandomList<Template>(maxStructures, structureReplaceChance, random);
        hackedMode = true;
        log.info("Created " + this);
    }

    /**
     * Initializes the generator from the source.
     */
    private void initialize() {
        log.trace("initialize() called");
        Profiler profiler = new Profiler();
        Set<String> terms = new HashSet<String>(10000);
        while (source.hasNext()) {
            Payload sourcePayload = source.next();
            try {
                String sourceContent = Strings.flush(RecordUtil.getReader(sourcePayload));
                structures.add(sourceContent);
                extractTerms(sourceContent, terms);
            } catch (IOException e) {
                log.warn("IOException while reading content from " + sourcePayload + ". Skipping Payload", e);
            } catch (XMLStreamException e) {
                log.warn("XMLStreamException while reading content from " + sourcePayload + ". Skipping Payload", e);
            }
            sourcePayload.close();
            profiler.beat();
        }
        source.close(true);
        this.terms.addAll(terms);
        if (this.terms.isEmpty()) {
            throw new IllegalStateException("Unable to extract any terms from source");
        }
        for (String structure: structures) {
            templates.add(generateTemplate(structure, random));
        }
        log.info(String.format("initialize() finished analyzing %d samples and got %d unique terms in %s",
                               profiler.getBeats(), terms.size(), profiler.getSpendTime()));
    }

    // Step through XML and add all terms from String.CONTENT
    private void extractTerms(String sourceContent, Set<String> terms) throws XMLStreamException {
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(new StringReader(sourceContent));
        while (reader.hasNext()) {
            reader.next();
            if (reader.getEventType() == XMLStreamConstants.START_ELEMENT && "String".equals(reader.getLocalName())) {
                for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
                    if ("CONTENT".equals(reader.getAttributeLocalName(i))) {
                        if (!hackedMode) {
                            Collections.addAll(terms, SPACE_SPLIT.split(reader.getAttributeValue(i)));
                        } else { // Entity-escape as the values will be used directly
                            for (String term: SPACE_SPLIT.split(reader.getAttributeValue(i))) {
                                terms.add(XMLUtil.encode(term));
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
    private final static Pattern SPACE_SPLIT = Pattern.compile(" ");

    /**
     * Generates a pseudo-random Alto Payload, using the structures generated by {@link #initialize()}.
     * @return a Payload with Alto XML.
     */
    private Payload generate() {
        genTime -= System.nanoTime();
        if (structures.isEmpty()) {
            throw new IllegalStateException("No structures in " + this);
        }
        String id = "random_alto_" + generateID();
        String structure = structures.getRandom();
        StringWriter alto = new StringWriter();
        try {
            XMLStreamReader inXML = xmlInputFactory.createXMLStreamReader(new StringReader(structure));
            XMLStreamWriter outXML = xmlOutputFactory.createXMLStreamWriter(alto);
            generateXML(inXML, outXML, id, false);
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Unable to create XML Stream", e);
        }
        try {
            if (createStream) {
                return new Payload(new ReaderInputStream(new StringReader(alto.toString()), "utf-8"), id);
            }
            try {
                return new Payload(new Record(id, base, alto.toString().getBytes("utf-8")));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("utf-8 must be supported", e);
            }
        } finally {
            genTime += System.nanoTime();
        }
    }

    private Payload generateFromTemplate() {
        genTime -= System.nanoTime();
        if (templates.isEmpty()) {
            throw new IllegalStateException("No templates in " + this);
        }
        String id = "random_alto_" + generateID();
        Template template = templates.getRandom();
        try {
            if (createStream) {
                return new Payload(new ReaderInputStream(new StringReader(template.getRandomAlto(id)), "utf-8"), id);
            }
            try {
                return new Payload(new Record(id, base, template.getRandomAlto(id).getBytes("utf-8")));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("utf-8 must be supported", e);
            }
        } finally {
            genTime += System.nanoTime();
        }
    }

    /**
     * Transfer the ALTO XML from reader to writer, replacing String.CONTENT attributes and the fileName element.
     * @param reader ALTO source.
     * @param writer ALTO destination.
     * @param id will be appended to the existing fileName.
     * @param hackedTerms if true, terms are replaced with {@code ¤¤¤termCount¤¤¤}, for example {@code ¤¤¤5¤¤¤} and
     *                    filename is replaced with {@code ¤¤¤999999999¤¤¤}.
     *                    if false, terms are replaced with new semi-random terms and filename appended with id.
     * @return the original filename or null if not located.
     * @throws XMLStreamException
     */
    private String generateXML(XMLStreamReader reader, XMLStreamWriter writer, String id, boolean hackedTerms)
            throws XMLStreamException {
        String filename = null;
        while (reader.hasNext()) {
            reader.next();
            switch(reader.getEventType()) {
                case XMLStreamConstants.START_DOCUMENT:
                    writer.writeStartDocument(reader.getEncoding(), reader.getVersion());
//                    writer.setNamespaceContext(reader.getNamespaceContext());
                    break;
                case XMLStreamConstants.START_ELEMENT:
                    writer.writeStartElement(reader.getNamespaceURI(), reader.getLocalName());
                    for (int i = 0 ; i < reader.getNamespaceCount() ; i++) {
                        if (reader.getNamespacePrefix(i) == null || reader.getNamespaceURI(i).isEmpty()) {
                            writer.writeDefaultNamespace(reader.getNamespaceURI(i));
                        } else {
                            writer.writeNamespace(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
                        }
                    }

                    boolean isStringElement = "String".equals(reader.getLocalName());
                    for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
                        String value = isStringElement && "CONTENT".equals(reader.getAttributeLocalName(i)) ?
                                transformContent(reader.getAttributeValue(i), hackedTerms) :
                                    reader.getAttributeValue(i);
                        writer.writeAttribute(reader.getAttributeLocalName(i), value);
                    }

                    if ("fileName".equals(reader.getLocalName())) {
                        filename = reader.getElementText();
                        writer.writeCharacters(hackedTerms ? FILENAME_PLACEHOLDER : filename + "_" + id);
                        reader.next(); // End element
                        writer.writeEndElement();
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    writer.writeEndElement();
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    writer.writeEndDocument();
                    break;
                case XMLStreamConstants.CHARACTERS:
                    writer.writeCharacters(String.valueOf(reader.getText()));
                    break;
                case XMLStreamConstants.CDATA:
                    writer.writeCData(reader.getPIData());
                    break;
                case XMLStreamConstants.COMMENT:
                    writer.writeComment(reader.getText());
                    break;
                case XMLStreamConstants.SPACE:
                    writer.writeCharacters(reader.getText());
                    break;
                default: log.warn("Unexpected XMLEvent: " + XMLUtil.eventID2String(reader.getEventType()));
            }
        }
        writer.flush();
        return filename;
    }
    private static final int FILENAME_MAGIC_NUMBER = 999999999;
    private static final String FILENAME_PLACEHOLDER = "¤¤¤" + FILENAME_MAGIC_NUMBER + "¤¤¤";

    private Template generateTemplate(String sourceContent, Random random) {
        try {
            StringWriter templateString = new StringWriter();
            XMLStreamReader inXML = xmlInputFactory.createXMLStreamReader(new StringReader(sourceContent));
            XMLStreamWriter outXML = xmlOutputFactory.createXMLStreamWriter(templateString);
            String filename = generateXML(inXML, outXML, id, true);
            return generateTemplateFromProcessed(templateString.toString(), filename, random);
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Unable to create TemplateString", e);
        }
    }
    private Template generateTemplateFromProcessed(String templateString, String filename, Random random) {
        Template template = new Template(terms, random);
        template.setFilename(filename);
        Matcher termMatcher = TERM_PATTERN.matcher(templateString);
        int nextStart = 0;
        while (termMatcher.find()) {
            if (termMatcher.start() == 0) { // Special case where the first entry is a term
                template.add("");
            } else {
                template.add(templateString.substring(nextStart, termMatcher.start()));
            }
            template.add(Integer.parseInt(termMatcher.group(1)));
            nextStart = termMatcher.start() + termMatcher.group().length();
        }
        if (nextStart < templateString.length()-1) {
            template.add(templateString.substring(nextStart));
        }
        return template;
    }
    private final static Pattern TERM_PATTERN = Pattern.compile("¤¤¤([0-9]+)¤¤¤");
    private class Template {
        private final Random random;
        private final List<String> terms;

        private final List<String> texts = new ArrayList<String>();
        private final List<Integer> termCounts = new ArrayList<Integer>();
        private String filename = null;

        private boolean lastAddWasTerm = false;
        private final StringBuffer sb = new StringBuffer();

        // terms must be entity-escaped
        private Template(List<String> terms, Random random) {
            this.terms = terms;
            this.random = random;
        }

        public void add(String text) {
            if (lastAddWasTerm) {
                throw new IllegalStateException("A term count must be added at this point");
            }
            texts.add(text);
            lastAddWasTerm = true;
        }

        public void add(int termCount) {
            if (!lastAddWasTerm) {
                throw new IllegalStateException("A text must be added at this point");
            }
            termCounts.add(termCount);
            lastAddWasTerm = false;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public synchronized String getRandomAlto(String id) {
            sb.setLength(0);
            fillRandomAlto(sb, id);
            return sb.toString();
        }

        public void fillRandomAlto(StringBuffer sb, String id) {
            for (int i = 0 ; i < texts.size() ; i++) {
                sb.append(texts.get(i));
                if (i < termCounts.size()) {
                    if (termCounts.get(i) == FILENAME_MAGIC_NUMBER) {
                        sb.append(filename == null ? "" : filename).append("_").append(id);
                    } else {
                        fillRandomTerms(sb, termCounts.get(i));
                    }
                }
            }
        }

        private void fillRandomTerms(StringBuffer sb, Integer numTerms) {
            for (int i = 0 ; i < numTerms ; i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                if (randomTermChance != 0.0d && random.nextDouble() < randomTermChance) {
                    addRandomString(sb);
                } else {
                    sb.append(terms.get(random.nextInt(terms.size())));
                }
            }
        }
    }

    /**
     * Generates new content with the same number of terms as the old. The new terms are taken at random from the
     * sample ALTO content, with random words generated as per {@link #randomTermChance}.
     * @param oldContent  the old content from an ALTO structure.
     * @param hackedTerms if true, terms are replaced with {@code ¤¤¤termCount¤¤¤}, for example {@code ¤¤¤5¤¤¤}.
     *                    if false, terms are replaced with new semi-random terms.
     * @return new semi-random content.
     */
    private synchronized String transformContent(String oldContent, boolean hackedTerms) {
        int numTerms = SPACE_SPLIT.split(oldContent).length;
        if (hackedTerms) {
            return "¤¤¤" + numTerms + "¤¤¤";
        }
        sb.setLength(0);
        for (int i = 0 ; i < numTerms ; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            if (randomTermChance != 0.0d && random.nextDouble() < randomTermChance) {
                addRandomString(sb);
            } else {
                sb.append(terms.get(random.nextInt(terms.size())));
            }
        }
        return sb.toString();
    }

    private void addRandomString(StringBuffer sb) {
        int length = random.nextInt(randomTermMaxLength - randomTermMinLength) + randomTermMinLength;
        for (int i = 0 ; i < length ; i++) {
            sb.append((char)('a' + random.nextInt(24)));
        }
    }

    private final StringBuffer sb = new StringBuffer();

    /**
     * @return an ID generated by concatenating {@link #seed} and {@link #delivered}.
     */
    private String generateID() {
        return seed + "_" + delivered;
    }

    /* Trivial interface methods */

    @Override
    public boolean hasNext() {
        return delivered < maxRecords;
    }

    @Override
    public void setSource(Filter filter) {
        if (!(filter instanceof ObjectFilter)) {
            throw new IllegalArgumentException("Only ObjectFilter allowed as source");
        }
        log.debug("Setting source " + filter);
        source = (ObjectFilter)filter;
    }

    @Override
    public boolean pump() throws IOException {
        log.trace("Pump called");
        checkSource();
        if (!hasNext()) {
            log.trace("pump(): hasNext() returned false");
            return false;
        }
        Payload payload = next();
        if (payload != null) {
            //noinspection DuplicateStringLiteralInspection
            Logging.logProcess("AltoGeneratorFilter",
                               "Calling close for Payload as part of pump()",
                               Logging.LogLevel.TRACE, payload);
            payload.close();
        }
        return hasNext();
    }

    private void checkSource() {
        if (source == null) {
            throw new IllegalStateException("No source defined for " + getClass().getSimpleName() + " filter");
        }
    }
    @Override
    public void close(boolean success) {
        log.info(String.format("Closing down %s with success=%b. Generated %d ALTO records at %f ms/record",
                               this, success, delivered, delivered == 0 ? 0 : genTime * 1.0d / 1000000 / delivered));
        delivered = maxRecords;
    }

    @Override
    public Payload next() {
        log.trace("next() called");
        if (!hasNext()) {
            throw new IllegalStateException("No more Payloads available");
        }
        if (delivered == 0) {
            initialize();
        }
        Payload altoPayload = hackedMode ? generateFromTemplate() : generate();
        delivered++;
        return altoPayload;

    }

    @Override
    public void remove() {
        delivered++;
    }

    @Override
    public String toString() {
        return String.format("AltoGeneratorFilter(id='%s', base='%s', seed=%d, randomTermChance=%f, createStream=%b, " +
                             "structured=%d/%d, terms=%d, delivered=%d/%d)",
                             id, base, seed, randomTermChance, createStream,
                             structures.size(), maxStructures, terms.size(), delivered, maxRecords);
    }

    private static class RandomList<T> extends ArrayList<T> {
        private final int maxElements;
        private final double replaceChance;
        private final Random random;

        public RandomList(int maxElements, double replaceChance, Random random) {
            this.maxElements = maxElements;
            this.replaceChance = replaceChance;
            this.random = random;
        }

        @Override
        public boolean add(T element) {
            while (size() >= maxElements) {
                if (random.nextDouble() <= replaceChance) {
                    remove(random.nextInt(size()));
                } else {
                    return false;
                }
            }
            return super.add(element);
        }

        public T getRandom() {
            return get(random.nextInt(size()));
        }
    }
}
