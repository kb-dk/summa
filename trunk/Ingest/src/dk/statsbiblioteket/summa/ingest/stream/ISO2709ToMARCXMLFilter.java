/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2009  The State and University Library
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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc4j.MarcReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcPermissiveStreamReader;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.impl.DataFieldImpl;
import org.marc4j.marc.impl.SubfieldImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/**
 * Wrapper for marc4j that takes an InputStream with MARC in ISO 2709 and
 * converts it to a MARC21Slim Stream.
 * </p><p>
 * Note: In order to produce output usable by
 * {@link dk.statsbiblioteket.summa.ingest.split.SBMARCParser},
 * the input needs to be in the danish variant of ISO2709 and the property
 * {@link #CONF_FIX_CONTROLFIELDS} needs to be true.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ISO2709ToMARCXMLFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(ISO2709ToMARCXMLFilter.class);

    /**
     * The charset to use when reading the InputStream. Directly supported by
     * marc4j {@code utf-8}}}, {{{iso-8859-1}}} and {{{marc-8}}} but all
     * Java-known charsets are legal ({@code cp850} is often found in files from
     * Windoes).
     * </p><p>
     * Note: If {@link #CONF_USE_PERMISSIVE} is true, the number of legal
     * charsets are restricted. See CONF_USE_PERMISSIVE for details. 
     * </p><p>
     * Optional. If not defined, the charset is inferred by marc4j.
     */
    public static final String CONF_INPUT_CHARSET =
            "summa.iso2709.input.charset";

    /**
     * If true, the {@link MarcPermissiveStreamReader} is used instead of
     * {@link FlexibleMarcStreamReader}. The permissive stream reader is capable
     * of some error-handling for bad input, but limits the available charsets
     * to {@code UTF-8}, {@code MARC-8} and {@code ISO-8859-1} all of which
     * are stated case-sensitive.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_USE_PERMISSIVE =
            "summa.iso2709.input.permissive";
    public static final boolean DEFAULT_USE_PERMISSIVE = false;


    /**
     * If true, controlfields are converted to datafields. This should be done
     * for data that are the danish version of ISO2709 and has been passed
     * through
     * {@link dk.statsbiblioteket.summa.ingest.stream.ISO2709ToMARCXMLFilter}.
     * </p><p>
     * Content in need of fixing looks like this: {@code
  <record>
    <leader>00614nam0 32001931  45  </leader>
    <controlfield tag="001">000&#31;a3893228845&#31;fa&#31;od</controlfield>
    <controlfield tag="004">000&#31;ae&#31;rn</controlfield>
    <controlfield tag="008">000&#31;a1997&#31;bde&#31;lger&#31;tm&#31;v8&#31;&amp;01</controlfield>
    <controlfield tag="009">000&#31;aa&#31;gxx</controlfield>
    <datafield tag="010" ind1="0" ind2="0">
      <subfield code="a">D33875127X</subfield>
    </datafield>
    ...
    }
     * and will be converted to this: {@code
    <record>
      <leader>00614nam0 32001931  45  </leader>
      <datafield tag="001" int1="0" ind2="0">
         <subfield code="a">3893228845</subfield>
         <subfield code="f">a</subfield>
         <subfield code="o">d</subfield>
       </datafield>
      <datafield tag="004" int1="0" ind2="0">
       ...
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_FIX_CONTROLFIELDS =
            "iso2709tomarcxml.controlfields.fix";
    public static final boolean DEFAULT_FIX_CONTROLFIELDS = false;

    /**
     * The delimiter used in garbled controlfields. This is expressed as a
     * regular expression and is used by {@link String#split}.
     * </p><p>
     * Optional. Default is the unicode character u001F.
     */
    public static final String CONF_CONTROLFIELDS_DELIMITER =
            "iso2709tomarcxml.controlfields.delimiter";
    public static final String DEFAULT_CONTROLFIELDS_DELIMITER =
            "\u001F";

    private boolean fixControlfields = DEFAULT_FIX_CONTROLFIELDS;
    private String controlfieldsDelimiter = DEFAULT_CONTROLFIELDS_DELIMITER;
    private boolean usePermissive = DEFAULT_USE_PERMISSIVE;


    private String inputcharset = null; // null = let marc4j handle this

    public ISO2709ToMARCXMLFilter(Configuration conf) {
        super(conf);
        inputcharset = conf.getString(CONF_INPUT_CHARSET, null);
        /* We go out of our way to parse the given charset as marc4j
           does not conform to the rules in Java Charset.
         */
        if ("".equals(inputcharset)) {
            inputcharset = null;
        }
        fixControlfields = conf.getBoolean(
                CONF_FIX_CONTROLFIELDS, fixControlfields);
        controlfieldsDelimiter = conf.getString(
                CONF_CONTROLFIELDS_DELIMITER, controlfieldsDelimiter);
        usePermissive = conf.getBoolean(CONF_USE_PERMISSIVE, usePermissive);
        log.debug(String.format(
                "Constructed ISO 2709 filter with charset '%s', "
                + "fixControlFields=%b, controlfieldDelimiter='%s' and "
                + "usePermissive=%b",
                inputcharset == null ?
                "inferred from the InputStream" : inputcharset,
                fixControlfields, controlfieldsDelimiter, usePermissive));
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getStream() == null) {
            throw new PayloadException("No Stream", payload);
        }
        Logging.logProcess(
                "ISO2709ToMARCXMLFilter", "Parsing Payload ISO 2709 Stream",
                Logging.LogLevel.TRACE, payload);
        payload.setStream(new ISO2MARCInputStream(
                payload.getStream(), fixControlfields, controlfieldsDelimiter));
        log.trace("Wrapped in ISO2MARCInputStream and assigned to " + payload);
        return true;
    }

    // Not thread-safe!
    /**
     * Takes an ISO2709-stream as input and outputs a MARC21Slip XML
     * representation. Note that this expects "strict" ISO2709 and outputs
     * true MARC21Slim. The danish version of ISO2709 and MARC21Slim are
     * slightly different.
     */
    class ISO2MARCInputStream extends InputStream {
        private MarcReader source;
        private InputStream sourceStream;

        private ByteArrayOutputStream outStream =
                new ByteArrayOutputStream(4000);

        private byte[] buffer = new byte[0];
        private int pos = 0;
        private int length = 0;

        private boolean closed = false;
        private boolean convertControlfieldsToDatafields;
        private String controlfieldDelimiter;

        /**
         *
         * @param stream the Stream with the ISO 2709 bytes.
         * @param convertControlfieldsToDatafields if true, all controlfields
         *        are converted to datafields before constructing XML. This
         *        produces the danish variant of MARX21SlimISO2709.
         *        Note: This requires that the input is in the danish variant
         *        of ISO2709.
         * @param controlfieldDelimiter the delimiter used to split the
         *        controlfield into subfields.
         */
        ISO2MARCInputStream(InputStream stream,
                            boolean convertControlfieldsToDatafields,
                            String controlfieldDelimiter) {
            if (usePermissive) {
                source = inputcharset == null
                         ? new MarcPermissiveStreamReader(stream, true, true)
                         : new MarcPermissiveStreamReader(
                                              stream, true, true, inputcharset);
            } else {
                source = inputcharset == null
                         ? new FlexibleMarcStreamReader(stream)
                         : new FlexibleMarcStreamReader(stream, inputcharset);
            }
            log.trace("Constructed reader");
            sourceStream = stream;
            this.convertControlfieldsToDatafields =
                    convertControlfieldsToDatafields;
            this.controlfieldDelimiter = controlfieldDelimiter;
        }

        @Override
        public int read() throws IOException {
            while (true) {
                if (length - pos > 0) { // Buffer has content
                    return buffer[pos++];
                }
                if (closed || !source.hasNext()) { // No more content
                    return -1; // EOF
                }
                try {
                    fillBuffer();
                } catch (Exception e) {
                    String message = "Exception while transforming ISO 2709 "
                                     + "into MARC21Slim";
                    log.warn(message, e);
                    sourceStream.close();
                    throw new IOException(message, e);
                }
            }
        }

        private MarcWriter out = new MarcXmlWriterFixed(
                outStream, "UTF-8", true);
        // Assumes that the buffer has been depleted
        private void fillBuffer() throws IOException {
            pos = 0;
            while (outStream.size() == 0) {
                if (!source.hasNext()) {
                    sourceStream.close();
                    out.close();
                    closed = true;
                    break;
                }
                Record marcRecord = source.next();
                if (convertControlfieldsToDatafields) {
                    convertControlfields(marcRecord);
                }
                if (marcRecord == null) {
                    log.debug("fillBuffer(): Got null MARC Record from Stream");
                    continue;
                }
                out.write(marcRecord);
/*                if (outStream.size() == 0) {
                    log.trace("fillBuffer(): No content in outStream after "
                              + "producing XML for MARC Record (probably due to"
                              + " caching). Processing next Record");
                }*/
            }
            if (outStream.size() == 0) {
                log.trace("Depleted InputStream with no extra content");
                sourceStream.close();
                out.close();
                length = 0 ;
                closed = true;
                return;
            }

//            log.trace("fillBuffer produced " + outStream.size() + " bytes");
            buffer = outStream.toByteArray();
//            log.debug("Produced\n" + outStream.toString("utf-8"));
            length = buffer.length; //outStream.size();
            pos = 0;
            outStream.reset();
/*            try {
                log.trace("fillBuffer(): Dumping the first 100 bytes:\n"
                          + new String(buffer, 0, Math.min(100, length),
                                       "utf8"));
            } catch (Exception e) {
                log.debug("Exception performing trace", e);
            }*/
        }

        private void convertControlfields(Record record) {
            for (Object cfObject: record.getControlFields()) {
                if (!(cfObject instanceof ControlField)) {
                    throw new IllegalStateException(String.format(
                            "Expected ControlField, got %s",
                            cfObject.getClass()));
                }
                ControlField cf = (ControlField)cfObject;
                DataField df = new DataFieldImpl(cf.getTag(), '0', '0');

                String content = cf.getData().indexOf("000") == 0 ?
                                 cf.getData().substring(3) : cf.getData();

                // 000\u0031a3893228845\u0031fa\u0031od
                 String[] tokens = content.split(controlfieldDelimiter);
                for (String token: tokens) {
                    if (token.length() == 0) {
                        //noinspection UnnecessaryContinue
                        continue; // leading or trailing delimiter
                    } else if (token.length() == 1) {
                        log.debug(String.format(
                                "Ignoring subfield definition of "
                                + "insufficient length %d in garbled "
                                + "controlfield."
                                + " tag='%s', content='%s', token='%s'",
                                token.length(), cf.getTag(), content, token));
                    } else {
                        // a3893228845
                        String code = token.substring(0, 1);
                        String subCon = token.substring(1);
                        df.addSubfield(
                                new SubfieldImpl(code.charAt(0), subCon));
                    }
                }
                //noinspection unchecked
                record.getDataFields().add(df);
            }
            record.getControlFields().clear();
            //noinspection unchecked
            Collections.sort(record.getDataFields());
        }

        @Override
        public void close() throws IOException {
            log.debug("Closing Stream explicitly (this might result in loss of"
                      + " data)");
            sourceStream.close();
            closed = true;
        }

        // TODO: Implement buffer-read for better performance
    }
}
