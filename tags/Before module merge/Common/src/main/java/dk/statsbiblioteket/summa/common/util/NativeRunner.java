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
package dk.statsbiblioteket.summa.common.util;

import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Native command executor.
 *
 * This class is deprecated in favor of sbutil's
 * {@link dk.statsbiblioteket.util.console.ProcessRunner}
 * @deprecated
 */
// TODO: Rewrite this to use ProcessBuilder
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
@Deprecated
public class NativeRunner  {
    private InputStream processInput =   null;
    private InputStream processOutput = null;
    private InputStream processError =  null;

    /**
     * The threads that polls the output from the commands. When a thread is
     * finished, it removes itself from this list.
     */
    private final List<Thread> threads =
            Collections.synchronizedList(new LinkedList<Thread>());

    private static final int MAXINITIALBUFFER = 1000000;
    private static final int THREADTIMEOUT = 1000; // Milliseconds

    private String[] commands;
    private String[] environment;

    public NativeRunner(String command) {
        String[] commands = new String[1];
        commands[0] = command;
        setParameters(commands, null, null);
    }
    public NativeRunner(String[] commands) {
        setParameters(commands, null, null);
    }
    public NativeRunner(String[] commands, String[] environment) {
        setParameters(commands, environment, null);
    }
    public NativeRunner(String[] commands, String[] environment,
                        InputStream processInput) {
        setParameters(commands, environment, processInput);
    }

    public void setParameters(String[] commands, String[] environment,
                              InputStream processInput) {
        this.commands = commands;
        this.environment = environment;
        this.processInput = processInput;
    }

    /**
     * Execute the native commands. Standard and error output will be collected
     * and can be retrieved later by {@link #getProcessOutput} and
     * {@link #getProcessError}.
     * @return the exit value from the native commands.
     * @param maxOutput  the maximum number of bytes that should be collected
     *                   from the output of the native commands.
     * @param maxError   the maximum number of bytes that should be collected
     *                   from the error-output of the native commands.
     * @throws Exception if execution of the native commands failed.
     */
    public synchronized int execute(int maxOutput,
                                    int maxError) throws Exception {
        try {
            Process p = Runtime.getRuntime().exec(commands, environment);
            ByteArrayOutputStream pOut =
                    collectProcessOutput(p.getInputStream(), maxOutput);
            ByteArrayOutputStream pError =
                    collectProcessOutput(p.getErrorStream(), maxError);
            feedProcess(p, processInput);
            while (true) {
                try {
                    p.waitFor();
                    break;
                } catch (InterruptedException e) {
                    // Ignoring interruptions, we just want to try waiting
                    // again.
                }
            }
            waitForThreads();
            processOutput = new ByteArrayInputStream(pOut.toByteArray());
            processError = new ByteArrayInputStream(pError.toByteArray());
            return p.exitValue();
        } catch (IOException e) {
            throw new Exception("Failure while running "
                                + stringsToString(commands), e);
        }
    }

    private String stringsToString(String[] strings) {
        StringWriter sw = new StringWriter(100);
        for (String s: strings) {
            sw.append(s).append(" ");
        }
        return sw.toString();
    }

    /**
     * Wait for the polling threads to finish.
     */
    private void waitForThreads() {
        long endTime = System.currentTimeMillis() + THREADTIMEOUT;
        while (System.currentTimeMillis() < endTime && threads.size() > 0) {
            try {
                wait(10);
            } catch (InterruptedException e) {
                // Ignore, as we are just waiting
            }
        }
    }


    /**
     * Execute the native commands. Standard and error output will not be
     * collected. It is the responsibility of the caller to empty these
     * OutputStreams, which can be accessed by {@link #getProcessOutput} and
     * {@link #getProcessError}. The maxRuntime is Long.MAX_VALUE.
     * @return the exit value from the native commands.
     * @throws Exception if execution of the native commands failed.
     */
    public int executeNoCollect() throws Exception {
        return executeNoCollect(Long.MAX_VALUE);
    }

    /**
     * Execute the native commands. Standard and error output will not be
     * collected. It is the responsibility of the caller to empty these
     * OutputStreams, which can be accessed by {@link #getProcessOutput} and
     * {@link #getProcessError}.
     * @param maxRuntime the maximum number of milliseconds that the native
     *                   commands is allowed to run.
     * @return the exit value from the native commands.
     * @throws Exception if execution of the native commands failed or
     *                   timed out.
     */
    public synchronized int executeNoCollect(long maxRuntime) throws Exception {
        try {
            Process p = Runtime.getRuntime().exec(commands, environment);
            processOutput = p.getInputStream();
            processError = p.getErrorStream();
            feedProcess(p, processInput);
            long endTime =
                    System.currentTimeMillis() + maxRuntime > maxRuntime ?
                    System.currentTimeMillis() + maxRuntime : maxRuntime;
            boolean noTimeout;
            while (noTimeout = (System.currentTimeMillis() < endTime)) {
                try {
                    p.waitFor(); // TODO: Make timeout-thingie
                    break;
                } catch (InterruptedException e) {
                    // Ignoring interruptions, we just want to try waiting
                    // again.
                }
            }
            if (noTimeout) {
                return p.exitValue();
            }
            throw new Exception("Thread timed out");
        } catch (IOException e) {
            throw new Exception("Failure while running "
                                + stringsToString(commands), e);
        }
    }

    /**
     * The OutputStream will either be the OutputStream directly from the
     * execution of the native commands with the method {@link #executeNoCollect}
     * or a cache with the output of the execution of the native commands by
     * {@link #execute}.
     * @return the output of the native commands.
     */
    public InputStream getProcessOutput() {
        return processOutput;
    }
    /**
     * The OutputStream will either be the error-OutputStream directly from the
     * execution of the native commands with the method {@link #executeNoCollect}
     * or a cache with the error-output of the execution of the native commands
     * by {@link #execute}.
     * @return the error-output of the native commands.
     */
    public InputStream getProcessError() {
        return processError;
    }

    public ByteArrayOutputStream collectProcessOutput(
            final InputStream inputStream, final int maxCollect) {
        final ByteArrayOutputStream stream =
                new ByteArrayOutputStream(Math.min(MAXINITIALBUFFER,
                                                   maxCollect));
        Thread t = new Thread() {
            public void run() {
                try {
                    InputStream reader = null;
                    OutputStream writer = null;
                    try {
                        reader = new BufferedInputStream(inputStream);
                        writer = new BufferedOutputStream(stream);
                        int c;
                        int counter = 0;
                        while ((c = reader.read()) != -1) {
                            counter++;
                            if (counter < maxCollect) {
                                writer.write(c);
                            }
                        }
                    } finally {
                        if (reader != null) {
                            reader.close();
                        }
                        if (writer != null) {
                            writer.close();
                        }
                    }
                } catch (IOException e) {
                    // This seems ugly
                    throw new RuntimeException("Couldn't read output from " +
                                               "process.", e);
                }
                threads.remove(this);
            }
        };
        threads.add(t);
        t.start();
        return stream;
    }

    private void feedProcess(final Process process,
                             InputStream processInput) {
        if (processInput == null) {
            // No complaints here - null just means no input
            return;
        }
        final OutputStream pIn = process.getOutputStream();
        final InputStream given = processInput;
        new Thread() {
            public void run() {
                try {
                    OutputStream writer = null;
                    try {
                        writer = new BufferedOutputStream(pIn);
                        int c;
                        while ((c = given.read()) != -1) {
                            writer.write(c);
                        }
                    } finally {
                        if (writer != null) {
                            writer.close();
                        }
                        pIn.close();
                    }
                } catch (IOException e) {
                    // This seems ugly
                    throw new RuntimeException("Couldn't write input to " +
                                               "process.", e);
                }
            }
        }.start();
    }

    public String getProcessOutputAsString() {
        return getStringContent(getProcessOutput());
    }
    public String getProcessErrorAsString() {
        return getStringContent(getProcessError());
    }

    private String getStringContent(InputStream stream) {
        if (stream == null) {
            return null;
        }
        BufferedInputStream in = new BufferedInputStream(stream, 1000);
        StringWriter sw = new StringWriter(1000);
        int c;
        try {
            while ((c = in.read()) != -1) {
                sw.append((char)c);
            }
            return sw.toString();
        } catch (IOException e) {
            return "Could not transform content of stream to String";
        }

    }
}




