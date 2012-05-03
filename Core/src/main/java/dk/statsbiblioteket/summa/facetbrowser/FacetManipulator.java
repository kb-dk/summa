/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.index.IndexManipulator;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;

/**
 * Dummy class that logs that index-time FacetBuilding currently is a no-op.
 * </p><p>
 * We should keep this as it is possible to move the startup-cost for faceting
 * to the index side at a later time. 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetManipulator implements IndexManipulator {
    private static Log log = LogFactory.getLog(FacetManipulator.class);

    public FacetManipulator(Configuration conf) {
        log.info(
            "The current FacetSystem does not do any index-time processing");
    }

    @Override
    public void open(File indexRoot) throws IOException {
        log.debug("open(" + indexRoot + ") called - doing nothing");
    }

    @Override
    public void clear() throws IOException {
        log.debug("Clear called - doing nothing");
    }

    @Override
    public boolean update(Payload payload) throws IOException {
        log.trace("Update called  doing nothing");
        return false; 
    }

    @Override
    public void commit() throws IOException {
        log.debug("commit() called - doing nothing");
    }

    @Override
    public void consolidate() throws IOException {
        log.debug("consolidate() called - doing nothing");
    }

    @Override
    public void close() throws IOException {
        log.debug("close() called - doing nothing");
    }

    @Override
    public void orderChangedSinceLastCommit() throws IOException {
        log.debug("orderChangedSinceLastCommit() called - doing nothing");
    }

    @Override
    public boolean isOrderChangedSinceLastCommit() throws IOException {
        return false;
    }
}
