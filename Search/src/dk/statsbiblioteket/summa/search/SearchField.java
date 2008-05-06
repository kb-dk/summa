/* $Id: SearchField.java,v 1.5 2007/10/11 12:56:24 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/11 12:56:24 $
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
package dk.statsbiblioteket.summa.search;

import java.util.*;

import dk.statsbiblioteket.summa.common.lucene.index.OldIndexField;
import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class SearchField {
    private String key;
    private Map <String, String> alias;
    private Map <String, Set<OldIndexField>> subkeys;


    public SearchField(String key){
        key = key;
        alias = new HashMap<String, String>(5);
        subkeys = new HashMap<String, Set<OldIndexField>>(10);
    }

    String addAlias(String key, String lang){
        return alias.put(lang, key);
    }

    boolean addSubKey(String lang, OldIndexField key){
        if (subkeys.containsKey(lang)){
          return  subkeys.get(lang).add(key);
        } else {
          Set<OldIndexField> s = new HashSet<OldIndexField>();
          s.add(key);
          subkeys.put(lang, s);
          return true;
        }
    }







}
