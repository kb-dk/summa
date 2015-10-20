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
package dk.statsbiblioteket.summa.ingest.split;

import dk.statsbiblioteket.summa.common.util.FutureInputStream;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;

/**
 * A stream representing content from an ARC file. The meta-data for the
 * stream can be queried at any time.
 * </p><p>
 * The ARC File Format 1.0 is supported, with the exception of GZIPped content.
 * The user of this stream is responsible for GUNZIPping the ARC file
 * beforehand.
 * @see <a hre="http://www.archive.org/web/researcher/ArcFileFormat.php">ArcFileFormat</a>
 */
// TODO: Implement this class
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ARCRecordStream extends FutureInputStream {
    private static Log log = LogFactory.getLog(ARCRecordStream.class);

    /**
     * The Stream is expected to contain an ARC record. The headers for the
     * Record will be extracted after which the position in the stream will
     * be at the start of the content.
     * </p><p>
     * When close is called or the end of the current ARC-record is reached,
     * the source is not closed.
     * @param source the Stream with ARC record content.
     */
    public ARCRecordStream(InputStream source) {
        super(source);
        setDoNotCloseSource(true);
        extractHeader();
    }
    
    private void extractHeader() {

    }


}
