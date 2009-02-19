/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.common.legacy;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.MarcAnnotations;
import dk.statsbiblioteket.summa.common.util.XSLTUtil;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import java.io.*;

/**
 * Legacy multi volume handling of Marc2-like records (the DanMarc2 subset used
 * at Statsbiblioteket). For a given Record, the content of its children are
 * passed through an XSLT. The outputs are concatenated to the parent Record and
 * the field 245 is renamed to 247 or 248 for all children, depending on the
 * type of child.
 * </p><p>
 * The result of the transforming and concatenation is put back into the Record-
 * content.
 * </p><p>
 * All this is rather kludgy and is expected to be replaced by a more explicit
 * XML-structure.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MarcMultiVolumeMerger extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(MarcMultiVolumeMerger.class);

    /**
     * The location of the XSLT that is applied to every Record-content.
     * </p><p>
     * This property is optional. Default is sb-multivolume.xslt that handles
     * Marc-records from Statsbiblioteket.
     */
    public static final String CONF_MERGE_XSLT =
            "summa.ingest.marcmultivolume.xslt";
    public static final String DEFAULT_MERGE_XSLT = "sb-multivolume.xslt";

    private Transformer transformer;

    public MarcMultiVolumeMerger(Configuration conf) {
        super(conf);
        String xsltLocation = conf.getString(CONF_MERGE_XSLT,
                                             DEFAULT_MERGE_XSLT);
        try {
            transformer = XSLTUtil.createTransformer(xsltLocation);
        } catch (TransformerException e) {
            throw new ConfigurationException(
                    "Unable to create Transformer for '" + xsltLocation + "'");
        }
    }

    @Override
    protected void processPayload(Payload payload) {
        if (payload.getRecord() == null
            || payload.getRecord().getContent() == null
            || payload.getRecord().getContent().length == 0) {
            log.debug("No content for " + payload);
            return;
        }
        Record record = payload.getRecord();
        String mergedContent = getMergedOrNull(record);
        if (mergedContent != null) {
            try {
                record.setContent(mergedContent.getBytes("utf-8"), false);
            } catch (UnsupportedEncodingException e) {
                //noinspection DuplicateStringLiteralInspection
                throw new IllegalArgumentException("utf-8 not supported");
            }
        }
    }

    /**
     *
     * @param record the Record whose content to transform to legacy merged
     *               format.
     * @return the content of the Record and its children, represented as
     *         merged MARC.
     */
    public String getLegacyMergedXML(Record record) {
        String result = getMergedOrNull(record);
        return result == null ? record.getContentAsUTF8() : result;
    }

    private String getMergedOrNull(Record record) {
        if (!record.hasChildren()) {
            log.debug("No children for " + record.getId());
            return null;
        } else if (record.getChildren() == null) {
            log.debug("Can not expand unresolved children of "
                      + record.toString(true));
            return null;
        } else {
            //noinspection DuplicateStringLiteralInspection
            log.debug("Processing " + record.getChildren().size()
                      + " children of " + record.getId());
        }

        //noinspection DuplicateStringLiteralInspection
        StringWriter output = new StringWriter(5000);
        try {
            addProcessedContent(output, record, 0);
            log.trace("Finished processing content for " + record);
        } catch (Exception e) {
            log.warn("Exception transforming " + record
                     + ". The content will not be updated", e);
            return null;
        }
        return output.toString();
    }

    /**
     * Append the content of record to the output. For each child, perform a
     * recursive call with level+1 as depth.
     * @param output where to append the content.
     * @param record the record to transform.
     * @param level if 0, the content is added without transformation, except
     *        for the end-record-tag. All children are then processed and
     *        appended and the end-record-tag is added.
     *        If level is >0, the content is transformed by the XSLT given
     *        by {@link #CONF_MERGE_XSLT} and appended, sans XML-header and
     *        record-tags.
     * @throws javax.xml.transform.TransformerException if there was an error
     *         during transformation.
     */
    private void addProcessedContent(StringWriter output, Record record,
                                     int level) throws TransformerException {
        //noinspection DuplicateStringLiteralInspection
        log.debug("Processing "+record.getId()+"  at level " + level);

        String content = record.getContentAsUTF8();
        int endPos = content.lastIndexOf("</record>");
        if (endPos == -1) {
            throw new IllegalArgumentException("The Record " + record
                                               + " did not end in </record>");
        }
        if (level == 0) {
            output.append(content.subSequence(0, endPos));
        } else {
            byte[] transformed = XSLTUtil.transformContent(
                    transformer, record.getContent());
            BufferedReader read;
            try {
                read = new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(transformed), "utf-8"));
            } catch (UnsupportedEncodingException e) {
                throw new TransformerException(
                        "utf-8 not supported while transforming " + record, e);
            }
            String line;
            try {
                while ((line = read.readLine()) != null) {
                    if (line.startsWith("<?")) {
                        line = line.substring(line.indexOf("?>") + 2);
                    }
                    if (line.contains("tag=\"24x\"")) {
                        MarcAnnotations.MultiVolumeType type =
                                MarcAnnotations.MultiVolumeType.fromString(
                                        record.getMeta(MarcAnnotations.
                                                META_MULTI_VOLUME_TYPE));
                        if (type.equals(MarcAnnotations.MultiVolumeType.BIND)) {
                            line = line.replace("tag=\"24x\"", "tag=\"248\"");
                        } else if (type.equals(
                                MarcAnnotations.MultiVolumeType.SEKTION)) {
                            line = line.replace("tag=\"24x\"", "tag=\"247\"");
                        }
                    }
                    line = line.replace(
                            "xmlns=\"http://www.loc.gov/MARC21/slim\"", "");

                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                throw new TransformerException(
                        "IOException reading content from " + record, e);
            }
        }

        if (record.getChildren() != null) {
            for (Record child: record.getChildren()) {
                addProcessedContent(output, child, level+1);
            }
        }
        if (level == 0) {
            output.append(content.subSequence(endPos, content.length()));
        }
    }
}
