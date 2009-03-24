package dk.statsbiblioteket.summa.common.lucene.analysis;

import java.io.FilterReader;
import java.io.Reader;
import java.io.IOException;

/**
 *
 */
public class LowerCasingReader extends FilterReader {

    /**
     * Creates a new filtered reader.
     *
     * @param in a Reader object providing the underlying stream.
     *
     * @throws NullPointerException if <code>in</code> is <code>null</code>
     */
    protected LowerCasingReader(Reader in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int codePoint = in.read();

        if (codePoint == -1) {
            return -1;
        }

        return Character.toLowerCase(codePoint);
    }

    @Override
    public int read(char[] buf, int offset, int count) throws IOException {
        int numRead = in.read(buf, offset, count);

        for (int i = offset+numRead; i >= offset; i--) {
            buf[i] = Character.toLowerCase(buf[i]);
        }
        
        return numRead;
    }

    /**
     * Prepare the reader for lower casing the character stream from another
     * reader.
     *
     * @param in the new reader to read character data from
     * @return always returns {@code this}
     */
    public LowerCasingReader setSource(Reader in) {
        this.in = in;
        return this;
    }
}
