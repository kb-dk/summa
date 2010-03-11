/**
 * Created: te 31-03-2009 12:20:27
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.common.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import dk.statsbiblioteket.util.Streams;

/**
 * Wraps an InputStream and an OutputStream, copying all bytes read from the
 * input to the output. If EOF is encountered or close is explicitely called,
 * the output stream will be closed.
 */
public class CopyingInputStream extends InputStream {
    private static Log log = LogFactory.getLog(CopyingInputStream.class);

    private InputStream in;
    private OutputStream out;
    private boolean flushOnClose;

    /**
     * @param in  the stream to read from.
     * @param out the stream to write a copy of the read bytes to.
     * @param flushOnClose if true, calling close will result in any remaining
     *                     bytes from the in stream to be copied to out.
     */
    public CopyingInputStream(InputStream in, OutputStream out,
                              boolean flushOnClose) {
        if (in == null) {
            throw new IllegalArgumentException("InputStream was null");
        }
        if (out == null) {
            throw new IllegalArgumentException("OutputStream was null");
        }
        this.in = in;
        this.out = out;
        this.flushOnClose = flushOnClose;
    }

    @Override
    public int read() throws IOException {
        int c = in.read();
        if (c == -1) {
            log.debug("EOF reached in read(), closing output stream");
            out.close();
        } else {
            out.write(c);
        }
        return c;
    }

    // Not strictly needed, but it's an easy speed-optimization
    @Override
    public int read(byte b[], int off, int len) throws IOException {
        int readLength = in.read(b, off, len);
        if (readLength == -1) {
            log.debug("EOF reached in read(buffer, offset, length), closing "
                      + "output stream");
            out.close();
        } else {
            out.write(b, off, readLength);
        }
        return readLength;
    }

    @Override
    public void close() throws IOException {
        log.debug("Closing input- and output-stream");
        if (flushOnClose) {
            long startTime = System.nanoTime();
            log.debug(
                    "Flushing remaining content from in stream to out stream");
            Streams.pipe(in, out);
            log.debug("Finished flushing in "
                      + (System.nanoTime() - startTime / 1000000.0) + "ms");
        }
        in.close();
        out.close();
    }
}
