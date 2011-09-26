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
package dk.statsbiblioteket.summa.common.shell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Stack;

/**
 * Implements a {@link ShellContext} discarding all input and returning
 * a configurable reply to {@link #readLine} requests.
 */
public class VoidShellContext implements ShellContext {

    private static final Log log = LogFactory.getLog(VoidShellContext.class);

    private Stack<String> lineBuffer;
    private String defaultLine;
    private String lastError;

    public VoidShellContext () {
        lineBuffer = new Stack<String>();
        defaultLine = "";
        lastError = null;
    }

    public VoidShellContext (String defaultLine) {
        this.defaultLine = defaultLine;
    }

    @Override
    public void error(String msg) {
        lineBuffer.clear();
        lastError = msg;
        log.error (msg);
    }

    @Override
    public void info(String msg) {
        log.info (msg);
    }

    @Override
    public void warn(String msg) {
        log.warn (msg);
    }

    @Override
    public void debug(String msg) {
        log.debug (msg);
    }

    @Override
    public String readLine() {
        if (!lineBuffer.empty()) {
            return lineBuffer.pop();
        }

        return defaultLine;
    }

    @Override
    public void pushLine(String line) {
        lineBuffer.push(line);
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    @Override
    public void prompt(String msg) {
        log.debug("Prompt: " + msg);
    }

    @Override
    public void clear(){
      // Nothing to do.
    }
}




