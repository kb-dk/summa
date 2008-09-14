package dk.statsbiblioteket.summa.control.bundle;

import java.io.File;
import java.io.IOException;

/**
 * An exntension of the {@link BundleRepository} class that allows writing
 * of bundles back into the repository
 */
public interface WritableBundleRepository extends BundleRepository {

    public static final String CONF_BUNDLE_DIR =
                                     "summa.control.repository.storage.bundle";

    public static final String CONF_API_DIR =
                                       "summa.control.repository.storage.api";

    /**
     * Upload a file to the repository. It is not the repository's
     * responsability to check the validity of the imported bundles. That duty
     * is the caller's.
     * <p></p>
     * The bundle will be installed locally in the repository in the directory
     * specified in {@link #CONF_BUNDLE_DIR}.
     * <p></p>
     * If the installation succeeds the repository should delete {@code bundle}
     * @param bundle the file to install in the repository
     * @return {@code true} if the bundle was installed. {@code false} if the
     *         bundle is already installed
     * @throws IOException on communication errors
     */
    public boolean installBundle (File bundle) throws IOException;

    /**
     * Install an API file in the repository's API file container.
     * <p></p>
     * The API file will be installed locally in the repository in the directory
     * specified in {@link #CONF_API_DIR}.
     * <p></p>
     * In contrast to {@link #installBundle(java.io.File)} the installation
     * will <i>not</i> delete {@code apiFile} on success.
     *  
     * @param apiFile the API file to install in the repository
     * @return {@code true} if the API file was installed. {@code false} if the
     *         file was already installed
     * @throws IOException on communication errors
     */
    public boolean installApi (File apiFile) throws IOException;
}



