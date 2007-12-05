/* $Id: SimpleParserTask.java,v 1.7 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.7 $
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
package dk.statsbiblioteket.summa.ingest.SimpleXML;import dk.statsbiblioteket.summa.ingest.ParserTask;import dk.statsbiblioteket.summa.ingest.Target;import dk.statsbiblioteket.summa.ingest.Ingest;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;

import org.xml.sax.ext.DefaultHandler2;

@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "hal")
public class SimpleParserTask extends ParserTask {
    protected SimpleParserTask(File f, Target target, Ingest in) {
        super(f, target, in);
    }

    protected DefaultHandler2 createContentHandler() {
        return new SimpleContentHandler(this,
                                         target.get("id_element"),
                                         target.get("record_element"));
    }
}
