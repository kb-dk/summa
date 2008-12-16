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
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Legacy multi volume handling of Marc2-like records (the DanMarc2 subset used
 * at Statsbiblioteket). For a given Record, the content and the content of
 * its children are passed through an XSLT. The outputs are concatenated and
 * the field 245 is renamed to 247 or 248 for children, depending on the type
 * of child.
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
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
