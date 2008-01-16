package dk.statsbiblioteket.summa.score.server;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * This class implements the core of what is the Summa Component Registry.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public class ClientManager implements Runnable, Configurable {


    public ClientManager(Configuration conf) {

        
    }

    public void run() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
