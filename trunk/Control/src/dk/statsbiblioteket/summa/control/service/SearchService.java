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

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.MalformedObjectNameException;
import java.rmi.RemoteException;
import java.lang.management.ManagementFactory;

import dk.statsbiblioteket.summa.search.SummaSearcher;
import dk.statsbiblioteket.summa.search.LuceneSearcher;
import dk.statsbiblioteket.summa.search.SearchResult;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper for a {@link SummaSearcher}, which will normally translate to a
 * {@link LuceneSearcher}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchService extends ServiceBase implements SummaSearcher,
                                                          SearchServiceMBean {
    private Log log = LogFactory.getLog(SearchService.class);

    /**
     * The class to instantiate and use for searching.
     * </p><p>
     * This is optional. Default is the {@link LuceneSearcher} class.
     */
    public static final String CONF_SEARCHER_CLASS =
            "summa.search.searcher-class";
    public static final Class<? extends SummaSearcher> DEFAULT_SEARCHER_CLASS =
            LuceneSearcher.class;

    private Configuration conf;
    private SummaSearcher searcher;

    public SearchService(Configuration conf) throws RemoteException {
        super(conf);
        this.conf = conf;
        exportRemoteInterfaces();
        setStatus(Status.CODE.constructed,
                  "Created SearchService object",
                  Logging.LogLevel.DEBUG);

        setStatus(Status.CODE.constructed,
                  "Remote interfaces are up for SearchService",
                  Logging.LogLevel.DEBUG);
    }

    public synchronized void start() throws RemoteException {
        log.debug("Starting SearchService");
        if (searcher != null) {
            log.debug("Start called on an already running searcher");
            stop();
        }
        Class<? extends SummaSearcher> searcherClass;
        try {
            searcherClass = conf.getClass(CONF_SEARCHER_CLASS,
                                          SummaSearcher.class);
        } catch (NullPointerException e) {
            log.warn(String.format(
                    "The property '%s' was not defined. Defaulting to '%s'",
                    CONF_SEARCHER_CLASS, DEFAULT_SEARCHER_CLASS));
            searcherClass = DEFAULT_SEARCHER_CLASS;
        } catch (IllegalArgumentException e) {
            String message = String.format(
                    "The property '%s' with content '%s' could not be resolved "
                    + "to a proper class",
                    CONF_SEARCHER_CLASS, conf.getString(CONF_SEARCHER_CLASS));
            setStatus(Status.CODE.crashed, message, Logging.LogLevel.ERROR, e);
            throw new RemoteException(message, e);
        } catch (Exception e) {
            String message = String.format(
                    "Exception constructing SummaSearcher class from the "
                    + "property '%s' with content '%s'",
                    CONF_SEARCHER_CLASS, conf.getString(CONF_SEARCHER_CLASS));
            setStatus(Status.CODE.crashed, message, Logging.LogLevel.ERROR, e);
            throw new RemoteException(message, e);
        }
        log.debug(String.format(
                "Got SummaSearcher class '%s'. Commencing creation",
                searcherClass));
        try {
            searcher = Configuration.create(searcherClass, conf);
        } catch (IllegalArgumentException e) {
            String message = String.format(
                    "The SummaSearcher-class '%s' was not a Configurable",
                    searcherClass);
            setStatus(Status.CODE.crashed, message, Logging.LogLevel.ERROR, e);
            throw new RemoteException(message, e);
        } catch (Exception e) {
            String message = String.format(
                    "Exception creating instance of SummaSearcher class '%s'",
                    searcherClass);
            setStatus(Status.CODE.crashed, message, Logging.LogLevel.ERROR, e);
            throw new RemoteException(message, e);
        }
        setStatus(Status.CODE.running, String.format(
                "Created and started the SummaSearcher '%s'", searcher),
                  Logging.LogLevel.INFO);
    }

    public synchronized void stop() throws RemoteException {
        if (searcher == null) {
            log.debug("stop called, but status is already stopped");
            return;
        }
        //noinspection OverlyBroadCatchBlock
        try {
            searcher.close();
            setStatus(Status.CODE.stopped, "Searcher closed successfully",
                      Logging.LogLevel.DEBUG);
        } catch (Exception e) {
            setStatus(Status.CODE.crashed, "Searcher closed with error",
                      Logging.LogLevel.WARN, e);
            throw new RemoteException(String.format(
                    "Unable to close searcher '%s'", searcher), e);
        } finally {
            //noinspection AssignmentToNull
            searcher = null;
        }
    }

    /* Simple pass-through to the underlying searcher */


    /**
     * Implementation of the {@link SummaSearcher} interface. See the JavaDocs
     * for {@link SummaSearcher#fullSearch} for details on usage.
     * @param filter      a query that narrows the search. A filter does not
     *                    affect scores.<br />
     *                    This parameter is optional. Default is null.
     * @param query       a query as entered by a user. This is expanded to
     *                    the underlying index query-system, normally with
     *                    the use of
     *           {@link dk.statsbiblioteket.summa.common.index.IndexDescriptor}.
     * @param startIndex  the starting index for the result, counting from 0.
     *                    If the result is to be merged with the result from
     *                    other searchers, this needs to be 0 in order to
     *                    ensure proper merging.
     * @param maxRecords  the maximum number of records to return.<br />
     *                    this parameter is mandatory and is rounded down to
     *                    the value specified in properties, using the key
     *                    {link #CONF_MAX_NUMBER_OF_RECORDS}.
     * @param sortKey     specifies how to sort. If this is null or
     *                    {@link #SORT_ON_SCORE}, sorting will be done on the
     *                    scores for the hits. If a field-name is specified,
     *                    sorting will be done on that field.<br />
     *                    This parameter is optional.
     *                    Default is {@link #SORT_ON_SCORE}.
     *                    Specifying null is the same as specifying
     *                    {@link #SORT_ON_SCORE}.
     * @param reverseSort if true, the sort is performed in reverse order.
     * @param resultFields the fields to extract content from.
     *                    This parameter is optional. Default is specified
     *                    in the conf. at {@link #CONF_RESULT_FIELDS}.
     * @param fallbacks   if the value of a given field cannot be extracted,
     *                    the corresponding value from fallbacks is returned.
     *                    Note that the length of fallbacks and fields must
     *                    be the same.
     * @return the result of a search, suitable for merging or XML construction.
     * @throws RemoteException if there was an exception during search.
     */
    public SearchResult fullSearch(String filter, String query, long startIndex,
                             long maxRecords, String sortKey,
                             boolean reverseSort, String[] resultFields,
                             String[] fallbacks) throws RemoteException {
        return searcher.fullSearch(filter, query, startIndex, maxRecords,
                                   sortKey, reverseSort,
                                   resultFields, fallbacks);
    }

    /**
     * Simple shortcut for fullSearch. Equivalent to {@code
     * fullSearch(null, query, startIndex, maxRecords, null, false, null, null);
     * }
     * @param query       a query as entered by a user. This is expanded to
     *                    the underlying index query-system, normally with
     *                    the use of
     *           {@link dk.statsbiblioteket.summa.common.index.IndexDescriptor}.
     * @param startIndex  the starting index for the result, counting from 0.
     *                    If the result is to be merged with the result from
     *                    other searchers, this needs to be 0 in order to
     *                    ensure proper merging.
     * @param maxRecords  the maximum number of records to return.<br />
     *                    this parameter is mandatory and is rounded down to
     *                    the value specified in properties, using the key
     *                    {link #CONF_MAX_NUMBER_OF_RECORDS}.
     * @return the search-result in XML, as specified in {@link #fullSearch}.
     * @throws RemoteException if there was an exception during search.
     */
    public String simpleSearch(String query, long startIndex,
                               long maxRecords) throws RemoteException {
        return searcher.simpleSearch(query, startIndex, maxRecords);
    }


    /**
     * Alias for {@link #stop}.
     * @throws RemoteException if an error occured during close.
     */
    public void close() throws RemoteException {
        stop();
    }

    /* Delegation of SummaSearcher methods */

    public void clearStatistics() throws RemoteException {
        checkSearcher();
        searcher.clearStatistics();
    }
    public double getAverageResponseTime() throws RemoteException {
        checkSearcher();
        return searcher.getAverageResponseTime();
    }
    public int getCurrentSearches() throws RemoteException {
        checkSearcher();
        return searcher.getCurrentSearches();
    }
    public String[] getFallbackValues() throws RemoteException {
        checkSearcher();
        return searcher.getFallbackValues();
    }
    public String getIndexLocation() throws RemoteException {
        checkSearcher();
        return searcher.getIndexLocation();
    }
    public String getLastQuery() throws RemoteException {
        checkSearcher();
        return searcher.getLastQuery();
    }
    public long getLastResponseTime() throws RemoteException {
        checkSearcher();
        return searcher.getLastResponseTime();
    }
    public int getMaxConcurrentSearches() throws RemoteException {
        checkSearcher();
        return searcher.getMaxConcurrentSearches();
    }
    public long getMaxRecords() throws RemoteException {
        checkSearcher();
        return searcher.getMaxRecords();
    }
    public long getQueryCount() throws RemoteException {
        checkSearcher();
        return searcher.getQueryCount();
    }
    public int getQueueLength() throws RemoteException {
        checkSearcher();
        return searcher.getQueueLength();
    }
    public String[] getResultFields() throws RemoteException {
        checkSearcher();
        return searcher.getResultFields();
    }
    public int getSearcherAvailabilityTimeout() throws RemoteException {
        checkSearcher();
        return searcher.getSearcherAvailabilityTimeout();
    }
    public int getSearchers() throws RemoteException {
        checkSearcher();
        return searcher.getSearchers();
    }
    public int getSearchQueueMaxSize() throws RemoteException {
        checkSearcher();
        return searcher.getSearchQueueMaxSize();
    }
    public String getSortKey() throws RemoteException {
        checkSearcher();
        return searcher.getSortKey();
    }
    public long getTotalResponseTime() throws RemoteException {
        checkSearcher();
        return searcher.getTotalResponseTime();
    }
    public String getWarmupData() throws RemoteException {
        checkSearcher();
        return searcher.getWarmupData();
    }
    public int getWarmupMaxTime() throws RemoteException {
        checkSearcher();
        return searcher.getWarmupMaxTime();
    }
    public long performWarmup() throws RemoteException {
        checkSearcher();
        return searcher.performWarmup();
    }
    public void reloadIndex() throws RemoteException {
        checkSearcher();
        searcher.reloadIndex();
    }
    public void setFallbackValues(
            String[] fallbackValues) throws RemoteException {
        checkSearcher();
        searcher.setFallbackValues(fallbackValues);
    }
    public void setMaxConcurrentSearches(int maxSearches) throws
                                                          RemoteException {
        checkSearcher();
        searcher.setMaxConcurrentSearches(maxSearches);
    }
    public void setMaxRecords(long maxRecords) throws RemoteException {
        checkSearcher();
        searcher.setMaxRecords(maxRecords);
    }
    public void setResultFields(String[] fieldNames) throws RemoteException {
        checkSearcher();
        searcher.setResultFields(fieldNames);
    }
    public void setSearcherAvailabilityTimeout(int ms) throws RemoteException {
        checkSearcher();
        searcher.setSearcherAvailabilityTimeout(ms);
    }
    public void setSearchers(int searchers) throws RemoteException {
        checkSearcher();
        searcher.setSearchers(searchers);
    }
    public void setSearchQueueMaxSize(int maxSize) throws RemoteException {
        checkSearcher();
        searcher.setSearchQueueMaxSize(maxSize);
    }
    public void setSortKey(String sortKey) throws RemoteException {
        checkSearcher();
        searcher.setSortKey(sortKey);
    }
    public void setWarmupData(String location) throws RemoteException {
        checkSearcher();
        searcher.setWarmupData(location);
    }
    public void setWarmupMaxTime(int maxTime) throws RemoteException {
        checkSearcher();
        searcher.setWarmupMaxTime(maxTime);
    }
    private void checkSearcher() throws RemoteException {
        if (searcher == null) {
            throw new RemoteException("SearchService not started");
        }
    }
}
