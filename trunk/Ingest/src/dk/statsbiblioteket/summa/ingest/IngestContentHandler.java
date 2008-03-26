/* $Id: IngestContentHandler.java,v 1.9 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.9 $
 * $Date: 2007/10/05 10:20:22 $
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
package dk.statsbiblioteket.summa.ingest;

import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.XMLConstants;
import java.io.UnsupportedEncodingException;
import java.io.StringWriter;
import java.io.StringReader;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.Status;
import dk.statsbiblioteket.summa.common.xml.DefaultNamespaceContext;
import dk.statsbiblioteket.util.qa.QAInfo;


/**
 * The IngestContentHandler parses XML and in the process extracts needed information for
 * constructing {@link dk.statsbiblioteket.summa.common.Record} objects.<br><br>
 *
 * The content handler addresses: recordID - creation and record bounderies detection.<br><br>
 *
 * Concrete implementations may address other content specific issues.<br>
 * @see dk.statsbiblioteket.summa.common.Record
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "hal, te")
public abstract class IngestContentHandler extends DefaultHandler2 {

    protected Record.ValidationState validationState;
    protected boolean isModified = false;
    protected boolean isDeleted = false;

    protected StringBuffer buf;

    protected DefaultNamespaceContext ns;
    protected boolean inRecord;
    protected boolean checkoutput;
    protected String recordTagName;

    protected ParserTask task;

    protected static Log log = LogFactory.getLog(IngestContentHandler.class);

    protected String _currentID = "";
    protected String recordLead;


    /**
     * Creates an IngestContentHandler setting the default namespace for the {@link javax.xml.namespace.NamespaceContext}.
     * Records will be delivered to the specified ParserTask through the
     * {@link dk.statsbiblioteket.summa.ingest.ParserTask#store(dk.statsbiblioteket.summa.common.Record)}
     *
     * @param DEFAULT_NAMESPACE         the default namespace for the data source xml.
     * @param taskrunner                the ParserTask to whom records needs to be delivered.
     */
    public IngestContentHandler(String DEFAULT_NAMESPACE, ParserTask taskrunner){
        super();
        task = taskrunner;
        validationState = Record.ValidationState.notValidated;
        buf = new StringBuffer(2000);
        inRecord = false;
        checkoutput = true;
        ns = new DefaultNamespaceContext(DEFAULT_NAMESPACE);
        recordTagName = "record";
        if (log.isInfoEnabled()) {log.info("IngesContentHandler created");}
    }


    /**
     * Returns a byte[] representation of the String.<br>
     * If the String is not a vaild XML document the current {@link Status} on this ContentHandler will be set to
     * Status.ERROR
     *
     * @param s             The String containing the record content
     * @return              a checked converted byte[], produced with encoding set to utf-8.
     */
    protected byte[] getCheckedContent(String s) {
        if (log.isDebugEnabled()) {log.debug("Checkking string xml:\n" + s);}
        Result r = null; Source xml = null; Transformer t = null; StringWriter w = null;

        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            w = new StringWriter();
            r = new StreamResult(w);
            xml = new StreamSource(new StringReader(s));
        } catch (TransformerConfigurationException e) {
            log.fatal("JVM lacking needed support for XML transformation", e);
            System.exit(-1);
        }

        try {
            t.transform(xml, r);
            s = w.toString();
        } catch (TransformerException e1) {
            log.error(e1.getMessage(), e1);
            validationState = Record.ValidationState.invalid;
            return s.getBytes();
        }

        try {
            return s.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            log.fatal("JVM lacking needed support for UTF-8", e);
            System.exit(-1);
            return null;
        }

    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

        if (inRecord &&  !(ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && recordTagName.equals(localName))){
            buf.append('<').append(ns.getPrefix(uri)).append(localName);
                for (int i = 0; i < atts.getLength(); i++) {
                    if (atts.getURI(i) != null && !"".equals(atts.getURI(i))) {
                        buf.append(' ').append(ns.getPrefix(atts.getURI(i))).append(atts.getLocalName(i)).append('=').append('"').append(xmlEncodeString(atts.getValue(i))).append('"');
                    } else {
                        buf.append(' ').append(atts.getLocalName(i)).append('=').append('"').append(xmlEncodeString(atts.getValue(i))).append('"');
                    }
                }
            buf.append('>');
        }
        super.startElement(uri,localName,qName,atts);
    }

     public void endElement(String uri, String localName, String qName) throws SAXException {
        if (log.isTraceEnabled()) {log.trace("in end element" + ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX) + ":" + uri + ";" + recordTagName + ":" + localName + " inRecord:" + inRecord );}

         if (inRecord) {
            buf.append('<').append('/').append(ns.getPrefix(uri)).append(localName).append(">");
        }

        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri)
            && recordTagName.equals(localName)) {
            byte[] content = null;
            try {
                if (checkoutput) {
                    content = getCheckedContent(recordLead + buf.toString());
                } else {
                    content = (recordLead + buf.toString()).getBytes("utf-8");
                }
            } catch (UnsupportedEncodingException e) {
                log.fatal(e.getMessage(), e);
                System.exit(-1);
            }
            // TODO: What about base?
            Record record;
            if (isModified) {
                record = new Record(getID(), "unknown", content,
                                    System.currentTimeMillis());
            } else {
                record = new Record(getID(), "unknown", content);
            }
            record.getMeta().put(Record.META_VALIDATION_STATE,
                                 validationState.toString());
            record.setDeleted(isDeleted);
            task.store(record);
            clearBuffers();
        }

        super.endElement(uri, localName, qName);

    }

    /**
     * The recordID on the current content buffer.
     *
     * @return          the recordID
     */
    protected abstract String getID();

    /**
     * Implementations needs to assure that content buffers are cleared.
     */
    protected abstract void clearBuffers();


    /**
     * This needs to go into a global utils, please do so
     * @param c
     * @param start
     * @param len
     * @return
     */
    protected static String xmlEscapeChars(char[] c, int start, int len){
        String s = new String(c, start, len);
        return xmlEncodeString(s);
    }

    /**
     * This needs to go into global utils, please do so
     * @param s
     * @return
     */
    protected static String xmlEncodeString(String s){
        s = s.replaceAll("&", "&amp;");
        s = s.replaceAll("<", "&lt;");
        s = s.replaceAll(">", "&gt;");
        s = s.replaceAll("'", "&apos;");
        s = s.replaceAll("\"", "&quot;");
        return s;
    }

}
