package dk.statsbiblioteket.solr;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.mockintblock.MockFixedIntBlockPostingsFormat;
import org.apache.solr.util.AbstractSolrTestCase;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.mortbay.jetty.webapp.WebAppContext;
 
public class SolrSearchConfigTest {
	
	//String solrWarPath="/home/teg/Desktop/apache-solr-3.6.0/example/webapps/solr.war";
	//String solrHome= "/home/teg/Desktop/apache-solr-3.6.0/example/solr";
	
//String solrWarPath="/home/teg/Desktop/apache-solr-4.0/example/webapps/solr.war";
	String solrWarPath="/home/teg/workspace/Solr/solr.war";
	//String solrWarPath="/home/teg/workspace/summa/SBSolr/target/war/summa-sbsolr-1.8.0-20120502-trunk-SNAPSHOT.war";
	
	
	String solrHome= "/home/teg/workspace/Solr/solr";
	String context="/solr";
	int port = 8080;
    
          
    @Test
    public void testEmbeddedJettyWithSolr() throws Exception {
    
    	
    	//Start up webserver
        EmbeddedJettyWithSolrServer server=  new EmbeddedJettyWithSolrServer(solrHome,solrWarPath,context,port);           
        server.run();
                
        //Wrap the server with HTTPSolrServer
        //I think this is the prefered way we should do it.
         String url = server.getServerUrl();    	
    	 SolrServer server1 = new HttpSolrServer( url );
  	     
    	 //This is not found
    	 SolrParams params = new SolrQuery("XXXXXXXXXXXYUUUUUUUEEEEEe");
         QueryResponse response = server1.query(params);
         assertEquals(0L, response.getResults().getNumFound());
    	 //"video" is found 3 times in my SOLR example index.
         params = new SolrQuery("video");
         
         response = server1.query(params);
         assertEquals(3L, response.getResults().getNumFound());
    
        //  Direct REST call
        String httpResponse= callURL(url+"/select/?indent=on&q=video&fl=name,id");
        System.out.print(httpResponse);        
    }
    
       
    
    /*
     * Just a simple way to call a HTTP Rest service without Jersey
     * 
     */
    private String callURL(String urlPath){
        StringBuffer response = new StringBuffer();    
        BufferedReader reader = null;
        URL url = null;  
        try {         
            url = new URL(urlPath);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(10000); //10 secs, but only called once.
            connection.setConnectTimeout(10000); //10 secs, but only called once.
            connection.connect();
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

            String line = null;
            while ((line = reader.readLine()) != null)
            {        
            response.append(line);
            }
            reader.close();
     
        } catch (Exception e) {
           e.printStackTrace();
        }
        return response.toString();
        
    }
    
    
}