/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.common.legacy;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.MarcAnnotations;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.summa.common.util.PayloadMatcher;
import dk.statsbiblioteket.summa.plugins.SaxonXSLT;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XSLT;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

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

    Transformer t = null;

    /**
     * The location of the XSLT that is applied to every Record-content.
     * </p><p>
     * This property is optional. Default is LegacyMultiVolumeConverter.xslt
     * that handles Marc-records from Statsbiblioteket.
     */
    public static final String CONF_MERGE_XSLT = "summa.ingest.marcmultivolume.xslt";
    public static final String DEFAULT_MERGE_XSLT = "LegacyMultiVolumeConverter.xslt";

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
    public static final String CONF_STRIP_XML_NAMESPACES = "summa.ingest.marcmultivolume.ignorexmlnamespaces";
    public static final boolean DEFAULT_STRIP_XML_NAMESPACES = false;

    /**
     * If defined, this contain a sub-Configuration with parameters for
     * {@link PayloadMatcher}. Any sub-Record that does not match is ignored
     * when a merge is performed.
     * </p><p>
     * Optional. If not defined, all sub Records are merged.
     */
    public static final String CONF_MERGE_RECORDS = "summa.ingest.marcmultivolume.mergerecords";

    /**
     * If true, a successful merge will result in the record having both parent Records and Child records removed
     * before being passed on in the chain.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_REMOVE_MERGED_RELATIVES = "summa.ingest.marcmultivolume.removemerged";
    public static final boolean DEFAULT_REMOVE_MERGED_RELATIVES = false;

    /**
     * If true, child records marked as deleted are ignored when merging.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_IGNORE_DELETED = "summa.ingest.marcmultivolume.ignoredeleted";
    public static final boolean DEFAULT_IGNORE_DELETED = true;

    private URL xsltLocation;
    @SuppressWarnings({"FieldCanBeLocal"})
    private boolean stripXMLNamespaces = DEFAULT_STRIP_XML_NAMESPACES;
    private final PayloadMatcher keep;
    private final boolean removeMerged;
    private final boolean ignoreDeleted;

    public MarcMultiVolumeMerger(Configuration conf) {
        super(conf);
        xsltLocation = Resolver.getURL(conf.getString(CONF_MERGE_XSLT, DEFAULT_MERGE_XSLT));
        if (xsltLocation == null) {
            throw new ConfigurationException(String.format(Locale.ROOT,
                    "Unable to get URL for property %s with content '%s",
                    CONF_MERGE_XSLT, conf.getString(CONF_MERGE_XSLT, DEFAULT_MERGE_XSLT)));
        }
        stripXMLNamespaces = conf.getBoolean(CONF_STRIP_XML_NAMESPACES, stripXMLNamespaces);
        if (conf.valueExists(CONF_MERGE_RECORDS)) {
            log.debug("Creating ignore matcher");
            try {
                keep = new PayloadMatcher(conf.getSubConfiguration(CONF_MERGE_RECORDS));
            } catch (SubConfigurationsNotSupportedException e) {
                throw new ConfigurationException("Sub configuration not supported", e);
            }
        } else {
            keep = null;
        }
        removeMerged = conf.getBoolean(CONF_REMOVE_MERGED_RELATIVES, DEFAULT_REMOVE_MERGED_RELATIVES);
        ignoreDeleted = conf.getBoolean(CONF_IGNORE_DELETED, DEFAULT_IGNORE_DELETED);
        log.info("Constructed " + this);
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getRecord() == null
            || payload.getRecord().getContent() == null
            || payload.getRecord().getContent().length == 0) {
            throw new PayloadException("No content in Record", payload);
        }
        Record record = payload.getRecord();
        String mergedContent = getMergedOrNull(record);
        if (mergedContent != null) {
            record.setContent(mergedContent.getBytes(StandardCharsets.UTF_8), false);
            if (removeMerged) {
                record.setParents(null);
                record.setChildren(null);
            }
        }
        return true;
    }

    /**
     * @param record the Record whose content to transform to legacy merged format.
     * @return the content of the Record and its children, represented as merged MARC.
     */
    public String getLegacyMergedXML(Record record) {
        String result = getMergedOrNull(record);
        return result == null ? record.getContentAsUTF8() : result;
    }

    private static final String NO_RELATIVES =
            "%s was marked as having %s, but none were resolved. Make sure that RecordReader (or whatever else "
            + "provides the Records) expands children";
    private String getMergedOrNull(Record record) {
        if (!record.hasChildren()) {
            log.trace("No children for " + record.getId());
            return null;
        } else if (record.getChildren() == null) {
            log.debug("Can not expand unresolved children of " + record.toString(true));
            return null;
        } else if (record.hasChildren() && record.getChildren() == null) {
            //noinspection DuplicateStringLiteralInspection
            log.debug(String.format(Locale.ROOT, NO_RELATIVES, record.getId(), "children"));
                return null;
        } else if (record.hasChildren() && countNotDeleted(record.getChildren()) == 0) {
            log.trace("No non-deleted children for " + record.getId());
            return null;
        } else if (record.hasParents() && record.getParents() == null) {
            //noinspection DuplicateStringLiteralInspection
            log.debug(String.format(Locale.ROOT, NO_RELATIVES, record.getId(), "parents"));
                return null;
        } else if (record.hasParents() && countNotDeleted(record.getParents()) == 0) {
            log.trace("No non-deleted parents for " + record.getId());
            return null;
        } else {
            //noinspection DuplicateStringLiteralInspection
            log.debug("Processing " + record.getChildren().size() + " children of " + record.getId());
        }

        //noinspection DuplicateStringLiteralInspection
        StringWriter output = new StringWriter(5000);
        try {
            addProcessedContent(output, countNotDeleted(record.getParents()) != 0, record, 0);
            log.trace("Finished processing content for " + record);
        } catch (Exception e) {
            log.warn("Exception transforming " + record + ". The content will not be updated", e);
            return null;
        }
        return output.toString();
    }

    private int countNotDeleted(List<Record> subRecords) {
        if (subRecords == null) {
            return 0;
        }
        int count = 0;
        for (Record sub: subRecords) {
            if (!sub.isDeleted()) {
                count++;
            }
        }
        return count;
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
    private void addProcessedContent(
            StringWriter output, boolean hasParent, Record record, int level) throws TransformerException {
        //noinspection DuplicateStringLiteralInspection
        log.debug("Processing " + record.getId() + "  at level " + level);

        String content = record.getContentAsUTF8();
        // This ugly hack is to ensure efficiency and check that the last
        // end-tag is for record, regardless of namespacing.
        //noinspection DuplicateStringLiteralInspection
        int endPos = content.indexOf("record", content.lastIndexOf("</"));
        if (endPos == -1) {
            String message = "The Record " + record + " did not end in </record>";
            if (log.isDebugEnabled()) {
                log.debug(message + ". The Record content was\n" + content);
            }
            // The exception will be caught and the message logged in the
            // Logging.PROCESS_LOG_NAME log.
            throw new IllegalArgumentException(message);
        }
        if (level == 0) {
            output.append(content.subSequence(0, endPos-2));
        } else {
            if(t == null) {
                t = SaxonXSLT.createTransformer(xsltLocation);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            SaxonXSLT.transform(t, new ByteArrayInputStream(record.getContent()), out);
            byte[] transformed = out.toByteArray();
            BufferedReader read;
            read = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(transformed), StandardCharsets.UTF_8));
            String line;
            try {
                while ((line = read.readLine()) != null) {
                    if (line.startsWith("<?")) {
                        line = line.substring(line.indexOf("?>") + 2);
                    }
                    line = handleTag24x(record, line, hasParent);
                    line = line.replace("xmlns=\"http://www.loc.gov/MARC21/slim\"", "");

                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                throw new TransformerException("IOException reading content from " + record, e);
            }
        }

        if (record.getChildren() != null) {
            for (Record child: record.getChildren()) {
                if (keep != null && !keep.isMatch(child)) {
                    log.debug("Ignoring sub Record as it matches the ignore setup. Ignored is " + record);
                } else if (ignoreDeleted && child.isDeleted()) {
                    log.debug("Ignoring sub Record as it is marked as deleted. Ignored is " + record);
                } else {
                    addProcessedContent(output, true, child, level+1);
                }
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
        String ERROR_NOTMULTI = "MarcAnnotation was NOTMULTI but the Record had ";
        if (log.isTraceEnabled()) {
            log.trace("Entering handleTag24x(" + record + ", " + line + ", " + hasParent + ")");
        }
        hasParent = hasParent || record.getParents() != null;
        MarcAnnotations.MultiVolumeType type = MarcAnnotations.MultiVolumeType.fromString(
            record.getMeta(MarcAnnotations.META_MULTI_VOLUME_TYPE));
        if (type == MarcAnnotations.MultiVolumeType.NOTMULTI) {
            if (hasParent && record.getChildren() != null) {
                Logging.logProcess(
                        this.getClass().getSimpleName(),
                        ERROR_NOTMULTI + "parents and children. We set it to " + MarcAnnotations.MultiVolumeType.SEKTION
                        + " for " + record, Logging.LogLevel.DEBUG, record.getId());
                type = MarcAnnotations.MultiVolumeType.SEKTION;
            } else if (hasParent) {
                Logging.logProcess(
                        this.getClass().getSimpleName(),
                        ERROR_NOTMULTI + "parents. We set it to " + MarcAnnotations.MultiVolumeType.BIND + " for "
                        + record, Logging.LogLevel.DEBUG, record.getId());
                type = MarcAnnotations.MultiVolumeType.BIND;
            } else if (record.getChildren() != null) {
                Logging.logProcess(
                        this.getClass().getSimpleName(),
                        ERROR_NOTMULTI + "children. We set it to " + MarcAnnotations.MultiVolumeType.HOVEDPOST + " for "
                        + record, Logging.LogLevel.DEBUG, record.getId());
                type = MarcAnnotations.MultiVolumeType.HOVEDPOST;
            }
        }
        if (type == MarcAnnotations.MultiVolumeType.BIND) {
            line = line.replace("tag=\"24x\"", "tag=\"248\"");
        } else if (type == MarcAnnotations.MultiVolumeType.SEKTION) {
            line = line.replace("tag=\"24x\"", "tag=\"247\"");
        } else {
            Logging.logProcess(this.getClass().getSimpleName(),
                    "Found 24x in " + record + ", but type was " + type + " so no replacement was done. "
                    + "The content will probably not be processed",
                    Logging.LogLevel.DEBUG, record.getId());
        }
        return line;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "MarcMultiVolumeMerger(xslt=%s, namespace-stripping=%b, mergedRemoval=%b",
                             xsltLocation, stripXMLNamespaces, removeMerged);
    }
}
