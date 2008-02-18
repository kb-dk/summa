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
package dk.statsbiblioteket.summa.ingest;

import java.io.InputStream;

import dk.statsbiblioteket.summa.common.configuration.Configurable;

/**
 * On an abstract level, ingesting is just a chain of filters. In the beginning
 * of the chain is a filter that extracts raw data from a source (such as XML
 * files on the local file system), then follows chains that transforms or
 * ingests the data. Normally the chain will consist of StreamFilters followed
 * by {@link RecordFilter}s, but that is not a requirement.
 * </p><p>
 * The format for the top-level datastream is as follows:<br />
 * {@code
 * STREAM: (BODY)*EOF
 * BODY: Length(long) Content(Length bytes)
 * EOF: -1, as used in the standard InputStream.
 * }
 * </p><p>
 * An example of use could be a file loader that scans for multiple files,
 * reads the content sequentially and sends a stream made up of a body for
 * each file read. If the length of the content is unknown at load-time,
 * the filter must state -1 as the length. Readers must then continue to read
 * content until EOF is reached after which the filter is considered depleted.
 * </p><p>
 * An overall principle for filters is that they should only fail in case of
 * catastrophic events, such as out of memory. If the input is not as expected,
 * the filter should skip corrupt input (with appropriate logging of errors)
 * and attempt to  
 */
public abstract class StreamFilter extends InputStream implements Configurable {
    /**
     * EOF should be returned by read() when the filter is depleted.
     */
    public static final int EOF = -1;

    /**
     * Assign the source for this filter in the chain of filters.
     * @param source the previous filter in the chain.
     */
    public abstract void setSource(StreamFilter source);

    /**
     * Closes this and any underlying IngestFilters in a manner depending on
     * the state of success. If the state is not successfull, implementations
     * should take appropriate actions, such as marking source-material for
     * later re-ingestion.
     * </p><p>
     * Note: The implementation is responsible for calling close(success) on
     *       any underlysing filters.
     * @param success if true, ingest was finished successfully. If false, an
     *                error occured and the ingest was incomplete.
     */
    public abstract void close(boolean success);
}
