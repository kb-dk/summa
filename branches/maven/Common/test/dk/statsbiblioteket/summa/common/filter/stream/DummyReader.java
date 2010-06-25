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
import java.util.Random;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.util.BitUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple reader that produces dummy output. Used for testing.
 */
public class DummyReader implements ObjectFilter {
    private static Log log = LogFactory.getLog(DummyReader.class);

    public static final String CONF_BODY_COUNT = "dummyreader.bodycount";
    public static final String CONF_BODY_SIZE  = "dummyreader.bodysize";

    private int bodyCount = 3;
    private int bodySize = 2000;

    private int bodysLeft;
    private Random random = new Random();
    private byte[] content;

    /**
     * Constructs a DummyReader with the properties<br />
     * {@link #CONF_BODY_COUNT} the number of bodies.<br />
     * {@link #CONF_BODY_SIZE} the size of the individual bodies.
     * @param configuration setup for the reader.
     */
    public DummyReader(Configuration configuration) {
        bodyCount = configuration.getInt(CONF_BODY_COUNT, bodyCount);
        bodySize =  configuration.getInt(CONF_BODY_SIZE,  bodySize);
        bodysLeft = bodyCount;
        content = createContent();

        log.info("Constructed DummyReader with bodyCount " + bodyCount
                 + " and bodySize " + bodySize + ", content '" + content + "'");
    }

    public void setSource(Filter source) {
        throw new UnsupportedOperationException("No source accepted");
    }

    public boolean pump() throws IOException {
        return hasNext();
    }

    public void close(boolean success) {
        log.info("Closing Dummyreader with success " + success);
    }

    private byte[] createContent() {
        byte[] size = BitUtil.longToBytes(bodySize);
        byte[] theLong = new byte[bodySize + 8];
        for(int i=0; i < 8; i++) {
            theLong[i] = size[i];
        }
        for(int i=8; i < bodySize+8; i++) {
            theLong[i] = (byte)random.nextInt(256);
        }
        return theLong;
    }

    /**
     * @return a payload with a random content.
      */
    @Override
    public Payload next() {
        if (content == null) {
            return null;
        }
        Payload p = new Payload(new Record("Dummy", "DummyBase", content));
        bodysLeft --;
        if(bodysLeft == 0) {
            content = null;
        }
        return p;
    }

    /**
     *
     * @return  true if more content.
     */
    @Override
    public boolean hasNext() {
        return (content != null);
    }

    @Override
    public void remove() {
        
    }
}



