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
package dk.statsbiblioteket.summa.support.suggest;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.support.api.SuggestResponse;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Interface for the storage responsible for persistence.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface SuggestStorage extends Configurable {

    /**
     * Open a storage at the specified location. If the location does not exist,
     * then create it and initialize.
     *
     * @param location Where the storage should be located.
     * @throws IOException If the open could not be completed.
     */
    void open(File location) throws IOException;

    /**
     * Return the location or the storage, as given in {@link #open}.
     *
     * @return The location or the storage, as given in {@link #open}.
     */
    File getLocation();

    /**
     * Close any connections and free memory. Can be called multiple times,
     * although that has no effect.
     */
    void close();

    /**
     * Create a SuggestResponse based on the given prefix.
     *
     * @param prefix     The prefix that should be satisfied for all suggestions.
     * @param maxResults The maximum number of suggestions to give.
     * @return A list of suggestions, wrapped in a response.
     * @throws IOException If the suggestion could not be handled.
     */
    SuggestResponse getSuggestion(String prefix, int maxResults) throws IOException;

    /**
     * Create a SuggestResponse containing the last {@code maxResult}
     * recorded suggestions within the last {@code ageSeconds} seconds.
     *
     * @param ageSeconds Number of seconds to look back
     * @param maxResults The maximum number of suggestions to give.
     * @return A list of suggestions, wrapped in a response.
     * @throws IOException If the suggestion could not be handled.
     */
    SuggestResponse getRecentSuggestions(int ageSeconds, int maxResults) throws IOException;

    /**
     * Add the given suggestion. If it already exists, update its queryCount
     * with 1 and set the number of hits. If it does not exist, create a new
     * entry with ueryCount set to 1.
     *
     * @param query The query to store.
     * @param hits  The number of hits that the query gives.
     * @throws IOException If the addition failed.
     */
    void addSuggestion(String query, int hits) throws IOException;

    /**
     * Add the given suggestion. If it already exists, set its queryCount and
     * the number of hits. If it does not exist, create a new entry with the
     * given parameters.
     *
     * @param query      The query to store.
     * @param hits       The number of hits that the query gives.
     * @param queryCount The number of times the suggestion has been added.
     * @throws IOException If the addition failed.
     */
    void addSuggestion(String query, int hits, int queryCount) throws IOException;

    /**
     * Extracts max stored suggestions from the underlying storage from start.
     * This is typically used for dumping the suggest data.
     *
     * @param start The position from which to start extraction.
     * @param max   The maximum number of suggestions to extract.
     * @return A list of suggestions. Each suggest-entry if represented as
     *         {@code query\thits\tqueryCount} where {@code \t} is tab.
     * @throws IOException If the suggestions could not be extracted.
     */
    ArrayList<String> listSuggestions(int start, int max) throws IOException;

    /**
     * Add a collection of suggestions to the underlying storage. This is
     * typically used for initial population.
     * </p><p>
     * The input format is the format of the data exported by
     * {@link #listSuggestions}. It is possible to leave out both queryCount and
     * hits, although it is highly recommended to provide hits. If no hits are
     * stated, the value 1 is used.
     *
     * @param suggestions A list of suggestions.
     * @throws IOException If the suggestions could not be added.
     */
    void addSuggestions(Iterator<String> suggestions) throws IOException;

    /**
     * Downloads and installs suggest-data from {@code in}.
     * </p><p>
     * Each suggest-entry if represented as
     * {@code query\thits\tqueryCount} in utf-8 where {@code \t} is tab.
     *
     * @param in Location of input data.
     * @throws IOException If the data could not be imported.
     */
    void importSuggestions(URL in) throws IOException;

    /**
     * Exports suggest-data to the file {@code out}.
     * </p><p>
     * Each suggest-entry will be represented as
     * {@code query\thits\tqueryCount} in utf-8 where {@code \t} is tab.
     *
     * @param out The output file for the export.
     * @throws IOException If the data could not be exported.
     */
    void exportSuggestions(File out) throws IOException;

    /**
     * Clears all suggestions.
     *
     * @throws IOException If the suggestions could not be cleared.
     */
    void clear() throws IOException;

    /**
     * Delete the specific suggestion from the Storage.
     *
     * @param suggestion The suggestion to delete.
     * @return True if suggestion was deleted or did not exists
     *         in storage.
     */
    boolean deleteSuggestion(String suggestion);
}
