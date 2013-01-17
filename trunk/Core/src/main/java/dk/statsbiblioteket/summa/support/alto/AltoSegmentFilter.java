/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 *
 */
public class AltoSegmentFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(AltoSegmentFilter.class);

    private final HPAltoAnalyzer analyzer;

    public AltoSegmentFilter(Configuration conf) {
        super(conf);
        analyzer = new HPAltoAnalyzer(conf);
        log.info("Created HPAltoAnalyzer");
    }

    // TODO: As we produce multiple Records we cannot use the ObjectFilterImpl
    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
