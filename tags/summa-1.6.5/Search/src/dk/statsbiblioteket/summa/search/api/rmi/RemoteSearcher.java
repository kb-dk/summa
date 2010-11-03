/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.search.api.rmi;

import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface needed for RMI on {@link dk.statsbiblioteket.summa.search.rmi.RMISearcherProxy}
 * to work.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface RemoteSearcher extends Remote, SummaSearcher {

    /**
     *  Make a search given a request.
     *  
     * @param request contains SearchNode-specific request-data.
     * @return Collection containing all responses.
     * @throws RemoteException if there is an error getting responses over RMI.
     */
    public ResponseCollection search(Request request) throws RemoteException;

    /**
     * Close this seaercher.
     * 
     * @throws RemoteException if an error is experienced during closing of this searcher.
     */
    public void close() throws RemoteException;

}




