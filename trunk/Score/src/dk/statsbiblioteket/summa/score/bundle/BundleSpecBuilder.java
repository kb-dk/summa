package dk.statsbiblioteket.summa.score.bundle;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.io.OutputStream;
import java.io.IOException;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.io.FileNotFoundException;

import dk.statsbiblioteket.util.FileAlreadyExistsException;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

/**
 *
 */
public class BundleSpecBuilder {

    private String mainJar;
    private String mainClass;
    private String bundleId;
    private String instanceId;
    private String description;
    private Bundle.Type bundleType;
    private Configuration properties;
    private HashSet<String> fileSet;

    public BundleSpecBuilder () {
        bundleType = Bundle.Type.SERVICE;
        properties = Configuration.newMemoryBased();
        fileSet = new HashSet<String>();

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

    public String getProperty (String name) { return properties.getString(name); }
    public void setProperty (String name, String value) { properties.set (name, value); }
    public void clearProperty (String name) { properties.purge(name); }
    public Configuration getProperties () { return properties; }

    public void addFile (String file) { fileSet.add(file); }
    public void removeFile (String file) { fileSet.remove(file); }
    public boolean hasFile (String file) { return fileSet.contains(file); }
    public Collection<String> getFiles () { return fileSet; }

    public void write (OutputStream out) throws IOException {
        if (out == null) {
            throw new NullPointerException("output argument is null");
        }
        write(new OutputStreamWriter(out));
    }

    public void write (Writer writer) throws IOException {
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

            if (description != null) {
                out.println ("  <description>" + description + "</description>");
            }

            if (properties.getStorage().size() != 0) {
                for (Map.Entry<String, Serializable> entry : properties) {
                    out.println ("  <property name=\"" + entry.getKey() + "\""
                                      + " value=\"" + entry.getValue() +"\"/>");
                }
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
        if (dir == null) {
            throw new NullPointerException("Directory argument is null");
        }

        if (dir.isFile()) {
            throw new FileAlreadyExistsException(dir);
        }

        dir.mkdirs();

        File target = new File (dir, getFilename());
        write (target);
        return target;
    }

    /**
     * Get the suggested file name for this buidler. Ie {@code client.xml}
     * or {@code service.xml} for client and service bundles accordingly.
     *
     * @return filename as per bundle type
     */
    public String getFilename () {
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
     * @param file file to read bundle spec from
     * @return a bundle spec ready for manipulations
     * @throws IOException if there is an error reading the file
     */
    public static BundleSpecBuilder open (File file) throws IOException {
        BundleSpecBuilder builder = open (new FileInputStream(file));
        String filename = file.toString();
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
    public static BundleSpecBuilder open (InputStream in) {
        BundleSpecBuilder builder = new BundleSpecBuilder();
        builder.read(in);
        return builder;
    }

    /**
     * Read the contents of a bundle spec into this bundle spec buidler
     * @param in stream to read spec from
     */
    public void read (InputStream in) {

        DocumentBuilder xmlParser;
        Document doc;
        Element docElement;
        NodeList children;

        try {
            xmlParser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch(ParserConfigurationException e){
            throw new BundleLoadingException("Error creating DocumentBuilder", e);
        }

        try {
            doc = xmlParser.parse(in);
        } catch (Exception e) {
            throw new BundleLoadingException("Error parsing bundle spec ", e);
        }

        docElement = doc.getDocumentElement();
        if (!docElement.getTagName().equals("bundle")) {
            throw new BundleFormatException("Bundle spec has root element '"
                                          + docElement.getTagName()
                                          + "', expected 'bundle'");
        }

        children = docElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if ("mainJar".equals(node.getNodeName())) {
                mainJar = node.getTextContent();
            } else if ("mainClass".equals(node.getNodeName())) {
                mainClass = node.getTextContent();
            } else if ("description".equals(node.getNodeName())) {
                description = node.getTextContent();
            } else if ("bundleId".equals(node.getNodeName())) {
                bundleId = node.getTextContent();
            } else if ("instanceId".equals(node.getNodeName())) {
                instanceId = node.getTextContent();
            } else if ("fileList".equals(node.getNodeName())) {
                readFilelist (node);
            } else if ("property".equals(node.getNodeName())) {
                String name = node.getAttributes().getNamedItem("name").getNodeValue();
                String value = node.getAttributes().getNamedItem("value").getNodeValue();

                if (name == null) {
                    name = "!unset!";
                } else if (value == null) {
                    value="!unset!";
                }
                setProperty(name, value);
            }
        }
    }

    private void readFilelist(Node node) {
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
    }

    public void checkFileList (File bundleDir) {
        // Check that each file in fileList exists
        for (String filename : fileSet) {
            File testFile = new File (bundleDir, filename);
            if (!testFile.exists()) {
                throw new BundleFormatException("Listed file '"
                                              + testFile + "' does not exist");
            }
        }

        // TODO: Check the converse - that each file is listed in fileList

        // TODO: Check md5 if the md5 attribute exists on the file element
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
    }

    private void recursiveScan (File rootDir, String child) {
        if (child == null) {
            /* This is the root of the scan tree */
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
     * @param rootDir root directory containing the files to be packed into the
     *                bundle
     * @param outputDir directory where to write the output file
     *
     * @throws NullPointerException if any of the arguments are null
     * @throws IOException if {@code rootDir} does not exist or there is an error writing
     *                     the bundle
     * @throws FileAlreadyExistsException if {@code outputDir} is a regualr file
     * @throws BundleFormatException if this builder does not have a bundle id
     * @returns a file handle pointing at the written bundle
     */
    public File buildBundle (File rootDir, File outputDir) throws IOException {
        /* Validate parameters */
        if (getBundleId() == null) {
            throw new BundleFormatException("Bundle does not have a bundle id");
        }

        if (rootDir == null) {
            throw new NullPointerException("rootDir argument is null");
        } else if (outputDir == null) {
            throw new NullPointerException("outputDir argument is null");
        }

        if (!rootDir.isDirectory()) {
            throw new IOException("rootDir not a directory '" + rootDir + "'");
        } else if (outputDir.isFile()) {
            throw new FileAlreadyExistsException("outputDir is a regular file '"
                                                 + outputDir + "'");
        }

        if (!hasFile(getFilename())) {
            addFile(getFilename());
        }

        if (getMainJar() != null && !hasFile(getMainJar())) {
            addFile(getMainJar());
        }

        /* Write the bundle spec */
        outputDir.mkdirs();
        write (new File(rootDir, getFilename()));

        /* Write the actual zip ball */
        File bundleFile =  new File (outputDir,
                                     getBundleId() + Bundle.BUNDLE_EXT);

        FileOutputStream fileWriter = new FileOutputStream(bundleFile);
        ZipOutputStream zipStream = new ZipOutputStream(fileWriter);

        byte[] buf = new byte[4096];
        int len;
        for (String file : getFiles()) {
            zipStream.putNextEntry(new ZipEntry(file));
            FileInputStream in = new FileInputStream(new File(rootDir, file));
            while ((len = in.read(buf)) > 0) {
                zipStream.write(buf, 0, len);
            }
        }

        /* Clean up */
        zipStream.flush();
        zipStream.finish();
        zipStream.close();
        fileWriter.flush();
        fileWriter.close();

        return bundleFile;
    }

    /**
     * Create a bundle stub entirely based on the information found in the bundle
     * spec.
     * @return
     */
    public BundleStub getStub () {
        List<String> libs = new ArrayList<String>();
        List<String> jvmArgs = new ArrayList<String>();
        /* Find all .jar files in lib/ */
        for (String lib : getFiles()) {
            if (lib.endsWith(".jar") && lib.startsWith("lib/")) {
                libs.add(lib);
            }
        }

        /* Construct JVM args */
        for (Map.Entry<String, Serializable> entry : getProperties()) {
            jvmArgs.add ("-D"+entry.getKey()+"="+entry.getValue());
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
            jvmArgs.add ("-Djava.security.policy="
                         + BundleStub.POLICY_FILE);
        }

        return new BundleStub(new File ("."),
                              getBundleId(),
                              getInstanceId(),
                              new File(getMainJar()),
                              getMainClass(),
                              libs,
                              jvmArgs);

    }
}
