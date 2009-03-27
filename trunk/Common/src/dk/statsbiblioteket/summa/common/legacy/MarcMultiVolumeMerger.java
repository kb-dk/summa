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
import dk.statsbiblioteket.util.xml.XSLT;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.MarcAnnotations;
import dk.statsbiblioteket.summa.common.Logging;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.URL;

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
     * This property is optional. Default is LegacyMultiVolumeConverter.xslt
     * that handles Marc-records from Statsbiblioteket.
     */
    public static final String CONF_MERGE_XSLT =
            "summa.ingest.marcmultivolume.xslt";
    public static final String DEFAULT_MERGE_XSLT =
            "LegacyMultiVolumeConverter.xslt";

    /**
     * If true, all namespaces in the XML is stripped prior to transformation.
     * This is not recommended, but i The Real World, there are a lot of XML
     * and XSLT's with garbled namespace matching.
     * </p><p>
     * Note: Setting this to true might have a noticeable impact on processor-
     * load and temporary object allocation.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_STRIP_XML_NAMESPACES =
            "summa.ingest.marcmultivolume.ignorexmlnamespaces";
    public static final boolean DEFAULT_STRIP_XML_NAMESPACES = false;

    private URL xsltLocation;
    private boolean stripXMLNamespaces = DEFAULT_STRIP_XML_NAMESPACES;

    public MarcMultiVolumeMerger(Configuration conf) {
        super(conf);
        xsltLocation = Resolver.getURL(conf.getString(
                CONF_MERGE_XSLT, DEFAULT_MERGE_XSLT));
        if (xsltLocation == null) {
            throw new ConfigurationException(String.format(
                    "Unable to get URL for property %s with content '%s",
                    CONF_MERGE_XSLT,
                    conf.getString(CONF_MERGE_XSLT, DEFAULT_MERGE_XSLT)));
        }
        stripXMLNamespaces = conf.getBoolean(CONF_STRIP_XML_NAMESPACES,
                                             stripXMLNamespaces);
        log.info("MarcMultiVolumeMerger for '" + xsltLocation
                 + "' ready for use.  Namespaces will "
                 + (stripXMLNamespaces ? "" : "not ")
                 + "be stripped from input before merging");
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

    private static final String NO_RELATIVES =
            "%s was marked as having %s, but none were resolved. Make sure"
            + " that RecordReader (or whatever else provides the Records) "
            + "expands children";
    private String getMergedOrNull(Record record) {
        if (!record.hasChildren()) {
            log.debug("No children for " + record.getId());
            return null;
        } else if (record.getChildren() == null) {
            log.debug("Can not expand unresolved children of "
                      + record.toString(true));
            return null;
        } else if (record.hasChildren() && record.getChildren() == null) {
            //noinspection DuplicateStringLiteralInspection
            log.debug(String.format(
                        NO_RELATIVES, record.getId(), "children"));
                return null;
        } else if (record.hasParents() && record.getParents() == null) {
            //noinspection DuplicateStringLiteralInspection
            log.debug(String.format(
                        NO_RELATIVES, record.getId(), "parents"));
                return null;
        } else {
            //noinspection DuplicateStringLiteralInspection
            log.debug("Processing " + record.getChildren().size()
                      + " children of " + record.getId());
        }

        //noinspection DuplicateStringLiteralInspection
        StringWriter output = new StringWriter(5000);
        try {
            addProcessedContent(output, record.getParents() != null, record, 0);
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
     * @param hasParent must be true if the record has a resolved parent.
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
    private void addProcessedContent(StringWriter output, boolean hasParent,
                                     Record record, int level) throws
                                                          TransformerException {
        //noinspection DuplicateStringLiteralInspection
        log.debug("Processing "+record.getId()+"  at level " + level);

        String content = record.getContentAsUTF8();
        // This ugly hack is to ensure efficiency and check that the last
        // end-tag is for record, regardless of namespacing.
        //noinspection DuplicateStringLiteralInspection
        int endPos = content.indexOf("record", content.lastIndexOf("</"));
        if (endPos == -1) {
            String message = "The Record " + record
                             + " did not end in </record>";
            if (log.isDebugEnabled()) {
                log.debug(message + ". The Record content was\n" + content);
            }
            // The exception will be caught and the message logged in the
            // Logging.PROCESS_LOG_NAME log.
            throw new IllegalArgumentException(message);
        }
        if (level == 0) {
            output.append(content.subSequence(0, endPos-3));
        } else {
            byte[] transformed = XSLT.transform(
            xsltLocation, record.getContent(), null, stripXMLNamespaces).
                    toByteArray();
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
                    line = handleTag24x(record, line, hasParent);
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
                addProcessedContent(output, true, child, level+1);
            }
        }
        if (level == 0) {
            output.append(content.subSequence(endPos-3, content.length()));
        }
    }

    private String handleTag24x(Record record, String line, boolean hasParent) {
        if (!line.contains("tag=\"24x\"")) {
            return line;
        }
        String ERROR_NOTMULTI =
                "MarcAnnotation was NOTMULTI but the Record had ";
        if (log.isTraceEnabled()) {
            log.trace("Entering handleTag24x(" + record + ", " + line + ", "
                      + hasParent + ")");
        }
        hasParent = hasParent || record.getParents() != null;
        MarcAnnotations.MultiVolumeType type =
                MarcAnnotations.MultiVolumeType.fromString(
                        record.getMeta(MarcAnnotations.
                                META_MULTI_VOLUME_TYPE));
        if (type == MarcAnnotations.MultiVolumeType.NOTMULTI) {
            if (hasParent && record.getChildren() != null) {
                Logging.logProcess(
                        this.getClass().getSimpleName(),
                        ERROR_NOTMULTI + "parents and children. We set it to "
                        + MarcAnnotations.MultiVolumeType.SEKTION + " for "
                        + record, Logging.LogLevel.DEBUG, null);
                type = MarcAnnotations.MultiVolumeType.SEKTION;
            } else if (hasParent) {
                Logging.logProcess(
                        this.getClass().getSimpleName(),
                        ERROR_NOTMULTI + "parents. We set it to "
                        + MarcAnnotations.MultiVolumeType.BIND + " for "
                        + record, Logging.LogLevel.DEBUG, null);
                type = MarcAnnotations.MultiVolumeType.BIND;
            } else if (record.getChildren() != null) {
                Logging.logProcess(
                        this.getClass().getSimpleName(),
                        ERROR_NOTMULTI + "children. We set it to "
                        + MarcAnnotations.MultiVolumeType.HOVEDPOST + " for "
                        + record, Logging.LogLevel.DEBUG, null);
                type = MarcAnnotations.MultiVolumeType.HOVEDPOST;
            }
        }
        if (type.equals(MarcAnnotations.MultiVolumeType.BIND)) {
            line = line.replace("tag=\"24x\"", "tag=\"248\"");
        } else if (type.equals(
                MarcAnnotations.MultiVolumeType.SEKTION)) {
            line = line.replace("tag=\"24x\"", "tag=\"247\"");
        } else {
            Logging.logProcess(this.getClass().getSimpleName(),
                    "Found 24x in " + record + ", but type was " + type
                    + " so no replacement was done. The content will probably "
                    + "not be processed", Logging.LogLevel.DEBUG, null);
        }
        return line;
    }
}
