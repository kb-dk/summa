/* $Id: OAIParserTask.java,v 1.10 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.10 $
 * $Date: 2007/10/05 10:20:24 $
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
package dk.statsbiblioteket.summa.ingest.OAI;

import java.io.*;
import java.net.URL;

import org.xml.sax.ext.DefaultHandler2;
import dk.statsbiblioteket.summa.ingest.ParserTask;
import dk.statsbiblioteket.summa.ingest.Ingest;
import dk.statsbiblioteket.summa.ingest.Target;
import dk.statsbiblioteket.summa.ingest.stream.XMLSplitterFilter;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Parses a Target, delegates content handling to the {@link dk.statsbiblioteket.summa.ingest.OAI.PMHContentHandler}.<br>
 * @deprecated in favor of {@link XMLSplitterFilter} as XMLSplitterFilter is
 * now supports namespaces.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "hal")
public class OAIParserTask extends ParserTask {
    /**
     * The property key in {@link Target} for the home URL.
     */
    public static final String HOME_URL = "homeURL";

    /**
     * Creates a {@link PMHContentHandler} with the home URL and name from
     * the given target.
     * @param f
     * @param target
     * @param in
     */
    public OAIParserTask(File f,  Target target, Ingest in) {
        super(f, target, in);
    }

    protected DefaultHandler2 createContentHandler() {
        return new PMHContentHandler(target.get(HOME_URL),
                                               target.getName(), this);
    }
}



