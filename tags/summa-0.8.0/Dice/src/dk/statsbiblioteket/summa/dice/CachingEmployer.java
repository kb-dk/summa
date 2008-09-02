/* $Id: CachingEmployer.java,v 1.2 2007/10/04 13:28:19 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:19 $
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
package dk.statsbiblioteket.summa.dice;

import dk.statsbiblioteket.summa.dice.caching.Cache;

import java.rmi.RemoteException;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 14/09/2006
 * Time: 11:02:32
 * {@link Employer} base class using an on-disk {@link Cache} to store
 * enqueued {@link Job}s.
 */
abstract public class CachingEmployer extends EmployerBase {


    public CachingEmployer(Config conf) throws RemoteException {
        super(conf);
    }

    // TODO: Implement CachingEmployer
}
