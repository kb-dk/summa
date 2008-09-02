/* $Id: TestWorker.java,v 1.3 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.3 $
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
package dk.statsbiblioteket.summa.dice;

import dk.statsbiblioteket.summa.dice.Config;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 30/08/2006
 * Time: 14:02:01
 * To change this template use File | Settings | File Templates.
 */
public class TestWorker extends CachingWorker {

    public TestWorker (Config conf) {
        super (conf);
    }

    protected Job processJob (Job job) {
        Iterator parts = ((Iterable)job.getData()).iterator();

        ArrayList newData = new ArrayList();

        while (parts.hasNext()) {
            newData.add (parts.next() + " - PROCESSED");
        }

        return new Job (newData, job.getHints(), job.getName());
    }
}
