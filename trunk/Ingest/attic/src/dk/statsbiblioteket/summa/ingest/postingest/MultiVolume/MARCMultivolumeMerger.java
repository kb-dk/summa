/* $Id: MARCMultivolumeMerger.java,v 1.5 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.5 $
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
package dk.statsbiblioteket.summa.ingest.postingest.MultiVolume;

import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.rmi.RemoteException;

/**
 * Provides merge functionality of records whit content type MARC-XML.<br>
 * Most functionality is provided by specifying a proper MARC-XML xslt for initial normalizing.
 * @deprecated Multi volume is now part of the Storage.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class MARCMultivolumeMerger {


    MultiVolumeRecord.Record[] _records;
    private Storage io;
    private Transformer trans;
    private String base;
    private IOMultiVolume multivol;


    private static final Log log = LogFactory.getLog(MARCMultivolumeMerger.class);

    /**
     * Creates a new MARCMultivolumeMerger connected to the Metadata storage io.
     *
     * @param io                where to find the original content.
     * @param xslt              how to normalize a record.
     * @param base              which target base.
     */
    public MARCMultivolumeMerger(Storage io, InputStream xslt, String base) {
        multivol = IOMultiVolumeSQL.getInstance();
        _records = multivol.getAllMainRecords(base);
        this.io = io;
        this.base = base;
        trans = getTransformer(xslt);
        log.info("merger ok:" + _records.length + "records to merge");
    }


    /**
     * Merge records in the Metadata storage.<br>
     * @param io            where to find original records.
     */
    public void MergeRecords(Storage io) {
        for (MultiVolumeRecord.Record r : _records) {
            if (MultiVolumeRecord.RecordType.MAIN.equals(r.type)) {
                if (r.content != null) {
                    log.info("Start update:" + r.id);
                    String orgConent = new String(r.content);
                    orgConent = orgConent.substring(0, orgConent.indexOf("</record>"));
                    orgConent += enRich(r, new StringBuffer());
                    orgConent += "</record>";
                    try {
                        Record record = new Record(r.id, base,
                                                   orgConent.getBytes("utf-8"),
                                                   System.currentTimeMillis());
                        io.flush(record);
                    } catch (IOException e) {
                        log.error("update failed on" + r.id);
                    } 
                    log.info("done update record:" + r.id);
                    log.trace(orgConent);
                }
            }
        }
    }

    // inserts a chunck content on b from r
    private StringBuffer enRich(MultiVolumeRecord.Record r, StringBuffer b) {
        for (MultiVolumeRecord.Record re : r.getChilds()) {
            if (MultiVolumeRecord.RecordType.BIND.equals(re.type)) {
                b.append(getContent(re, re.type));
            } else if (MultiVolumeRecord.RecordType.SECTION.equals(re.type)) {
                b.append(getContent(re, re.type));
                enRich(re, b);
            }
        }
        return b;
    }

    //get content through the xslt normalization filter.
    private String getContent(MultiVolumeRecord.Record r, MultiVolumeRecord.RecordType type) {
        if (r.content != null) {
            String record = new String(r.content);
            if ("".equals(record.trim())) {
                return "";
            }

            final Source so = new StreamSource(new StringReader(record));
            StringWriter wr = new StringWriter();
            Result re = new StreamResult(wr);

            try {
                trans.transform(so, re);
                wr.flush();
                log.trace(wr.getBuffer().toString());
                BufferedReader read = new BufferedReader(new StringReader(wr.getBuffer().toString()));
                String line;
                StringBuffer buf = new StringBuffer();
                while ((line = read.readLine()) != null) {
                    if (line.startsWith("<?")) {
                        line = line.substring(line.indexOf("?>") + 2);
                    }
                    if (line.contains("tag=\"24x\"")) {
                        if (type.equals(MultiVolumeRecord.RecordType.BIND)) {
                            line = line.replace("tag=\"24x\"", "tag=\"248\"");
                        } else if (type.equals(MultiVolumeRecord.RecordType.SECTION)) {
                            line = line.replace("tag=\"24x\"", "tag=\"247\"");
                        }
                    }
                    line = line.replace("xmlns=\"http://www.loc.gov/MARC21/slim\"", "");

                    buf.append(line).append("\n");

                }
                if (r.type == MultiVolumeRecord.RecordType.BIND || r.type == MultiVolumeRecord.RecordType.SECTION) {
                    log.info("Deleting record: " + r.id + " reason:" + r.type);
//                    Record record = new Record(r.id, Status.DELETED, null, this.base);
                    // TODO: Why do we create a new Record?
                    io.flush(Record.createDeletedRecord(r.id, base));
                }
                return buf.toString();
            } catch (TransformerException e) {
                log.error("Transformer exception", e);
            } catch (IOException e) {
                log.error("IOException", e);
            }
        }
        return "";
    }

    Transformer getTransformer(InputStream xslt) {
        Transformer trans = null;
        final TransformerFactory tfactory = TransformerFactory.newInstance();
        try {
            trans = tfactory.newTransformer(new StreamSource(xslt));
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        } catch (TransformerException e) {
            log.error("Unable to instantiate Transformer, a system configuration error?", e);
        } finally {
            try {
                assert xslt != null;
                xslt.close();
            } catch (IOException e) {
                // do nothing
            }
        }
        return trans;
    }


}



