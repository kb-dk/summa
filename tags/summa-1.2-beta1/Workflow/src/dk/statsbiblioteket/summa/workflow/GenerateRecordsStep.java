/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.workflow;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.summa.storage.api.StorageWriterClient;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.ingest.source.RecordGenerator;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;

/**
 * A {@link WorkflowStep} that adds a number of template-based pseudo-random
 * records to a given storage.
 * </p><p>
 * The properties for the step is the properties from {@link RecordGenerator}
 * combined with the properties from {@link StorageWriterClient} (basically
 * this means {@link ConnectionConsumer#CONF_RPC_TARGET}).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class GenerateRecordsStep implements WorkflowStep {
    private static Log log = LogFactory.getLog(GenerateRecordsStep.class);

    private RecordGenerator generator;
    private StorageWriterClient writer;

    public GenerateRecordsStep(Configuration conf) {
        generator = new RecordGenerator(conf);
        writer = new StorageWriterClient(conf);
        log.debug("Generated GenerateRecordsStep");
    }

    public void run() {
        log.debug("Starting record generation");
        Profiler profiler = new Profiler();
        profiler.setBpsSpan(1000);
        int feedback = 1000;
        while (generator.hasNext()) {
            try {
                writer.flush(generator.next().getRecord());
                profiler.beat();
                if (profiler.getBeats() % feedback == 0) {
                    log.debug("Created and ingested " + profiler.getBeats()
                              + " records with a current average speed of "
                              + profiler.getBps(true) + " records/sec");
                }
            } catch (IOException e) {
                throw new RuntimeException(
                        "Exception while flushing record", e);
            }
        }
        log.info(String.format(
                "Finished creating %d records in %s, total average speed %s "
                + "records/second with an average speed for the last 1000 "
                + "records of %s",
                profiler.getBeats(), profiler.getSpendTime(),
                profiler.getBps(false), profiler.getBps(true)));
    }
}
