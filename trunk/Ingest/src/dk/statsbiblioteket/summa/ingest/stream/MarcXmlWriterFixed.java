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

import com.ibm.icu.text.Normalizer;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.marc4j.Constants;
import org.marc4j.MarcException;
import org.marc4j.MarcWriter;
import org.marc4j.converter.CharConverter;
import org.marc4j.marc.*;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.Iterator;

/**
 * Nearly direct copy of {@link org.marc4j.MarcXmlWriter} that fixed the
 * duplicate default namespace declaration bug by reverting to the old
 * implementation.
 * </p><p>
 * This class will be removed from Summa when the bug is fixed natively in
 * marc4j.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, marc4j")
public class MarcXmlWriterFixed implements MarcWriter {


    protected static final String CONTROL_FIELD = "controlfield";

    protected static final String DATA_FIELD = "datafield";

    protected static final String SUBFIELD = "subfield";

    protected static final String COLLECTION = "collection";

    protected static final String RECORD = "record";

    protected static final String LEADER = "leader";

    private boolean indent = false;

    private TransformerHandler handler = null;

    private Writer writer = null;


    /**
     * Character encoding. Default is UTF-8.
     */
    private String encoding = "UTF8";

    private CharConverter converter = null;

    private boolean normalize = false;

    /**
     * Constructs an instance with the specified output stream.
     *
     * The default character encoding for UTF-8 is used.
     *
     * @throws org.marc4j.MarcException
     */
    public MarcXmlWriterFixed(OutputStream out) {
        this(out, false);
    }

    /**
     * Constructs an instance with the specified output stream and indentation.
     *
     * The default character encoding for UTF-8 is used.
     *
     * @throws org.marc4j.MarcException
     */
    public MarcXmlWriterFixed(OutputStream out, boolean indent) {
        this(out, "UTF8", indent);
    }

    /**
     * Constructs an instance with the specified output stream and character
     * encoding.
     *
     * @throws org.marc4j.MarcException
     */
    public MarcXmlWriterFixed(OutputStream out, String encoding) {
        this(out, encoding, false);
    }

    /**
     * Constructs an instance with the specified output stream, character
     * encoding and indentation.
     *
     * @throws org.marc4j.MarcException
     */
    public MarcXmlWriterFixed(OutputStream out, String encoding, boolean indent) {
        if (out == null) {
            throw new NullPointerException("null OutputStream");
        }
        if (encoding == null) {
            throw new NullPointerException("null encoding");
        }
        try {
            setIndent(indent);
            writer = new OutputStreamWriter(out, encoding);
            writer = new BufferedWriter(writer);
            this.encoding = encoding;
            setHandler(new StreamResult(writer), null);
        } catch (UnsupportedEncodingException e) {
            throw new MarcException(e.getMessage(), e);
        }
        writeStartDocument();
    }

    /**
     * Constructs an instance with the specified result.
     *
     * @param result
     * @throws org.xml.sax.SAXException
     */
    public MarcXmlWriterFixed(Result result) {
        if (result == null)
            throw new NullPointerException("null Result");
        setHandler(result, null);
        writeStartDocument();
    }

    /**
     * Constructs an instance with the specified stylesheet location and result.
     *
     * @param result
     * @throws org.xml.sax.SAXException
     */
    public MarcXmlWriterFixed(Result result, String stylesheetUrl) {
        this(result, new StreamSource(stylesheetUrl));
    }

    /**
     * Constructs an instance with the specified stylesheet source and result.
     *
     * @param result
     * @throws org.xml.sax.SAXException
     */
    public MarcXmlWriterFixed(Result result, Source stylesheet) {
        if (stylesheet == null)
            throw new NullPointerException("null Source");
        if (result == null)
            throw new NullPointerException("null Result");
        setHandler(result, stylesheet);
        writeStartDocument();
    }

    public void close() {
    	writeEndDocument();
    	try {
    		writer.close();
    	} catch (IOException e) {
    		throw new MarcException(e.getMessage(), e);
    	}
    }

    /**
     * Returns the character converter.
     *
     * @return CharConverter the character converter
     */
    public CharConverter getConverter() {
        return converter;
    }

    /**
     * Sets the character converter.
     *
     * @param converter
     *            the character converter
     */
    public void setConverter(CharConverter converter) {
        this.converter = converter;
    }

    /**
     * If set to true this writer will perform Unicode normalization on data
     * elements using normalization form C (NFC). The default is false.
     *
     * The implementation used is ICU4J 2.6. This version is based on Unicode
     * 4.0.
     *
     * @param normalize
     *            true if this writer performs Unicode normalization, false
     *            otherwise
     */
    public void setUnicodeNormalization(boolean normalize) {
        this.normalize = normalize;
    }

    /**
     * Returns true if this writer will perform Unicode normalization, false
     * otherwise.
     *
     * @return boolean - true if this writer performs Unicode normalization,
     *         false otherwise.
     */
    public boolean getUnicodeNormalization() {
        return normalize;
    }

    protected void setHandler(Result result, Source stylesheet)
            throws MarcException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            if (!factory.getFeature(SAXTransformerFactory.FEATURE))
                throw new UnsupportedOperationException(
                        "SAXTransformerFactory is not supported");

            SAXTransformerFactory saxFactory = (SAXTransformerFactory) factory;
            if (stylesheet == null)
                handler = saxFactory.newTransformerHandler();
            else
                handler = saxFactory.newTransformerHandler(stylesheet);
            handler.getTransformer()
                    .setOutputProperty(OutputKeys.METHOD, "xml");
            handler.setResult(result);

        } catch (Exception e) {
            throw new MarcException(e.getMessage(), e);
        }
    }

    /*
     * This method is rewritten by Toke Eskildsen and is the only change from
     * the original class.
     */
    protected void writeStartDocument() {
        try {
//            AttributesImpl atts = new AttributesImpl();
            handler.startDocument();
            // The next line duplicates the namespace declaration for Marc XML
             handler.startPrefixMapping("", Constants.MARCXML_NS_URI);
            // add namespace declaration using attribute - need better solution
//            atts.addAttribute(Constants.MARCXML_NS_URI, "xmlns", "xmlns",
//                              "CDATA", Constants.MARCXML_NS_URI);
//            handler.startElement(Constants.MARCXML_NS_URI, COLLECTION, COLLECTION, atts);
            handler.startElement("", COLLECTION, COLLECTION, null);
        } catch (SAXException e) {
            throw new MarcException(
                    "SAX error occured while writing start document", e);
        }
    }

    /**
     * Writes the root end tag to the result.
     *
     * @throws SAXException
     */
    protected void writeEndDocument() {
        try {
            if (indent)
                handler.ignorableWhitespace("\n".toCharArray(), 0, 1);

            handler
                    .endElement(Constants.MARCXML_NS_URI, COLLECTION,
                            COLLECTION);
            handler.endPrefixMapping("");
            handler.endDocument();
        } catch (SAXException e) {
            throw new MarcException(
                    "SAX error occured while writing end document", e);
        }
    }

    /**
     * Writes a Record object to the result.
     *
     * @param record -
     *            the <code>Record</code> object
     * @throws SAXException
     */
    public void write(Record record) {
        try {
            toXml(record);
        } catch (SAXException e) {
            throw new MarcException("SAX error occured while writing record", e);
        }
    }

    /**
     * Returns true if indentation is active, false otherwise.
     *
     * @return boolean
     */
    public boolean hasIndent() {
        return indent;
    }

    /**
     * Activates or deactivates indentation. Default value is false.
     *
     * @param indent
     */
    public void setIndent(boolean indent) {
        this.indent = indent;
    }

    protected void toXml(Record record) throws SAXException {
        char temp[];
        AttributesImpl atts = new AttributesImpl();
        if (indent)
            handler.ignorableWhitespace("\n  ".toCharArray(), 0, 3);

        handler.startElement(Constants.MARCXML_NS_URI, RECORD, RECORD, atts);

        if (indent)
            handler.ignorableWhitespace("\n    ".toCharArray(), 0, 5);

        handler.startElement(Constants.MARCXML_NS_URI, LEADER, LEADER, atts);
        Leader leader = record.getLeader();
        temp = leader.toString().toCharArray();
        handler.characters(temp, 0, temp.length);
        handler.endElement(Constants.MARCXML_NS_URI, LEADER, LEADER);

        Iterator i = record.getControlFields().iterator();
        while (i.hasNext()) {
            ControlField field = (ControlField) i.next();
            atts = new AttributesImpl();
            atts.addAttribute("", "tag", "tag", "CDATA", field.getTag());

            if (indent)
                handler.ignorableWhitespace("\n    ".toCharArray(), 0, 5);

            handler.startElement(Constants.MARCXML_NS_URI, CONTROL_FIELD,
                    CONTROL_FIELD, atts);
            temp = getDataElement(field.getData());
            handler.characters(temp, 0, temp.length);
            handler.endElement(Constants.MARCXML_NS_URI, CONTROL_FIELD,
                    CONTROL_FIELD);
        }

        i = record.getDataFields().iterator();
        while (i.hasNext()) {
            DataField field = (DataField) i.next();
            atts = new AttributesImpl();
            atts.addAttribute("", "tag", "tag", "CDATA", field.getTag());
            atts.addAttribute("", "ind1", "ind1", "CDATA", String.valueOf(field
                    .getIndicator1()));
            atts.addAttribute("", "ind2", "ind2", "CDATA", String.valueOf(field
                    .getIndicator2()));

            if (indent)
                handler.ignorableWhitespace("\n    ".toCharArray(), 0, 5);

            handler.startElement(Constants.MARCXML_NS_URI, DATA_FIELD,
                    DATA_FIELD, atts);
            Iterator j = field.getSubfields().iterator();
            while (j.hasNext()) {
                Subfield subfield = (Subfield) j.next();
                atts = new AttributesImpl();
                atts.addAttribute("", "code", "code", "CDATA", String
                        .valueOf(subfield.getCode()));

                if (indent)
                    handler.ignorableWhitespace("\n      ".toCharArray(), 0, 7);

                handler.startElement(Constants.MARCXML_NS_URI, SUBFIELD,
                        SUBFIELD, atts);
                temp = getDataElement(subfield.getData());
                handler.characters(temp, 0, temp.length);
                handler
                        .endElement(Constants.MARCXML_NS_URI, SUBFIELD,
                                SUBFIELD);
            }

            if (indent)
                handler.ignorableWhitespace("\n    ".toCharArray(), 0, 5);

            handler
                    .endElement(Constants.MARCXML_NS_URI, DATA_FIELD,
                            DATA_FIELD);
        }

        if (indent)
            handler.ignorableWhitespace("\n  ".toCharArray(), 0, 3);

        handler.endElement(Constants.MARCXML_NS_URI, RECORD, RECORD);
    }

    protected char[] getDataElement(String data) {
        String dataElement = null;
        if (converter == null)
            return data.toCharArray();
        dataElement = converter.convert(data);
        if (normalize)
            dataElement = Normalizer.normalize(dataElement, Normalizer.NFC);
        return dataElement.toCharArray();
    }


}
