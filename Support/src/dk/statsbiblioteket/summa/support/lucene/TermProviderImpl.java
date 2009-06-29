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
package dk.statsbiblioteket.summa.support.lucene;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.lucene.distribution.TermStat;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.IndexWatcher;
import dk.statsbiblioteket.summa.search.IndexListener;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;

/**
 * A wrapper for {@link TermStat} that watches for new persistent term
 * statistics and ensures up-to-date data. Used by SummaIndexReader.
 * </p><p>
 * As the term stat provider is meant to be used with a Lucene-like document
 * searcher (i.e. an inverted field/term-based index), some optimization might
 * take place. Removing all terms with a count of 1 might be such an
 * optimization as the document frequency used for scoring will always be at
 * least 1 for some searchers. By "some searchers" we mean "Lucene et al".
 * </p><p>
 * The wrapper received a Configuration that will be used directly to create
 * a {@link TermStat} and an {@link IndexWatcher}.
 * </p><p>
 * Important property for IndexWatcher is
 * {@link IndexWatcher#CONF_INDEX_WATCHER_INDEX_ROOT}. The default is
 * {@link #DEFAULT_TERMSTAT_ROOT}.
 * </p><p>
 * None of the properties are mandatory and the TermProvider will normally work
 * fine with the defaults.
 * @see {@link SummaIndexReader}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermProviderImpl implements IndexListener, TermProvider {
    private static Log log = LogFactory.getLog(TermProviderImpl.class);

    /**
     * The default root for the IndexWatcher for the TermProvider. If no root
     * is defined, the default will be used (resolved under the persistent
     * folder).
     */
    public static final String DEFAULT_TERMSTAT_ROOT = "index/termstat";

    private TermStat termStat = null;
    private boolean termStatActive = false;
    private IndexWatcher indexWatcher;

    public TermProviderImpl(Configuration conf) {
        log.debug("Creating TermProviderImpl");
        termStat = new TermStat(conf);
        if (!conf.valueExists(IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT)) {
            log.trace("Using default root '" + DEFAULT_TERMSTAT_ROOT + "'");
            conf.set(IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT,
                     DEFAULT_TERMSTAT_ROOT);
        }
        indexWatcher = new IndexWatcher(conf);
        indexWatcher.addIndexListener(this);
        indexWatcher.startWatching();
    }

    public synchronized void indexChanged(File indexFolder) {
        log.debug(String.format("indexChanged(%s) called", indexFolder));
        termStatActive = false;
        try {
            log.trace("Closing down any previously opened stats");
            termStat.close();
            if (indexFolder == null) {
                log.warn("No persistent term stats. Disabling distributed term "
                         + "statistics");
                return;
            }
            if (termStat.open(indexFolder)) {
                termStatActive = true;
            } else {
                log.warn(String.format(
                        "Unable to open term stats from '%s'. Disabling "
                        + "distributed term stats", indexFolder));
            }
        } catch (IOException e) {
            log.warn(String.format(
                    "IOException while opening '%s'. Distributed term stats "
                    + "will be disabled until new stats arrives", indexFolder), e);
        }
    }

    /**
     * The API for Indexreader dictates an in for numDocs, which is rather
     * limited in a distributed environment. We "solve" this by returning min
     * og the read docCount and Integer.MAX_VALUE.
     * @return the total number of documents in the distributed indexes og -1
     *         if the distributes term stats are not available.
     */
    public synchronized int numDocs() {
        if (termStatActive) {
            return termStat.getDocCount() > Integer.MAX_VALUE ?
                   Integer.MAX_VALUE : (int)termStat.getDocCount();
        }
        return -1;
    }

    /**
     * Returns the document frequency for the given term across all indexes.
     * @param term a term in the form "field:value".
     * @return the document frequency for the term or -1 if the distributed
     *         term stats are not available or does not contain the term.
     * @throws java.io.IOException if an error happened during resolving.
     */
    public int docFreq(String term) throws IOException {
        return termStatActive ? termStat.getTermCount(term) : -1;
    }

    /**
     * Closes down the provider. All requests for numdocs or or docFreq will
     * result in -1. After close, no further action can be taken on the
     * provider.
     */
    public void close() {
        log.debug("Closing down TermProviderImpl");
        indexWatcher.stopWatching();
        termStatActive = false;
        try {
            termStat.close();
        } catch (IOException e) {
            log.warn("Exception while closing underlying TermStat", e);
        }
    }
}
