/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.releasetest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.filter.RecordReader;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import junit.framework.TestCase;

/**
 * te forgot to document this class.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StorageTest extends TestCase {
    private static Log log = LogFactory.getLog(StorageTest.class);

    public void setUp () throws Exception {
        super.setUp();
        IngestTest.deleteOldStorages();
    }

    public void testRecordReader() throws Exception {
        Storage storage = IndexTest.fillStorage();

        MemoryStorage ms = new MemoryStorage();
        ms.put(RecordReader.CONF_START_FROM_SCRATCH, false);
        ms.put(RecordReader.CONF_BASE, "fagref");
        Configuration conf = new Configuration(ms);

        RecordReader reader = new RecordReader(conf);
        reader.clearProgressFile();
        reader.close(false);

        reader = new RecordReader(conf);
        assertTrue("The reader should have something", reader.hasNext());
        int pumps = 0;
        while (reader.pump()) {
            log.trace("Pump #" + ++pumps + " completed");
        }
        log.debug("Pumped at total of " + pumps + " times");
        reader.close(false);

        reader = new RecordReader(conf);
        assertTrue("The second reader should have something", reader.hasNext());
        int newPumps = 0;
        while (reader.hasNext()) {
            Payload payload = reader.next();
            log.trace("Pump #" + ++newPumps + " completed. Got " + payload);
        }
        log.debug("newPumps was " + newPumps);
        assertEquals("Pump-round 1 & 2 should give the same number",
                     pumps, newPumps);
        reader.close(true);

        reader = new RecordReader(conf);
        int thirdPumps = 0;
        while (reader.hasNext()) {
            Payload payload = reader.next();
            log.trace("Pump #" + ++thirdPumps + " completed. Got " + payload);
        }
        reader.close(true);
        assertEquals("The third reader should pump nothing", 0, thirdPumps);

        storage.close();
    }

}



