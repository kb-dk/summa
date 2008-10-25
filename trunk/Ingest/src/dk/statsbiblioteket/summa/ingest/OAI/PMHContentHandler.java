/* $Id: PMHContentHandler.java,v 1.13 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.13 $
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
package dk.statsbiblioteket.summa.ingest.OAI;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import dk.statsbiblioteket.summa.ingest.IngestContentHandler;
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.xml.XMLConstants;

/**
 * Content handler for PMH OAI meta data format.<br>
 * Only meta-data in the OAI_DC_NAMESPACE:<br>
 * <code>http://www.openarchives.org/OAI/2.0/oai_dc/</code> will be included
 * in the records..
 * @deprecated in favor of {@link dk.statsbiblioteket.summa.ingest.split.XMLSplitterFilter} as XMLSplitterFilter is
 * now supports namespaces.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "hal, te")
public class PMHContentHandler extends IngestContentHandler {
    boolean id_chars;

    public static final String DEFAULT_NAMESPACE =
            "http://www.openarchives.org/OAI/2.0/";
    public static final String DUBLINCORE_NAMESPACE =
            "http://purl.org/dc/elements/1.1/";
    public static final String OAI_DC_NAMESPACE =
            "http://www.openarchives.org/OAI/2.0/oai_dc/";


    public static final String RECORD_TAG = "record";

    public PMHContentHandler(OAIParserTask taskrunner){
        super(DEFAULT_NAMESPACE, taskrunner);
        commonInit("");
    }

    /**
     * Sets up a standard OAI-PMH handler with the addition of the specified
     * targetID and targetName, which will be stated in the header of every
     * generated record.
     * @param TargetID   the ID for the target.
     * @param TargetName the name for the target.
     * @param taskrunner
     */
    public PMHContentHandler(String TargetID, String TargetName,
                             OAIParserTask taskrunner) {
        super(DEFAULT_NAMESPACE,taskrunner);
        String attributes =
                "\"  OAItargetID=\"" + xmlEncodeString(TargetID)
                + "\" OAItargetName=\"" + xmlEncodeString(TargetName);
        commonInit(attributes);
    }

    private void commonInit(String extraAttributes) {
        isModified = true;

        ns.setNameSpace(DUBLINCORE_NAMESPACE, "dc");
        ns.setNameSpace(OAI_DC_NAMESPACE,"oai_dc");
        ns.setNameSpace(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi");
        recordLead = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                     + "<record xmlns=\"" + DEFAULT_NAMESPACE
                     + "\" xmlns:dc=\"" + DUBLINCORE_NAMESPACE
                     + "\" xmlns:oai_dc=\"" + OAI_DC_NAMESPACE
                     + "\" xmlns:xsi=\"" +  XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI
                     + extraAttributes
                     + "\">";
        recordTagName = RECORD_TAG;
    }

    public void characters(char[] ch, int start, int length) throws SAXException {

        String s = xmlEscapeChars(ch,start,length);
        if (inRecord) {
            buf.append(s);
        }
        if (id_chars) {
            _currentID += s;
        }
        super.characters(ch,start,length);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && "identifier".equals(localName)){
            id_chars = false;
        }
        super.endElement(uri,localName,qName);
    }

    protected String getID() {
        return _currentID;
    }

    protected void clearBuffers() {
        buf = new StringBuffer();
        _currentID = "";
    }


    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && "record".equals(localName)){
            inRecord = true;
        }
        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && "identifier".equals(localName)){
            id_chars = true;
        }

        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && "header".equals(localName)){
            for (int i = 0 ; i<atts.getLength(); i++){
                if ("status".equals(atts.getLocalName(i)) && "deleted".equals(atts.getValue(i))){
                    isDeleted = true;
                }
            }
        }
         super.startElement(uri,localName,qName,atts);
    }
}



