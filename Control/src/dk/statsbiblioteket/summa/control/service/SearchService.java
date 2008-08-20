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
package dk.statsbiblioteket.summa.control.service;

import java.rmi.RemoteException;
import java.io.IOException;

import dk.statsbiblioteket.summa.search.SummaSearcher;
import dk.statsbiblioteket.summa.search.SummaSearcherFactory;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.search.rmi.RMISearcherProxy;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper for a {@link SummaSearcher}, which will normally translate to a
 * {@link SummaSearcher}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchService extends ServiceBase {
    private Log log = LogFactory.getLog(SearchService.class);

    /**
     * The search service will fall back to this class if the configuration does
     * not specify {@link SummaSearcher#PROP_CLASS}
     */
    public static final Class<? extends SummaSearcher> DEFAULT_SEARCHER_CLASS =
            RMISearcherProxy.class;

    private Configuration conf;
    private SummaSearcher searcher;

    public SearchService(Configuration conf) throws IOException {
        super(conf);

        setStatus(Status.CODE.not_instantiated,
                  "Setting up",
                  Logging.LogLevel.DEBUG);

        this.conf = conf;
        exportRemoteInterfaces();

        setStatus(Status.CODE.constructed,
                  "Remote interfaces up",
                  Logging.LogLevel.DEBUG);
    }

    public synchronized void start() throws RemoteException {
        if (searcher != null) {
            log.debug("Start called on an already running searcher");
        }

        setStatusRunning("Creating Searcher");
        Class<? extends SummaSearcher> searcherClass;

        if (!conf.valueExists (SummaSearcher.PROP_CLASS)) {
            conf.set (SummaSearcher.PROP_CLASS,
                      RMISearcherProxy.class.getName());
        }

        try {
            searcher = SummaSearcherFactory.createSearcher (conf);
        } catch (IllegalArgumentException e) {
            String message = String.format(
                    "The SummaSearcher-class '%s' was not a Configurable: %s",
                    conf.getString (SummaSearcher.PROP_CLASS),
                    e.getMessage ());
            setStatus(Status.CODE.crashed, message, Logging.LogLevel.ERROR, e);
            throw new RemoteException(message, e);
        } catch (Exception e) {
            String message = String.format(
                    "Exception creating instance of SummaSearcher class '%s': %s",
                    conf.getString (SummaSearcher.PROP_CLASS),
                    e.getMessage());
            setStatus(Status.CODE.crashed, message, Logging.LogLevel.ERROR, e);
            throw new RemoteException(message, e);
        }

        setStatusIdle();
    }

    public synchronized void stop() throws RemoteException {
        if (searcher == null) {
            log.debug("stop called, but status is already stopped");
            return;
        }
        //noinspection OverlyBroadCatchBlock
        try {
            searcher.close();
            searcher = null;
            setStatus(Status.CODE.stopped, "Searcher closed successfully",
                      Logging.LogLevel.DEBUG);
        } catch (Exception e) {
            setStatus(Status.CODE.crashed,
                      "Searcher closed with error: " + e.getMessage (),
                      Logging.LogLevel.WARN, e);
            throw new RemoteException(
                              String.format("Unable to close searcher '%s': %s",
                                            searcher,
                                            e.getMessage ()), e);
        } finally {
            //noinspection AssignmentToNull
            searcher = null;
        }
    }

}
