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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.InputStream;
import java.io.IOException;

/**
 * Convenience wrapper that allows for a thread to block until the InputStream
 * has been closed. One use-case is a producer of InputStreams that needs to
 * wait for a previously produced stream to close before constructing a new one.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FutureInputStream extends InputStream {
    private static Log log = LogFactory.getLog(FutureInputStream.class);

    private InputStream is;
    private boolean closed = false;

    private boolean doNotCloseSource = false;


    private final Object waiter = new Object();

    public FutureInputStream(InputStream is) {
        this.is = is;
    }

    /**
     * Waits until close has been called.
     */
    public void waitForClose() {
        while (!closed) {
            synchronized (waiter) {
                try {
                    waiter.wait();
                } catch (InterruptedException e) {
                    log.debug("Received InterruptedException while waiting "
                              + "for close. Retrying waitForclose()");
                }
            }
        }
    }

    /**
     * Waits until close has been called for a maximum of timeout milliseconds.
     * @param timeout the maximum number of milliseconds to wait for close.
     * @return true if the stream is closed after waiting, else false.
     */
    public boolean waitForClose(long timeout) {
        long startTime = System.currentTimeMillis();
        while (!closed && (System.currentTimeMillis() - startTime) < timeout) {
            synchronized (waiter) {
                try {
                    waiter.wait(timeout);
                } catch (InterruptedException e) {
                    log.debug("Received InterruptedException while timeout-"
                              + "waiting for close. Retrying waitForclose("
                              + timeout + ")");
                }
            }
        }
        return closed;
    }

    /**
     * @param doNotCloseSource if set to true, the source InputStream will not
     * be closed when close is called in the FutureInputStream. The default is
     * false.
     */
    public void setDoNotCloseSource(boolean doNotCloseSource) {
        this.doNotCloseSource = doNotCloseSource;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            log.trace("Closing FutureInputStream");
            if (doNotCloseSource) {
                log.trace("Skipping close of source as doNotCloseSurce is set "
                          + "to true");
            } else {
                super.close();
            }
            closed = true;
        } else {
            log.debug("close() called on already closed. Ignoring");
        }
        synchronized (waiter) {
            waiter.notifyAll();
        }
    }

    /**
     * @return true if the InputStream has been closed.
     */
    public boolean isClosed() {
        return closed;
    }

    /* Direct delegations */

    @Override
    public int read() throws IOException {
        return is.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return is.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return is.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return is.skip(n);
    }

    @Override
    public int available() throws IOException {
        return is.available();
    }

    @Override
    public void mark(int readlimit) {
        is.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        is.reset();
    }

    @Override
    public boolean markSupported() {
        return is.markSupported();
    }
}

