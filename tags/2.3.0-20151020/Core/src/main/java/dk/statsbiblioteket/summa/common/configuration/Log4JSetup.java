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
package dk.statsbiblioteket.summa.common.configuration;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class Log4JSetup implements ServletContextListener {
    private static boolean initialized = false;

    @SuppressWarnings({"CallToPrintStackTrace", "UseOfSystemOutOrSystemErr"})
    public synchronized static void ensureInitialized(ServletContextEvent sce) {
        if (Log4JSetup.initialized) {
            return;
        }
        //System.out.println("Setting up log4j");
        String confLocation = sce.getServletContext().getInitParameter("log4j.configuration");
        if (confLocation == null || confLocation.isEmpty()) {
            System.err.println("Expected the context parameter log4j.configuration to be defined");
            return;
        }
        confLocation = confLocation.replace("file:", ""); // To be compatible with the standard log4j setup syntax
        //System.out.println("Resolved log4j location " + confLocation);

        Logger logger = Logger.getLogger(Log4JSetup.class);
        try {
            LogManager.resetConfiguration();
            if (confLocation.endsWith(".xml")) {
                //System.out.println("The location ends with .xml so the DOM-loader will be used");
                DOMConfigurator.configure(confLocation);
            } else {
                Properties p = new Properties();
                //System.out.println("Loading properties");
                p.load(new FileInputStream(confLocation));
                //System.out.println("Got " + p.size() + " properties from " + confLocation + ". Configuring log4j");
                PropertyConfigurator.configure(p);
            }
            //System.out.println("Configured log4j from " + confLocation);
            logger.info("Configured log4j from " + confLocation);
        } catch (IOException e) {
            System.err.println("Exception configuring log4j with setup from " + confLocation);
            e.printStackTrace();
        }
        initialized = true;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ensureInitialized(sce);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // No-op
    }
}
