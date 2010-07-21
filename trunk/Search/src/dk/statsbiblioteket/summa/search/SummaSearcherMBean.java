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
package dk.statsbiblioteket.summa.search;

import java.rmi.RemoteException;

import dk.statsbiblioteket.util.qa.QAInfo;

/* TODO: consider adding notification on queries/response time if not too costly
http://java.sun.com/j2se/1.5.0/docs/guide/jmx/tutorial/essential.html#wp1053109
    */

/**
 * Exposes the underlying {@link dk.statsbiblioteket.summa.search.api.SummaSearcher} as an
 * MBean and extends with several control and statistic mechanisms.
 * @see dk.statsbiblioteket.summa.search.api.SummaSearcher
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface SummaSearcherMBean {
    /**
     * @return the maximum number of queued queries.
     * @throws RemoteException if the value could not be retrieved.
     * @see SummaSearcherImpl#CONF_SEARCH_QUEUE_MAX_SIZE
     * @see SearchNodeImpl#CONF_NUMBER_OF_CONCURRENT_SEARCHES
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
     * @see SummaSearcherImpl#CONF_SEARCH_QUEUE_MAX_SIZE
     * @see SearchNodeImpl#CONF_NUMBER_OF_CONCURRENT_SEARCHES
     */
    public void setSearchQueueMaxSize(int maxSize) throws RemoteException;

    /**
     * @return the timeout in milliseconds before a search is dropped.
     * @throws RemoteException if the value could not be retrieved.
     * @see SummaSearcherImpl#CONF_SEARCHER_AVAILABILITY_TIMEOUT
     * @see SummaSearcherImpl#CONF_SEARCHER_AVAILABILITY_TIMEOUT
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
     * @see SummaSearcherImpl#CONF_SEARCHER_AVAILABILITY_TIMEOUT
     */
    public void setSearcherAvailabilityTimeout(int ms) throws RemoteException;

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
     */
    public int getCurrentSearches() throws RemoteException;

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




