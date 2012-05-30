package dk.statsbiblioteket.summa.support.embeddedsolr;

import java.io.File;

import dk.statsbiblioteket.summa.common.configuration.Resolver;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

public class EmbeddedJettyWithSolrServer extends Thread {
	private Server server;
	private String serverUrl;
	//private static String default_solrWarPath = "Core/target/test-classes/support/solr.war";
	private static String default_solrWarPath = "support/solr.war";

	/*
	 *Starts up a Jetty server running the SOLR instance. 
	 * 
	 */
	public EmbeddedJettyWithSolrServer (String solr_solr_home_dir, String solr_war_path , String context, int port) throws Exception{
	    this.setDaemon(true);   		  

        solr_war_path = Resolver.getFile(solr_war_path).toString();
	    if (!new File(solr_war_path ).exists()){
	    	throw new Exception("The Solr war at " + solr_war_path  + " should exist. Run 'mvn clean install -DskipTests' in the core-module");	
	    }
	    
	    System.setProperty("solr.solr.home", solr_solr_home_dir);
	    
	    server = new Server(port);    	   
		WebAppContext webapp = new WebAppContext(solr_war_path,context);                      				                          
						
		//Hvis solr fejler under opstart med "mockedint classcast exception" er det fordi denne jar mangler på jetty's classpath.
		//Det er uklart hvorfor dette bliver aktiveret når man kører unittest     	
    	 webapp.setExtraClasspath(Thread.currentThread().getContextClassLoader().getResource(
             "support/lucene-test-framework-4.0.jar").getFile().toString());
		
		server.setHandler(webapp);			             
		serverUrl="http://localhost:"+port+context;
	}

	/*
	 *Starts up a Jetty server running the SOLR instance with default solr.war location 
	 * 
	 */
	public EmbeddedJettyWithSolrServer (String solr_solr_home_dir, String context, int port) throws Exception{
	      this(solr_solr_home_dir,default_solrWarPath,context,port);
	}
	
	@Override
	public void run()  {
		try{
			server.start();
		}
		catch(Exception e){
			e.printStackTrace();
		}

	}

	public String getServerUrl(){
	return serverUrl;
	}
	
	public boolean isStarted(){
		
		return server.isStarted();
	}


}
