/* $Id: Rename.java,v 1.3 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.3 $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: Rename.java,v 1.3 2007/10/05 10:20:24 te Exp $
 */
package dk.statsbiblioteket.summa.preingest;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Trivial filter that changes the extension for the files.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "te")
public class Rename implements IngestFilter {
    private static Log log = LogFactory.getLog(Rename.class);

    public void applyFilter(File input, Extension ext, String encoding) {
        File dest = new File(input.getAbsolutePath() + File.pathSeparator
                             + ext);
        if (!input.renameTo(dest)) {
            log.error("Could not rename the file \"" + input + "\" to \""
                      + dest + "\"");
        }
    }
}
