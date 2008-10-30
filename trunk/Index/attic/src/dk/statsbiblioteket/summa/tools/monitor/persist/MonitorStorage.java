/* $Id: MonitorStorage.java,v 1.3 2007/10/05 10:20:24 te Exp $
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
package dk.statsbiblioteket.summa.tools.monitor.persist;

import java.util.TreeSet;
import java.util.ArrayList;

import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class MonitorStorage {


    private int maxFracSize = 50000;  // number of objects in largest frac before spilt
    private int spiltFactor = 25000 ; //objects in frac after split;
    private String persistentDir;

    private ArrayList<String> fracments;

    private TreeSet _currentSet;
    private String _currentBound;








    public synchronized boolean isNew(MonitorObject o){
        return false;
    }

    public synchronized boolean add(MonitorObject o){
       return false;
    }

    private TreeSet getCurrent(MonitorObject o){
        int num = 0;
        for(String s : fracments){
            if (s.compareTo(o.id) >= 0){
                if (s.equals(_currentBound)){
                    return _currentSet;
                }
                return loadFromFile(num);
            }
            ++num;
        }
        // id larger than previous found id

      return  null;
    }


    private TreeSet loadFromFile(int num){
        return null;
    }



}



