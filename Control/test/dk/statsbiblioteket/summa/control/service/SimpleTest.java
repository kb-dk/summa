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
package dk.statsbiblioteket.summa.control.service;

import java.rmi.RemoteException;
import java.io.IOException;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class SimpleTest extends ServiceBase {
    private Log log = LogFactory.getLog(SimpleTest.class);

    private class ActualService implements Runnable {

        private Log log = LogFactory.getLog(ActualService.class);

        public boolean stopped = true;

        public void run() {
            stopped = false;
            log.info ("service started");
            while (!stopped) {
                try {
                    Thread.sleep(2000);
                    log.info ("service running... ");
                } catch (InterruptedException e) {
                    log.info ("service interrupted!");
                    stopped = true;
                }
            }
            log.info ("service stopped");
        }
    }

    private Status status;
    private ActualService service;

    public SimpleTest (Configuration conf) throws IOException {
        super(conf);

        setStatus(Status.CODE.constructed, "Created SimpleTest object",
                  Logging.LogLevel.DEBUG);

        service = new ActualService();

        exportRemoteInterfaces();

        setStatus(Status.CODE.constructed, "Remote interfaces up",
                  Logging.LogLevel.DEBUG);
    }

    public void start() throws RemoteException {
        System.out.println ("START");
        setStatus(Status.CODE.startingUp, "Starting SimpleTest service" + this,
                  Logging.LogLevel.DEBUG);

        if (service.stopped) {
            new Thread (service).start();
        } else {
            log.warn ("Trying to start service, but it is already running");
        }

        setStatusRunning("The service is running");
    }

    public void stop() throws RemoteException {
        log.trace ("Recieved request to stop.");
        if (service.stopped) {
            log.warn ("Trying to stop service but it is already stopped");
            return;
        }

        setStatus(Status.CODE.stopping, "Shutting down SimpleTest service" + this,
                Logging.LogLevel.DEBUG);

        service.stopped = true;

        setStatus(Status.CODE.stopped, "SimpleTest service " + this + " down.",
                Logging.LogLevel.INFO);

        log.info ("Real (or atleast most) services should call System.exit(0) "
                  + "here");
    }
}




