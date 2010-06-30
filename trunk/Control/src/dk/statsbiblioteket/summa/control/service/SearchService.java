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
package dk.statsbiblioteket.summa.control.service;

import java.rmi.RemoteException;
import java.io.IOException;

import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.SummaSearcherFactory;
import dk.statsbiblioteket.summa.search.rmi.RMISearcherProxy;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.api.InvalidServiceStateException;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper for a {@link dk.statsbiblioteket.summa.search.api.SummaSearcher},
 * which will normally translate to a
 * {@link dk.statsbiblioteket.summa.search.SummaSearcherImpl}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchService extends ServiceBase {
    private static final long serialVersionUID = 6841583184L;
    private Log log = LogFactory.getLog(SearchService.class);

    /**
     * The search service will fall back to this class if the configuration does
     * not specify {@link dk.statsbiblioteket.summa.search.api.SummaSearcher#CONF_CLASS}
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
            throw new InvalidServiceStateException(getClientId(), getId(),
                                                   "start", "Already running");
        }

        setStatusRunning("Creating Searcher");

        if (!conf.valueExists (SummaSearcher.CONF_CLASS)) {
            conf.set (SummaSearcher.CONF_CLASS,
                      RMISearcherProxy.class.getName());
        }

        try {
            searcher = SummaSearcherFactory.createSearcher (conf);
        } catch (IllegalArgumentException e) {
            String message = String.format(
                    "The SummaSearcher-class '%s' was not a Configurable: %s",
                    conf.getString (SummaSearcher.CONF_CLASS),
                    e.getMessage ());
            setStatus(Status.CODE.crashed, message, Logging.LogLevel.ERROR, e);
            throw new RemoteException(message, e);
        } catch (Exception e) {
            String message = String.format(
                    "Exception creating instance of SummaSearcher class '%s': %s",
                    conf.getString (SummaSearcher.CONF_CLASS),
                    e.getMessage());
            setStatus(Status.CODE.crashed, message, Logging.LogLevel.ERROR, e);
            throw new RemoteException(message, e);
        }

        setStatusIdle();
    }

    public synchronized void stop() throws RemoteException {
        if (searcher == null) {
            throw new InvalidServiceStateException(getClientId(), getId(),
                                                   "start", "Not running");
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




