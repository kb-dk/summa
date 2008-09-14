/* $Id: MARCContentHandler.java,v 1.17 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.17 $
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
package dk.statsbiblioteket.summa.ingest.MARCXML;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import dk.statsbiblioteket.summa.ingest.IngestContentHandler;
import dk.statsbiblioteket.summa.ingest.Target;
import dk.statsbiblioteket.summa.ingest.ParserTask;
import dk.statsbiblioteket.summa.ingest.postingest.MultiVolume.MultiVolumeRecord;
import dk.statsbiblioteket.summa.ingest.postingest.MultiVolume.IOMultiVolumeSQL;
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.xml.XMLConstants;

/**
 * Provides functionality for simple content extraction of MARCXML.<br>
 *
 * This content handler is configured by a {@link dk.statsbiblioteket.summa.ingest.Target}
 * and can handle<br>
 *
 * <ul>
 *  <li>recordID extraction</li>
 *  <li>State extraction</li>
 *  <li>marking records for multi volume creation {@link dk.statsbiblioteket.summa.ingest.postingest.MultiVolume}</li>
 * <ul>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class MARCContentHandler extends IngestContentHandler {


    /**
     * An Id entry is an updatable ID candidate and a priority for this candidate
     * when adding a new ID candidate, proiroties are consulted and only highest priority candidate survive.<br>
     *
     * @author Hans Lund, State and University Library, Aarhus Denmark
     * @version $Id: MARCContentHandler.java,v 1.17 2007/10/05 10:20:24 te Exp $
     */
    private static class ID_Entry {
        private String id;
        private int pri;

        ID_Entry() {
            id = null;
            pri = 0;
        }

        ID_Entry(String id, int pri) {
            log.trace("id entry: id=" + id + " pri=" + pri);
            this.id = id;
            this.pri = pri;
        }

        void addID(String id, int pri) {
            if (pri > this.pri) {
                log.trace("adding id with higher pri=" + pri + " id=" + id);
                this.pri = pri;
                this.id = id;
            }
        }

        String getID() {
            return this.id;
        }
    }

    /**
     * Sipmle position scheme for references.
     *
     * @author Hans Lund, State and University Library, Aarhus Denmark
     * @version $Id: MARCContentHandler.java,v 1.17 2007/10/05 10:20:24 te Exp $
     */
    private static class RefPoS {
        private String id;
        private String pos;

        RefPoS(String id, String pos) {
            this.id = id;
            this.pos = pos;
        }

        String getPos() {
            return pos;
        }

        String getId() {
            return id;
        }
    }

    /**
     * If true {@link dk.statsbiblioteket.summa.ingest.IngestContentHandler#characters(char[], int start, int lenght)}
     * will return a char[] part of the record ID;
     */
    boolean id_chars = false;

    /**
     *If true the current data field is containing an ID subfield
     */
    boolean id_field = false;

    /**
     * If true {@link dk.statsbiblioteket.summa.ingest.IngestContentHandler#characters(char[], int start, int lenght)}
     * will return a char[] part of the status code;
     */
    boolean in_status = false;

    /**
     *If true the current data field is containing an status subfield
     */
    boolean before_status = false;


    boolean suppres_mod;
    boolean status_from_lead;
    Map<String, Integer> P_SUBFIELDS;

    String ID_FIELD;
    int cur_pri;
    //String curID;
    ID_Entry id_ent = new ID_Entry();


    private boolean inType04 = false;
    private boolean charType04 = false;

    String type04 = "";

    boolean in014 = false;
    boolean char14 = false;
    String refOP14 = "";

    boolean in15 = false;
    boolean in15z = false;
    boolean in15v = false;

    String in15zVal = "";
    String in15vVal = "";

    // Used for fields 014 and 015. SB-Horizon uses "z", standard is "a".
    String flerbindSubfield = "a";

    ArrayList<RefPoS> ref15 = new ArrayList<RefPoS>();


    boolean in245 = false;
    boolean in245g = false;

    String sortOP245 = "";

    MultiVolumeRecord.RecordType thisRecord = null;

    String prefix;
    String base;

    /**
     * Creates a MARC content handler.
     * @param t             the Target to ingest
     * @param task          the task ingesting the target.
     */
    public MARCContentHandler(Target t, ParserTask task){
        super("http://www.loc.gov/MARC21/slim", task);
        isModified = true;
//        state = Status.MODIFIED;
        id_field = false;
        id_ent = new ID_Entry();
        ref15 = new ArrayList<RefPoS>();

        recordLead = "<?xml version=\"1.0\" encoding=\"utf-8\"?><record xmlns=\"" + ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX) + "\" >";
        recordTagName = "record";

        ID_FIELD = t.get("target_id_key");
        status_from_lead = "leader".equals(ID_FIELD) ? true : false;

        P_SUBFIELDS = new HashMap<String, Integer>();
         for (Map.Entry<String, String> en :   t.getKeyVal().entrySet()){
              if (en.getKey().startsWith("subfield_")){
                  P_SUBFIELDS.put(en.getKey().substring(9), Integer.parseInt(en.getValue()));
              }
         }
         checkoutput = t.isCheck();
         suppres_mod = t.isFullIngest();
         prefix = t.getId_prefix();
         base = t.getBase();

    }



    public void characters(char[] ch, int start, int length) throws SAXException {
        String s = xmlEscapeChars(ch, start, length);

        if (inRecord) buf.append(s);  // this should also gi into IngestContent handler



        if (id_chars) {
            _currentID += s;
        }

        // special haldeling for multibind records
        if (charType04) {
            type04 += s;

        } else if (char14) {
            refOP14 += s;

        } else if (in15z) {

            in15zVal += s;

        } else if (in15v) {
            in15vVal += s;
        } else if (in245g) {
            sortOP245 += s;
        }


        if (in_status) {
            char statuschar;
            if ("leader".equals(ID_FIELD)) {
                statuschar = s.charAt(s.indexOf(6));
            } else {
                statuschar = s.charAt(0);
            }
            if (suppres_mod && statuschar != 'd') {
                isModified = false;
//                state = Status.NEW;
            } else if (statuschar == 'd') {
                isDeleted = true;
//                state = Status.DELETED;
            } else {
                isModified = true;
//                state = Status.MODIFIED;
            }
            in_status = false;
        }
        super.characters(ch, start, length);

    }

    protected String getID() {
        return id_ent.getID();
    }

    protected void clearBuffers() {
        buf = new StringBuffer();
        isModified = true;
        id_ent = new ID_Entry();
    }

     public void endElement(String uri, String localName, String qName) throws SAXException {

        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && "datafield".equals(localName)) {
            if (in15) {
                if (!"".equals(in15zVal)) {
                    ref15.add(new RefPoS(in15zVal, in15vVal));
                }
            }
            id_field = false;
            inType04 = false;
            in014 = false;
            in15 = false;
            in245 = false;
        }

        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && "subfield".equals(localName)) {

            if (in15z) {
                in15zVal = in15vVal.trim();
            } else if (in15v) {
                in15zVal = in15zVal.trim();
            } else if (id_chars) {
                _currentID = _currentID.trim();
                id_ent.addID(_currentID, cur_pri);
                _currentID = "";
                cur_pri = -1;

            }

            charType04 = false; char14 = false;  in15z = false;  in15v = false;  in245g = false;  id_chars = false;
        }


        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri) && recordTagName.equals(localName)) {  //end record
            log.trace("record end: id=" + getID() + "\n" +
                    "idfield=" + ID_FIELD + "\n" +
                    "record:\n" +
                    buf);

            type04 = type04.trim();

            //log.info("type code:" + type04);

            if ("b".equals(type04)) {
                thisRecord = MultiVolumeRecord.RecordType.BIND;
            } else if (("h".equals(type04) || "s".equals(type04)) && !"".equals(refOP14)) {
                thisRecord = MultiVolumeRecord.RecordType.SECTION;
            } else if ("h".equals(type04)) {
                thisRecord = MultiVolumeRecord.RecordType.MAIN;
            }



            if (thisRecord != null) {
                log.info("thisRecord:" + thisRecord);
                String thisID = getID();
                IOMultiVolumeSQL.getInstance().addRecord(prefix+ thisID, thisRecord, base);
                if (!thisRecord.equals(MultiVolumeRecord.RecordType.BIND)) {

                    if (!"".equals(in15zVal)) {
                        ref15.add(new RefPoS(in15zVal.trim(), in15vVal.trim()));
                    }
                    log.info("ref15size:" + ref15.size());
                    for (RefPoS r : ref15) {
                        log.info("adding child form 015down: childID: " + r.getId() + " pos" + r.getPos() +"  on parent" + getID());
                        String childID;
                        int childPos = -1;
                        if (!"".equals(r.getId())) {
                            childID = r.getId();
                            if (!"".equals(r.getPos().trim())) {
                                try {
                                    childPos = Integer.parseInt(r.getPos());
                                } catch (Exception e) {
                                    // ok use -1
                                }
                            }
                            if (childID != null && childPos > -1) {
                                IOMultiVolumeSQL.getInstance().addChild(prefix+ thisID, prefix+ childID, MultiVolumeRecord.RecordType.SECTIONORBIND, childPos, base);
                            } else if (childID != null) {
                                IOMultiVolumeSQL.getInstance().addChild(prefix+ thisID, prefix+ childID, MultiVolumeRecord.RecordType.SECTIONORBIND, base);
                            }
                        }
                    }

                }

                if (!"".equals(refOP14)) {
                    log.info("adding child from refOP14");
                    IOMultiVolumeSQL.getInstance().addChild(prefix + refOP14.trim(), prefix + thisID, thisRecord, base);
                }

                if (MultiVolumeRecord.RecordType.BIND.equals(thisRecord) || MultiVolumeRecord.RecordType.SECTION.equals(thisRecord) || MultiVolumeRecord.RecordType.MAIN.equals(thisRecord))
                {

                    byte[] content;
                    if (checkoutput) {
                        content = getCheckedContent(recordLead + buf.toString()  + "</" + recordTagName + ">");
                    } else {
                        content = (recordLead + buf.toString() + "</" + recordTagName + ">").getBytes();
                    }
                    log.info("update record: " + getID());
                    IOMultiVolumeSQL.getInstance().updateRecord(prefix+ getID(), thisRecord, content, isDeleted);
                }
            }

           inType04 = false; charType04 = false; type04 = ""; in014 = false; char14 = false; refOP14 = ""; in15 = false;
           in15z = false; in15v = false; in15zVal = ""; in15vVal = ""; ref15 = new ArrayList<RefPoS>(); in245 = false; in245g = false;
           sortOP245 = ""; thisRecord = null;

        }

        super.endElement(uri, localName, qName);

    }


    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        super.startElement(uri, localName, qName, atts);
        //log.info(recordTagName +":" + localName);
        if (log.isTraceEnabled()) {
            log.trace("Start element:" + uri + " " + localName + " " + qName);
        }

        if (ns.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX).equals(uri)) {

            if (recordTagName.equals(localName)) {
                inRecord = true;
            }

            if ("leader".equals(localName) && status_from_lead) {
                in_status = true;
            }

            if ("datafield".equals(localName)) {

                if (ID_FIELD.equals(atts.getValue("tag"))) {
                    id_field = true;
                } else if ("004".equals(atts.getValue("tag"))) {
                    inType04 = true;
                    if (!status_from_lead) {
                        before_status = true;
                    }
                } else if ("014".equals(atts.getValue("tag"))) {
                    in014 = true;
                } else if ("015".equals(atts.getValue("tag"))) {
                    in15 = true;
                } else if ("245".equals(atts.getValue("tag"))) {
                    in245 = true;
                }
            }

            if (in245 && "subfield".equals(localName) && "g".equals(atts.getValue("code"))) {
                in245g = true;
            }

            if (in15) {
                if ("subfield".equals(localName) && "z".equals(atts.getValue("code"))) {
                    in15z = true;
                } else if ("subfield".equals(localName) && "v".equals(atts.getValue("code"))) {
                    in15v = true;
                }
            }

            if (in014 && "subfield".equals(localName) && "z".equals(atts.getValue("code"))) {
                char14 = true;
            }


            if (before_status && "subfield".equals(localName) && "r".equals(atts.getValue("code"))) {
                in_status = true;
                before_status = false;
            }

            if (id_field && "subfield".equals(localName) && P_SUBFIELDS.containsKey(atts.getValue("code"))) {
                cur_pri = P_SUBFIELDS.get(atts.getValue("code"));
                id_chars = true;
            }

            if (inType04 && "subfield".equals(localName) && "a".equals(atts.getValue("code"))) {
                charType04 = true;
            }
        }
    }

    public void endDocument() throws SAXException {
        super.endDocument();
    }


}



