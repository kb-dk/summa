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

import java.io.IOException;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configurable;

/**
 * As opposed to the {@link StreamFilter}, the RecordFilter operates at
 * {@link Record}-level. It is expected that most filters will be of this
 * type.
 * </p><p>
 * Records are pumped through the chain of filters by calling
 */
public interface RecordFilter extends Configurable {
    /**
     * This call might be blocking. If true is returned, it is expected that
     * a Record can be returned by {@link #getNextRecord()}. If false is
     * returned, it is guaranteed that no more Records can be returned by
     * getNextRecord().
     * </p><p>
     * If getNextRecord() has returned null, hasMoreRecords must return false.
     * @return true if more Records are available, else false.
     * @throws IOException if a fatal error occured when checking for Records.
     *                     If an exception is thrown, the state of the
     *                     RecordFilter is considered unreliable.
     */
    public boolean hasMoreRecords() throws IOException;

    /**
     * Assign the source for this filter in the chain of filters.
     * Depending on the position in the filter chain, either this method,
     * {@link #setSource(RecordFilter)} or none of them will be called.
     * It is up to the implementation to use the correct source.
     * @param source the previous filter in the chain.
     */
    public abstract void setSource(StreamFilter source);

    /**
     * Assign the source for this filter in the chain of filters.
     * Depending on the position in the filter chain, either this method,
     * {@link #setSource(StreamFilter)} or none of them will be called.
     * It is up to the implementation to use the correct source.
     * @param source the previous filter in the chain.
     */
    public abstract void setSource(RecordFilter source);

    /**
     * Returns a Record. If no more Records can be summoned, null must be
     * returned.
     * @return a Record or null if there are no more Records.
     * @throws IOException if a fatal error occured when getting Record.
     *                     If an exception is thrown, the state of the
     *                     RecordFilter is considered unreliable.
     */
    public Record getNextRecord() throws IOException;

    /**
     * Closes this and any underlying Filters in a manner depending on
     * the state of success. If the state is not successfull, implementations
     * should take appropriate actions, such as marking source-material for
     * later re-ingestion.
     * @param success if true, ingest was finished successfully. If false, an
     *                error occured and the ingest was incomplete.
     */
    public abstract void close(boolean success);
}
