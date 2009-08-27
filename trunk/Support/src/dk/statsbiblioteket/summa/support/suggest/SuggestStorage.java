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
package dk.statsbiblioteket.summa.support.suggest;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.support.api.SuggestResponse;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Interface for the storage responsible for persistence
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract interface SuggestStorage extends Configurable {

    public static final String IMPORT_FILE = "suggest_in.dat";
    public static final String EXPORT_FILE = "suggest_out.dat";

    /**
     * Open a storage at the specified location. If the location does not exist,
     * then create it and initialize.
     * @param location where the storage should be located.
     * @throws java.io.IOException if the open could not be completed.
     */
    void open(File location) throws IOException;

    /**
     * @return the location og the storage, as given in {@link #open}.
     */
    File getLocation();

    /**
     * Close any connections and free memory. Can be called multiple times,
     * although that has no effect.
     */
    void close();

    /**
     * Create a SuggestResponse based on the given prefix.
     * @param prefix the prefix that should be satisfied for all suggestions.
     * @param maxResults the maximum number of suggestions to give.
     * @return a list of suggestions, wrapped in a response.
     * @throws java.io.IOException if the suggestion could not be handled.
     */
    SuggestResponse getSuggestion(String prefix, int maxResults) throws
                                                                 IOException;

    /**
     * Add the given suggestion. If it already exists, update its queryCount
     * with 1 and set the number of hits. If it does not exist, create a new
     * entry with queryCount set to 1.
     * @param query the query to store.
     * @param hits the number of hits that the query gives.
     * @throws java.io.IOException if the addition failed.
     */
    void addSuggestion(String query, int hits) throws IOException;

    /**
     * Add the given suggestion. If it already exists, se its queryCount and the
     * number of hits. If it does not exist, create a new entry with the given
     * parameters.
     * @param query the query to store.
     * @param hits the number of hits that the query gives.
     * @param queryCount the number of times the suggestion has been added.
     * @throws java.io.IOException if the addition failed.
     */
    void addSuggestion(String query, int hits, int queryCount) throws
                                                               IOException;

    /**
     * Extracts max stored suggestions from the underlying storage from start.
     * This is typically used for dumping the suggest data.
     * @param start the position from which to start extraction.
     * @param max   the maximum number of suggestions to extract.
     * @return a list of suggestions. Each suggest-entry if represented as
     *        {@code query\thits\tqueryCount} where {@code \t} is tab.
     * @throws IOException if the suggestions could not be extracted.
     */
    ArrayList<String> listSuggestions(int start, int max) throws IOException;

    /**
     * Batch-add suggestions to the underlying storage. This is typically used
     * for initial population.
     * </p><p>
     * The input format is the format of the data exported by
     * {@link #listSuggestions}. It is possible to leave out both queryCount and
     * hits, although it is highly recommended to provide hits. If no hits are
     * stated, the value 1 is used.
     * @param suggestions a list of suggestions.
     * @throws IOException if the suggestions could not be added.
     */
    void addSuggestions(ArrayList<String> suggestions) throws IOException;

    /**
     * Imports suggest-data from the file "suggest_in.dat" in the folder for the
     * suggest storage.
     * </p><p>
     * Each suggest-entry if represented as
     * {@code query\thits\tqueryCount} in utf-8 where {@code \t} is tab.
     * @throws IOException if the data could not be imported.
     */
    void importSuggestions() throws IOException;

    /**
     * Exports suggest-data to the file "suggest_out.dat" in the folder for the 
     * suggest storage.
     * </p><p>
     * Each suggest-entry will be represented as
     * {@code query\thits\tqueryCount} in utf-8 where {@code \t} is tab.
     * @throws IOException if the data could not be exported.
     */
    void exportSuggestions() throws IOException;

    /**
     * Clears all suggestions.
     * @throws IOException if the suggestions could not be cleared.
     */
    void clear() throws IOException;
}
