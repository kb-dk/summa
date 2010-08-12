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

import jline.ConsoleReader;
import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Silent shell context, used for running one or more command(s) in a silent
 * mode.
 *
 * @author Henrik Kirk <mailto:hbk@statsbiblioteket.dk>
 * @since Aug 12, 2010
 */
public class ShellContextImplTest extends TestCase {

    ConsoleReader consoleReader;
    PrintStream outputStream;
    ByteArrayOutputStream byteArrayOutput;

    final String ERROR = "[ERROR] ";
    final String WARN = "[WARN] ";
    final String DEBUG = "[DEBUG] ";
    final String PROMPT = ">";
    final String MESSAGE = "test message";
    final String CR = "\n";

    @Override
    public void setUp() {
        try {
            consoleReader = new ConsoleReader();
        } catch (Exception e) {
            fail("Exception thrown when creating ConsoleReader");
        }
        byteArrayOutput = new ByteArrayOutputStream();
        outputStream = new PrintStream(byteArrayOutput);

    }

    public void testErrors() {
        ShellContext context = new ShellContextImpl(consoleReader,
                                              outputStream, outputStream, true);
        context.error(MESSAGE);
        assertEquals(ERROR + MESSAGE + CR, byteArrayOutput.toString());
    }

    public void testGetLastError() {
        ShellContext context = new ShellContextImpl(consoleReader,
                                              outputStream, outputStream, true);
        context.error(MESSAGE);
        assertEquals(MESSAGE, context.getLastError());
        context.info("New message");
        assertEquals(MESSAGE, context.getLastError());        
    }

    public void testWarn() {
        ShellContext context = new ShellContextImpl(consoleReader,
                                              outputStream, outputStream, true);
        context.warn(MESSAGE);
        assertEquals(WARN + MESSAGE + CR, byteArrayOutput.toString());
    }

    public void testPrompt() {
        ShellContext context = new ShellContextImpl(consoleReader,
                                              outputStream, outputStream, true);
        context.prompt(PROMPT);
        assertEquals(PROMPT, byteArrayOutput.toString());
    }

    public void testinfo() {
        ShellContext context = new ShellContextImpl(consoleReader,
                                              outputStream, outputStream, true);
        context.info(MESSAGE);
        assertEquals(MESSAGE + CR, byteArrayOutput.toString());
    }

    public void testDebug() {
        ShellContext context = new ShellContextImpl(consoleReader,
                                              outputStream, outputStream, true);
        context.debug(MESSAGE);
        assertEquals(DEBUG + MESSAGE + CR, byteArrayOutput.toString());
        byteArrayOutput.reset();
        context = new ShellContextImpl(consoleReader,
                                       outputStream, outputStream, false);
        context.debug(MESSAGE);
        assertEquals("", byteArrayOutput.toString());
    }

    public void testDifferentStreams() {
        ByteArrayOutputStream errorArray = new ByteArrayOutputStream();
        PrintStream errorStream = new PrintStream(errorArray);
        ShellContext context = new ShellContextImpl(consoleReader, errorStream,
                outputStream, true);

        context.error(MESSAGE);
        assertEquals("",byteArrayOutput.toString());
        assertEquals(ERROR + MESSAGE + CR, errorArray.toString());
    }

    public void testClearScreen() {
        ShellContext context = new ShellContextImpl(consoleReader, outputStream,
                        outputStream, true);
        context.info(MESSAGE);
        try {
            context.clear();
        } catch(Exception e) {
            fail("Exception should not be thrown when clearing screen");
        }

    }
}
