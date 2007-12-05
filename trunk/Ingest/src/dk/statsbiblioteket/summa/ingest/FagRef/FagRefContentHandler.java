/* $Id: FagRefContentHandler.java,v 1.9 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.9 $
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
package dk.statsbiblioteket.summa.ingest.FagRef;

import dk.statsbiblioteket.summa.ingest.IngestContentHandler;
import dk.statsbiblioteket.summa.common.Status;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

import javax.xml.XMLConstants;

/**
 * A fagreferent is a contact person with regard to a specific field, such as
 * economy, botany, history and other. An example document is shown below.
 *
 * {@code
<?xml version="1.0" encoding="utf-8" ?>
<fagref>
    <navn>Jens Hansen</navn>
    <navn_sort>Hansen Jens</navn_sort>
    <stilling>Economics specialist</stilling>
    <titel>Cand.econ</titel>
    <email>jh@invalid.invalid</email>
    <emneLink>http://invalid.invalid/guides/economics/international/</emneLink>
    <emneLink>http://invalid.invalid/guides/economics/denmark/</emneLink>
    <beskrivelse>
        I am an economics expert at Big Library in Denmark, with special
attention to Danish economy. I am also a fictional character that has no
relation to living persong (trush me - there is no clever hidden mocking here).
    </beskrivelse>
    <emner>
        <emne>Economy</emne>
        <emne>Danish economy</emne>
    </emner>
</fagref>}
 *
 * @deprecated : Use the general {@link dk.statsbiblioteket.summa.ingest.SimpleXML.SimpleContentHandler}
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "hal, te")
public class FagRefContentHandler extends IngestContentHandler {
    /**
     * The unique identifier for the fagreferent is the email address.
     * This must consequently always be specified.
     */
    public static final String ID_FIELD = "email";
    /**
     * Fagreferent records are identified as "fagref".
     */
    public static final String RECORD_TAG = "fagref";
    /**
     * No special namespace for fagreferenter.
     */
    //public static final String DEFAULT_NAMESPACE ="";

    boolean id_chars;

    public FagRefContentHandler(FagRefParserTask taskrunner){
        super(null, taskrunner);
        inRecord = true;
        isModified = false;
        id_chars = false;
        recordTagName = RECORD_TAG;
        recordLead = "<?xml version=\"1.0\" encoding=\"utf-8\"?><fagref>";
    }


    public void characters(char[] ch, int start, int length)
            throws SAXException {
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

    public void startElement(String uri, String localName, String qName,
                             Attributes atts) throws SAXException {
        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri)) {
            if (recordTagName.equals(localName)) {
                inRecord = true;
            }
        }

        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && ID_FIELD.equals(localName)){
            id_chars = true;
        }

        super.startElement(uri, localName, qName, atts);
    }


}
