/* $Id: TestRecordIteratorFromSplitsThread.java,v 1.3 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:21 $
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
package dk.statsbiblioteket.summa.storage.lucene;

import dk.statsbiblioteket.summa.storage.RecordIterator;
import org.apache.log4j.Logger;

/**
 * TestRecordIteratorFromSplitsThread.
 * User: bam. Date: Jul 26, 2006.
 */
public class TestRecordIteratorFromSplitsThread extends Thread{

    private static final Logger log = Logger.getLogger(TestRecordIteratorFromSplitsThread.class);

    private RecordIterator iterator;
    private int id;

    TestRecordIteratorFromSplitsThread(RecordIterator iterator, int id) {
        this.iterator = iterator;
        this.id = id;
    }

    public void run() {
        int counter = 0;
        while (iterator.hasNext()) {
            String recName = iterator.next().getId();
            if (counter%30==0) {
                log.debug("id = " + id);
                log.debug("counter = " + counter);
                log.debug("recName = " + recName);
            }
            counter++;
        }
        log.debug("Thread with id '"+id+"' has run out...");
    }
}
