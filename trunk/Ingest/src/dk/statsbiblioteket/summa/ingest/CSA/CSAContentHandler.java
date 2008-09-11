/* $Id: CSAContentHandler.java,v 1.8 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.8 $
 * $Date: 2007/10/05 10:20:23 $
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
package dk.statsbiblioteket.summa.ingest.CSA;

import dk.statsbiblioteket.summa.ingest.IngestContentHandler;
import dk.statsbiblioteket.summa.ingest.ParserTask;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;

/**
 * @deprecated  use {@link dk.statsbiblioteket.summa.ingest.SimpleXML}
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "hal")
public class CSAContentHandler extends IngestContentHandler {


    //todo clean up all code limiting to records with issn 0066-4308

    //public static final String DEFAULT_NAMESPACE ="";
    public static final String ID_FIELD = "an";

    private int count = 0;
    private String idPrefix;

    private boolean id_chars, add_me, in_add_me;

    private String add_me_chars;
    public CSAContentHandler(ParserTask taskrunner, String prefix) {
        super(null, taskrunner);
        recordTagName = "rec";
        id_chars = false;
        isModified = false;
        id_chars = false;
        add_me = false;
        in_add_me = false;
        add_me_chars = "";
        idPrefix = prefix + "_";
        recordLead = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<rec>";
    }


    public void characters(char[] ch, int start, int length) throws SAXException {
        String s = xmlEscapeChars(ch, start, length);
        if (inRecord) buf.append(s);

        if (id_chars) {
           _currentID += s;
        }

        if (in_add_me) {
            add_me_chars += s;
        }
        super.characters(ch, start, length);
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri)) {
            if (recordTagName.equals(localName)) {
                inRecord = true;
            }
        }

        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && "is".equals(localName)){
            in_add_me = true;
        }


        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && ID_FIELD.equals(localName)){
            id_chars = true;
        }

        super.startElement(uri, localName, qName, atts);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && ID_FIELD.equals(localName)){
            id_chars = false;
        }

        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && "is".equals(localName)){
            in_add_me = false;
            if ("0066-4308".equals(add_me_chars)){
                add_me = true;
            }
        }
        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && recordTagName.equals(localName) && add_me){
            //System.out.println("record: " + ++count + "\n");
            super.endElement(uri,localName,qName);
            add_me = false;
            add_me_chars = "";
        } else if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && !recordTagName.equals(localName)){
            super.endElement(uri,localName,qName);
        }

        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && recordTagName.equals(localName)){
            add_me = false;
            add_me_chars = "";
            clearBuffers();
        }

    }

   protected String getID() {
       return   idPrefix + _currentID;
    }

    protected void clearBuffers() {
        buf = new StringBuffer();
        _currentID = "";
    }
}



