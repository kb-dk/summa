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
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.xml.stream.*;
import java.io.*;
import java.util.*;
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
    private final double structureReplaceChance;
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
    private ObjectFilter source = null;

    private final List<String> structures;
    // TODO: Consider using a weighted approach: http://stackoverflow.com/questions/6737283/weighted-randomness-in-java
    private final List<String> terms = new ArrayList<String>();

    public AltoGeneratorFilter(Configuration conf) {
        id = conf.getString(ObjectFilterImpl.CONF_FILTER_NAME, "AltoGenerator");
        maxRecords = conf.getInt(CONF_RECORDS, DEFAULT_RECORDS);
        maxStructures = conf.getInt(CONF_STRUCTURES, DEFAULT_STRUCTURES);
        structures = new ArrayList<String>(maxStructures) {
            @Override
            public boolean add(String s) {
                while (size() >= maxStructures) {
                    if (random.nextDouble() <= structureReplaceChance) {
                        remove(random.nextInt(size()));
                    } else {
                        return false;
                    }
                }
                return super.add(s);
            }
        };
        seed = conf.getLong(CONF_RANDOM_SEED, random.nextLong());
        random.setSeed(seed);
        randomTermChance = conf.getDouble(CONF_RANDOM_TERM_CHANCE, DEFAULT_RANDOM_TERM_CHANCE);
        structureReplaceChance = conf.getDouble(CONF_STRUCTURE_REPLACE_CHANCE, DEFAULT_STRUCTURE_REPLACE_CHANCE);
        createStream = conf.getBoolean(CONF_STREAM, DEFAULT_STREAM);
        base = conf.getString(CONF_BASE, DEFAULT_BASE);
        log.info("Created " + this);
    }

    /**
     * Initializes the generator from the source.
     */
    private void initialize() {
        log.trace("initialize() called");
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
        }
        source.close(true);
        this.terms.addAll(terms);
        if (this.terms.isEmpty()) {
            throw new IllegalStateException("Unable to extract any terms from source");
        }
    }

    // Step through XML and add all terms from String.CONTENT
    private void extractTerms(String sourceContent, Set<String> terms) throws XMLStreamException {
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(new StringReader(sourceContent));
        while (reader.hasNext()) {
            reader.next();
            if (reader.getEventType() == XMLStreamConstants.START_ELEMENT && "String".equals(reader.getLocalName())) {
                for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
                    if ("CONTENT".equals(reader.getAttributeLocalName(i))) {
                        Collections.addAll(terms, SPACE_SPLIT.split(reader.getAttributeValue(i)));
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
        if (structures.isEmpty()) {
            throw new IllegalStateException("No structures in " + this);
        }
        String id = "random_alto_" + generateID();
        String structure = structures.get(random.nextInt(structures.size()));
        StringWriter alto = new StringWriter();
        try {
            XMLStreamReader inXML = xmlInputFactory.createXMLStreamReader(new StringReader(structure));
            XMLStreamWriter outXML = xmlOutputFactory.createXMLStreamWriter(alto);
            generateXML(inXML, outXML);
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Unable to create XML Stream", e);
        }
        if (createStream) {
            return new Payload(new ReaderInputStream(new StringReader(alto.toString()), "utf-8"), id);
        }
        try {
            return new Payload(new Record(id, base, alto.toString().getBytes("utf-8")));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("utf-8 must be supported", e);
        }
    }

    private void generateXML(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
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
                                transformContent(reader.getAttributeValue(i)) :
                                    reader.getAttributeValue(i);
                        writer.writeAttribute(reader.getAttributeLocalName(i), value);
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    writer.writeEndElement();
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    writer.writeEndDocument();
                    break;
                case XMLStreamConstants.CHARACTERS:
                    writer.writeCharacters(String.valueOf(reader.getTextCharacters()));
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
    }

    /**
     * Generates new content with the same number of terms as the old. The new terms are taken at random from the
     * sample ALTO content, with random words generated as per {@link #randomTermChance}.
     * @param oldContent the old content from an ALTO structure.
     * @return new semi-random content.
     */
    private synchronized String transformContent(String oldContent) {
        int numTerms = SPACE_SPLIT.split(oldContent).length;
        sb.setLength(0);
        for (int i = 0 ; i < numTerms ; i++) {
            if (sb.length() > 0) {
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
            sb.append('a' + random.nextInt(24));
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
        log.debug("Closing down " + this + " with success=" + success);
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
        Payload altoPayload = generate();
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
                             "structured=%d/%d, delivered=%d/%d)",
                             id, base, seed, randomTermChance, createStream,
                             structures.size(), maxStructures, delivered, maxRecords);
    }
}
