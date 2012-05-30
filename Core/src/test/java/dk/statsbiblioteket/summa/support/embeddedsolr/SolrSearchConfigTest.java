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

    String solrWarPath = "target/test-classes/support/solr.war";
    String solrHome = "target/test-classes/support/solr_home"; //data-dir (index) will be created here.
    String context = "/solr";
    int port = 8983;

    @Before
    public void setUp() throws Exception {
        System.setProperty("basedir", ".");
    }


    @Test
    public void testEmbeddedJettyWithSolr() throws Exception {
        System.out.println("The problem is that IDEA has 'target/test-classes' in the class path and not 'target' "
                           + "itself where the war resides. We need to generate the war and put it in the classpath "
                           + "(i.e. target/test-classes or whereever resources are normally copied)");
        System.out.println(Thread.currentThread().getContextClassLoader().getResource(solrWarPath));
        assertTrue("The Solr war at " + solrWarPath + " should exist", new File(solrWarPath).exists());
        //Start up webserver
        EmbeddedJettyWithSolrServer server=  new EmbeddedJettyWithSolrServer(solrHome, solrWarPath, context, port);
        server.run();
        
        //Wrap the server with HTTPSolrServer
        //I think this is the prefered way we should do it.
        String url = server.getServerUrl();
        SolrServer server1 = new HttpSolrServer(url);
        // Thread.sleep(500000);
        //This is not found
        SolrParams params = new SolrQuery("eeerere");
        QueryResponse response = server1.query(params);
        assertEquals(0L, response.getResults().getNumFound());
        //"video" is found 3 times in my SOLR example index.
        params = new SolrQuery("*:*");

        response = server1.query(params);
        assertEquals(3L, response.getResults().getNumFound());

        //  Direct REST call
        String httpResponse = callURL(url+"/select/?indent=on&q=video&fl=name,id");
        System.out.print(httpResponse);
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