/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.index.facet;

import java.io.IOException;
import java.io.File;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.index.IndexManipulator;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Handles iterative updates of a FacetBrowser map. This is reliant on
 * search-index-specific internal id's, such at DocID from Lucene and must be
 * positioned after a provider of such id's (such as
 * {@link dk.statsbiblioteket.summa.index.lucene.LuceneManipulator}) in the
 * chain of execution.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetManipulator implements IndexManipulator {
    private static Log log = LogFactory.getLog(FacetManipulator.class);

    public FacetManipulator(Configuration conf) throws ConfigurationException {

    }

    public void open(File indexRoot) throws IOException {
        //TODO: Implement this
        //noinspection DuplicateStringLiteralInspection
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void clear() throws IOException {
        //TODO: Implement this
        //noinspection DuplicateStringLiteralInspection
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public boolean update(Payload payload) throws IOException {
        //TODO: Implement this
        //noinspection DuplicateStringLiteralInspection
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void commit() throws IOException {
        //TODO: Implement this
        //noinspection DuplicateStringLiteralInspection
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void consolidate() throws IOException {
        //TODO: Implement this
        //noinspection DuplicateStringLiteralInspection
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void close() throws IOException {
        //TODO: Implement this
        //noinspection DuplicateStringLiteralInspection
        throw new UnsupportedOperationException("Not implemented yet");
    }
}



