/* $Id: CSAXMLDirectoryDigester.java,v 1.7 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.7 $
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
package dk.statsbiblioteket.summa.ingest.CSA;

import dk.statsbiblioteket.summa.ingest.*;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;

/**
 * @deprecated  use {@link dk.statsbiblioteket.summa.ingest.SimpleXML}
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "hal")
public class CSAXMLDirectoryDigester extends Digester {

   
    protected ParserTask createParser(File f) {
        return new CSAParserTask(f,target,in);
    }
}
