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

import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.RemoteException;
import java.rmi.Remote;

// TODO: Add setters and getters for all tweakables
// TODO: Add actions, such as reloading index
/**
 * The interface that all searchers in Summa should implement. The interface is
 * expected to be implemented by classes that are used with RMI or a similar
 * mechanism.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface SummaSearcher extends Remote, BasicSearcher {

    /**
     * The result fields are the fields extracted from each hit from a search.
     * @return The result fields for the searcher.
     * @throws RemoteException if the fields could not be retrieved.
     * @see SummaSearcher#CONF_RESULT_FIELDS
     */
L    public String[] getResultFields() throws RemoteException;

    /**
     * The result fields are the fields extracted from each hit from a search.
     * </p><p>
     * This value should only be tweaked manually for experimental purposes, as
     * it is not persistent across instantiations of searchers. In order to make
     * the change persistent, the underlying configuration for the searcher must
     * be updated - this is normally done through the Control module.
     * @param fieldNames the result fields to use in the searcher.
     * @throws RemoteException if the fields could not be set.
     * @see SummaSearcher#CONF_RESULT_FIELDS
     */
    // TODO: consider merging this setter with the fallback value setter
    public void setResultFields(String[] fieldNames) throws RemoteException;


    /**
     * If a the content of a result field cannot be extracted from a document,
     * the value at the same array position is returned.
     * @return The fallback values for the searcher.
     * @throws RemoteException if the values could not be retrieved.
     * @see SummaSearcher#CONF_FALLBACK_VALUES
     */
    public String[] getFallbackValues() throws RemoteException;

    /**
     * If a the content of a result field cannot be extracted from a document,
     * the value at the same array position is returned.
     * </p><p>
     * This value should only be tweaked manually for experimental purposes, as
     * it is not persistent across instantiations of searchers. In order to make
     * the change persistent, the underlying configuration for the searcher must
     * be updated - this is normally done through the Control module.
     * @param fallbackValues the new fallback values. Null is an acceptable
     *                       value and will result in null being used as
     *                       default value for all result fields.
     * @throws RemoteException if the values could not be set.
     * @see SummaSearcher#CONF_FALLBACK_VALUES
     */
    public void setFallbackValues(String[] fallbackValues) throws
                                                           RemoteException;

    /**
     * Warmup data are used for warming the search engine after a persistent
     * index has been opened. Warmup significantly reduces response time on
     * subsequent searches for some search engines, with Lucene being one.
     * @return the location of the data used for warmup or null if no location
     *         is specified.
     * @throws RemoteException if the location could not be retrieved.
     * @see SummaSearcher#CONF_WARMUP_DATA
     */
    public String getWarmupData() throws RemoteException;

    /**
     * Warmup data are used for warming the search engine after a persistent
     * index has been opened. Warmup significantly reduces response time on
     * subsequent searches for some search engines, with Lucene being one.
     * </p><p>
     * This value should only be tweaked manually for experimental purposes, as
     * it is not persistent across instantiations of searchers. In order to make
     * the change persistent, the underlying configuration for the searcher must
     * be updated - this is normally done through the Control module.
     * @param location where the warmup-data can be retrieved or null if no
     *                 warmup should be performed.
     * @throws RemoteException if the location could not be changed.
     * @see SummaSearcher#CONF_WARMUP_DATA
     */
    public void setWarmupData(String location) throws RemoteException;

    /**
     * @return the maximum amount of milliseconds used on warming the searcher.
     * @throws RemoteException if the time could not be retrieved.
     * @see SummaSearcher#CONF_WARMUP_MAXTIME
     * @see SummaSearcher#CONF_WARMUP_DATA
     */
    public int getWarmupMaxTime() throws RemoteException;

    /**
     * This value should only be tweaked manually for experimental purposes, as
     * it is not persistent across instantiations of searchers. In order to make
     * the change persistent, the underlying configuration for the searcher must
     * be updated - this is normally done through the Control module.
     * @param maxTime the maximum amount of milliseconds to use on warmup.
     *        If Integer.MAX_VALUE is specified, the warming stops when all
     *        queries specified in the warmup data has been used.
     * @throws RemoteException if the value could not be retrieved.
     * @see SummaSearcher#CONF_WARMUP_MAXTIME
     * @see SummaSearcher#CONF_WARMUP_DATA
     */
    public void setWarmupMaxTime(int maxTime) throws RemoteException;

    /**
     * @return the maximum number of records (hits) this searcher is willing to
     *         return, providing a limit on the number explicitely requested
     *         when a search is invoked.
     * @throws RemoteException if the value could not be retrieved.
     * @see SummaSearcher#CONF_MAX_RECORDS
     */
    public long getMaxRecords() throws RemoteException;

    /**
     * This value should only be tweaked manually for experimental purposes, as
     * it is not persistent across instantiations of searchers. In order to make
     * the change persistent, the underlying configuration for the searcher must
     * be updated - this is normally done through the Control module.
     * @param maxRecords the maximum number of records (hits) this searcher is
     *                   willing to return, providing a limit on the number
     *                   explicitely requested when a search is invoked.
     * @throws RemoteException if the value could not be changed.
     * @see SummaSearcher#CONF_MAX_RECORDS
     */
    public void setMaxRecords(long maxRecords) throws RemoteException;

    /**
     * @return the number of underlying searchers used by this searcher.
     * @throws RemoteException if the value could not be retrieved.
     * @see SummaSearcher#CONF_NUMBER_OF_SEARCHERS
     * @see SummaSearcher#CONF_NUMBER_OF_CONCURRENT_SEARCHES
     */
    public int getSearchers() throws RemoteException;

    /**
     * This value should only be tweaked manually for experimental purposes, as
     * it is not persistent across instantiations of searchers. In order to make
     * the change persistent, the underlying configuration for the searcher must
     * be updated - this is normally done through the Control module.
     * @param searchers the number of underlying searchers to use.
     *                  This must be 1 or more.
     * @throws RemoteException if the number could not be changes.
     * @see SummaSearcher#CONF_NUMBER_OF_SEARCHERS
     * @see SummaSearcher#CONF_NUMBER_OF_CONCURRENT_SEARCHES
     */
    public void setSearchers(int searchers) throws RemoteException;

    /**
     * @return the maximum number of concurrent searches allowed.
     * @throws RemoteException if the value could not be retrieved.
     * @see SummaSearcher#CONF_NUMBER_OF_CONCURRENT_SEARCHES
     * @see SummaSearcher#CONF_SEARCH_QUEUE_MAX_SIZE
     * @see SummaSearcher#CONF_NUMBER_OF_SEARCHERS
     */
    public int getMaxConcurrentSearches() throws RemoteException;

    /**
     * Changing the number of concurrent searches will only affect new searches.
     * This value should only be tweaked manually for experimental purposes, as
     * it is not persistent across instantiations of searchers. In order to make
     * the change persistent, the underlying configuration for the searcher must
     * be updated - this is normally done through the Control module.
     * @param maxSearches the maximum number of concurrent searches allowed.
     * @throws RemoteException if the value could not be changed.
     * @see SummaSearcher#CONF_NUMBER_OF_CONCURRENT_SEARCHES
     * @see SummaSearcher#CONF_SEARCH_QUEUE_MAX_SIZE
     * @see SummaSearcher#CONF_NUMBER_OF_SEARCHERS
     */
    public void setMaxConcurrentSearches(int maxSearches) throws
                                                          RemoteException;

    /**
     * @return the maximum number of queued queries.
     * @throws RemoteException if the value could not be retrieved.
     * @see SummaSearcher#CONF_SEARCH_QUEUE_MAX_SIZE
     * @see SummaSearcher#CONF_NUMBER_OF_CONCURRENT_SEARCHES
     */
    public int getSearchQueueMaxSize() throws RemoteException;

    /**
     * Changing the queue max size will only affect new searches.
     * </p><p>
     * This value should only be tweaked manually for experimental purposes, as
     * it is not persistent across instantiations of searchers. In order to make
     * the change persistent, the underlying configuration for the searcher must
     * be updated - this is normally done through the Control module.
     * @param maxSize the maximum number of queued queries.
     * @throws RemoteException if the value could not be changed.
     * @see SummaSearcher#CONF_SEARCH_QUEUE_MAX_SIZE
     * @see SummaSearcher#CONF_NUMBER_OF_CONCURRENT_SEARCHES
     */
    public void setSearchQueueMaxSize(int maxSize) throws RemoteException;

    /**
     * @return the timeout in milliseconds before a search is dropped.
     * @throws RemoteException if the value could not be retrieved.
     * @see SummaSearcher#CONF_SEARCHER_AVAILABILITY_TIMEOUT
     * @see SummaSearcher#CONF_SEARCHER_AVAILABILITY_TIMEOUT
     */
    public int getSearcherAvailabilityTimeout() throws RemoteException;

    /**
     * Changing the timeout only affect new searches.
     * </p><p>
     * This value should only be tweaked manually for experimental purposes, as
     * it is not persistent across instantiations of searchers. In order to make
     * the change persistent, the underlying configuration for the searcher must
     * be updated - this is normally done through the Control module.
     * @param ms the timeout in milliseconds before a search is dropped.
     * @throws RemoteException if the value could not be changed.
     * @see SummaSearcher#CONF_SEARCHER_AVAILABILITY_TIMEOUT
     */
    public void setSearcherAvailabilityTimeout(int ms) throws RemoteException;

    /**
     * @return the default sort key for searches.
     * @throws RemoteException if the key could not be determined.
     * @see SummaSearcher#CONF_DEFAULT_SORTKEY
     */
    public String getSortKey() throws RemoteException;

    /**
     * This value should only be tweaked manually for experimental purposes, as
     * it is not persistent across instantiations of searchers. In order to make
     * the change persistent, the underlying configuration for the searcher must
     * be updated - this is normally done through the Control module.
     * @param sortKey the default sort key for searches.
     * @throws RemoteException if the value could not be changed.
     * @see SummaSearcher#CONF_DEFAULT_SORTKEY
     */
    public void setSortKey(String sortKey) throws RemoteException;


    /* Index manipulation */

    /**
     * Force an explicit warmup of the searchers.
     * @return the number of nanoseconds used on warmup.
     * @throws RemoteException if warmup could not be performed.
     * @see SummaSearcher#CONF_WARMUP_DATA
     * @see SummaSearcher#CONF_WARMUP_MAXTIME
     */
    public long performWarmup() throws RemoteException;

    /**
     * Force reload of the underlying index. When the method returns, the reload
     * is finished and the underlying searchers are ready.
     * @throws RemoteException if the reload failed. If an exception is thrown,
     *                         the status of the underlying searchers might be
     *                         compromised.
     */
    public void reloadIndex() throws RemoteException;

    /**
     * @return the location of the current index or null if no index is opened.
     * @throws RemoteException if the location could not be retrieved.
     */
    public String getIndexLocation() throws RemoteException;


    /* Statistics */

    /**
     * @return the length of the queue with queries waiting to be resolved.
     *         This number includes currently running searches.
     * @throws RemoteException if the length could not be determined.
     * @see #getCurrentSearches
     */
    public int getQueueLength() throws RemoteException;

    /**
     * @return the number of currently running searches.
     * @throws RemoteException if the value could not be extracted.
     * @see #getQueueLength
     */
    public int getCurrentSearches() throws RemoteException;

    /**
     * @return the last received query. This is null, if no query has been
     *         received yet.
     * @throws RemoteException if the query could not be received. This should
     *                         never happen under normal circumstances.
     */
    public String getLastQuery() throws RemoteException;

    /**
     * @return the last response time in nanoseconds or -1 if no search has
     *         been performed yet.
     * @throws RemoteException if the last response time could not be retrieved.
     */
    public long getLastResponseTime() throws RemoteException;

    /**
     * @return the number of queries used for searches.
     * @throws RemoteException if the number could not be retrieved.
     */
    public long getQueryCount() throws RemoteException;

    /**
     * @return the total number of nanoseconds spend on searching.
     * @throws RemoteException if the number could not be retrieved.
     */
    public long getTotalResponseTime() throws RemoteException;

    /**
     * @return the average number of nanoseconds spend on a search.
     *         If the number cannot be calculated, Double.NaN is returned.
     * @throws RemoteException if the number could not be retrieved.
     */
    public double getAverageResponseTime() throws RemoteException;

    /**
     * Clear all statistics on query strings and response time.
     * @throws RemoteException if the data could not be cleared.
     */
    public void clearStatistics() throws RemoteException;
}
