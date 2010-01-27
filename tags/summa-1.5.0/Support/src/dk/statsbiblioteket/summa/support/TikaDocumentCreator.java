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
package dk.statsbiblioteket.summa.support;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.filter.object.TikaFilter;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.index.lucene.DocumentCreatorBase;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.exception.TikaException;
import org.apache.lucene.document.Document;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ext.DefaultHandler2;

import java.io.IOException;
import java.net.URL;

// TODO: Handle Tika Exceptions gracefully, optionally accepting the Payload
/**
 * Parses the embedded Stream from a Payload through Tika and creates a Lucene
 * Document from it.
 * </p><p>
 * Note 1: The DocumentCreator needs an index-description. The setup for
 * retrieving the description must be stored in the sub-property
 * {@link dk.statsbiblioteket.summa.common.index.IndexDescriptor#CONF_DESCRIPTOR} with parameters from
 * {@link dk.statsbiblioteket.summa.common.index.IndexDescriptor}.
 * </p><p>
 * Note 2: This document creator uses the Tika setup keys from
 * {@link TikaFilter}.
 * </p><p>
 * Note 2: The id is taken from the Payload.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TikaDocumentCreator extends DocumentCreatorBase {
    private static Log log = LogFactory.getLog(TikaDocumentCreator.class);

    /**
     * If true, processing errors are ignored. If false, errors during
     * processing results in the current payload being discarded.
     */
    public static final String CONF_IGNORE_ERRORS = "tika.flow.ignoreerrors";
    public static final boolean DEFAULT_IGNORE_ERRORS = true;

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static final String FIELD_TITLE = "tika.title";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static final String HTML_TITLE = "title";

    private LuceneIndexDescriptor descriptor;
    private String recordBase;
    private boolean ignoreErrors = DEFAULT_IGNORE_ERRORS;

    private Parser parser;
//    private DocumentShaperFilter extender;

    /**
     * Initialize the underlying parser and set up internal structures.
     * {@link #setSource} must be called before further use.
     * @param conf the configuration for the document creator..
     * @throws dk.statsbiblioteket.summa.common.configuration.Configurable.ConfigurationException if there was a problem with
     *         the configuration.
     */
    public TikaDocumentCreator(Configuration conf) throws
                                                        ConfigurationException {
        super(conf);
        log.debug("Creating Tika Document Creator");
        descriptor = LuceneIndexUtils.getDescriptor(conf);
        if (conf.valueExists(TikaFilter.CONF_TIKA_CONFIG)){
            URL tikaConfUrl = Resolver.getURL(conf.getString(
                    TikaFilter.CONF_TIKA_CONFIG));

            if (tikaConfUrl == null) {
                throw new ConfigurationException(String.format(
                        "Unable to find Tika configuration '%s'",
                        conf.getString(TikaFilter.CONF_TIKA_CONFIG)));
            } else {
                log.debug(String.format("Using Tika configuration '%s'",
                                        tikaConfUrl));
            }

            try {
                parser = new AutoDetectParser(new TikaConfig(tikaConfUrl));
                log.trace("Created Tika parser");
            } catch (Exception e) {
                throw new ConfigurationException(
                        "Unable to load Tika configuration", e);
            }
        } else {
            //noinspection DuplicateStringLiteralInspection
            log.debug("Using default Tika configuration");
            parser = new AutoDetectParser();
        }
        ignoreErrors = conf.getBoolean(CONF_IGNORE_ERRORS, ignoreErrors);
        recordBase = conf.getString(
                TikaFilter.CONF_BASE, TikaFilter.DEFAULT_BASE);
    }

    @Override
    public String getName() {
        return "Tika document creator";
    }

    /**
     * Convert the content of the Stream embedded in the payload to a Lucene
     * Document by using Tika.
     * @param payload the container for the Stream to convert.
     */
    @Override
    public boolean processPayload(Payload payload) throws PayloadException {
        // TODO: Catch exceptions and continue
        //noinspection DuplicateStringLiteralInspection
        log.debug("processPayload(" + payload + ") called");
        long startTime = System.nanoTime();
        if (payload.getStream() == null) {
            //noinspection DuplicateStringLiteralInspection
            throw new PayloadException("No Stream present", payload);
        }

        try {
            processInner(payload);
        } catch (PayloadException e) {
            if (ignoreErrors) {
                Logging.logProcess(
                        "TikadocumentCreator",
                        "Encountered exception while Tika-processing. Further "
                        + "Tika-processing is aborted but the Payload was not "
                        + "discarded as ignoreErrors is true",
                        Logging.LogLevel.DEBUG, payload, e);
            } else {
                throw new PayloadException(
                        "Exception Tika-processing", e, payload);
            }
        }


        //noinspection DuplicateStringLiteralInspection
        log.debug("Added Lucene Document to "
                  + payload + ". Content character count was " + characterCount
                  + ". Processing time was "
                  + (System.nanoTime() - startTime) / 1000000D + " ms");
        IndexUtils.assignSingleField(document, payload,
                                     IndexUtils.RECORD_BASE, recordBase);
        // recordID is assigned by the indexer
//        IndexUtils.assignSingleField(document, payload,
//                                     IndexUtils.RECORD_FIELD, payload.getId());
        return true;
    }

    private Payload innerPayload = null;
    private void processInner(Payload payload) throws PayloadException {
        innerPayload = payload;
        Metadata meta = new Metadata();

        // Give the filename to the parser to help it sniff the content type
        // based on the extension
        if (payload.getData("arc.url") != null) {
            meta.add(Metadata.RESOURCE_NAME_KEY,
                     payload.getData("arc.url").toString());
        }
        ContentHandler handler = createHandler();
        try {
            parser.parse(payload.getStream(), handler, meta);
        } catch (IOException e) {
            throw new PayloadException(
                    "IOException during Tika parse", e, payload);
        } catch (SAXException e) {
            throw new PayloadException(
                    "SAXException during Tika parse", e, payload);
        } catch (TikaException e) {
            throw new PayloadException(
                    "TikaException during Tika parse", e, payload);
        }
        try {
            payload.getStream().close();
        } catch (IOException e) {
            throw new PayloadException(
                    "IOException while closing stream", e, payload);
        }
        payload.getData().put(Payload.LUCENE_DOCUMENT, document);

        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding " + meta.size() + " meta data to document");
        // Add all extracted metadata to the stored record metadata
        for (String key : meta.names()) {
            if (log.isTraceEnabled()) {
                String value = meta.get(key);
                //noinspection DuplicateStringLiteralInspection
                log.trace("record.meta(" + key + ") = '" + value + "'");
            }
            //record.addMeta(key, meta.get(key));
        }
    }

    private Document document; // Used by TransformerHandler
    private long characterCount = 0;
    private long lastCharacterCount = 0;
    private ContentHandler createHandler() {
        document = new Document();

        return new DefaultHandler2() {
            boolean inTitle = false;

            @Override
            public void startElement(String uri, String localName, String qName,
                                     Attributes attributes)
                                                           throws SAXException {
                if (HTML_TITLE.equals(localName)) {
                    inTitle = true;
                }
                if (log.isTraceEnabled()) {
                    log.trace((lastCharacterCount > 0 ?
                              "(" + lastCharacterCount + " chars) "
                              : "") + "<" + localName+ ">");
                }
                lastCharacterCount = 0;
            }

            @Override
            public void endElement(String uri, String localName, String qName)
                                                           throws SAXException {
                inTitle = false; // No nested tags in title
                if (log.isTraceEnabled()) {
                    log.trace("(" + lastCharacterCount + " chars) </"
                              + localName+ ">");
                }
                lastCharacterCount = 0;
            }

            @Override
            public void characters(char ch[], int start, int length)
                                                           throws SAXException {
                // TODO: Check byte content (images et al)
                characterCount += length;
                lastCharacterCount += length;
                String content = new String(ch, start, length).trim();
                if ("".equals(content)) {
                    return;
                }
                if (inTitle) {
                    try {
                        // Overwrite old title if it exists
                        document.removeField(FIELD_TITLE);
                        log.debug("Extracted title '" + content);
                        addFieldToDocument(descriptor, document, FIELD_TITLE,
                                           content, 1.0F);
                        innerPayload.getData().put(FIELD_TITLE, content);
                    } catch (IndexServiceException e) {
                        throw new SAXException(String.format(
                                "Unable to add content to document for field "
                                + "'%s'", FIELD_TITLE));
                    }
                } else {
                    try {
                        //noinspection DuplicateStringLiteralInspection
                        addToFreetext(descriptor, document, "dummy", content);
                    } catch (IndexServiceException e) {
                        throw new SAXException(String.format(
                                "Unable to add freetext content to document "
                                + "for '%s'", FIELD_TITLE));
                    }
                }
            }

            /**
             * @return the number of characters received and consequently added
             *         to the document.
             */
            public long getCharacterCount() {
                return characterCount;
            }
        };
    }


    @Override
    public synchronized void close(boolean success) {
        super.close(success);
        log.info("Closing down tikaDocumentcreator. " + getProcessStats());
    }

}
