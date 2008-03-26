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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.stream.StreamFilter;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.document.Document;

/**
 * An ObjectFilter works on Record and Document level. It can be fed from either
 * a another ObjectFilter or a StreamFilter. It is expected that most filters
 * will be of this type.
 * </p><p>
 * Records and Documents  are pumped through the chain of filters by calling
 * {@link #next()} on the last filter in the chain.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface ObjectFilter extends Configurable, Filter,
                                      Iterator<ObjectFilter.Payload> {
    /**
     * The payload is the object that gets pumped through a filter chain.
     * It is a simple wrapper for Record and Document. Only one of these have
     * to be present.
     */
    public class Payload {
        private Record record;
        private Document document;

        public Payload(Record record) {
            this.record = record;
        }
        public Payload(Document document) {
            this.document = document;
        }
        public Payload(Record record, Document document) {
            this.record = record;
            this.document = document;
        }

        public Record getRecord() {
            return record;
        }
        public void setRecord(Record record) {
            if (record == null && document == null) {
                throw new IllegalStateException("Either record or document must"
                                                + " be defined");
            }
            this.record = record;
        }

        public Document getDocument() {
            return document;
        }
        public void setDocument(Document document) {
            if (document == null && record == null) {
                throw new IllegalStateException("Either record or document must"
                                                + " be defined");
            }
            this.document = document;
        }
    }

    /**
     * This call might be blocking. If true is returned, it is expected that
     * a Payload can be returned by {@link #next()}. If false is returned,
     * it is guaranteed that no more Payloads can be returned by getNext().
     * </p><p>
     * If getNext() has returned null, hasNext must return false.
     * @return true if more Payloads are available, else false.
     */
    public boolean hasNext();
}