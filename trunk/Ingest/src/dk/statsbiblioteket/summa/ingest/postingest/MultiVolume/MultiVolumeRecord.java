/* $Id: MultiVolumeRecord.java,v 1.3 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.3 $
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


import java.io.*;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A Special Record with the following characteristics.
 *
 * Supports partial discovery where child records always can be added.
 * @deprecated Multi volume is now part of the Storage.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class MultiVolumeRecord implements Serializable {


    public enum RecordType implements Serializable{
        MAIN,SECTION,BIND,MAINORSECTION,SECTIONORBIND
    }

    static class Record implements Serializable{

        protected String id;
        protected RecordType type;
        protected byte[] content;



        protected Record(String id, RecordType type, byte[] content){
            this.id = id;
            this.type = type;
            this.content = content;
        }


        synchronized Record[] getChilds(){
            return IOMultiVolumeSQL.getInstance().getChilds(this.id);
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Record record = (Record) o;

            return !(id != null ? !id.equals(record.id) : record.id != null);

        }

        public int hashCode() {
            return (id != null ? id.hashCode() : 0);
        }


    }
}



