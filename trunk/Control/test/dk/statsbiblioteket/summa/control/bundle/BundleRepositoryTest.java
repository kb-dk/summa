package dk.statsbiblioteket.summa.control.bundle;

import junit.framework.*;
import dk.statsbiblioteket.summa.control.bundle.BundleSpecBuilder;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;

import dk.statsbiblioteket.util.Strings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
                                            ClientConnection.REPOSITORY_CLASS_PROPERTY,
                                            BundleRepository.class,
                                            URLRepository.class);
        repo = Configuration.create(repositoryClass, conf);
    }

    public void tearDwon () throws Exception {

    }

    public void testList () throws Exception {
        String filter = ".*";

        List<String> list = repo.list (filter);
        System.out.println ("Repo list for '" + filter + "':\n\t"
                            + Strings.join (list, "\n\t"));

        filter = "storage";
        list = repo.list (filter);
        System.out.println ("Repo list for '" + filter + "':\n\t"
                            + Strings.join (list, "\n\t"));
    }
}
