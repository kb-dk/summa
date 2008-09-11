/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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

import java.util.Iterator;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * An ObjectFilter processes a triplet of Stream, Record and Document. Not all
 * parts of the triplet needs to be present. It is expected that most filters
 * will be of this type.
 * </p><p>
 * Streams, Records and Documents are pumped through the chain of filters by
 * calling {@link #pump()} on the last filter in the chain. It is up to the
 * individual filters to process the stream if present.
 * </p><p>
 * Important: pump()-implementations that extracts payloads from source-
 *            ObjectFilters are required to call close() on the extracted
 *            payloads.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface ObjectFilter extends Configurable, Filter, Iterator<Payload> {
    /**
     * This call might be blocking. If true is returned, it is expected that
     * a Payload can be returned by {@link #next()}. If false is returned,
     * it is guaranteed that no more Payloads can be returned by getNext().
     * </p><p>
     * If next() has returned null, hasNext must return false.
     * @return true if more Payloads are available, else false.
     */
    public boolean hasNext();
}


