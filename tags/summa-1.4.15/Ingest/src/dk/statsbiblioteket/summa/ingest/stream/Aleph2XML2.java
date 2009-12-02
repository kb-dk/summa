/* $Id: Aleph2XML2.java,v 1.11 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.11 $
 * $Date: 2007/10/05 10:20:24 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.util.ParseUtil;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.ArrayList;

/**
 * This filter converts dumps from XLibris Aleph library system to MARC-XML.
 * It is normally followed by the
 * {@link dk.statsbiblioteket.summa.ingest.split.SBMARCParser} called from
 * the {@link dk.statsbiblioteket.summa.ingest.split.StreamController} filter.
 * </p><p>
 * The Aleph2XML2-filter is stream-to-stream.
 * </p><p>
 * Example record<br>
 * </p><p>
 * Prior:<br>
 * <pre>
 * {@code
 * 000000001 FMT   L ML
 * 000000001 LDR   L -----nam----------a-----
 * 000000001 00100 L $$ax100006565$$fa
 * 000000001 00400 L $$rc$$ae
 * 000000001 00800 L $$a1979$$bdk$$e1$$f0$$g0$$ldan$$tm
 * 000000001 00900 L $$aa
 * 000000001 09600 L $$aXI,5b S$$z840860
 * 000000001 24500 L $$aKemiske stoffer$$canvendelse og kontrol$$eudarb. af Byggesektorgruppen på den teknologisk-samfundsvidenskabelige planlæggeruddannelse på RUC
 * 000000001 26000 L $$a<Roskilde>$$b<s.n.>$$c1979
 * 000000001 30000 L $$a303 sp.$$bill.
 * 000000001 50600 L $$aSpecialeafh.
 * 000000001 71000 L $$aRoskilde Universitetscenter$$c<<Den >>teknologisk-samfundsvidenskabelige planlæggeruddannelse$$cByggesektorgruppen
 * 000000001 BAS   L 30
 * 000000001 CAT   L $$aBATCH$$b00$$c19971106$$lKEM01$$h1228
 * 000000001 CAT   L $$aBATCH$$b00$$c20040617$$lKEM01$$h0923
 * 000000001 CAT   L $$c20060322$$lKEM01$$h0946
 * 000000001 CAT   L $$c20060329$$lKEM01$$h1019
 * 000000001 C0100 L $$asbu
 * 000000001 U0700 L $$kThe Chemistry Library$$rXI,5b S
 * 000000001 V0700 L $$aXI,5b S
 * }
 * </pre>
 * </p><p>
 * After:<br>
 * {@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <collection xmlns="http://www.loc.gov/MARC21/slim">
 *   <record>
 *       <datafield tag="FMT" ind1="" ind2="">ML</datafield>
 *       <datafield tag="LDR" ind1="" ind2="">-----nam----------a-----</datafield>
 *       <datafield tag="001" ind1="0" ind2="0">
 *           <subfield code="a">x100006565</subfield>
 *           <subfield code="f">a</subfield>
 *       </datafield>
 *       <datafield tag="004" ind1="0" ind2="0">
 *           <subfield code="r">c</subfield>
 *           <subfield code="a">e</subfield>
 *       </datafield>
 *       <datafield tag="008" ind1="0" ind2="0">
 *           <subfield code="a">1979</subfield>
 *           <subfield code="b">dk</subfield>
 *           <subfield code="e">1</subfield>
 *           <subfield code="f">0</subfield>
 *           <subfield code="g">0</subfield>
 *           <subfield code="l">dan</subfield>
 *           <subfield code="t">m</subfield>
 *       </datafield>
 *       <datafield tag="009" ind1="0" ind2="0">
 *           <subfield code="a">a</subfield>
 *       </datafield>
 *       <datafield tag="096" ind1="0" ind2="0">
 *           <subfield code="a">XI,5b S</subfield>
 *           <subfield code="z">840860</subfield>
 *       </datafield>
 *       <datafield tag="245" ind1="0" ind2="0">
 *           <subfield code="a">Kemiske stoffer</subfield>
 *           <subfield code="c">anvendelse og kontrol</subfield>
 *           <subfield code="e">udarb. af Byggesektorgruppen på den
 *               teknologisk-samfundsvidenskabelige planlæggeruddannelse på RUC
 *           </subfield>
 *       </datafield>
 *       <datafield tag="260" ind1="0" ind2="0">
 *           <subfield code="a">&lt;Roskilde&gt;</subfield>
 *           <subfield code="b">&lt;s.n.&gt;</subfield>
 *           <subfield code="c">1979</subfield>
 *       </datafield>
 *       <datafield tag="300" ind1="0" ind2="0">
 *           <subfield code="a">303 sp.</subfield>
 *           <subfield code="b">ill.</subfield>
 *       </datafield>
 *       <datafield tag="506" ind1="0" ind2="0">
 *           <subfield code="a">Specialeafh.</subfield>
 *       </datafield>
 *       <datafield tag="710" ind1="0" ind2="0">
 *           <subfield code="a">Roskilde Universitetscenter</subfield>
 *           <subfield code="c">&lt;&lt;Den &gt;&gt;teknologisk-samfundsvidenskabelige
 *               planlæggeruddannelse
 *           </subfield>
 *           <subfield code="c">Byggesektorgruppen</subfield>
 *       </datafield>
 *       <datafield tag="BAS" ind1="" ind2="">30</datafield>
 *       <datafield tag="CAT" ind1="" ind2="">
 *           <subfield code="a">BATCH</subfield>
 *           <subfield code="b">00</subfield>
 *           <subfield code="c">19971106</subfield>
 *           <subfield code="l">KEM01</subfield>
 *           <subfield code="h">1228</subfield>
 *       </datafield>
 *       <datafield tag="CAT" ind1="" ind2="">
 *           <subfield code="a">BATCH</subfield>
 *           <subfield code="b">00</subfield>
 *           <subfield code="c">20040617</subfield>
 *           <subfield code="l">KEM01</subfield>
 *           <subfield code="h">0923</subfield>
 *       </datafield>
 *       <datafield tag="CAT" ind1="" ind2="">
 *           <subfield code="c">20060322</subfield>
 *           <subfield code="l">KEM01</subfield>
 *           <subfield code="h">0946</subfield>
 *       </datafield>
 *       <datafield tag="CAT" ind1="" ind2="">
 *           <subfield code="c">20060329</subfield>
 *           <subfield code="l">KEM01</subfield>
 *           <subfield code="h">1019</subfield>
 *       </datafield>
 *       <datafield tag="C01" ind1="0" ind2="0">
 *           <subfield code="a">sbu</subfield>
 *       </datafield>
 *       <datafield tag="U07" ind1="0" ind2="0">
 *           <subfield code="k">The Chemistry Library</subfield>
 *           <subfield code="r">XI,5b S</subfield>
 *       </datafield>
 *       <datafield tag="V07" ind1="0" ind2="0">
 *           <subfield code="a">XI,5b S</subfield>
 *       </datafield>
 *       <datafield tag="994" ind1="0" ind2="0">
 *           <subfield code="z">KEM01-000000001</subfield>
 *       </datafield>
 *   </record>
 * </collection>
 * }
 * Note the existence of the 994*z field. This is specific for SB and contains
 * subfield CAT*l + "-" + the alephID and is used as the Record-id in Summa.
 */
// TODO: Check whether FMT must be ML and whether LDR sould be <leader>
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.QA_NEEDED,
       author = "te, hal")
public class Aleph2XML2 extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(Aleph2XML2.class);

    /**
     * The delimiter used after ID-prefix.
     * </p><p>
     * Optional. Default is "-".
     */
    public static final String CONF_ID_DELIMITER =
            "summa.aleph2xml.iddelimiter";
    public static final String DEFAULT_ID_DELIMITER = "-";
    private String idDelimiter = DEFAULT_ID_DELIMITER;

    public Aleph2XML2(Configuration conf){
        super(conf);
        idDelimiter = conf.getString(CONF_ID_DELIMITER, idDelimiter);
    }

    /**
     * Trims, entity-encodes and format-checks a string, prior to insertion
     * into a MARC element.
     * @param content the content to prepare.
     * @return the tranformed content, ready for insertion into MARC-XML.
     */
    private String prepareString(String content) {
        content = content.trim();
        content = ParseUtil.encode(content);
        // some records has this illegal char in the record - remove it
        return content.replaceAll("\\p{Cntrl}","");
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getStream() == null) {
            throw new PayloadException("No stream in " + payload);
        }
        log.debug("Wrapping the Stream in " + payload
                  + " in a Aleph2XMLInputStream");
        payload.setStream(new Aleph2XMLInputStream(payload.getStream(),
                                                     payload.toString()));
        return true;
    }

    /**
     * On-the-fly creation of MARC-XML from Aleph-input.
     */
    class Aleph2XMLInputStream extends InputStream {
/*        private static final String HEADER =
                ParseUtil.XML_HEADER
                + "\n<collection xmlns=\"http://www.loc.gov/MARC21/slim\">"
                + "\n<record>";*/
        private static final String HEADER =
                ParseUtil.XML_HEADER
                + "\n<collection xmlns=\"http://www.loc.gov/MARC21/slim\">\n";
        private static final String FOOTER = "</collection>\n";

        private static final String RECORD_START ="<record>\n";
        private static final String RECORD_END = "</record>\n";

        private static final String DATA_START = "<datafield tag=\"";
        private static final String DATA_END = "</datafield>\n";

        private static final String SUBFIELD_START = "<subfield code=\"";
        private static final String SUBFIELD_END = "</subfield>\n";


        private BufferedReader source;
        /**
         * The main buffer for the reader. Anything is this is to be passed on.
         */
        private ArrayList<Byte> buffer = new ArrayList<Byte>(10000);
        private int bufferPos = -1;
        /**
         * An ID or description of this stream to be used when outputting
         * debug information.
         */
        private String debugID;

        private StringBuffer recordBuffer = new StringBuffer(10000);
        private boolean notReadYet = true; // Nothing has been read from source
        private boolean eofReached = false;

        public Aleph2XMLInputStream(InputStream source, String debugID) {
            this.source = new BufferedReader(new InputStreamReader(source));
            this.debugID = debugID;
        }

        @Override
        public void close() throws IOException {
            source.close();
        }

        @Override
        public int read() throws IOException {
            while (true) {
                if (bufferPos != -1) {
                    if (bufferPos < buffer.size()) {
                        return buffer.get(bufferPos++);
                    } else {
                        buffer.clear();
                        bufferPos = -1;
                    }
                }
                if (eofReached) {
                    return Payload.EOF;
                }
                if (notReadYet) {
                    notReadYet = false;
                    appendToBuffer(HEADER);
                    bufferPos = 0;
                } else {
                    processNextLine();
                }
            }
        }

        /**
         * Reads a line from the input-stream and adds the corresponding output
         * to recordBuffer. This method might not update the recordBuffer, but
         * it will always advance through source.
         * </p><p>
         * When a record is deemed finished, recordBuffer will be assigned to
         * buffer and a new recordBuffer will be allocated. When buffer has
         * content, this content will be emptied before processNewLine will
         * be called again.
         * @throws java.io.IOException if a line could not be read from source.
         */
        private void processNextLine() throws IOException {
            String line = source.readLine();
            if (line == null) {
                // TODO: Should we call close in the stream here?
                eofReached = true;
                finishRecord();
                appendToBuffer(FOOTER);
                bufferPos = 0;
                return;
            }
            if ("".equals(line)) {
                return;
            }
            processLineContent(line);
        }

        /**
         * The last Aleph-ID (the counter 000000000 => 999999999) encountered.
         */
        private String lastAlephID = null; // Last processed record
        /**
         * The prefix for field 994*z is collected from any Aleph CAT*l-field.
         * The 994*z-field is appended after all the directly converted fields.
         */
        private String field994Prefix = "";
        /**
         * Whether or not the current Aleph record is considered valid.
         * Records that are not ok will not be passes on in the Stream.
         */
        private boolean isOK = true;
        /**
         * Process the given line and potentially signal that the XML for a new
         * record is ready to be passed on in the stream.
         * @param line the String to process.
         */
        private void processLineContent(String line) {
            if (buffer.size() == 0) {
                appendToBuffer(RECORD_START);
            }
            //  * 000000001 00800 L $$a1979$$bdk$$e1$$f0$$g0$$ldan$$tm
            String[] strparts = line.split("[ ]{1,5}", 4);
            if (strparts.length != 4) {
                log.debug("Received unexpected string " + line
                          + " with last id " + lastAlephID);
                return;
            }
            String alephID = strparts[0];
            while (alephID.length() < 9){
                alephID = "0" + alephID;
            }
            if (lastAlephID == null) {
                lastAlephID = alephID;
            }
            if (log.isTraceEnabled()) {
                log.trace("Processing line for aleph-ID '" + alephID + "': "
                          + line);
            }

            if (!alephID.equals(lastAlephID)) {
                /* We've reached the beginning of a new record and thus
                       the end of a previous one (and yes, we have handled
                       the start-case).
                     */
                finishRecord();
                lastAlephID = alephID;
            }

            String fieldTag = strparts[1];
            String ind1 = "";
            String ind2 = "";
            boolean inCAT = false;
            if (fieldTag.length() >= 3) {
                if (fieldTag.length() > 3) {
                    try{
                        ind1 = fieldTag.substring(3,4);
                        ind2 = fieldTag.substring(4,5);
                    } catch (StringIndexOutOfBoundsException e){
                        log.warn(String.format(
                                "StringIndexOutOfBounds while extracting ind1 "
                                + "and ind2 from line '%s' from %s",
                                line, debugID), e);
                        isOK = false;
                    }
                } else if (fieldTag.equals("CAT")) {
                     inCAT = true;
                } else if (fieldTag.equals("DEL")){ // deleted nat record
                    recordBuffer.append(DATA_START).append("004").
                            append("\" ind1=\"\" ind2=\"\" >\n").
                            append(SUBFIELD_START).append("r\">d").
                            append(SUBFIELD_END).append(DATA_END);
                }
                fieldTag = fieldTag.substring(0,3);
            } else {
                // TODO: Add this to content-log instead of general log
                log.warn(String.format(
                        "Aleph field '%s' from line '%s' in %s did not have "
                        + "length >= 3", fieldTag, line, debugID));
            }

            makeDatafield(strparts, fieldTag, ind1, ind2, inCAT);
        }

        private void makeDatafield(String[] tokens, String fieldTag,
                                   String ind1, String ind2, boolean inCAT) {
            recordBuffer.append(DATA_START).append(prepareString(fieldTag)).
                    append("\" ind1=\"").append(prepareString(ind1)).
                    append("\" ind2=\"").append(prepareString(ind2)).
                    append("\">");
            if (tokens[3].indexOf("$$") >= 0) {
                String[] subf = tokens[3].split("\\$\\$");
                int i = 0;
                int j = 0;
                while (i < subf.length) {
                    if (!subf[i].equals("")) {
                        String code = subf[i].substring(0,1);
                        String subfield = subf[i].substring(1);
                        if (inCAT && code.equals("l")){
                           field994Prefix = subfield;
                        }
                        if (j == 0) {
                            recordBuffer.append("\n");
                        }
                        recordBuffer.append(SUBFIELD_START).
                                append(prepareString(code)).append("\">").
                                append(prepareString(subfield)).
                                append(SUBFIELD_END);
                        j++;
                    }
                    i++;
                }
            } else {
                recordBuffer.append(prepareString(tokens[3]));
            }
            recordBuffer.append(DATA_END);
        }

        // TODO: Check whether this should be field 994*a instead of 994*z
        // TODO: Put the id in 001*a - same content as 994*z
        /**
         * If valid record-data exists, add the Aleph-ID to field 994*z, close
         * the XML for the record and pass the content to the main buffer.
         */
        private void finishRecord() {
            if (recordBuffer.length() <= HEADER.length()) {
                log.debug("finishRecord: No content received");
            } else {
                recordBuffer.append(DATA_START).
                        append("994\" ind1=\"0\" ind2=\"0\">\n").
                        append(SUBFIELD_START).append("z\">").
                        append(field994Prefix).append(idDelimiter).
                        append(lastAlephID).
                        append(SUBFIELD_END).append("\n").
                        append(DATA_END);
                recordBuffer.append(RECORD_END);
                if (isOK){
                    log.debug("Adding content of record buffer for "
                              + lastAlephID + " to main buffer");
                    appendToBuffer(recordBuffer.toString());
                    if (log.isTraceEnabled()) {
                        log.trace("Generated XML for record " + lastAlephID
                                  + ":\n" + recordBuffer);
                    }
                    bufferPos = 0;
                } else {
                    log.warn(String.format("Skipping Aleph record %s from %s "
                                           + "containing errors:\n%s",
                                           lastAlephID, debugID, buffer));
                }
            }
            recordBuffer = new StringBuffer(1000);
            isOK = true;
            field994Prefix = "";
        }

        private void appendToBuffer(String content) {
            try {
                byte[] bytes = content.getBytes("utf-8");
                for (byte aByte : bytes) {
                    buffer.add(aByte);
                }
            } catch (UnsupportedEncodingException e) {
                //noinspection DuplicateStringLiteralInspection
                throw new RuntimeException("utf-8 not supported", e);
            }

        }
    }
    
    @Override
    public synchronized void close(boolean success) {
        super.close(success);
        log.info("Closing down Aleph2XML2. " + getProcessStats());
    }

/*
    public void applyFilter(File input, Extension ext, String encoding) {

        try{
            StringBuffer sb = new StringBuffer();
            StringBuffer record = new StringBuffer();
            boolean isOK = true;
            boolean inCAT = false;
            String id = "";
            File output = new File(input.getAbsolutePath() + "." + ext);
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), encoding));
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(input),encoding));
            String str;
            long idx = 0;
            long huskidx = -1;
            sb.append(HEADER);

            while (((str = in.readLine()) != null) && (idx < 60000)){
                String[] strparts = str.split("[ ]{1,5}",4);
                if (strparts.length == 4) {
                    idx = new Long(strparts[0]);
                    if (huskidx == -1) {
                        huskidx = idx;
                    }
                    if (idx != huskidx) {
                        String idex =  "" + huskidx;
                        while (idex.length() < 9){
                             idex = "0" + idex;
                        }
                        record.append(DATA_START).
                                append("994\" ind1=\"0\" ind2=\"0\">\n").
                                append(SUBFIELD_START).append("z\">").
                                append(id).append('-').append(idex).
                                append(SUBFIELD_END).append("\n").
                                append(DATA_END);
                        record.append(RECORD_END);
                        id = "";
                        if (isOK){
                            sb.append(record);
                        } else {
                            log.error("Skipping record containing errors:\n"
                                      + record);
                            isOK = true;
                        }
                        record.setLength(0);
                        out.write(sb.toString().trim());
                        sb.setLength(0);
                        record.append(RECORD_START);
                        huskidx = idx;
                    }
                    String felt = strparts[1];
                    String ind1 = "";
                    String ind2 = "";
                    if (felt.length() >= 3) {

                        if (felt.length() > 3) {
                            inCAT= false;
                            try{
                            ind1 = felt.substring(3,4);
                            ind2 = felt.substring(4,5);
                            } catch (StringIndexOutOfBoundsException e){
                                log.warn(e.getMessage() + str + "buffer:\n"
                                         + sb  + "\nFile:\n"
                                         + input.getAbsolutePath());
                                isOK = false;
                            }
                        } else if (felt.equals("CAT")) {
                             inCAT = true;
                        } else if (felt.equals("DEL")){ // deleted nat record
                            record.append(DATA_START).append("004").
                                    append("\" ind1=\"\" ind2=\"\" >").
                                    append(SUBFIELD_START).append("r\" >d").
                                    append(SUBFIELD_END).append(DATA_END);
                            inCAT = false;
                        } else {
                            inCAT = false;
                        }
                        felt = felt.substring(0,3);
                    } else {
                        log.warn("Felt \"" + felt +"\" did not have length >= "
                                 + "3. Felt subtracted from line \""
                                 + str + "\"");
                    }
                    record.append(DATA_START).append(prepareString(felt)).
                            append("\" ind1=\"").append(prepareString(ind1)).
                            append("\" ind2=\"").append(prepareString(ind2)).
                            append("\">");
                    if (strparts[3].indexOf("$$") >= 0) {
                        String[] subf = strparts[3].split("\\$\\$");
                        int i = 0;
                        int j = 0;
                        while (i < subf.length) {
                            if (!subf[i].equals("")) {
                                String code = subf[i].substring(0,1);
                                String subfield = subf[i].substring(1);
                                if (inCAT && code.equals("l")){
                                   id = subfield; 
                                }
                                if (j == 0) {
                                    sb.append("\n");
                                }
                                record.append(SUBFIELD_START).
                                        append(prepareString(code)).append("\">").
                                        append(prepareString(subfield)).
                                        append(SUBFIELD_END);
                                j++;
                            }
                            i++;
                        }
                    } else {
                        record.append(prepareString(strparts[3]));
                    }
                    record.append(DATA_END);
                }
            }

            String idex =  "" + idx;
            while (idex.length() < 9){
                 idex = "0" + idex;
            }
            record.append(DATA_START).append("994\" ind1=\"0\" ind2=\"0\">\n").append(SUBFIELD_START).append("z\">").append(id).append('-').append(idex).append(SUBFIELD_END).append("\n").append(DATA_END);
            record.append(RECORD_END);
            id = "";
            if (isOK){
                sb.append(record);
            } else {
                log.error("Skipping record containing errors:\n" + record);
                isOK = true;
            }
            record.setLength(0);
            out.write(sb.toString().trim());
            sb.setLength(0);           


            sb.append("</collection>\n");
            out.write(sb.toString().trim());
            sb.setLength(0);
            in.close();
            out.close();
            input.renameTo(new File(input.getAbsolutePath() + ".done"));

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
    */
}
