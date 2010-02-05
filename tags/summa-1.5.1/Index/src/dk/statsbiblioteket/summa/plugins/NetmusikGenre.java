/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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






