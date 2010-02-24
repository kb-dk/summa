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
package dk.statsbiblioteket.summa.support.api;

/**
 * Search keys for the
 * {@link dk.statsbiblioteket.summa.support.didyoumean.DidYouMeanSearchNode}
 *
 * @author Mikkel Kamstrup Erlandsen <mailto:mke@statsbiblioteket.dk>
 * @since Feb 9, 2010
 */
public interface DidYouMeanKeys {
    /**
     * Did-You-Mean search query key.
     */
    public static final String SEARCH_QUERY =
                                           "summa.support.didyoumean.query";
    /**
     * Maximum number of results in a Did-You-Mean query key. 
     */
    public static final String SEARCH_MAX_RESULTS =
                                      "summa.support.didyoumean.maxresults";

}
