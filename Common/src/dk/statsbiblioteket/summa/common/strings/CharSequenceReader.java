package dk.statsbiblioteket.summa.common.strings;

import java.io.Reader;
import java.io.IOException;

/**
 * A {@link Reader} implementation that can read characters from any object
 * implementing the {@link CharSequence} interface. This includes commonly
 * used structures such as {@link StringBuilder}s and {@link StringBuffer}s
 */
public class CharSequenceReader extends Reader {

    private CharSequence seq;
    private int cursor;

    /**
     * Create a new reader that reads characters from {@code seq}
     * @param seq the CharacterSequence to read characters from
     */
    public CharSequenceReader(CharSequence seq) {
        if (seq == null) {
            throw new NullPointerException("Input CharSequence is null");
        }

        reset(seq);
    }

    /**
     * Prepare the reader to read from a new CharSequence. You are allowed to
     * call this method even though the reader has been closed.
     *
     * @param seq the new CharacterSequence to read from
     * @return always returns {@code this}
     */
    public CharSequenceReader reset(CharSequence seq) {
        if (seq == null) {
            throw new NullPointerException("Can not reset internal CharSequence"
                                           + " to null");
        }

        this.seq = seq;
        cursor = 0;

        return this;
    }

    @Override
    public int read(char[] chars, int offset, int count) throws IOException {
        if (seq == null) {
            throw new IOException("Reading from a closed CharSequenceReader");
        }

        int len = seq.length();
        int start = cursor;

        while(cursor < len
              && cursor-start < count
              && cursor-start+offset < chars.length) {

            chars[cursor-start+offset] = seq.charAt(cursor);
            cursor++;
        }

        if (start == cursor) {
            return -1;
        }

        return cursor - start;
    }

    @Override
    public int read() {
        if (cursor == seq.length()) {
            return -1;
        }

        char next = seq.charAt(cursor);
        cursor++;
        return (int)next;
    }

    @Override
    public void close() throws IOException {
        // Allow to free the memory of the CharSequence
        seq = null;
    }
}
