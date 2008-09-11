/* $Id: RemoteCache.java,v 1.2 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:17 $
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
package dk.statsbiblioteket.summa.dice.caching;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.Remote;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Direct extension of the {@link Cache} interface casting
 * all {@link IOException}s as {@link RemoteException}s.
 *
 * This interface allows a {@link RemoteCacheService} to be exported
 * over RMI.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment = "Needs javadocs")
public interface RemoteCache<E> extends Cache<E>, Remote {

    public long startPut () throws RemoteException;

    public void putPart (E part, long id) throws RemoteException;

    public void endPut (long id) throws RemoteException;

    public long lookup (long id) throws RemoteException;

    public E readPart (long handle) throws RemoteException;

    public void close (long handle) throws RemoteException;

    public String getDataURL (long id) throws RemoteException;
}



