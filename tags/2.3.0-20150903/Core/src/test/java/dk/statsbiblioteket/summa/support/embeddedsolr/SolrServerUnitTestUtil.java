package dk.statsbiblioteket.summa.support.embeddedsolr;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.index.IndexController;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.support.solr.SolrManipulator;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
    public static void populateSolrHome(File solr_home) throws IOException{
        String solrHome_merged=solr_home.getAbsolutePath()+SOLR_HOME_SUFFIX;
        File  solrHome_merged_dir = new File(solrHome_merged);
        FileUtils.deleteDirectory(solrHome_merged_dir); //Clean up first
        FileUtils.copyDirectory(solr_home, solrHome_merged_dir);
    }

    /**
     *  Index all document specified in the String[].
     *
     */
    public static void indexFiles(String[] files) throws Exception{
        ObjectFilter data = getDataProvider(files);
        ObjectFilter indexer = getIndexer();
        indexer.setSource(data);
        for (int i = 0 ; i < files.length ; i++) {
            indexer.next();
        }
        indexer.close(true);
    }

    /**
     *  Index all given documents.
     *
     */
    public static void indexDocuments(String[] docs) throws Exception {
        ObjectFilter data = getDataProviderFromDocs(docs);
        ObjectFilter indexer = getIndexer();
        indexer.setSource(data);
        while (indexer.hasNext()) {
            indexer.next();
        }
        indexer.close(true);
    }


    private static ObjectFilter getDataProvider(String[] files) throws IOException {
        List<Payload> samples = new ArrayList<>(files.length);
        for (int i = 0 ; i < files.length ; i++) {
            Record record = new Record(
                //Doc1, doc 2. etc.. Actually in test this will be the real recordId in the index
                "doc" + i, "test_base", Resolver.getUTF8Content(files[i]).getBytes("utf-8"));
            Payload payload = new Payload(record);
            payload.getRecord().setDeleted(false);
            samples.add(payload);
        }
        return new PayloadFeederHelper(samples);
    }

    private static ObjectFilter getDataProviderFromDocs(String[] docs) throws IOException {
        List<Payload> samples = new ArrayList<>(docs.length);
        for (int i = 0 ; i < docs.length ; i++) {
            Record record = new Record("doc" + i, "test_base", docs[i].getBytes("utf-8"));
            Payload payload = new Payload(record);
            payload.getRecord().setDeleted(false);
            samples.add(payload);
        }
        return new PayloadFeederHelper(samples);
    }

    private static IndexController getIndexer() throws IOException {
        Configuration controllerConf = Configuration.newMemoryBased(IndexController.CONF_FILTER_NAME, "testcontroller");
        Configuration manipulatorConf = controllerConf.createSubConfigurations(IndexControllerImpl.CONF_MANIPULATORS, 1).get(0);
        manipulatorConf.set(IndexControllerImpl.CONF_MANIPULATOR_CLASS, SolrManipulator.class.getCanonicalName());
        manipulatorConf.set(SolrManipulator.CONF_ID_FIELD, IndexUtils.RECORD_FIELD); // 'id' is the default ID field for Solr
        return new IndexControllerImpl(controllerConf);
    }

}
