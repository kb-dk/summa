package dk.statsbiblioteket.summa.control.bundle;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.control.api.bundle.BundleRepository;
import dk.statsbiblioteket.util.FileAlreadyExistsException;
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 */
public class BundleSpecBuilder {
    private Log log = LogFactory.getLog(BundleSpecBuilder.class);

    private String mainJar;
    private String mainClass;
    private String bundleId;
    private String instanceId;
    private String description;
    private boolean autoStart;
    private Bundle.Type bundleType;
    private Configuration properties;
    private List<String> jvmArgs;
    private HashSet<String> fileSet;
    private HashSet<String> apiSet;

    public BundleSpecBuilder () {
        bundleType = Bundle.Type.SERVICE;
        properties = Configuration.newMemoryBased();
        jvmArgs = new ArrayList<String>();
        fileSet = new HashSet<String>();
        apiSet = new HashSet<String>();
        autoStart = false;

    }

    public String getMainJar () { return mainJar; }
    public void setMainJar (String mainJar) { this.mainJar = mainJar; }
    
    public String getMainClass () { return mainClass; }
    public void setMainClass (String mainClass) { this.mainClass = mainClass; }

    public String getBundleId () { return bundleId; }
    public void setBundleId (String bundleId) { this.bundleId = bundleId; }

    public String getInstanceId () { return instanceId; }
    public void setInstanceId (String instanceId) { this.instanceId = instanceId; }

    public Bundle.Type getBundleType () { return bundleType; }
    public void setBundleType (Bundle.Type bundleType) { this.bundleType = bundleType; }

    public String getDescription () { return description; }
    public void setDescription (String description) { this.description = description; }

    public boolean isAutoStart () { return autoStart; }
    public void setAutoStart (boolean autoStart) { this.autoStart = autoStart; }

    public String getProperty (String name) { return properties.getString(name); }
    public void setProperty (String name, String value) { properties.set (name, value); }
    public void clearProperty (String name) { properties.purge(name); }
    public Configuration getProperties () { return properties; }

    public void addFile (String file) { fileSet.add(file); }
    public void removeFile (String file) { fileSet.remove(file); }
    public boolean hasFile (String file) { return fileSet.contains(file); }
    public Collection<String> getFiles () { return fileSet; }

    public void addApi (String jarFile) { apiSet.add(jarFile); }
    public void removeApi (String jarFile) { apiSet.remove(jarFile); }
    public boolean hasApi (String jarFile) { return apiSet.contains(jarFile); }
    public Collection<String> getApi () { return apiSet; }

    public List<String> getJvmArgs () { return jvmArgs; }
    public void addJvmArg (String arg) { jvmArgs.add(arg); }

    public void write (OutputStream out) throws IOException {
        if (out == null) {
            throw new NullPointerException("output argument is null");
        }
        write(new OutputStreamWriter(out));
    }

    public void write (Writer writer) throws IOException {
        log.trace("write called");
        if (writer == null) {
            throw new NullPointerException("writer argument is null");
        }

        PrintWriter out = new PrintWriter(writer);
        try {
            out.println("<bundle>");

            if (instanceId != null) {
                out.println ("  <instanceId>" + instanceId + "</instanceId>");
            }

            if (bundleId != null) {
                out.println ("  <bundleId>" + bundleId + "</bundleId>");
            }

            if (mainJar != null) {
                out.println ("  <mainJar>" + mainJar + "</mainJar>");
            }

            if (mainClass != null) {
                out.println ("  <mainClass>" + mainClass + "</mainClass>");
            }

            out.println("  <autoStart>" + autoStart + "</autoStart>");

            if (description != null) {
                out.println ("  <description>" + description + "</description>");
            }

            if (!jvmArgs.isEmpty()) {
                for (String arg : jvmArgs) {
                    out.println("  <jvmArg>" + arg + "</jvmArg>");
                }
            }

            if (properties.getStorage().size() != 0) {
                for (Map.Entry<String, Serializable> entry : properties) {
                    out.println ("  <property name=\"" + entry.getKey() + "\""
                                      + " value=\"" + entry.getValue() +"\"/>");
                }
            }

            if (!apiSet.isEmpty()) {
                out.println ("  <publicApi>");
                for (String file : apiSet) {
                    out.println ("    <file>" + file + "</file>");
                }
                out.println ("  </publicApi>");
            }

            if (!fileSet.isEmpty()) {
                out.println ("  <fileList>");
                for (String file : fileSet) {
                    out.println ("    <file>" + file + "</file>");
                }
                out.println ("  </fileList>");
            }

            out.println("</bundle>");
        } finally {
            out.close();
        }
        log.trace("Finished write");
    }

    /**
     * Write the bundle spec to a file, any existing file will be overwritten.
     * @param outputFile file to write to
     * @throws IOException if there is an error writing the file
     */
    public void write (File outputFile) throws IOException {
        if (outputFile == null) {
            throw new NullPointerException("outputFile argument is null");
        }

        if (outputFile.isDirectory()) {
            outputFile = new File (outputFile, getFilename());
        }

        log.trace ("Writing spec to file: " + outputFile);

        write (new FileOutputStream(outputFile));
    }

    /**
     * <p>Write the bundle spec to a given directory. The filename of the output
     * file is decided based upon the spec type set by {@link #setBundleType}.
     * </p>
     *
     * <p>The name of the output file is decided by the {@link #getFilename()}
     * method. If the directory {@code dir} does not exist it will be created.
     * </p>
     *
     * <p><i>Important:</i> Any existing files will be overwritten.</p>
     *
     * @param dir parent dir of the bundle spec to write
     * @return A {@link File} pointing at the output file
     * @throws FileAlreadyExistsException if the output dir is a regular file
     */
    public File writeToDir (File dir) throws IOException {
        log.trace("writeToDir called");
        if (dir == null) {
            throw new NullPointerException("Directory argument is null");
        }

        if (dir.isFile()) {
            throw new FileAlreadyExistsException(dir);
        }

        dir.mkdirs();

        File target = new File (dir, getFilename());
        write (target);
        log.trace("Finished writeToDir");
        return target;
    }

    /**
     * Get the suggested file name for this buidler. Ie {@code client.xml}
     * or {@code service.xml} for client and service bundles accordingly.
     *
     * @return filename as per bundle type
     */
    public String getFilename () {
        log.trace("Getting file name for bundle type '" + bundleType + "'");
        if (Bundle.Type.CLIENT == bundleType) {
            return "client.xml";
        } else if (Bundle.Type.SERVICE == bundleType) {
            return "service.xml";
        } else {
            return bundleType.name().toLowerCase() + ".xml";
        }

    }

    /**
     * Open a new builder from a file. If the file name is either
     * {@code client.xml} or {@code service.xml} the bundle type
     * will be set accordingly.
     * <p></p>
     * If {@code file} is a directory the method will look for a
     * {@code service.xml} or {@code client.xml} inside it.
     * @param file file to read bundle spec from
     * @return a bundle spec ready for manipulations
     * @throws IOException if there is an error reading the file
     */
    public static BundleSpecBuilder open(File file) throws IOException {
        BundleSpecBuilder builder;
        File desc;

        if (file.isDirectory()) {
            File serviceDesc = new File (file, "service.xml");
            File clientDesc = new File (file, "client.xml");

            if (serviceDesc.isFile()) {
                builder = open(new FileInputStream(serviceDesc));
                desc = serviceDesc;
            } else if (clientDesc.isFile()) {
                builder = open(new FileInputStream(clientDesc));
                desc = clientDesc;
            } else {
                throw new BundleLoadingException("No client.xml or service.xml "
                                                 + "inside " + file);
            }
        } else if (file.isFile()) {
            builder = open(new FileInputStream(file));
            desc = file;
        } else {
            throw new BundleLoadingException("No such file or directory "
                                             + file);
        }

        String filename = desc.toString();
        if (filename.endsWith("client.xml")) {
            builder.setBundleType(Bundle.Type.CLIENT);
        } else if (filename.endsWith("service.xml")) {
            builder.setBundleType(Bundle.Type.SERVICE);
        }

        return builder;
    }

    /**
     * Read a bundle spec from an input stream.
     * @param in inout stream to read from
     * @return a bundle spec ready for manipulations
     */
    public static BundleSpecBuilder open(InputStream in) {
        BundleSpecBuilder builder = new BundleSpecBuilder();
        builder.read(in);
        return builder;
    }

    /**
     * Read the contents of a bundle spec into this bundle spec buidler
     * @param in stream to read spec from
     */
    public void read (InputStream in) {
        log.trace("read called");
        DocumentBuilder xmlParser;
        Document doc;
        Element docElement;
        NodeList children;

        try {
            log.trace("read: Creating XML parser");
            xmlParser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch(ParserConfigurationException e){
            throw new BundleLoadingException("Error creating DocumentBuilder", e);
        }

        try {
            log.trace("read: Parsing input");
            doc = xmlParser.parse(in);
        } catch (Exception e) {
            throw new BundleLoadingException("Error parsing bundle spec ", e);
        }
        log.trace("read: calling getDocumentElement");
        docElement = doc.getDocumentElement();
        if (!docElement.getTagName().equals("bundle")) {
            throw new BundleFormatException("Bundle spec has root element '"
                                          + docElement.getTagName()
                                          + "', expected 'bundle'");
        }

        log.trace("read: Getting children nodes");
        children = docElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if ("mainJar".equals(node.getNodeName())) {
                mainJar = node.getTextContent();
            } else if ("mainClass".equals(node.getNodeName())) {
                mainClass = node.getTextContent();
            } else if ("autoStart".equals(node.getNodeName ())) {
                autoStart = Boolean.parseBoolean (node.getTextContent()); 
            } else if ("description".equals(node.getNodeName())) {
                description = node.getTextContent();
            } else if ("bundleId".equals(node.getNodeName())) {
                bundleId = node.getTextContent();
            } else if ("instanceId".equals(node.getNodeName())) {
                instanceId = node.getTextContent();
            } else if ("fileList".equals(node.getNodeName())) {
                readFilelist (node);
            } else if ("publicApi".equals(node.getNodeName())) {
                readPublicApi (node);
            } else if ("jvmArg".equals(node.getNodeName())) {
                addJvmArg(node.getTextContent());
            } else if ("property".equals(node.getNodeName())) {
                String name = node.getAttributes().getNamedItem("name").getNodeValue();
                String value = node.getAttributes().getNamedItem("value").getNodeValue();

                if (name == null) {
                    name = "!unset!";
                } else if (value == null) {
                    value="!unset!";
                }
                setProperty(name, value);
            } else {
                log.debug("Unknown node '" + node.getNodeName()
                          + "' with content '" + node.getTextContent() + "'");
            }
        }
        log.trace("Finished read");
    }

    private void readFilelist(Node node) {
        log.trace("readFileList called");
        NodeList files = node.getChildNodes();

        // Check that each file in fileList exists
        for (int i = 0; i < files.getLength(); i++) {

            // Skip garbage text and comments
            if (files.item(i).getNodeType() == Node.TEXT_NODE ||
                files.item(i).getNodeType() == Node.COMMENT_NODE) {
                continue;
            }

            // Assert that we only have file nodes
            if (!"file".equals(files.item(i).getNodeName())) {
                throw new BundleFormatException("Illegal child '"
                + files.item(i).getNodeName() + "' of fileList");
            }

            String file = files.item(i).getTextContent();
            addFile(file);
        }
        log.trace("Finished readFileList");
    }

    private void readPublicApi(Node node) {
        log.trace("readPublicApi called");
        NodeList files = node.getChildNodes();

        // Add all <file> nodes of <publicApi> to the public API set
        for (int i = 0; i < files.getLength(); i++) {

            // Skip garbage text and comments
            if (files.item(i).getNodeType() == Node.TEXT_NODE ||
                files.item(i).getNodeType() == Node.COMMENT_NODE) {
                continue;
            }

            // Assert that we only have file nodes
            if (!"file".equals(files.item(i).getNodeName())) {
                throw new BundleFormatException("Illegal child '"
                + files.item(i).getNodeName() + "' of publicApi");
            }

            String file = files.item(i).getTextContent();
            addApi(file);
        }
        log.trace("Finished readPublicApi");
    }

    /**
     * Make sure that all files specified in the bundle's {@code fileList}
     * can be found in {@code bundleDir}.
     * <p/>
     * It will also be affirmed that the file specified as mainJar is mentioned
     * in the fileList.
     *
     * @param bundleDir the directory to check for files in
     * @throws BundleFormatException if one of the files specified in the
     *                               bundle's fileList is not found in
     *                               {@code bundleDir}
     */
    public void checkFileList (File bundleDir) {
        log.trace("checkFileList called");
        // Check that each file in fileList exists
        for (String filename : fileSet) {
            File testFile = new File (bundleDir, filename);
            if (!testFile.exists()) {
                throw new BundleFormatException("Listed file '"
                                              + testFile + "' does not exist");
            }
        }

        if (!fileSet.contains(getMainJar())) {
            throw new BundleFormatException("mainJar '"
                                              + getMainJar()
                                              + "' not in fileList");
        }

        // TODO: Check the converse - that each file is listed in fileList

        // TODO: Check md5 if the md5 attribute exists on the file element
        log.trace("Finished checkFileList");
    }

    /**
     * Like {@link #checkFileList(java.io.File)} but check for files in a
     * collection of directories, much like having multiple elements in a class
     * path.
     * <p/>
     * It will also be affirmed that the file specified as mainJar is mentioned
     * in the fileList.
     * 
     * @param bundleDirs array of directories to check to check for files in
     * @throws BundleFormatException if one of the files specified in the
     *                               bundle's fileList is not found in any of
     *                               the directories in {@code bundleDirs}
     */
    public void checkFileList (File[] bundleDirs) {
        log.trace("checkFileList called");
        // Check that each file in fileList exists
        for (String filename : fileSet) {
            boolean fileFound = false;
            for (File dir: bundleDirs) {
                File testFile = new File (dir, filename);
                if (testFile.exists()) {
                    fileFound = true;
                    break;
                }
            }
            if (!fileFound) {
                throw new BundleFormatException("Listed file '"
                                              + filename + "' not found");
            }
        }

        if (!fileSet.contains(getMainJar())) {
            throw new BundleFormatException("mainJar '"
                                              + getMainJar()
                                              + "' not in fileList");
        }

        // TODO: Check the converse - that each file is listed in fileList

        // TODO: Check md5 if the md5 attribute exists on the file element
        log.trace("Finished checkFileList");
    }

    /**
     * Verify that all files listed in {@code <publicApi>} are present in
     * {@code <fileList>} as well.
     */
    public void checkPublicApi () {
        log.trace("checkPublicApi called");
        // Check that each file in fileList exists
        for (String filename : apiSet) {
            if (!fileSet.contains(filename)) {
                throw new BundleFormatException("Listed public API file '"
                                              + filename + "' is not present "
                                              + "in the file list");
            }
        }
        log.trace("Finished checkFileList");
    }

    /**
     * <p>Recusively scan a directory and add all files to the file list of this
     * builder.</p>
     *
     * <p>The scanner will skip past any hidden files or directories.</p>
     *
     * <p>The bundle spec file as according to the bundle type of this builder
     * will automatically be added to the list even if it does not exist
     * on disk.</p>
     *
     * @param bundleDir directory to scan
     * @throws IOException if the file is not a directory
     * @throws NullPointerException if the bundleDir is null
     * @throws FileNotFoundException if the file {@code bundleDir} does not exist
     */
    public void buildFileList (File bundleDir) throws IOException {
        log.trace("buildFileList(" + bundleDir + ") called");
        if (bundleDir == null) {
            throw new NullPointerException("bundleDir argument is null");
        }
        if (!bundleDir.exists()) {
            throw new FileNotFoundException (bundleDir.getAbsolutePath());
        }
        if (!bundleDir.isDirectory()) {
            throw new IOException("'" + bundleDir + "' is not a directory");
        }
        recursiveScan(bundleDir, null);

        if (!hasFile(getFilename())) {
            addFile(getFilename());
        }
        log.trace("Finished buildFileList(" + bundleDir + ")");
    }

    /**
     * Convenience method to expand {@code <publicApi>} into the property
     * {@code java.rmi.server.codebase}.
     * <p></p>
     * Only the basename of the files mentioned in {@code <publicApi>}
     * will be used in the codebase.
     * @param repo A {@link BundleRepository} used to expand the location
     *             of the public api jars. 
     */
    public void expandCodebase (BundleRepository repo) {
        // Expand all public API refs into the codebase
        String codeBase = "";
        for (String apiFile : getApi()) {
            apiFile = new File(apiFile).getName();
            String apiUrl;
            try {
                apiUrl = repo.expandApiUrl(apiFile);
            } catch (IOException e) {
                apiUrl = "ERROR";
            }
            codeBase += apiUrl + " ";
            log.trace ("Updated codebase: '" + codeBase + "'");
        }        

        if (!"".equals(codeBase)) {
            setProperty("java.rmi.server.codebase", codeBase.trim());
        }
    }

    private void recursiveScan (File rootDir, String child) {
        log.trace("recursiveScan(" + rootDir + ", " + child + ") called");
        if (child == null) {
            /* This is the sourceRoot of the scan tree */
            for (String file : rootDir.list()) {
                recursiveScan(rootDir, file);
            }
            return;
        }

        File file = new File (rootDir, child);

        if (file.isHidden()) {
            return;
        }

        if (file.isFile()) {
            addFile(child);
        } else {
            for (String subChild : file.list()) {
                recursiveScan(rootDir, child + "/" + subChild);
            }
        }        
        log.trace("Finished recursiveScan(" + rootDir + ", " + child + ")");
    }

    /**
     * <p>Write a complete bundle file to a directory based on this bundle spec.
     * The file list of this bundle (see {@link #getFiles}) will be traversed
     * and all files in it will be added to a zip file that will be written
     * in {@code outputDir} with the name {@code <bundleId>.bundle}.</p>
     *
     * <p>If the {@code mainJar} and spec file is not in the file list,
     * they will be added automatically.</p>
     *
     * @param rootDirs directories containing the files to be packed into the
     *                bundle
     * @param outputDir directory where to write the output {@code .bundle} file
     *
     * @throws NullPointerException if any of the arguments are null
     * @throws IOException if any of the {@code rootDirs} does not exist or
     *                     there is an error writing the bundle
     * @throws FileAlreadyExistsException if {@code outputDir} is a regular file
     * @throws FileNotFoundException if any of the files listed in the bundle's
     *                               fileSet can not be found in any of the
     *                               specified root directories
     * @throws BundleFormatException if this builder does not have a bundle id
     * @return a file handle pointing at the written bundle
     */
    public File buildBundle (File[] rootDirs, File outputDir)
                                                            throws IOException {
        log.trace("buildBundle([" + Strings.join(rootDirs, ", ") + "], "
                  + outputDir + ") called");

        /* Validate parameters */
        if (getBundleId() == null) {
            throw new BundleFormatException("Bundle does not have a bundle id");
        }

        if (rootDirs == null) {
            throw new NullPointerException("rootDir argument is null");
        } else if (outputDir == null) {
            throw new NullPointerException("outputDir argument is null");
        }

        for (File dir : rootDirs) {
            if (!dir.isDirectory()) {
                throw new IOException("rootDir not a directory '" + dir + "'");
            } else if (outputDir.isFile()) {
                throw new FileAlreadyExistsException("outputDir is a regular "
                                                     + "file '"
                                                     + outputDir + "'");
            }
        }

        /* Make sure the bundle spec file is listed */
        if (!hasFile(getFilename())) {
            addFile(getFilename());
        }

        if (getMainJar() != null && !hasFile(getMainJar())) {
            addFile(getMainJar());
        }

        /* Write the actual zip ball */
        File bundleFile =  new File (outputDir,
                                     getBundleId() + Bundle.BUNDLE_EXT);

        /* Make sure destination dir exist */
        log.trace("buildBundle: Making dir '" + outputDir + "'");
        outputDir.mkdirs();

        log.trace("buildBundle: Creating fileWriter for '" + bundleFile + "'");
        FileOutputStream fileWriter = new FileOutputStream(bundleFile);
        log.trace("buildBundle: Wrapping fileWriter i ZIP output stream");
        ZipOutputStream zipStream = new ZipOutputStream(fileWriter);

        log.trace("buildBundle: Writing files to zip stream");
        byte[] buf = new byte[4096];
        int len;
        for (String file : getFiles()) {
            log.trace("buildBundle: Adding '" + file + "' to ZIP stream");

            // Don't write the bundle spec from the file, but from the
            // in-memory bundle spec
            if (file.equals("service.xml") || file.equals("client.xml")) {
                continue;
            }

            zipStream.putNextEntry(new ZipEntry(file));
            FileInputStream in = new FileInputStream(findFile(rootDirs, file));
            while ((len = in.read(buf)) > 0) {
                zipStream.write(buf, 0, len);
            }
        }

        /* Write the bundle spec to the zip stream.
         * The write() call closees the stream */
        log.trace("buildBundle: Writing bundle spec to zip stream");
        zipStream.putNextEntry(new ZipEntry(getFilename()));
        write(zipStream);

        log.trace("buildBundle: Flushing and closing streams");

        /* Clean up:
         * The streams are closed in the write() call above */

        log.trace("buildBundle([" + Strings.join(rootDirs, ", ") + "], "
                  + outputDir + ") finished");
        return bundleFile;
    }

    /**
     * Like {@link #buildBundle(java.io.File[], java.io.File)}, but only take
     * a single root directory to resolve files from.
     *
     * @param rootDir the single directory to check for files in
     * @param outputDir the directory in which to place the final bundle
     *
     * @return a file object opinting to the newly created bundle
     *
     * @throws NullPointerException if any of the arguments are null
     * @throws IOException if {@code rootDir} does not exist or there is an
     *                     error writing the bundle
     * @throws FileAlreadyExistsException if {@code outputDir} is a regualr file
     * @throws FileNotFoundException if any of the files listed in the bundle's
     *                               fileSet can not be found in the
     *                               specified root directory
     * @throws BundleFormatException if this builder does not have a bundle id
     */
    public File buildBundle (File rootDir, File outputDir)
                                                            throws IOException {
        if (rootDir == null) {
            throw new NullPointerException("rootDir argument is null");
        }

        return buildBundle(new File[]{rootDir}, outputDir);
    }

    /**
     * Locate {@code file} in one of the directories specified in {@code dirs}
     * @param dirs directories in which to look for {@code file}
     * @param file the name of the file to look for
     * @return a {@link File} pointing to the first found file on the file
     *         system matching {@code file}.
     * @throws FileNotFoundException if {@code file} can not be found in any of
     *                               the directories {@code dirs}
     */
    private File findFile(File[] dirs, String file)
                                                  throws FileNotFoundException {
        for (File dir : dirs) {
            File f = new File(dir, file);
            if (f.exists()) {
                return f;
            }
        }

        throw new FileNotFoundException(file);
    }

    /**
     * Create a bundle stub entirely based on the information found in the bundle
     * spec.
     * @return
     */
    public BundleStub getStub () {
        log.trace("getStub called");
        List<String> libs = new ArrayList<String>(20);
        List<String> jvmArgs = new ArrayList<String>(20);
        /* Find all .jar files in lib/ */
        for (String lib : getFiles()) {
            if (lib.endsWith(".jar") && lib.startsWith("lib/")) {
                log.trace("getStub() adding lib: " + lib);
                libs.add(lib);
            }
        }

        /* Construct JVM args.
         * Non-integer values needs to be enclosed in single-pings to
         * handle spaces gracefully. If we keep the pings on the integers
         * too we get some errors when launching */
        for (Map.Entry<String, Serializable> entry : getProperties()) {
            String val = (String) entry.getValue();
            //val = val.replace(" ", "\\ ");
            String arg ="-D"+entry.getKey()+"="+val;
            jvmArgs.add(arg);
            log.trace("getStub() adding argument: " + arg);
        }

        /* Detect JMX support */
        if (hasFile(BundleStub.JMX_PASSWORD_FILE)) {
            jvmArgs.add ("-Dcom.sun.management.jmxremote.password.file="
                         + BundleStub.JMX_PASSWORD_FILE);
        }
        if (hasFile(BundleStub.JMX_ACCESS_FILE)) {
            jvmArgs.add ("-Dcom.sun.management.jmxremote.access.file="
                         + BundleStub.JMX_ACCESS_FILE);
        }
        if (hasFile(BundleStub.POLICY_FILE)) {
            jvmArgs.add ("-Djava.security.manager");
            jvmArgs.add ("-Djava.security.policy="
                         + BundleStub.POLICY_FILE);
        }

        log.trace("getStub() creating BundleStub");
        BundleStub stub = new BundleStub(new File ("."),
                              getBundleId(),
                              getInstanceId(),
                              new File(getMainJar()),
                              getMainClass(),
                              libs,
                              jvmArgs);
        log.trace("Finished getStub(), returning: " + stub);
        return stub;

    }

    /**
     * Return a string representation of the bundle spec suitable for
     * displaying to humans.
     * <p/>
     * Note that the format of the returned message is not stable in any way
     * and should never be subject to parsing or other formal inspection. The
     * XML format is to be used for that.
     *
     * @param printFiles whether or not to include the file list in the result
     * @return a "pretty print" version of the bundle spec
     */
    public String getDisplayString (boolean printFiles) {
        String msg =
        "Description:\n" + getDescription() + "\n\n";

        msg += "Metadata:\n"
        + "\tInstance id : " + getInstanceId() + "\n"
        + "\tBundle id   : " + getBundleId() + "\n"
        + "\tMain jar    : " + getMainJar() + "\n"
        + "\tMain class  : " + getMainClass() + "\n"
        + "\tAuto start  : " + isAutoStart() + "\n"
        + "\n";

        msg += "JVM Properties:\n";
        for (Map.Entry<String, Serializable> entry: getProperties()) {
            msg += "\t" + entry.getKey() + " = " + entry.getValue() + "\n";
        }
        msg += "\n";

        // Push the API through a TreeSet to get alphabetic sorting
        msg+= "Public API:\n";
        for (String api : new TreeSet<String>(getApi())) {
            msg += "\t" + api + "\n";
        }
        msg += "\n";

        if (printFiles) {
            // Push the fileList through a TreeSet to get alphabetic sorting
            msg += "Files:\n";
            for (String file : new TreeSet<String>(getFiles())) {
                msg += "\t" + file + "\n";
            }
            msg += "\n";
        }

        return msg;
    }
}



