package dk.statsbiblioteket.summa.common.shell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Stack;

/**
 * Implements a {@link ShellContext} discarding all input and returning
 * a configurable reply to {@link #readLine} requests.
 */
public class VoidShellContext implements ShellContext {

    private static final Log log = LogFactory.getLog(VoidShellContext.class);

    private Stack<String> lineBuffer;
    private String defaultLine;

    public VoidShellContext () {
        lineBuffer = new Stack<String>();
        defaultLine = "";
    }

    public VoidShellContext (String defaultLine) {
        this.defaultLine = defaultLine;
    }

    public void error(String msg) {
        log.error (msg);
    }

    public void info(String msg) {
        log.info (msg);
    }

    public void warn(String msg) {
        log.warn (msg);
    }

    public void debug(String msg) {
        log.debug (msg);
    }

    public String readLine() {
        if (!lineBuffer.empty()) {
            return lineBuffer.pop();
        }

        return defaultLine;
    }

    public void pushLine(String line) {
        lineBuffer.push(line);
    }

    public void prompt(String msg) {
        log.debug("Prompt: " + msg);
    }
}



