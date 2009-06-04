package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.util.List;
import java.util.ArrayList;

/**
 * Buffers all incominf payloads for later retrieval
 */
public class PayloadBufferFilter extends ObjectFilterImpl {

   List<Payload> payloads;

    public PayloadBufferFilter(Configuration conf) {
        super(conf);
        payloads = new ArrayList<Payload>();
    }

    protected boolean processPayload(Payload payload) throws PayloadException {
        payloads.add(payload);
        return true;
    }

    public Payload get(int idx) {
        return payloads.get(idx);
    }

    public int size() {
        return payloads.size();
    }
}
