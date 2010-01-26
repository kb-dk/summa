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
package dk.statsbiblioteket.summa.common.filter.stream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.util.BitUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple reader that produces dummy output. Used for testing.
 */
public class DummyReader extends StreamFilter {
    private static Log log = LogFactory.getLog(DummyReader.class);

    public static final String CONF_BODY_COUNT = "dummyreader.bodycount";
    public static final String CONF_BODY_SIZE  = "dummyreader.bodysize";

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

    public void setSource(Filter source) {
        throw new UnsupportedOperationException("No source accepted");
    }

    public boolean pump() throws IOException {
        return read() != Payload.EOF;
    }

    public void close(boolean success) {
        log.info("Closing Dummyreader with success " + success);
    }

    private ArrayList<Integer> createContent() {
        ArrayList<Integer> content = new ArrayList<Integer>(bodySize + 8);
        byte[] theLong = BitUtil.longToBytes(bodySize);
        for (byte l: theLong) {
            content.add(0xff & l);
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
            return Payload.EOF;
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
        return Payload.EOF;
    }
}



