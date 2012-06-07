package dk.statsbiblioteket.summa.support.embeddedsolr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class SolrServerUnitTestUtil {

	/*
	 * SOLR_HOME_SUFFIX will be added to the SOLR_HOME directory.
	 */
    public static final String SOLR_HOME_SUFFIX="_merged";
	
	
	/*
    * Just a simple way to call a HTTP Rest service without Jersey
    *
    */
	public static String callURL(String urlPath){
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
	

	/**
	 *  Finds the absolute file path to a given ressource.   
	 */
	
	public static String resolve(String type, String path) throws FileNotFoundException {
		if (new File(path).exists()) {
			return path;
		}
		File resolved = Resolver.getFile(path);
		if (resolved == null) {
			throw new FileNotFoundException("Unable to locate the " + type + " path '" + path + "'");
		}
		return resolved.toString();
	}
	
	/**
	 * Merge the two directories solrhome and solrhome_default to solrhome_merged. Copy solrhome_default first 
	 * so the solrhome files are not replaced. 
	 * 
	 */
	public static void  populateSolrHome(File solr_home, File solr_home_default) throws IOException{			
		   String solrHome_merged=solr_home.getAbsolutePath()+SOLR_HOME_SUFFIX;
		    File  solrHome_merged_dir = new File(solrHome_merged);			
			FileUtils.deleteDirectory(solrHome_merged_dir); //Clean up first
		    FileUtils.copyDirectory(solr_home_default, solrHome_merged_dir);		
		    FileUtils.copyDirectory(solr_home, solrHome_merged_dir);					
		}

}
