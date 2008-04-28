/* $Id: IOMultiVolume.java,v 1.4 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:23 $
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
package dk.statsbiblioteket.summa.ingest.postingest.MultiVolume;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The multi volume merger needs its own persistent layer, where information about structure between records can be accumulated during ingest.<br>
 * This interface defines the core set of functionality needed to support such a work flow.
 * @deprecated Multi volume is now part of the Storage.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public interface IOMultiVolume {

    /**
     * Creates an array containing all records where<code>MultiVolumeRecord.RecordType == MultiVolumeRecord.RecordType.MAIN</code>.<br>
     *
     * @param base          the target base to retrive MAIN records from.
     * @return              an array of MAIN records.
     */
    public MultiVolumeRecord.Record[] getAllMainRecords(String base);

    /**
     * Adds a parent child relation between two records based on records ID's.<br>
     * Implementors needs to address model consistency, to avoid nonsense assignments like adding relations between two MAIN type records.
     * @param parentID                  the parent in this relationship.
     * @param childID                   the child in this relationship.
     * @param child                     the type of child.
     * @param base                      the target base the records belong to.
     */
    public void addChild(String parentID, String childID, MultiVolumeRecord.RecordType child, String base);

    /**
     * Adds a parent child relation, inserting the relation in a specified position.<br>
     * Implementors needs to address model consistency, to avoid nonsense assignments like adding relations between two MAIN type records.
     *
     * @param parentID                  the parent in this relationship.
     * @param childID                   the child in this relationship.
     * @param child                     the type of child.
     * @param childPosition             the position of the parent child reference in the inherited hiracy level.
     * @param base                      the target base the records belong to.
     */
    public void addChild(String parentID, String childID, MultiVolumeRecord.RecordType child, int childPosition, String base);

    /**
     * Updates a record with new information.<br>
     * Information can be type, content and state
     *
     * @param id                        the record id
     * @param type                      the type of record
     * @param data                      the content
     * @param isDeleted                 if true, the record is deleted
     */
    public void updateRecord(String id, MultiVolumeRecord.RecordType type, byte[] data, boolean isDeleted);

    /**
     * Adds a record. Adding a record is simply a marker insertion - use {@link IOMultiVolume#updateRecord} to set content.<br>
     * @param id                        the record id.
     * @param type                      the type of the record.
     * @param base                      the target base the record belongs to.
     */
    public void addRecord(String id, MultiVolumeRecord.RecordType type, String base);

    /**
     * Creates an array of all Child records in the first deeper level of the complete record hierarchy.
     * @param id                        the current record id
     * @return                          an array of child records.
     */
    public MultiVolumeRecord.Record[] getChilds(String id);

}
