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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Superclass for filters reading resources from a file system.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class FileSystemReader implements ObjectFilter {

    /**
     * The root folder to scan from.
     * </p><p>
     * This property must be specified.
     */
    public static final String CONF_ROOT_FOLDER =
        "summa.ingest.filereader.rootfolder";
    /**
     * Whether to perform a recursive scan or not (valid values: true, false).
     * </p><p>
     * This property is optional. Default is "true".
     */
    public static final String CONF_RECURSIVE =
            "summa.ingest.filereader.recursive";
    public static final boolean DEFAULT_RECURSIVE = true;
    /**
     * The file pattern to match.
     * </p><p>
     * This property is optional. Default is ".*\.xml".
     */
    public static final String CONF_FILE_PATTERN =
            "summa.ingest.filereader.filepattern";
    public static final String DEFAULT_FILE_PATTERN = ".*\\.xml";
    /**
     * The postfix for the file when it has been fully processed.
     * Setting this to null or the empty String means that no renaming is done.
     * </p><p>
     * This property is optional. Default is ".completed".
     */
    public static final String CONF_COMPLETED_POSTFIX =
            "summa.ingest.filereader.completedpostfix";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String DEFAULT_COMPLETED_POSTFIX = ".completed";
    /**
     * If true, scans are performed in reverse unicode order.
     * </p><p>
     * This property is optional. Default is false.
     */
    public static final String CONF_REVERSE_SORT =
            "summa.ingest.filereader.sort.reverse";
    public static final boolean DEFAULT_REVERSE_SORT = false;

    @Override
    public void remove() {
        throw new UnsupportedOperationException("No remove supported");
    }
}
