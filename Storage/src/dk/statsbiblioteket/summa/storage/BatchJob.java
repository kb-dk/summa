package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;

import javax.script.*;
import java.io.*;

/**
 * Encapsulation of a scripted batch job run on a subset of the storage.
 * <p/>
 * Any {@link ScriptEngine} supported by the JVM can be used as a scripting
 * backend. See
 * <a href="https://scripting.dev.java.net/">scripting.dev.java.net</a> for a
 * full list of scripting engines for the Java platform.
 * <p/>
 * The script environment will have a number of variables loaded on
 * execution time. These are
 * <ul>
 *   <li><tt>record</tt> - The {@link Record} object to operate on</li>
 *   <li><tt>log</tt> - A Commons-Logging {@link Log} object</li>
 *   <li><tt>base</tt> - The name of the base being operated on
 *                       - possibly {@code null} when iterating over all
 *                         bases</li>
 *   <li><tt>minMtime</tt> - The minimum modification time for the records
 *                           in the batch</li>
 *   <li><tt>maxMtime</tt> - The maximum modification time for the records
 *                           in the batch</li>
 *   <li><tt>options</tt> - A {@link QueryOptions} instance</li>
 *   <li><tt>state</tt> - A private variable to be used by the script to hold
 *                        arbitrary state in between invocations. This variable
 *                        is initialized to {@code null} by the runtime</li>
 *   <li><tt>out</tt> - A {@link PrintWriter} collecting any output to return
 *                      from the script. Notably methods {@code out.print()}
 *                      and {@code out.println()} are available</li>
 *   <li><tt>commit</tt> - A boolean flag that must be set to {@code true}
 *                         if the script wants to commit any changes it has
 *                         done to {@code record}. This variable will be set to
 *                         {@code false} prior to each evaluation of the script
 *                         </li>
 *   <li><tt>first</tt> - A boolean flag which is {@code true} on the first
 *                        invocation of the script. Always {@code false}
 *                        otherwise</li>
 *   <li><tt>last</tt> - A boolean flag which is {@code true} when the last
 *                       record in the batch is reached. Always {@code false}
 *                       otherwise</li>
 * </ul>
 *
 * @author mke
 * @since Jan 7, 2010
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class BatchJob {

    private String script;
    private String jobName;
    private String extension;
    private ScriptEngineManager scriptMan;
    private ScriptEngine engine;
    private CompiledScript compiledScript;
    private StringWriter outBuffer;
    private PrintWriter outWriter;

    /**
     * Create a new batch job. Before running the job with {@link #eval()}
     * eval be sure to set the job context to operate on with
     * {@link #setContext(Record, boolean, boolean)}.
     *
     * @param jobName The name of the job to instantiate.
     *                The job name must match the regular expression
     *                {@code [a-zA-z_-]+.job.[a-zA-z_-]+} and correspond to a
     *                resource in the classpath. Fx {@code count.job.js}
     * @param log Inserted into the namespace of the batch script as {@code log}
     * @param base Inserted into the namespace of the batch script
     *             as {@code base}
     * @param minMtime Inserted into the namespace of the batch script
     *                 as {@code minMtime}
     * @param maxMtime Inserted into the namespace of the batch script
     *                 as {@code minMtime}
     * @param options Inserted into the namespace of the batch script
     *                 as {@code options}
     * @throws IOException if there is an error loading the resource
     * @throws ScriptException if there is an error parsing or compiling
     *                         the script
     * @throws IllegalArgumentException if {@code jobName} doesn't match
     *                                  the required regular expression
     * @throws FileNotFoundException if the resource {@code jobName} was not
     *                               found in the classpath
     */
    public BatchJob(String jobName, Log log,
              String base, long minMtime, long maxMtime, QueryOptions options)
                                           throws IOException, ScriptException {
        validateJobName(jobName);
        this.jobName = jobName;

        extension = jobName.substring(jobName.lastIndexOf('.')+1);
        scriptMan = new ScriptEngineManager();
        engine = scriptMan.getEngineByExtension(extension);
        outBuffer = new StringWriter();
        outWriter = new PrintWriter(outBuffer);

        engine.put("log", log);
        engine.put("base", base);
        engine.put("minMtime", minMtime);
        engine.put("maxMtime", maxMtime);
        engine.put("options", options);
        engine.put("state", null);
        engine.put("out", outWriter);
        engine.put("commit", new StringBuilder());

        InputStream _script = ClassLoader.getSystemResourceAsStream(jobName);
        if (_script == null) {
            throw new FileNotFoundException(jobName);
        }
        script = Strings.flush(_script);

        if (engine instanceof Compilable) {
            compiledScript = ((Compilable) engine).compile(script);
        } else {
            compiledScript = null;
        }
    }

    private void validateJobName(String jobName) {
        if (!jobName.matches("[a-zA-z_-]+.job.[a-zA-z_-]+")) {
            throw new IllegalArgumentException(
                    "Invalid job name '" + jobName +"'");
        }
    }

    /**
     * Call this method to update the variables {@code record}, {@code first},
     * and {@code last} in the execution context of the batch script. In normal
     * operation one iterates over a set of records and calling this method
     * followed by {@link #eval()} in each iteration.
     *
     * @param record the new record object for the batch script to operate on
     * @param first whether or not this is the first execution of the
     *              batch script
     * @param last whether or not this is the last execution of the batch script
     */
    public void setContext(Record record, boolean first, boolean last) {
        engine.put("record", record);
        engine.put("first", first);
        engine.put("last", last);
        engine.put("commit", false);
    }

    /**
     * Evaluate the batch script on the current context (set by
     * {@link #setContext(Record, boolean, boolean)})
     * @throws ScriptException if there is an error running the script
     */
    public void eval() throws ScriptException {
        if (compiledScript != null) {
            compiledScript.eval();
        } else {
            engine.eval(script);
        }
    }

    /**
     * Should be inspected after calls to {@link #eval()} in order to determine
     * if the script has requested that the record be updated in storage.
     * @return {@code true} if the script has set the variable {@code commit}
     *         to true, indicating that it requests the current record has
     *         changed and be updated in storage
     */
    public boolean shouldCommit() {
        return (Boolean) engine.get("commit");
    }

    /**
     * Returns the {@code jobName} as passed to the constructor
     * @return the name of the system resource from which the batch script
     *         was loaded
     */
    public String toString() {
        return jobName;
    }

    /**
     * Returns the {@code jobName} as passed to the constructor
     * @return the name of the system resource from which the batch script
     *         was loaded
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * Returns the file extension of {@code jobName} as passed to the
     * constructor
     * @return the file extension of the system resource from which the
     *         batch script was loaded
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Get the actual script code
     * @return the script code executed when calling {@link #eval}
     */
    public String getScript() {
        return script;
    }

    /**
     * Get the output of the script as a string. The script has access to a
     * {@link StringBuilder} in the {@code out} variable. This method simply
     * returns {@code out.toString()}.
     *
     * @return the script output as a string
     */
    public String getOutput() {
        outWriter.flush();
        return outBuffer.toString();
    }
}
