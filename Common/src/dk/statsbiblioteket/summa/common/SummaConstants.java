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
package dk.statsbiblioteket.summa.common;

import java.io.*;
import java.util.Properties;

import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Loads the current MAVEN_REVISION_NUMBER from property file. The property-file
 * is updated by maven
 * 
 * @author Thomas Egense <mailto:teg@statsbiblioteket.dk>
 */
@SuppressWarnings({"FieldCanBeLocal"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "teg")
public class SummaConstants {
    private static Log log = LogFactory.getLog(SummaConstants.class);

	private static String VERSION = "pom.version";
	private static String propertyFileName = "common.properties";
	
    public static String getVersion() {
        //URL url = ClassLoader.getSystemResource(propertyFileName);
        String properties;
        try {
            properties = Resolver.getUTF8Content(propertyFileName);
        } catch (IOException e) {
            String message = "Unknown version number. (" + propertyFileName
                             + " not found on classpath)";
            log.warn(message, e);
    	    return message;
        }

    	Properties p = new Properties();
    	try {
            p.load(new StringReader(properties));
    	}
    	catch (IOException e) {
            String message =  "Unknown version number. (" + propertyFileName
                              + ") found, but reading gave IOException";
            log.warn(message, e);
            return message;
    	}

    	String version = p.getProperty(VERSION);
    	if (version == null) {
    		String message = "Unknown version number. 'version' property not "
                             + "found in " + "(" + propertyFileName + ")";
            log.warn(message);
            return message;
    	}
    	    	
    	return version;    	    	    	    	
    }

}
