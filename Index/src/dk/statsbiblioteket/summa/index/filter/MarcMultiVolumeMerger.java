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
package dk.statsbiblioteket.summa.index.filter;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.StringWriter;

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
 * structure at som epoint in the future.
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


    public MarcMultiVolumeMerger(Configuration conf) {
        String xsltLocation = conf.getString(CONF_MERGE_XSLT,
                                             DEFAULT_MERGE_XSLT);
    }

    protected void processPayload(Payload payload) {
        if (payload.getRecord() == null
            || payload.getRecord().getContent() == null
            || payload.getRecord().getContent().length == 0) {
            log.debug("No content for " + payload);
            return;
        }
        Record record = payload.getRecord();
        if (record.getChildren() == null || record.getChildren().size() == 0) {
            log.debug("No children for " + payload);
            return;
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("Processing " + record.getChildren().size() + " level 1"
                  + " children");
        StringWriter output = new StringWriter(5000);
        addProcessedContent(output, record, 0);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Append the content of record to the output. For each child, perform a
     * recursive call with level+1 as depth.
     * @param output where to append the content.
     * @param record the record to transform.
     * @param level if 0, the content is added without transformation.
     *        If level is 1,
     */
    private void addProcessedContent(StringWriter output, Record record,
                                     int level) {
        // TODO: Implement this
    }
}
