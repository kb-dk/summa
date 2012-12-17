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

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.util.Properties;

//TODO: remove this from here and stuff in netmusik
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class NetmusikGenre {
    /** Singleton instance. */
    private static NetmusikGenre thisInstance = null;
    /** Properties used to create this instance. */
    private static Properties prop;


    /**
     * Return the singleton instance of the NetmusikGenre class.
     * @return The singleton instance of this class.
     */
    @SuppressWarnings({"DefaultFileTemplate", "CallToPrintStackTrace"})
    private static synchronized NetmusikGenre getInstance(){
        if (thisInstance == null){
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            prop = new Properties();
            try {
                prop.loadFromXML(loader.getResourceAsStream("genremap.properties.xml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return thisInstance;
    }

    /**
     * Return the genre property.
     * @param genreOrg The genre.
     * @return The genre property.
     */
    public static String getGenre(String genreOrg){
        getInstance();
        return prop.getProperty(genreOrg, "");
    }
}
