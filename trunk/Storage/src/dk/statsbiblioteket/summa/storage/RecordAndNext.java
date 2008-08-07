/* $Id: RecordAndNext.java,v 1.4 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:22 $
 * $Author: te $
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
package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.Serializable;

/**
 * Tuple class for returning both a Record and a next value from Storage to RecordIterator.
 * Created by IntelliJ IDEA. User: bam. Date: Nov 17, 2005.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "bam, hal")
public class RecordAndNext implements Serializable {

    private final Record rec;
    private final boolean next;

    /**
     * Construct a RecordAndNext object with the given Record rec and boolean next.
     */
    public RecordAndNext(Record rec, boolean next) {
        this.rec = rec;
        this.next = next;
    }

    /**
     * Get the Record of this RecordAndNext.
     */
    public Record getRecord() {
        return rec;
    }
    /**
     * Get the boolean Next value of this RecordAndNext.
     */
    public boolean getNext() {
        return next;
    }
}
