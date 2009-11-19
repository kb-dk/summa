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

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;

/**
 * Default implementation of import and export of suggestions.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class SuggestStorageImpl implements SuggestStorage {
    private static Log log = LogFactory.getLog(SuggestStorageImpl.class);
    private static final int BATCH_SIZE = 1000;

    /*public void importSuggestions() throws IOException {
        File location = getLocation(IMPORT_FILE);

        if (!location.exists()) {
            throw new FileNotFoundException(String.format(
                    "Unable to import suggest data from '%s' as the file does "
                    + "not exist", location));
        }

        importSuggestions(location.toURI().toURL());
    }*/

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
        if (buffer.size() > 0) {
            addSuggestions(buffer.iterator());
        }
        lines.close();
        log.info(String.format(
                "Finished importing %d suggestions in %ds",
                counter, (System.currentTimeMillis() - importStart)/1000));
    }

    @Override
    public void exportSuggestions(File target) throws IOException {
        long exportStart = System.currentTimeMillis();
        target = target.getAbsoluteFile();
        log.info(String.format("Exporting suggestions to '%s'", target));
        if (target.isFile()) {
            log.info(String.format("Deleting old export '%s'", target));
            target.delete();
        } else if (target.isDirectory()) {
            throw new IOException("Export target is a directory: " + target);
        }

        // Create all parent dirs
        target.getParentFile().mkdirs();

        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(target), "utf-8"));
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
        log.info(String.format(
                "Exported %d suggestions to '%s' in %ds",
                exported, target,
                (System.currentTimeMillis() - exportStart)/1000));
    }    

}