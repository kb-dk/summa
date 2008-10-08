/* $Id: SimpleContentHandler.java,v 1.11 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.11 $
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
package dk.statsbiblioteket.summa.ingest.SimpleXML;

import dk.statsbiblioteket.summa.ingest.IngestContentHandler;
import dk.statsbiblioteket.summa.ingest.stream.XMLSplitterFilter;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.xml.sax.SAXException;import org.xml.sax.Attributes;

import javax.xml.XMLConstants;

/**
 * The simpleContentHandler will scan the XML for records, and extract the record id from a given XMLelement.
 * @deprecated in favor of {@link XMLSplitterFilter}.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
       state = QAInfo.State.UNDEFINED,
       author = "hal")
public class SimpleContentHandler extends IngestContentHandler {

    public static String ID_FIELD;


    boolean id_chars;

    public SimpleContentHandler(SimpleParserTask taskrunner, String id_element, String record_element){
        super(null, taskrunner);
        inRecord = true;
        isModified = false;
        id_chars = false;
        ID_FIELD = id_element;
        recordTagName = record_element;
        recordLead = "<?xml version=\"1.0\" encoding=\"utf-8\"?><" + record_element + ">";
    }


    public void characters(char[] ch, int start, int length) throws SAXException {
        String s = xmlEscapeChars(ch, start, length);
        if (inRecord) {
            buf.append(s);
        }
        if (id_chars) {
           _currentID += s;
        }
        super.characters(ch, start, length);
    }

    protected String getID() {
       return _currentID;
    }

    protected void clearBuffers() {
        buf = new StringBuffer();
        _currentID = "";
    }


    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && ID_FIELD.equals(localName)){
            id_chars = false;
        }
        super.endElement(uri,localName,qName);
    }

    public void startElement(String uri, String localName,
                             String qName, Attributes atts)
            throws SAXException {
        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && ID_FIELD.equals(localName)){
            id_chars = true;
        }
        super.startElement(uri, localName, qName, atts);
    }

}



