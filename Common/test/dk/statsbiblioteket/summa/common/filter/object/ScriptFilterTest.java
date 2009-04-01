package dk.statsbiblioteket.summa.common.filter.object;

import junit.framework.TestCase;

import java.io.StringReader;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;

/**
 * Test cases for {@link ScriptFilter}
 */
public class ScriptFilterTest extends TestCase {

    public PayloadBufferFilter prepareFilterChain(ObjectFilter filter,
                                                  Record... records) {
        // Set up the source filter
        PushFilter source = new PushFilter(records.length+1, 2048);

        for (int i = 0; i < records.length; i++) {
            Payload p = new Payload(records[i]);
            source.add(p);
        }
        source.signalEOF();

        // Set up the endpoint filter
        PayloadBufferFilter buf = new PayloadBufferFilter(
                                                Configuration.newMemoryBased());

        // Connect filters
        filter.setSource(source);
        buf.setSource(filter);

        return buf;

    }

    public void testRenameRecordIdJS() throws Exception {
        ObjectFilter filter = new ScriptFilter(
                   new StringReader("payload.getRecord().setId('processed');"));
        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "base1", "test content 1".getBytes()),
                       new Record("id2", "base1", "test content 2".getBytes()));

        // Flush the filter chain
        while (buf.pump()){;}

        assertEquals(2, buf.size());
        assertEquals("processed", buf.get(0).getRecord().getId());
        assertEquals("processed", buf.get(1).getRecord().getId());
    }

    public void testDropAllJS() throws Exception {
        ObjectFilter filter = new ScriptFilter(
                   new StringReader("allowPayload = false;"));
        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "base1", "test content 1".getBytes()),
                       new Record("id2", "base1", "test content 2".getBytes()));

        // Flush the filter chain
        while (buf.pump()){;}

        assertEquals(0, buf.size());
    }

    public void testDropOneJS() throws Exception {
        ObjectFilter filter = new ScriptFilter(
                   new StringReader(
                           "if (payload.getRecord().getId() == 'id1') { " +
                           "    allowPayload = false;" +
                           "}"));
        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "base1", "test content 1".getBytes()),
                       new Record("id2", "base1", "test content 2".getBytes()));

        // Flush the filter chain
        while (buf.pump()){;}

        assertEquals(1, buf.size());
        assertEquals("id2", buf.get(0).getRecord().getId());
    }

}
