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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc4j.Constants;
import org.marc4j.MarcException;
import org.marc4j.MarcReader;
import org.marc4j.converter.CharConverter;
import org.marc4j.converter.impl.AnselToUnicode;
import org.marc4j.marc.*;
import org.marc4j.marc.impl.Verifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Implementation of MarcReader from marc4j, that allows for the input charset
 * to be anything Java supports, instead of the limited choices MarcStreamReader
 * provides. This also allows for a user-specified CharConverter.
 * </p><p>
 * The method to override was getDataAsString, but unfortunately it was private,
 * so the code from MarcStreamReader had to be copy-pasted and tweaked.
 * </p><p>
 * While we were at it, implementation of Iterator<Record> was added.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FlexibleMarcStreamReader implements MarcReader, Iterator<Record> {
    private static Log log = LogFactory.getLog(FlexibleMarcStreamReader.class);

    private DataInputStream input = null;
    private MarcFactory factory = MarcFactory.newInstance();

    private CharConverter charConverter = null; // Precedence over encoding
    private String encoding = null;
    private boolean override = false;

    public FlexibleMarcStreamReader(InputStream input) {
        this.input = new DataInputStream(new BufferedInputStream(input));
    }

    public FlexibleMarcStreamReader(InputStream input, String encoding) {
        this.input = new DataInputStream(new BufferedInputStream(input));

        if (encoding != null) {
            String trimmed = encoding.toUpperCase().replace("-", "").replace("_", "");
            if ("MARC8".equals(trimmed)) {
                log.debug("Encoding was MARC-8. Enabling AnselToUnicode CharConverter");
                charConverter = new AnselToUnicode();
            } else if ("UTF8".equals(trimmed)) {
                encoding = "UTF8";
            } else if ("ISO88591".equals(trimmed)) {
                encoding = "ISO-8859-1";
            } else {
                log.debug("Unknown encoding '" + encoding + "' will be used directly by new String(byte[], encoding)");
            }
            override = true;
        }
        this.encoding = encoding;
    }

    public FlexibleMarcStreamReader(InputStream input, CharConverter charConverter) {
        this.input = new DataInputStream(new BufferedInputStream(input));
        this.charConverter = charConverter;
    }

    /* Re-writing of getDataAsString */
    protected String getDataAsString(byte[] bytes) {
        if (charConverter != null) {
            return charConverter.convert(bytes);
        }
        try {
            if (encoding == null) {
                log.debug("Encoding is null. Returning default new String(byte[]). This won't work for anything else"
                          + " than plain ASCII");
                return new String(bytes);
            }
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new MarcException("unsupported encoding '" + encoding + "'", e);
        }
    }

    /**
     * Function not supported by this class.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove not supported");
    }

    /* Copy-paste of methods from MarcStreamParser */

    /**
     * Returns true if the iteration has more records, false otherwise.
     */
    @Override
    public boolean hasNext() {
        try {
            if (input.available() == 0) {
                return false;
            }
        } catch (IOException e) {
            throw new MarcException(e.getMessage(), e);
        }
        return true;
    }

    /**
     * Returns the next record in the iteration.
     *
     * @return Record - the record object
     */
    @Override
    public Record next() {
        Record record = factory.newRecord();

        try {

            byte[] byteArray = new byte[24];
            input.readFully(byteArray);

            int recordLength = parseRecordLength(byteArray);
            byte[] recordBuf = new byte[recordLength - 24];
            input.readFully(recordBuf);
            parseRecord(record, byteArray, recordBuf, recordLength);
            return (record);
        } catch (EOFException e) {
            throw new MarcException("Premature end of file encountered", e);
        } catch (IOException e) {
            throw new MarcException("an error occured reading input", e);
        }
    }

    private void parseRecord(Record record, byte[] byteArray, byte[] recordBuf, int recordLength) {
        Leader ldr;
        ldr = factory.newLeader();
        ldr.setRecordLength(recordLength);
        int directoryLength;

        try {
            parseLeader(ldr, byteArray);
            directoryLength = ldr.getBaseAddressOfData() - (24 + 1);
        } catch (IOException | MarcException e) {
            throw new MarcException("error parsing leader with data: " + new String(byteArray), e);
        }

        // if MARC 21 then check encoding
        switch (ldr.getCharCodingScheme()) {
            case ' ':
                if (!override) {
                    encoding = "ISO-8859-1";
                }
                break;
            case 'a':
                if (!override) {
                    encoding = "UTF8";
                }
        }
        record.setLeader(ldr);

        if ((directoryLength % 12) != 0) {
            throw new MarcException("invalid directory");
        }
        DataInputStream inputrec = new DataInputStream(new ByteArrayInputStream(recordBuf));
        int size = directoryLength / 12;

        String[] tags = new String[size];
        int[] lengths = new int[size];

        byte[] tag = new byte[3];
        byte[] length = new byte[4];
        byte[] start = new byte[5];

        String tmp;

        try {
            for (int i = 0; i < size; i++) {
                inputrec.readFully(tag);
                tmp = new String(tag);
                tags[i] = tmp;

                inputrec.readFully(length);
                tmp = new String(length);
                lengths[i] = Integer.parseInt(tmp);

                inputrec.readFully(start);
            }

            if (inputrec.read() != Constants.FT) {
                throw new MarcException("Expected field terminator at end of directory");
            }

            for (int i = 0; i < size; i++) {
//                int fieldLength = getFieldLength(inputrec);
                if (Verifier.isControlField(tags[i])) {
                    byteArray = new byte[lengths[i] - 1];
                    inputrec.readFully(byteArray);

                    if (inputrec.read() != Constants.FT) {
                        throw new MarcException("expected field terminator at end of field");
                    }

                    ControlField field = factory.newControlField();
                    field.setTag(tags[i]);
                    field.setData(getDataAsString(byteArray));
                    record.addVariableField(field);
                } else {
                    byteArray = new byte[lengths[i]];
                    inputrec.readFully(byteArray);

                    try {
                        record.addVariableField(parseDataField(tags[i], byteArray));
                    } catch (IOException e) {
                        throw new MarcException(
                                "error parsing data field for tag: " + tags[i] + " with data: " + new String(byteArray),
                                e);
                    }
                }
            }

            if (inputrec.read() != Constants.RT) {
                throw new MarcException("expected record terminator");
            }
        } catch (IOException e) {
            throw new MarcException("an error occured reading input", e);
        }
    }

    private DataField parseDataField(String tag, byte[] field) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(field);
        char ind1 = (char) bais.read();
        char ind2 = (char) bais.read();

        DataField dataField = factory.newDataField();
        dataField.setTag(tag);
        dataField.setIndicator1(ind1);
        dataField.setIndicator2(ind2);

        int code;
        int size;
        int readByte;
        byte[] data;
        Subfield subfield;
        while (true) {
            readByte = bais.read();
            if (readByte < 0) {
                break;
            }
            switch (readByte) {
                case Constants.US:
                    code = bais.read();
                    if (code < 0) {
                        throw new IOException("unexpected end of data field");
                    }
                    if (code == Constants.FT) {
                        break;
                    }
                    size = getSubfieldLength(bais);
                    data = new byte[size];
                    int l = bais.read(data);
                    if (l == -1) {
                        throw new IllegalStateException(
                                "Attempted to read " + size + " bytes for sub field in " + tag + " but got EOS");
                    }
                    if (l != data.length) {
                        byte[] nData = new byte[l];
                        System.arraycopy(data, 0, nData, 0, l);
                        data = nData;
                    }
                    subfield = factory.newSubfield();
                    subfield.setCode((char) code);
                    subfield.setData(getDataAsString(data));
                    dataField.addSubfield(subfield);
                    break;
                case Constants.FT:
                    break;
            }
        }
        return dataField;
    }

/*    private int getFieldLength(DataInputStream bais) throws IOException {
        bais.mark(9999);
        int bytesRead = 0;
        while (true) {
            switch (bais.read()) {
                case Constants.FT:
                    bais.reset();
                    return bytesRead;
                case -1:
                    bais.reset();
                    throw new IOException("Field not terminated");
                case Constants.US:
                default:
                    bytesRead++;
            }
        }
    }
  */
    private int getSubfieldLength(ByteArrayInputStream bais) throws IOException {
        bais.mark(9999);
        int bytesRead = 0;
        while (true) {
            switch (bais.read()) {
                case Constants.US:
                case Constants.FT:
                    bais.reset();
                    return bytesRead;
                case -1:
                    bais.reset();
                    throw new IOException("subfield not terminated");
                default:
                    bytesRead++;
            }
        }
    }

    private int parseRecordLength(byte[] leaderData) throws IOException {
        InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(leaderData), StandardCharsets.UTF_8);
        int length;
        char[] tmp = new char[5];
        isr.read(tmp);
        try {
            length = Integer.parseInt(new String(tmp));
        } catch (NumberFormatException e) {
            throw new MarcException("unable to parse record length", e);
        }
        return (length);
    }

    private void parseLeader(Leader ldr, byte[] leaderData) throws IOException {
        InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(leaderData), StandardCharsets.UTF_8);
        char[] tmp = new char[5];
        isr.read(tmp);
        // Skip over bytes for record length, ff we get here, its already been
        // computed.
        ldr.setRecordStatus((char) isr.read());
        ldr.setTypeOfRecord((char) isr.read());
        tmp = new char[2];
        isr.read(tmp);
        ldr.setImplDefined1(tmp);
        ldr.setCharCodingScheme((char) isr.read());
        char indicatorCount = (char) isr.read();
        char subfieldCodeLength = (char) isr.read();
        char baseAddr[] = new char[5];
        isr.read(baseAddr);
        tmp = new char[3];
        isr.read(tmp);
        ldr.setImplDefined2(tmp);
        tmp = new char[4];
        isr.read(tmp);
        ldr.setEntryMap(tmp);
        isr.close();
        try {
            ldr.setIndicatorCount(Integer.parseInt(String.valueOf(indicatorCount)));
        } catch (NumberFormatException e) {
            throw new MarcException("unable to parse indicator count", e);
        }
        try {
            ldr.setSubfieldCodeLength(Integer.parseInt(String.valueOf(subfieldCodeLength)));
        } catch (NumberFormatException e) {
            throw new MarcException("unable to parse subfield code length", e);
        }
        try {
            ldr.setBaseAddressOfData(Integer.parseInt(new String(baseAddr)));
        } catch (NumberFormatException e) {
            throw new MarcException("unable to parse base address of data", e);
        }
    }
}
