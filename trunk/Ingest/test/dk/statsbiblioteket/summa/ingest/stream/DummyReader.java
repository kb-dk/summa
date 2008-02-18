/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.ingest.stream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.ingest.StreamFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple reader that produces dummy output. Used for testing.
 */
public class DummyReader extends StreamFilter {
    private static Log log = LogFactory.getLog(DummyReader.class);

    public static final String CONF_BODY_COUNT = "DummyReader.bodyCount";
    public static final String CONF_BODY_SIZE  = "DummyReader.bodySize";

    private int bodyCount = 2;
    private int bodySize = 2000;

    private int bodysLeft;
    private Random random = new Random();
    private ArrayList<Integer> content;

    /**
     * Constructs a DummyReader with the properties<br />
     * {@link #CONF_BODY_COUNT} the number of bodies.<br />
     * {@link #CONF_BODY_SIZE} the size of the individual bodies.
     * @param configuration setup for the reader.
     */
    public DummyReader(Configuration configuration) {
        bodyCount = configuration.getInt(CONF_BODY_COUNT, bodyCount);
        bodySize =  configuration.getInt(CONF_BODY_SIZE,  bodySize);
        bodysLeft = bodyCount-1;
        content = createContent();

        log.info("Constructed DummyReader with bodyCount " + bodyCount
                 + " and bodySize " + bodySize);
    }

    public void close(boolean success) {
        log.info("Closing Dummyreader with success " + success);
    }

    private ArrayList<Integer> createContent() {
        ArrayList<Integer> content = new ArrayList<Integer>(bodySize + 8);
        long length = bodySize;
        int[] theLong = new int[8];
        for (int i = 7 ; i >= 0 ; i--) {
            theLong[i] = (byte)length;
            length >>>= 8;
        }
        for (int l: theLong) {
            content.add(l);
        }
        while (content.size() < bodySize + 8) {
            content.add(random.nextInt(256));
        }
        return content;
    }

    /**
     * @return a random byte.
     * @throws IOException if a read is performed when there are no bytes left.
     */
    public int read() throws IOException {
        if (content == null) {
            throw new IOException("Attempting read on depleted Dummy Reader");
        }
        if (content.size() > 0) {
            // Inside a body
            return content.remove(0);
        }
        if (bodysLeft > 0) {
            // Change to next body
            bodysLeft--;
            content = createContent();
            return content.remove(0);
        }
        // No more bodies or bytes
        //noinspection AssignmentToNull
        content = null;
        return EOF;
    }
}
