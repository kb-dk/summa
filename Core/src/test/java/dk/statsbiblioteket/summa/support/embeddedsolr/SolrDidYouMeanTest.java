package dk.statsbiblioteket.summa.support.embeddedsolr;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.index.IndexController;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.search.SearchNode;
import dk.statsbiblioteket.summa.support.solr.SolrManipulator;
import dk.statsbiblioteket.summa.support.solr.SolrSearchNode;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse.Suggestion;
import org.apache.solr.common.params.SolrParams;
import org.junit.Before;
import org.junit.Test;

public class SolrDidYouMeanTest {
    
    String solrHome = "support/solr_home1"; //data-dir (index) will be created here.  
    EmbeddedJettyWithSolrServer server;
    SolrServer solrServer;
    
    
    @Before
    public void setUp() throws Exception {
        System.setProperty("basedir", "."); //for logback
        server =  new EmbeddedJettyWithSolrServer(Resolver.getFile(solrHome).toString());
        server.run();
        solrServer = new HttpSolrServer(server.getServerUrl());
    }

    @Test
    public void testFacets() throws Exception {

    	//Data:
    	//3 books by Thomas Egense, 1 located at Matematik, 2 at Statsbiblioteket. 2 from 2011, 1 from 2012
    	//2 books by Toke Eskildsen, 2 located at Statsbiblioteket, 1 from 2012, 1 from 2010
        //Facets defined for year,location_normalised,author_normalised
    	String[] files = new String[]{
    			"support/solr_test_documents/didyoumeantest.xml",
    		
    	};
        SolrServerUnitTestUtil.indexFiles(files);
             	    	                
        SolrQuery query = new SolrQuery("gense");
        query.setParam("spellcheck","true");
        query.setParam("spellcheck.dictionary","summa_spell");        
        query.setParam("spellcheck.count","5");
        
        
        QueryResponse response = solrServer.query(query);
        Map<String, Suggestion> suggestionMap = response.getSpellCheckResponse().getSuggestionMap();
        
        
                
        //  Direct REST call. /edismax is defined to search in all summa-fields with boosts        
        String httpResponse = SolrServerUnitTestUtil.callURL(server.getServerUrl()+"/select/?spellcheck=true&spellcheck.dictionary=summa_spell&spellcheck.extendedResults=true&spellcheck.maxCollationsTries=5&spellcheck.collate=true&spellcheck.collateExtendedResults=true&spellcheck.count=5&q=gense");
     System.out.println(httpResponse);
        
    }

    
       
}