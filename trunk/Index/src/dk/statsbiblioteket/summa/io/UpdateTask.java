/* $Id: UpdateTask.java,v 1.2 2007/10/04 13:28:20 te Exp $
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

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: hal
 * Date: May 17, 2006
 * Time: 11:08:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class UpdateTask extends IOTask {


    String content;
    String url;

    public UpdateTask(IndexService service, String name, String content, String xslturl) {
        super(service, name);
        this.content = content;
        this.url = xslturl;
    }

    public void run() {
        try {
            service.updateXMLRecord(content, name, url);
        } catch (IOException e) {
            log.error(e);
        } catch (IndexServiceException e) {
            log.error(e);
        }
    }

}
