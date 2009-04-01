package dk.statsbiblioteket.summa.common.filter.object;

import junit.framework.TestCase;

import java.io.StringReader;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.filter.Payload;

/**
 * Test cases for {@link ScriptFilter}
 */
public class ScriptFilterTest extends TestCase {

    /**
     * Create a filter that will emit one payload for each of the supplied
     * records
     * @param records add a payload to the source for each record
     * @return an ObjectFilter with the requested payloads
     */
    public ObjectFilter getPayloadSource(Record... records) {
        PushFilter source = new PushFilter(records.length+1, 2048);

        for (int i = 0; i < records.length; i++) {
            Payload p = new Payload(records[i]);
            source.add(p);
        }
        source.signalEOF();

        return source;
    }

    public void testSimpleJS() throws Exception {
        ObjectFilter source = getPayloadSource(
                new Record("id1", "base1", "test content 1".getBytes()),
                new Record("id2", "base1", "test content 2".getBytes()));
        ObjectFilter filter = new ScriptFilter(new StringReader("print(payload.getId()+'\\n'); true"));
        filter.setSource(source);

        while (filter.pump()){;}
        //assertEquals(true, filter.pump());

    }

}
