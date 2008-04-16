/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.index;

import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Handles iterative updates of a Lucene index.
 * </p><p>
 * The location of the Lucene files is derived from the property
 * {@link IndexManipulator#CONF_INDEX_ROOT_LOCATION} and is a sub-folder to
 * that. The name of the subfolder is a timestamp for the construction time og
 * the index with the format YYYYMMDD-HHMM. When a Lucene-updater   
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LuceneUpdater implements IndexManipulator {
    private static Log log = LogFactory.getLog(LuceneUpdater.class);

    public LuceneUpdater(Configuration conf) {

    }

    public void clear() throws IOException {
        // TODO: Implement this
    }

    public void update(Payload payload) throws IOException {
        // TODO: Implement this
    }

    public void commit() throws IOException {
        // TODO: Implement this
    }

    public void consolidate() throws IOException {
        // TODO: Implement this
    }

    public void close() {
        // TODO: Implement this
    }
}
