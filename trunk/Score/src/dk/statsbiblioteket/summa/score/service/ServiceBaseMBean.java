package dk.statsbiblioteket.summa.score.service;

import dk.statsbiblioteket.summa.score.api.Service;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Empty interface used to expose a {@link Service} as an MBean
 *
 * @see ServiceBase
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        author = "mke")
public interface ServiceBaseMBean extends Service {
}
