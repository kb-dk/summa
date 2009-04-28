/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.control.api;

import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A runtime exception thrown if there is an error loading a {@link Service}
 * or reading a service package file.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="The method needs Javadoc")
public class ServicePackageException extends ClientException {

    public ServicePackageException (Client client, String id, String msg) {
        super (client, msg + ", for , service '" + id + "'");
    }

    public ServicePackageException (Client client, String id, String msg,
                                    Throwable cause) {
        super (client, msg + ", for , service '" + id + "'", cause);
    }

}



