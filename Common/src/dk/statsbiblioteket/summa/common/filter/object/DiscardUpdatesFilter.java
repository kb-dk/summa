/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;

/**
 * Keeps track of the ids of processed Records. Records with new ids are passed
 * along while Records with already encountered ids are discarded.
 * </p><p>
 * The design scenario is batch ingesting of a huge amount of records with a
 * lot of updates. By using a FileReader in reverse order and this filter,
 * only the latest versions of the Records are processed.
 * </p><p>
 * It is expected that this filter will be extended in the future to contact a
 * Storage upon startup to determine the ids of already ingested Records.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DiscardUpdatesFilter implements ObjectFilter {
    private static Log log = LogFactory.getLog(DiscardUpdatesFilter.class);

    private ObjectFilter source;
    private Payload payload;
    private Set<String> encountered = new HashSet<String>(10000);

    @SuppressWarnings({"UnusedDeclaration"})
    public DiscardUpdatesFilter(Configuration conf) {
        // No configuration for this filter
    }

    public void setSource(Filter filter) {
        if (filter == null) {
            //noinspection DuplicateStringLiteralInspection
            throw new IllegalArgumentException("Source filter was null");
        }
        if (!(filter instanceof ObjectFilter)) {
            throw new IllegalArgumentException(
                    "Only ObjectFilters accepted as source. The filter "
                    + "provided was of class " + filter.getClass());
        }
        source = (ObjectFilter)filter;
    }

    public boolean pump() throws IOException {
        if (!hasNext()) {
            return false;
        }
        Payload payload = next();
        if (payload != null) {
            payload.close();
        }
        return hasNext();
    }

    public void close(boolean success) {
        log.trace("Closing DiscardUpdatesFilter");
        source.close(success);
    }

    public boolean hasNext() {
        checkPayload();
        return payload != null;
    }

    public Payload next() {
        checkPayload();
        try {
            return payload;
        } finally {
            payload = null;
        }
    }

    private void checkPayload() {
        while (payload == null && source.hasNext()) {
            Payload potential = source.next();
            if (potential != null) {
                if (!encountered.contains(potential.getId())) {
                    encountered.add(potential.getId());
                    payload = potential;
                }
            }
        }
    }

    public void remove() {
        log.warn("Removal not supported for DiscardUpdatesFilter");
    }
}
