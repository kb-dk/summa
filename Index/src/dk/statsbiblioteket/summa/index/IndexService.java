/* $Id: IndexService.java,v 1.3 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/05 10:20:22 $
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
package dk.statsbiblioteket.summa.index;

import dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.io.IOException;

/**
 * This interface describes the indexService component.
 * The indexService component, is a <a href="http://jakarta.apache.dk/lucene/docs/index.html">Lucene</a> based index engine for XML file repositories.
 * <p />
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public interface IndexService extends Remote {

    /**
     * Resumes the indexing service on an exsisting index.
     *
     * @param indexPath          The directory where the index is stored.
     * @throws RemoteException
     * @throws dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException
     */
    void resumeIndex(String indexPath) throws RemoteException, IndexServiceException;

    /**
     * Makes a new index - if an index exists it will be truncated.
     *
     * @param indexPath         The path where the index will be stored
     * @throws RemoteException
     * @throws IndexServiceException
     */
    void startIndex(String indexPath) throws RemoteException, IndexServiceException;


    /**
     * Returns a HashMap of the Analysers {@see org.apache.lucene.analysis.Analyzer} that has been used to generate the index.
     * The keys of the HashMap are the String objects of the XML:lang attributes found in the XML documents during indexing.
     * This HashMap is used by search services to generate proper readers when querying the index.
     *
     * @return HashMap of lang:Analyser pairs
     *
     * @throws RemoteException
     */
    HashMap getIndexAnalyzers() throws RemoteException;

    /**
     * Returns a String representation of the XML search description; dynamically generated as the index evolves.
     *
     * @return XML document in a String.
     *
     * @throws RemoteException
     */
    String getSearchDescriptor() throws RemoteException;

    //void setXSLT(String urlToXLST) throws RemoteException;

    void addXMLRecord(String xml, String id, String xsltURL) throws RemoteException, IndexServiceException, IOException;

    void removeRecord(String id) throws RemoteException, IndexServiceException;

    void updateXMLRecord(String xml, String id, String xsltURL) throws RemoteException, IndexServiceException, IOException;

    String getIndexPath() throws RemoteException;

    void optimizeAll() throws RemoteException, IndexServiceException;

    void closeAll() throws IndexServiceException;

    String getLastIndexedID() throws RemoteException;

    SearchDescriptor getDescriptor() throws RemoteException;
}
