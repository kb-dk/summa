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
 * A batch job can 
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

    public BatchJob(String jobName, Log log,
              String base, long minMtime, long maxMtime, QueryOptions options)
                                           throws IOException, ScriptException {
        this.jobName = jobName;
        extension = jobName.substring(jobName.lastIndexOf('.')+1);
        scriptMan = new ScriptEngineManager();
        engine = scriptMan.getEngineByExtension(extension);

        engine.put("log", log);
        engine.put("base", base);
        engine.put("minMtime", minMtime);
        engine.put("maxMtime", maxMtime);
        engine.put("options", options);
        engine.put("log", log);
        engine.put("state", null);
        engine.put("out", new StringBuilder());
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

    public void setContext(Record record, boolean first, boolean last) {
        engine.put("record", record);
        engine.put("first", first);
        engine.put("last", last);
        engine.put("commit", false);
    }

    public void eval() throws ScriptException {
        if (compiledScript != null) {
            compiledScript.eval();
        } else {
            engine.eval(script);
        }
    }

    public boolean shouldCommit() {
        return (Boolean) engine.get("commit");
    }

    public String toString() {
        return jobName;
    }

    public String getJobName() {
        return jobName;
    }

    public String getExtension() {
        return extension;
    }

    public String getScript() {
        return script;
    }

    public String getOutput() {
        return engine.get("out").toString();
    }
}
