/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.ingest.split.ThreadedStreamParser;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MARC21Slim XML-ifier for the danMARC2 linjeformat, specified at
 * http://www.danbib.dk/index.php?doc=linjeformat
 * </p><p>
 * The converter is flexible and allows for deviations from the standard.
 * Deviations are line length, divider sign, EOL and record divider.
 * An example of a variation is
 * {@code
001 00/0 »a9789221077442«
004 00/0 »ru»ae«
008 00/0 »tm»a1991»bch»da»dy»leng»v4»&2«
009 00/0 »aa»gxx«
021 00/0 »a9221077446»d45.00F«
021 00/0 »e9789221077442«
088 00/0 »a331.6»a(470)»a(47)«
096 00/0 »b331.6 In»cb«
245 00/0 »aIn hiding of narrowness»cthe new Danish labour salespoint»eedited by Standup Guy«
...
}
 * </p><p>
 *  FIXME: Currently @-based escaping only maps to Unicode, not
 *         "referencetegnsættet" as it should.
 * </p><p>
 * Note: Calling hasNext() while a conversion is in progress blocks until the
 *       conversion has been finished.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LinjeformatToMARC21Slim extends ThreadedStreamParser {
    private static Log log = LogFactory.getLog(LinjeformatToMARC21Slim.class);

    /**
     * The namespace for MARC21Slim.
     */
    public static final String MARC21SLIM = "http://www.loc.gov/MARC21/slim";
    private static final String FIELD =    "datafield";
    private static final String TAG =      "tag";
    private static final String SUBFIELD = "subfield";
    private static final String CODE =     "code";

    /**
     * The charset to use when reading the input.
     * The output is always UTF-8 XML.
     * </p><p>
     * Optional. Default is iso-8859-1.
     */
    public static final String CONF_INPUT_CHARSET =
            "summa.linjeformat.input.charset";
    public static final String DEFAULT_INPUT_CHARSET = "iso-8859-1";

    /**
     * The divider sign for sub fields. This can be multiple characters.
     * </p><p>
     * Optional. Default is * (asterisk).
     */
    public static final String CONF_DIVIDER =
            "summa.linjeformat.subfielddivider";
    public static final String DEFAULT_DIVIDER = "*";

    /**
     * An explicit EOL character is not part of the standard, but used by some
     * systems. If defined, all field-lines are expected to end with this sign
     * followed by (CR LF) | LF.
     * </p><p>
     * Optional. Default is blank (not defined).
     */
    public static final String CONF_EOL = "summa.linjeformat.eol";
    public static final String DEFAULT_EOL = "";

    /**
     * The string used for dividing records. Lines containing only this
     * character followed by (CR LF) | LF will be treated as dividers.
     * </p><p>
     * Optional. Default is $.
     */
    public static final String CONF_EOR = "summa.linjeformat.eor";
    public static final String DEFAULT_EOR = "$";

    private String inputCharset = DEFAULT_INPUT_CHARSET;
    private String divider = DEFAULT_DIVIDER;
    private String eol = DEFAULT_EOL;
    private String eor = DEFAULT_EOR;
    private XMLOutputFactory outputFactory;

    public LinjeformatToMARC21Slim(Configuration conf) {
        super(conf);
        inputCharset = conf.getString(CONF_INPUT_CHARSET, inputCharset);
        divider = conf.getString(CONF_DIVIDER, divider);
        if (divider.length() == 0) {
            throw new ConfigurationException(
                    "The value for property " + CONF_DIVIDER
                    + " must not be empty");
        }
        eol = conf.getString(CONF_EOL, eol);
        eor = conf.getString(CONF_EOR, eor);
        outputFactory = XMLOutputFactory.newInstance();
        log.info(String.format(
                "Ready: input charset='%s', divider='%s', eol='%s', eor='%s'",
                inputCharset, divider, eol, eor));
    }

    @Override
    protected void protectedRun() throws Exception {
        log.debug("Stream parsing content from " + sourcePayload);
        if (sourcePayload.getStream() == null) {
            throw new PayloadException("No stream", sourcePayload);
        }
        LineNumberReader in = new LineNumberReader(
                new InputStreamReader(sourcePayload.getStream(), inputCharset));
        in.setLineNumber(1); // Not geeky enough to count lines from 0

        MonitoredPipedInputStream payloadIn = new MonitoredPipedInputStream();
        PipedOutputStream payloadPipe = new PipedOutputStream();
        payloadPipe.connect(payloadIn);

        Payload payload = new Payload(payloadIn);

        // TODO: Consider transferring all metadata
        payload.getData().put(
                Payload.ORIGIN,
                sourcePayload.getData(Payload.ORIGIN) + "!toMARC21Slim");
        addToQueue(payload);

        XMLStreamWriter writer =
                outputFactory.createXMLStreamWriter(payloadPipe, "utf-8");
        writer.setDefaultNamespace(MARC21SLIM);
        writer.writeStartDocument("utf-8", "1.0");  // <?xml version="1.0" ...?>
        writer.writeCharacters("\n");
        writer.writeStartElement("collection"); // <collection xmlns="...">
        writer.writeDefaultNamespace(MARC21SLIM);
        writer.writeCharacters("\n");

        long records = 0;
        try {
            records = writeRecords(in, payloadIn, writer);
            if (payloadIn.isClosed()) {
                String message = String.format(
                        "Stopping after %d records due to close of delivery %s."
                        + " The output stream will not be valid XML",
                        records, payload);
                Logging.logProcess(
                        "LinjeformatToMARC21Slim", message,
                        Logging.LogLevel.WARN, sourcePayload);
                log.warn(message);
                return;
            }
            if (!running) {
                String message = String.format(
                        "Stopping after %d records due to running == false. "
                        + "The output stream will not be complete",
                        records);
                Logging.logProcess(
                        "LinjeformatToMARC21Slim", message,
                        Logging.LogLevel.WARN, sourcePayload);
                log.warn(message);
            }

            writer.writeEndElement();  // </collection>
            writer.writeCharacters("\n");
            writer.writeEndDocument(); // Logical end
        } finally {
            writer.flush();
            writer.close();
            payloadPipe.flush();
            payloadPipe.close();
        }

        log.debug(String.format(
                "Ending processing of %s with %d constructed MARC-records and"
                + " running = %b", sourcePayload, records, running));
    }

    // Iterate through all linjeformat-records and output XML
    private long writeRecords(
            LineNumberReader in, MonitoredPipedInputStream payloadIn,
            XMLStreamWriter writer) throws IOException, XMLStreamException {
        String lastLine = null;
        String line;
        boolean inRecord = false;
        long records = 0;
        while ((line = getLine(in)) != null && running
               && !payloadIn.isClosed()) {
            if (line.equals("")) { // Divider
                if (line.equals(lastLine)) {
                    Logging.logProcess(
                            "LinjeformatToMARC21Slim", String.format(
                                "Encountered multiple divider lines at line %d",
                                in.getLineNumber()), Logging.LogLevel.TRACE,
                            sourcePayload);
                    continue;
                } else {
                    if (inRecord) {
                        writer.writeEndElement(); // </record>
                        writer.writeCharacters("\n");
                        inRecord = false;
                    } else {
                        log.debug("No end element written as no record data has"
                                  + " been encountered even though EndOfRecord "
                                  + "was encountered at line "
                                  + in.getLineNumber());
                    }
                }
            } else {
                if (!inRecord) {
                    Logging.logProcess(
                            "LinjeformatToMARC21Slim",
                            "Starting XML for record at line "
                            + in.getLineNumber(), Logging.LogLevel.TRACE,
                            sourcePayload);
                    inRecord = true;
                    records++;
                    writer.writeCharacters("\n");
                    writer.writeStartElement("record"); // <record>
                    writer.writeAttribute("format", "danMARC2");
                    writer.writeCharacters("\n");
                }
                writeField(writer, line, in.getLineNumber());
            }
            lastLine = line;
        }
        return records;
    }

    /**
     * Convertes the given linjeformat line to XML, according to danMARC2 rules.
     * @param writer the XML stream to write to.
     * @param line a danMARC2 linjeformat line, containing field and subfields.
     * @param lineNumber where the line is in the input stream.
     * @throws javax.xml.stream.XMLStreamException if the writer encountered
     *        an exception.
     */
    private void writeField(XMLStreamWriter writer, String line,
                            int lineNumber) throws XMLStreamException {
        if (log.isTraceEnabled()) {
            log.trace("Converting line #" + lineNumber + " '" + line
                      + "' to XML");
        }
        // 004 00 *r . *a .
        // 008 00/0 »tm»a1991»bch»da»dy»leng»v4»&2«
        String[] tokens = line.split(" ", 3);
        if (tokens.length != 3) {
            Logging.logProcess(
                    "LinjeformatToMARC21Slim",
                    String.format(
                            "Expected 3 space-divided tokens, got %d. "
                            + "Skipping line #%d '%s'",
                            tokens.length, lineNumber, line),
                    Logging.LogLevel.DEBUG, sourcePayload);
            return;
        }
        if (tokens[0].length() < 3) {
            Logging.logProcess(
                    "LinjeformatToMARC21Slim",
                    String.format(
                            "Expected token #1 to have length 3, but it was %d."
                            + " Token was '%s' from line #%d '%s'",
                            tokens[0].length(), tokens[0], lineNumber, line),
                    Logging.LogLevel.TRACE, sourcePayload);
        }
        // TODO: Special-case leader
        writer.writeCharacters("  ");
        writer.writeStartElement(FIELD);
        writer.writeAttribute(TAG, tokens[0]);
        writer.writeAttribute("ind1", "0");
        writer.writeAttribute("ind2", "0");
        writer.writeCharacters("\n");
        writeSubFields(writer, line, lineNumber, tokens[2]); // Ignore tokens[1]
        writer.writeCharacters("  ");
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }

    private void writeSubFields(
            XMLStreamWriter writer, String line, int lineNumber,
            String content) throws XMLStreamException {
        if (!eol.equals("") && content.endsWith(eol)) {
            content = content.substring(0, content.length()-1);
        }
        if (!content.startsWith(divider)) {
            Logging.logProcess(
                    "LinjeformatToMARC21Slim",
                    String.format(
                            "Expected content for line #%d '%s' to start with"
                            + " divider '%s'",
                            lineNumber, line, divider),
                    Logging.LogLevel.DEBUG, sourcePayload);
            return;
        }
        List<String> tokens = splitAndReplace(line, lineNumber, content);
        // »tm»a1991»bch»da»dy»leng»v4»&2«
        for (String token: tokens) {
            if ("".equals(token)) {
                Logging.logProcess(
                        "LinjeformatToMARC21Slim",
                        String.format(
                                "Expected subfield content for line #%d '%s'",
                                lineNumber, line),
                        Logging.LogLevel.DEBUG, sourcePayload);
                continue;
            }
            writer.writeCharacters("    ");
            writer.writeStartElement(SUBFIELD);
            writer.writeAttribute(CODE, Character.toString(token.charAt(0)));
            if (token.length() > 1) {
                writer.writeCharacters(token.substring(1, token.length()));
            }
            writer.writeEndElement();
            writer.writeCharacters("\n");
        }
    }

    private StringBuffer rbuffer = new StringBuffer(200);
    // Split on divider and replace all @-occurences
    // »tm»a1991»bch»da»dy»leng»v4»&2 -> tm a19991 bch da dy leng v4 &2
    private List<String> splitAndReplace(
            String line, int lineNumber, String content) {
        List<String> result = new ArrayList<String>(10);
        int index = 0;
        rbuffer.setLength(0);
        while (index < content.length()) {
            if (content.charAt(index) == '@') { // Resolve escaping
                index = unescape(line, lineNumber, content, index);
                continue;
            }
            if ((divider.length() == 1 // Divider encountered
                 && content.charAt(index) == divider.charAt(0))
                || content.substring(
                    index, Math.min(index + divider.length(), content.length()))
                    .equals(divider)) {
                if (rbuffer.length() > 0) {
                    result.add(rbuffer.toString());
                    rbuffer.setLength(0);
                }
                index += divider.length();
                continue;
            }
            rbuffer.append(content.charAt(index++));
        }
        if (rbuffer.length() > 0) {
            result.add(rbuffer.toString());
        }
        return result;
    }

    private int unescape(String line, int lineNumber, String content, int index) {
        index++;
        if (index == content.length()) {
            Logging.logProcess(
                    "LinjeformatToMARC21Slim",
                    String.format(
                            "Encountered @ at EOL for line #%d '%s'",
                            lineNumber, line),
                    Logging.LogLevel.DEBUG, sourcePayload);
            return index;
        }
        if ((content.charAt(index) == '¤')    // Sort sign
            || (content.charAt(index) == '@') // Escape escape
            || (content.charAt(index) == '*') // Standard divider
            || (content.charAt(index) == 'å') // aa (we just copy)
            || (content.charAt(index) == 'Å') // Aa (we just copy)
            || (divider.length() == 1         // Divider
                && content.charAt(index) == divider.charAt(0))) {
            // TODO: Sort sign should be handled correctly
            // Unfortunately we have no established escape mechanism
            // for sort-signs in Summa
            rbuffer.append(content.charAt(index));
            index++;
        } else { // @xxxx Unicode assumed. See JavaDoc for the class
            if (index >= content.length() - 3) {
                Logging.logProcess(
                        "LinjeformatToMARC21Slim",
                        String.format(
                                "Expected 4-digit Unicode after @ but "
                                + "reached EOL for line #%d '%s'",
                                lineNumber, line),
                        Logging.LogLevel.DEBUG, sourcePayload);
                return index;
            }
            String unicode = "" + content.charAt(index++)
                             + content.substring(index, index + 3);
            index += 3;
            try {
                char u = (char)Integer.parseInt(unicode, 16);
                rbuffer.append(u);
            } catch (NumberFormatException e) {
                Logging.logProcess(
                        "LinjeformatToMARC21Slim",
                        String.format(
                                "Expected 4-digit Unicode after @ but "
                                + "got exception for line #%d '%s'",
                                lineNumber, line),
                        Logging.LogLevel.DEBUG, sourcePayload, e);
              return index;
            }
        }
        return index;
    }

    private StringBuffer buffer = new StringBuffer(100);
    /**
     * Reads the next line from danMARC2 linjeformat. According to the standard,
     * lines are a maximum of 79 characters. If the data for a given field takes
     * up more than this, the data are continued on the next line, preceded by
     * 4 spaces. This method concatenates such multi-lines to a single line.
     * </p><p>
     * If {@link #eol} is non-empty, lines containing the {@link #divider}
     * character are read until {@link #eol}.  
     * @param in the reader to get the String from.
     * @return the next logical line for danMARC2 linjeformat or null is EOF.
     *         Record-dividing lines are always returned as blanks, regardless
     *         if any {@link #eor} is specified.
     * @throws IOException if the next line could not be read.
     */
    private String getLine(LineNumberReader in) throws IOException {
        buffer.setLength(0);
        String line = in.readLine();
        if (line == null) { // EOF
            log.debug("EOF reached for " + sourcePayload);
            return null;
        }
        if (eor.equals(line)) { // Record divider
            return "";
        }
        if (!eol.equals("") && line.endsWith(eol)) { // Single line
            return line;
        }

        // Potential multi line
        buffer.append(line);
        while (true) {
            in.mark(100000);
            line = in.readLine();
            if (line == null) {
                return buffer.toString();
            }
            if (!eol.equals("") && line.endsWith(eol)) { // Explicit EOL
                buffer.append(line);
                return buffer.toString();
            } else if (!line.startsWith("    ")) { // Implicit EOL
                in.reset();
                return buffer.toString();
            }
            buffer.append(line.substring(4, line.length()));
        }
    }

    /**
     * A piped input stream where you can check if it has been closed
     */
    private static class MonitoredPipedInputStream extends PipedInputStream {
        private boolean closed = false;

        public synchronized boolean isClosed() {
            return closed;
        }

        @Override
        public synchronized void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
