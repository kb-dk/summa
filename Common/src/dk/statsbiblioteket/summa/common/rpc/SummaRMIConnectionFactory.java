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
package dk.statsbiblioteket.summa.common.rpc;

import dk.statsbiblioteket.util.rpc.RMIConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.rmi.Remote;

/**
 * <p>A thin {@link Configurable} wrapper around a
 * {@link RMIConnectionFactory}.</p>
 * 
 */
public class SummaRMIConnectionFactory<E extends Remote>
                                               extends RMIConnectionFactory<E>
                                               implements Configurable {
    
    public SummaRMIConnectionFactory (Configuration conf) {
        super ();
    }
}




