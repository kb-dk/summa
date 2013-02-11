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

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of import and export of suggestions.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public abstract class SuggestStorageImpl implements SuggestStorage {
    private static Log log = LogFactory.getLog(SuggestStorageImpl.class);
    /**
     * Default batch size.
     */
    private static final int BATCH_SIZE = 1000;
    /**
     * Milli seconds in a second.
     */
    private static final int MILLI_IN_SECOND = 1000;

    @Override
    public void importSuggestions(URL in) throws IOException {
        log.info(String.format("Importing suggestions from '%s'", in));
        long importStart = System.currentTimeMillis();
        InputStream inStream = in.openConnection().getInputStream();
        InputStreamReader reader = new InputStreamReader(inStream);
        BufferedReader lines = new BufferedReader(reader);
        String line;
        ArrayList<String> buffer = new ArrayList<String>(BATCH_SIZE);
        int counter = 0;
        while ((line = lines.readLine()) != null) {
            counter++;
            log.trace("Importing suggestion " + line);
            if ("".equals(line)) {
                continue;
            }
            buffer.add(line);
            if (buffer.size() == BATCH_SIZE) {
                addSuggestions(buffer.iterator());
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) {
            addSuggestions(buffer.iterator());
        }
        lines.close();
        log.info(String.format("Finished importing %d suggestions in %ds",
                               counter, (System.currentTimeMillis() - importStart) / MILLI_IN_SECOND));
    }

    @Override
    public void exportSuggestions(File target) throws IOException {
        long exportStart = System.currentTimeMillis();
        target = target.getAbsoluteFile();
        log.info(String.format("Exporting suggestions to '%s'", target));
        if (target.isFile()) {
            log.info(String.format("Deleting old export '%s'", target));
            if (!target.delete()) {
                log.warn("Unable to delete '" + target + "'");
            }
        } else if (target.isDirectory()) {
            throw new IOException("Export target is a directory: " + target);
        }

        // Create all parent dirs
        if (!target.getParentFile().mkdirs()) {
            log.warn("Unable to create folder '" + target.getParentFile() + "'");
        }

        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(target), "utf-8"));
        int exported = 0;
        while (true) {
            List<String> buffer = listSuggestions(exported, BATCH_SIZE);
            if (buffer.isEmpty()) {
                break;
            }
            exported += buffer.size();
            for (String entry : buffer) {
                out.write(entry);
                out.write('\n');
            }
        }
        out.close();
        log.info(String.format("Exported %d suggestions to '%s' in %ds",
                               exported, target, (System.currentTimeMillis() - exportStart) / MILLI_IN_SECOND));
    }
}
