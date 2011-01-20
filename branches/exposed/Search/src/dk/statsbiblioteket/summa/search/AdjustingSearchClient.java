/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;

/**
 * An extension of {@link SearchClient} that pipes requests and responses
 * through an {@link InteractionAdjuster}.
 * The properties for SearchClient must be specified as usual, along with the
 * properties for the InteractionAdjuster.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AdjustingSearchClient extends SearchClient {
    private static Log log = LogFactory.getLog(AdjustingSearchClient.class);

    private final InteractionAdjuster adjuster;

    public AdjustingSearchClient(Configuration conf) {
        super(conf);
        adjuster = new InteractionAdjuster(conf);
        log.debug("Created AdjustingSearchClient");
    }

    @Override
    public ResponseCollection search(Request request) throws IOException {
        log.debug(
            "Rewriting request, performing search and adjusting responses");
        Request adjusted = adjuster.rewrite(request);
        ResponseCollection responses = super.search(request);
        adjuster.adjust(adjusted, responses);
        return responses;
    }

    public InteractionAdjuster getAdjuster() {
        return adjuster;
    }
}
