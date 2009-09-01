/* $Id:$
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

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Default implementation of import and export of suggestions.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class SuggestStorageImpl implements SuggestStorage {
    private static Log log = LogFactory.getLog(SuggestStorageImpl.class);
    private static final int BATCH_SIZE = 1000;

    public void importSuggestions() throws IOException {
        File location = getLocation(IMPORT_FILE);
        if (!location.exists()) {
            throw new FileNotFoundException(String.format(
                    "Unable to import suggest data from '%s' as the file does "
                    + "not exist", location));
        }
        log.info(String.format("Importing suggestions from '%s'", location));

        BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream(location), "utf-8"));
        String line;
        ArrayList<String> buffer = new ArrayList<String>(BATCH_SIZE);
        int counter = 0;
        while ((line = in.readLine()) != null) {
            counter++;
            log.trace("Importing suggestion " + line);
            if ("".equals(line)) {
                continue;
            }
            buffer.add(line);
            if (buffer.size() == BATCH_SIZE) {
                addSuggestions(buffer);
                buffer.clear();
            }
        }
        if (buffer.size() > 0) {
            addSuggestions(buffer);
        }
        in.close();
        log.info(String.format("Finished importing %d suggestions", counter));
    }

    public void exportSuggestions() throws IOException {
        File location = getLocation(EXPORT_FILE);
        log.info(String.format("Exporting suggestions to '%s'", location));
        if (location.exists()) {
            log.info(String.format("Deleting old export '%s'", location));
            location.delete();
        }
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(location), "utf-8"));
        int exported = 0;
        while (true) {
            List<String> buffer = listSuggestions(exported, BATCH_SIZE);
            if (buffer.size() == 0) {
                break;
            }
            exported += buffer.size();
            for (String entry: buffer) {
                out.write(entry);
                out.write('\n');
            }
        }
        out.close();
        log.info(String.format("Exported %d suggestions to '%s'",
                               exported, location));
    }

    private File getLocation(String filename) throws IOException {
        if (getLocation() == null) {
            throw new IllegalArgumentException(
                    "No location. Please call open()");
        }
        return new File(
                getLocation().getParentFile(), filename).getAbsoluteFile();
    }

}