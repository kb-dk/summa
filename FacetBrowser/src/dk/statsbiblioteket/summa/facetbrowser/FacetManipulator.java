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
package dk.statsbiblioteket.summa.facetbrowser;

import java.io.IOException;
import java.io.File;

import dk.statsbiblioteket.summa.index.IndexManipulator;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMapFactory;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

/**
 * te forgot to document this class.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetManipulator implements IndexManipulator {
    private static Logger log = Logger.getLogger(FacetManipulator.class);

    private TagHandler tagHandler;
    private CoreMap core;

    public FacetManipulator(Configuration conf) {
        log.info("Constructing FacetManipulator");
    }

    public void clear() throws IOException {
    }

    public void close() throws IOException {
    }

    public void commit() throws IOException {
    }

    public void consolidate() throws IOException {
    }

    public void open(File indexRoot) throws IOException {
    }

    public boolean update(Payload payload) throws IOException {
        return false;
    }
}
