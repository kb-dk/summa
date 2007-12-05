/* $Id: OAIHarvestDirectoryDigester.java,v 1.21 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.21 $
 * $Date: 2007/10/05 10:20:23 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.ingest.OAI;


import org.apache.commons.logging.Log;       import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import dk.statsbiblioteket.summa.ingest.Digester;
import dk.statsbiblioteket.summa.ingest.RecordFormatException;
import dk.statsbiblioteket.summa.ingest.ParserTask;
import dk.statsbiblioteket.summa.ingest.Target;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Provides functionality to digest a set of PMH havested OAI-targets.<br> 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, te")
public class OAIHarvestDirectoryDigester extends Digester {

    private static final Log log = LogFactory.getLog(OAIHarvestDirectoryDigester.class);

    //private static long timeStamp;
    /**
     * Special file containing information about the {@link Digester#target},
     * such as name and base URL.
     */
    private static final String TARGET_INFO = "identify.xml";
    /**
     * Special file that defines the lastModified time for {@link
     * Digester#target}. The time is taken from the file's lastModified stamp.
     */
    private static final String TIMESTAMP = "timestamp";
    /**
     * Special file containing meta data about the {@link Digester#target}. The
     * current implementation does not parse this file. It is reserved for
     * future use.
     */
    private static final String METADATAFORMATS = "metadataformats.xml";

    /**
     * Update the target based on meta data in the given directory and call the
     * super digest. Exceptions while updating meta data aborts the digest.
     *
     * @param directory where to start the traversal.
     * @throws IOException           if a file or folder could not be accessed.
     * @throws RecordFormatException if a record was invalid.
     */
    public void digest(
            File directory) throws IOException, RecordFormatException {
        String ERROR_PARSING = "Error parsing targetInfo: ";
        try {
            parseSpecial(directory);
            super.digest(directory);
        } catch (SAXException e) {
            throw new RecordFormatException(ERROR_PARSING + e.getMessage());
        } catch (XPathExpressionException e) {
            throw new RecordFormatException(ERROR_PARSING + e.getMessage());
        } catch (ParserConfigurationException e) {
            throw new RecordFormatException(ERROR_PARSING + e.getMessage());
        }
    }

    protected boolean isParsable(File f) {
        return !isSpecialFile(f);
    }

    /**
     * Looks for three special files in the given directory. The files specify
     * target info, metadata formats and last updated timestamp. It is allowed
     * for one or more of the files to be missing.
     * @param directory where the files are supposedly located.
     * @throws IOException if the directory or a file could not be read.
     * @throws SAXException if the targetInfo file was malformed.
     * @throws XPathExpressionException if the targetInfo file was malformed.
     * @throws ParserConfigurationException if there was an internal error
     *         with the DOM parser.
     */
    private synchronized void parseSpecial(File directory)
            throws IOException, SAXException, XPathExpressionException,
                   ParserConfigurationException {
        if (directory.isDirectory()) {
            File tar = new File(directory.getAbsolutePath(), TARGET_INFO);
            if (tar.exists()) {
                log.debug("Getting target info:");
                targetinfo(tar);
            } else {
                log.error ("No such file " + tar + " needed by OAIHarvestDirectoryDigester. Skipping.");
                return;
            }
            File meta = new File(directory.getAbsolutePath(), METADATAFORMATS);
            if (meta.exists()) {
                log.debug("Getting metadataInfo");
                metadataformats(meta);
            }
            File time = new File(directory.getAbsolutePath(), TIMESTAMP);
            if (time.exists()) {
                log.debug("Getting time");
                timestamp(time);
            }
        }
    }

    protected ParserTask createParser(File f) {
        return new OAIParserTask(f,target,in);
    }

    /**
     * Checks whether a given files belongs to the group of reserved file names.
     * Files belonging to this group contains meta data and shold be processed
     * separately.
     *
     * @param file a file on the local file system.
     * @return true if the file belongs to the reserved group.
     */
    private boolean isSpecialFile(File file) {
        String name = file.getName();
        return name.equals(TIMESTAMP) ||
               name.equals(METADATAFORMATS) ||
               name.equals(TARGET_INFO);
    }

    /**
     * Extracts {@link Target#name} and {@link Target#base} from the targetInfo
     * file (assumably {@link #TARGET_INFO}) and assigns it to {@link#target}.
     *
     * @param targetInfo information about the {@link Target}.
     * @throws SAXException        if the XML in targetInfo could not be parsed
     *                             properly.
     * @throws java.io.IOException if targetInfo could not be read.
     * @throws javax.xml.xpath.XPathExpressionException if the XPath could not
     *         extract repository name or baseURL from targetInfo.
     *
     * @throws javax.xml.parsers.ParserConfigurationException if the
     *         DocumentBuilderFactory could not build a Document.
     *
     */
    private void targetinfo(File targetInfo)
            throws ParserConfigurationException, IOException, SAXException,
                   XPathExpressionException {

        Target processedTarget = target.createEqualTarget();
        log.info("Getting target info on file:"
                 + targetInfo.getAbsolutePath());
        XPath p = XPathFactory.newInstance().newXPath();
        Document dom = DocumentBuilderFactory.newInstance().
                newDocumentBuilder().parse(new FileInputStream(targetInfo));
        String name = p.evaluate("/OAI-PMH/Identify/repositoryName", dom);
        log.info("got name:" + name);
        String baseURL = p.evaluate("/OAI-PMH/Identify/baseURL", dom);
        log.info("got url:" + baseURL);
        assert target != null;
        processedTarget.setName(name);

        processedTarget.add(OAIParserTask.HOME_URL,
                            new URL(baseURL).toExternalForm());
        processedTarget.init();
        target = processedTarget;
    }

    /**
     * Update {@link Digester#target} lastModified to the timestamp from the
     * given File (presumably {@link#timestamp}).
     *
     * @param timestampFile the lastModified from this file will get assigned to
     *                      lastModified for the target.
     */
    private void timestamp(File timestampFile) {
        Target processedTarget = target.createEqualTarget();
        target.add("lastModified",
                   String.valueOf(timestampFile.lastModified()));
        processedTarget.init();
        target = processedTarget;

    }

    /**
     * Handles the metadata specified in the metadataFile. This implementation
     * is currently blank.
     *
     * @param metadataFile meta data about the {@link Digester#target}.
     */
    private void metadataformats(File metadataFile) {

    }
}
