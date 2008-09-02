/* $Id: AddTask.java,v 1.2 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:20 $
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
package dk.statsbiblioteket.summa.io;

import dk.statsbiblioteket.summa.index.IndexService;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;

/**
 * @deprecated as part of the old workflow.
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "hal")
public class AddTask extends IOTask {

    String url;
    String content;

    public AddTask(IndexService service, String xslturl, String content, String name) {
        super(service, name);
        this.url = xslturl;
        this.content = content;
    }


    public void run() {
        try {
            this.service.addXMLRecord(content, name, url);
        } catch (IOException e) {
            log.error(e);
        } catch (IndexServiceException e) {
            log.error(e);
        }
    }
}
