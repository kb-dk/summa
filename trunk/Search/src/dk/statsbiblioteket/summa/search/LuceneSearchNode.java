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
package dk.statsbiblioteket.summa.search;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

/**
 * Lucene-specific search node.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LuceneSearchNode implements SearchNode, Configurable {
    private static Log log = LogFactory.getLog(LuceneSearchNode.class);

    private LuceneIndexDescriptor descriptor;
    private IndexSearcher searcher;
    private String location = null;

    public LuceneSearchNode(Configuration conf) throws IOException {
        descriptor = LuceneIndexUtils.getDescriptor(conf);
    }

    public void open(String location) throws IOException {
        log.debug("Open called for location '" + location + "'");
        this.location = location;
        close();
        URL urlLocation = Resolver.getURL(location);
        if ("".equals(urlLocation.getFile())) {
            throw new IOException(String.format(
                    "Could not resolve file from location '%s'", location));
        }
        searcher = new IndexSearcher(
                FSDirectory.getDirectory(urlLocation.getFile()));
        log.debug("Open finished for location '" + location + "'");
    }

    public void close() {
        if (searcher != null) {
            try {
                searcher.close();
            } catch (IOException e) {
                log.warn(String.format(
                        "Could not close index-connection to location '%s'. "
                        + "This will probably result in a resource-leak",
                        location), e);
            }
            //noinspection AssignmentToNull
            searcher = null;
        }
    }

    public void warmup(String query, String sortKey, String[] fields) {
        // TODO: Implement this
    }


    public String fullSearch(String filter, String query, long startIndex,
                             long maxRecords, String sortKey,
                             boolean reverseSort, String[] fields,
                             String[] fallbacks) throws RemoteException {
        // TODO: Implement this
        return null;

        // SummaQueryParser port from stable
    }
}
