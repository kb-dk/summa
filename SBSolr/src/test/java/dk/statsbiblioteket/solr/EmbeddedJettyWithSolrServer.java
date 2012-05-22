package dk.statsbiblioteket.solr;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

public class EmbeddedJettyWithSolrServer extends Thread {
	private Server server;
	private String serverUrl;
	
	/*
	 *Starts up a Jetty server running the SOLR instance. 
	 * 
	 */

	public EmbeddedJettyWithSolrServer (String solr_solr_home_dir, String solr_war_path , String context, int port){
	    this.setDaemon(true);   		  
		server = new Server(port);    	   
		WebAppContext webapp = new WebAppContext(solr_war_path,context);                      				                          
		
		//På et tidspunkt var det nødvendigt at tilføje denne til jetty-classpath. Slå dem til igen hvis fejlen opstår.
		//Det er en mockedint classe som fejler med ClassCastException (fordi klassen ikke er på classpath) 
    	//webapp.setExtraClasspath("test_libs/lucene-test-framework-4.0-SNAPSHOT.jar");
		server.setHandler(webapp);			             
		serverUrl="http://localhost:"+port+context;
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
