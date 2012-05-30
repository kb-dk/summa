package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
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
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.CoreContainer;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.mortbay.jetty.webapp.WebAppContext;

public class SolrSearchConfigTest {
    
    String solrHome = "target/test-classes/support/solr_home1"; //data-dir (index) will be created here.
    String context = "/solr";
    int port = 8983;

    @Before
    public void setUp() throws Exception {
        System.setProperty("basedir", "."); //for logback
    }

    @Test
    public void testEmbeddedJettyWithSolr() throws Exception {
               
        //Start up webserver
        EmbeddedJettyWithSolrServer server=  new EmbeddedJettyWithSolrServer(solrHome, context, port);
        server.run();
        
        //Wrap the server with HTTPSolrServer      
        String url = server.getServerUrl();
        SolrServer server1 = new HttpSolrServer(url);
         //Thread.sleep(5000000L); //use to test server in browser. Or ingest data etc.

        //This is not found. There is no data in index
        SolrParams params = new SolrQuery("eeerere");
        QueryResponse response = server1.query(params);
        assertEquals(0L, response.getResults().getNumFound());

        
        //  Direct REST call. /edismax is defined to search in all summa-fields with boosts
        String httpResponse = callURL(url+"/edismax/?q=video");
        System.out.print(httpResponse);//No data
    }



    /*
    * Just a simple way to call a HTTP Rest service without Jersey
    *
    */
    private String callURL(String urlPath){
        StringBuilder response = new StringBuilder();
        try {
            URL url = new URL(urlPath);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(10000); //10 secs, but only called once.
            connection.setConnectTimeout(10000); //10 secs, but only called once.
            connection.connect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return response.toString();

    }


}