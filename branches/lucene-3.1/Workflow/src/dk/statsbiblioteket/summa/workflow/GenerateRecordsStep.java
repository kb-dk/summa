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
 * A {@link WorkflowStep} that commits a number of template-based pseudo-random
 * records to a given storage.
 * </p><p>
 * The properties for the step is the properties from {@link RecordGenerator}
 * combined with the properties from {@link StorageWriterClient} (basically
 * this means {@link ConnectionConsumer#CONF_RPC_TARGET}).
 * <p/>
 * Records are committed in batches in a size specified by the
 * {@link #CONF_BATCH_SIZE} property in each invocation of {@link #run()} .
 * This means that one will have to invoke the {@code run()} method
 * {@link RecordGenerator#CONF_RECORDS}/{@link #CONF_BATCH_SIZE} times before
 * the record generator is depleted.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class GenerateRecordsStep implements WorkflowStep {
    private static Log log = LogFactory.getLog(GenerateRecordsStep.class);

    /**
     * The number of records to extract in each invocation of the {@link #run()}
     * method. Default value is {@link #DEFAULT_BATCH_SIZE}.
     */
    public static final String CONF_BATCH_SIZE =
                                "summa.workflow.step.generaterecords.batchsize";

    /**
     * Default value for the {@link #CONF_BATCH_SIZE} property
     */
    public static final int DEFAULT_BATCH_SIZE = 100;

    private RecordGenerator generator;
    private StorageWriterClient writer;
    private int batchSize;

    public GenerateRecordsStep(Configuration conf) {
        generator = new RecordGenerator(conf);
        writer = new StorageWriterClient(conf);
        batchSize = conf.getInt(CONF_BATCH_SIZE, DEFAULT_BATCH_SIZE);
        log.debug("Generated GenerateRecordsStep");
    }

    public void run() {
        if (generator == null) {
            log.trace("Record generator depleted");
            return;
        }

        log.debug("Starting record generation");
        Profiler profiler = new Profiler();
        profiler.setBpsSpan(1000);
        int feedback = 1000;
        int count = 0;
        while (generator.hasNext() && count < batchSize) {
            count++;
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

        // Reset our selves if generator is depleted
        if (!generator.hasNext()) {
            log.info(String.format(
                    "Record generator depleted. Created "
                    + "%d records in %s, total average speed %s"
                    + " records/second with an average speed for the last 1000"
                    + " records of %s",
                    profiler.getBeats(), profiler.getSpendTime(),
                    profiler.getBps(false), profiler.getBps(true)));
            generator = null;
        }
    }
}

