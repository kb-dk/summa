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

import dk.statsbiblioteket.util.FileAlreadyExistsException;

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
    private HashMap<String,String> properties;
    private HashSet<String> fileSet;

    public BundleSpecBuilder () {
        properties = new HashMap<String,String>();
        fileSet = new HashSet<String>();
        bundleType = Bundle.Type.SERVICE;
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

    public String getProperty (String name) { return properties.get(name); }
    public void setProperty (String name, String value) { properties.put (name, value); }
    public void clearProperty (String name) { properties.remove(name); }

    public void addFile (String file) { fileSet.add(file); }
    public void removeFile (String file) { fileSet.remove(file); }
    public void hasFile (String file) { fileSet.contains(file); }

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

            if (!properties.isEmpty()) {
                for (Map.Entry<String,String> entry : properties.entrySet()) {
                    out.println ("  <property name=\"" + entry.getKey() + "\""
                                      + " value=\"" + entry.getValue() +"\"/>");
                }
            }

            if (!fileSet.isEmpty()) {
                out.println ("  <fileSet>");
                for (String file : fileSet) {
                    out.println ("    <file>" + file + "</file>");
                }
                out.println ("  </fileSet>");
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

}
