/* $Id: IndexChangeEvent.java,v 1.2 2007/10/04 13:28:19 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:19 $
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
/**
 * Created: te 2007-08-28 11:26:01
 * CVS:     $Id: IndexChangeEvent.java,v 1.2 2007/10/04 13:28:19 te Exp $
 */
package dk.statsbiblioteket.summa.common.lucene.index;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.document.Document;

import java.util.List;

/**
 * A description of a change to a Lucene index.
 * @see IndexChanger, IndexChangeListener
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexChangeEvent {
    /**
     * The events that are observable on a Lucene index.<br />
     * documentsAdded:   Some documents were added.<br />
     * documentsRemoved: Some documents were removed.<br />
     * newIndex:         A new index was opened.
     */
    public static enum Event {documentsAdded, documentsRemoved, newIndex}

    private Event event;
    private List<Document> addedDocuments;
    private int[] removedDocIDs;
    IndexChanger sender;

    /**
     * Creates a documentsAdded event. The supplied documents must be all the
     * added documents. Note that the order of the documents matter and that
     * the ID of the first document is assumed to be equal to the total number
     * of documents in the index before addition.
     * @param sender         the IndexChanger that had the documents added.
     * @param addedDocuments the documents that was added.
     */
    public IndexChangeEvent(IndexChanger sender,
                            List<Document> addedDocuments) {
        this.sender = sender;
        event = Event.documentsAdded;
        this.addedDocuments = addedDocuments;
    }

    /**
     * Creates a documentsRemoved event. The supplied document IDs must be all
     * the removed documents.<br />
     * <strong>Important:</strong> The IDs of the documents refer to their
     * position in the index, before removal starts. Thus, if an index contains
     * 3 documents of which the last two are removed, the list of removed
     * document IDs will be [1, 2].
     * 
     * @param sender        the IndexChanger thad had documents removed.
     * @param removedDocIDs the IDs of the documents that was removed.
     */
    public IndexChangeEvent(IndexChanger sender, int[] removedDocIDs) {
        this.sender = sender;
        event = Event.documentsRemoved;
        this.removedDocIDs = removedDocIDs;
    }

    /**
     * Creates a newIndex event.
     * @param sender the IndexChanger that opened a new index.
     */
    public IndexChangeEvent(IndexChanger sender) {
        this.sender = sender;
        event = Event.newIndex;
    }

    /* Getters */

    public Event getEvent() {
        return event;
    }

    public List<Document> getAddedDocuments() {
        if (event != Event.documentsAdded) {
            throw new IllegalStateException("No documents were added. "
                                            + "The event was " + event
                                            + ", not " + Event.documentsAdded);
        }
        return addedDocuments;
    }

    public int[] getRemovedDocuments() {
        if (event != Event.documentsRemoved) {
            throw new IllegalStateException("No documents were removed. "
                                            + "The event was " + event
                                            + ", not " + Event.documentsAdded);
        }
        return removedDocIDs;
    }
}



