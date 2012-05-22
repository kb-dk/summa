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
		//System.setProperty("solr.solr.home", solr_solr_home_dir);   		   
		server = new Server(port);    	   
		WebAppContext webapp = new WebAppContext(solr_war_path,context);                      				                          
		
		//This is because jetty in unit-test for some reason loads classes from lucene-test-framework. TODO find out why and stop it.
		webapp.setExtraClasspath("/home/teg/Desktop/solr_release/trunk/lucene/build/test-framework/lucene-test-framework-4.0-SNAPSHOT.jar");
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
