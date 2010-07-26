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
package dk.statsbiblioteket.summa.common.index;

import java.rmi.RemoteException;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Thrown in case of index-related problems, such as un-available or corrupted
 * indexes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class IndexException extends RemoteException {
    private static final long serialVersionUID = 168436846L;
    private String indexLocation = null;

    public IndexException(String s) {
        super(s);
    }
    public IndexException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs an exception where the location of the index is specified.
     * The location will be automatically appended to the error message and
     * is explicitely retrievable with {@link #getIndexLocation()}.
     * This is the recommended constructor.
     * @param s             error message.
     * @param indexLocation the location for the index which caused the
     *                      exception.
     * @param cause         the cause, if any, of the exception.
     */
    public IndexException(String s, String indexLocation, Throwable cause) {
        super(s + " (index at '" + indexLocation + "')", cause);
        this.indexLocation = indexLocation;
    }

    /**
     * @return the index-location for the index which caused the exception.
     */
    public String getIndexLocation() {
        return indexLocation;
    }
}




