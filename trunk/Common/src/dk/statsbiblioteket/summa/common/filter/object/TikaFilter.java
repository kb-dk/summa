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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.Checksums;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.sax.XHTMLContentHandler;
import org.apache.tika.metadata.Metadata;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;

/**
 * A {@link Filter} passing an input stream through Apache Tika's automatic
 * content type sniffer extracting metadata and converting the document to
 * a simple XHTML format.
 * <p/>
 * The resulting XHTML is stored as content and meta-data as Record meta-data.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class TikaFilter extends ObjectFilterImpl {

    private static final Log log = LogFactory.getLog(TikaFilter.class);

    /**
     * Optional property defining the resource name of the configuration
     * used for the Tika content handling sub system. If this property is
     * undefined the Tika's own defaults will be used
     */
    public static final String CONF_TIKA_CONFIG = "summa.filter.tikaconfig";

    /**
     * Property defining the base assigned to generated records. The default
     * base is {@code tika}
     */
    public static final String CONF_BASE = "summa.filter.tikabase";

    /**
     * Default value for the {@link #CONF_BASE} property
     */
    public static final String DEFAULT_BASE = "tika";

    private Parser parser;
    private TransformerHandler handler;
    private String recordBase;

    public TikaFilter(Configuration conf) {
        super(conf);

        if (conf.valueExists(CONF_TIKA_CONFIG)){
            URL tikaConfUrl = Resolver.getURL(conf.getString(CONF_TIKA_CONFIG));

            if (tikaConfUrl == null) {
                throw new ConfigurationException(
                                     "Unable to find Tika configuration: "
                                     + conf.getString(CONF_TIKA_CONFIG));
            } else {
                log.debug("Using Tika configuration: " + tikaConfUrl);
            }

            try {
                parser = new AutoDetectParser(new TikaConfig(tikaConfUrl));
            } catch (Exception e) {
                throw new ConfigurationException("Unable to load Tika "
                                                 + "configuration: "
                                                 + e.getMessage(), e);
            }
        } else {
            log.debug("Using default Tika configuration");
            parser = new AutoDetectParser();
        }

        try {
            handler = getXmlContentHandler();
        } catch (TransformerConfigurationException e) {
            throw new ConfigurationException("Unable to create XML writer: "
                                             + e.getMessage(), e);
        }

        recordBase = conf.getString(CONF_BASE, DEFAULT_BASE);
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        InputStream raw;
        if (payload.getStream() == null) {
            Logging.logProcess("TikaFilter", "Using Record content as source",
                               Logging.LogLevel.DEBUG, payload);
            raw = new ByteArrayInputStream(payload.getRecord().getContent());
        } else {
            Logging.logProcess("TikaFilter", "Using embedded Stream as source",
                               Logging.LogLevel.DEBUG, payload);
            raw = payload.getStream();
        }

        // Tika uses the Metadata object both for input params and extracted
        // metadata output
        Metadata meta = new Metadata();

        // Give the filename to the parser to help it sniff the content type
        // based on the extension
        if (payload.getData(Payload.ORIGIN) != null) {
            meta.add(Metadata.RESOURCE_NAME_KEY,
                     payload.getData(Payload.ORIGIN).toString());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        handler.setResult(new StreamResult(out));

        try {
            parser.parse(raw, handler, meta);
        } catch (Exception e) {
            throw new PayloadException("Failed to parse stream for payload "
                                       + payload + ": " + e.getMessage(), e);
        }

        try {
            raw.close();
        } catch (IOException e) {
            log.warn("Exception while closing stream from " + payload, e);
        }

        byte[] recordContent = out.toByteArray();

        // If the payload does not have an id we construct one based on the
        // origin of the record, and if we don't have that we simply use a flat
        // md5 of the content
        String recordId;
        if (payload.getId() == null) {
            if (payload.getData(Payload.ORIGIN) != null) {
                recordId = payload.getStringData(Payload.ORIGIN);
                log.debug("Using payload origin as record id: " + recordId);
            } else {
                log.debug("No origin for payload, using md5 checksum instead");
                try {
                    byte[] idDigest = Checksums.md5(
                                       new ByteArrayInputStream(recordContent));
                    recordId = new String(idDigest);
                } catch (IOException e) {
                    throw new PayloadException("Failed to calculate record id: "
                                               + e.getMessage(), e);
                }
            }

        } else {
            log.debug("Found payload id " + payload.getId());
            recordId = payload.getId();
        }

        Record record = new Record(recordId, recordBase, recordContent);

        // Add all extracted metadata to the stored record metadata
        for (String key : meta.names()) {
            if (log.isTraceEnabled()) {
                String value = meta.get(key); 
                log.trace("record.meta(" + key + ") = '" + value + "'");
            }
            record.addMeta(key, meta.get(key));
        }

        payload.setRecord(record);

        return true;
    }

    // Lifted from org.apache.tika.cli.TikaCLI v0.3
    private TransformerHandler getXmlContentHandler()
            throws TransformerConfigurationException {
        SAXTransformerFactory factory = (SAXTransformerFactory)
            SAXTransformerFactory.newInstance();
        TransformerHandler handler = factory.newTransformerHandler();
        handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");
        handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
        return handler;
    }
}

