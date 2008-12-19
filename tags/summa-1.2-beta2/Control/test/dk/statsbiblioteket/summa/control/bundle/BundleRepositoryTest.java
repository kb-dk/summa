package dk.statsbiblioteket.summa.control.bundle;

import junit.framework.*;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.bundle.BundleRepository;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;

import dk.statsbiblioteket.util.Strings;

import java.util.List;


/**
 *
 */
public class BundleRepositoryTest extends TestCase {

    BundleRepository repo;
    Configuration conf;

    public void setUp () throws Exception {
        conf = new Configuration (new FileStorage("configuration.xml"));

        Class<? extends BundleRepository> repositoryClass =
                                    conf.getClass(
                                            ClientConnection.CONF_REPOSITORY_CLASS,
                                            BundleRepository.class,
                                            URLRepository.class);
        repo = Configuration.create(repositoryClass, conf);
    }

    public void tearDwon () throws Exception {

    }

    public void testList () throws Exception {
        Configuration localConf = Configuration.newMemoryBased(
                                         BundleRepository.CONF_REPO_ADDRESS,
                                         "file:///tmp/summatest");
        BundleRepository remoteRepo = new RemoteURLRepositoryServer (localConf);

        String filter = ".*";

        List<String> list = repo.list (filter);
        System.out.println ("Repo list for '" + filter + "':\n\t"
                            + Strings.join (list, "\n\t"));

        filter = "storage";
        list = repo.list (filter);
        System.out.println ("Repo list for '" + filter + "':\n\t"
                            + Strings.join (list, "\n\t"));
    }

    public void testRemoteRepo () throws Exception {



    }
}



