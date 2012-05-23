/* $Id: NetmusikGenre.java,v 1.4 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:23 $
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
package dk.statsbiblioteket.summa.plugins;

import java.util.Properties;
import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;

//TODO: remove this from here and stuff in netmusik
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class NetmusikGenre {


   private static NetmusikGenre _this = null;
   private static Properties prop;


    private static synchronized NetmusikGenre getInstance(){
       if (_this == null){
          ClassLoader loader = Thread.currentThread().getContextClassLoader();
          prop = new Properties();
           try {
               prop.loadFromXML(loader.getResourceAsStream("genremap.properties.xml"));
           } catch (IOException e) {
               e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
           }
       }
        return _this;
    }

    public static String getGenre(String genre_org){
        return  getInstance().prop.getProperty(genre_org, "");
    }
}

