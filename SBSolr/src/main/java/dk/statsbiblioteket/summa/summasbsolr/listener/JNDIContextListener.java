//$Id$
package dk.statsbiblioteket.summa.summasbsolr.listener;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class JNDIContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        if(System.getProperty("solr.home") == null) {

            try {
                Context initContext = new InitialContext();
                Context envContext = (Context) initContext.lookup("java:/comp/env");

                String home = (String) envContext.lookup("solr/home");

                if(home != null) {
                    System.setProperty("solr.home",home);
                }
            } catch (Exception e) {
                //ignore
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}
