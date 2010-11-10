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

import dk.statsbiblioteket.summa.common.shell.commands.Exec;
import dk.statsbiblioteket.summa.common.shell.notifications.SyntaxErrorNotification;
import dk.statsbiblioteket.util.qa.QAInfo;
import jline.ConsoleReader;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.Iterator;
import java.util.Stack;
import java.util.Arrays;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class ShellTest extends TestCase {
    Core core;
    ShellContext ctx;

    @Override
    public void setUp () throws Exception {        
        ctx = new ShellContext () {
            private Stack<String> lineBuffer = new Stack<String>();
            private ConsoleReader lineIn = null; ///createConsoleReader();
            private String lastError = null;

            @Override
            public void error(String msg) {
                lineBuffer.clear();
                lastError = msg;
                //System.out.println ("[ERROR] " + msg);
            }
            // TODO should add msg to lineBuffer and test on this one
            @Override
            public void info(String msg) {
                //System.out.println(msg);
            }
            @Override
            public void warn(String msg) {
                //System.out.println("[WARNING] " + msg);
            }
            @Override
            public void debug(String msg) {
                //System.out.println("[DEBUG] " + msg);
            }
            @Override
            public String readLine() {
                if (!lineBuffer.empty()) {
                    return lineBuffer.pop();
                }

                try {
                    return lineIn.readLine().trim();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read input", e);
                }
            }
            @Override
            public void pushLine(String line) {
                lineBuffer.push(line.trim());
            }
            @Override
            public String getLastError() {
                return lastError;
            }
            @Override
            public void prompt(String prompt) {
                //System.out.print(prompt);
            }

            @Override
            public void clear() {
              try {
                lineIn.clearScreen();
              } catch(IOException e) {
                error("clearing screen");
              }
            }
            };

        core = new Core(ctx, true, true);
    }

    @SuppressWarnings("unused")
    private static ConsoleReader createConsoleReader() {
        try {
            return new ConsoleReader();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create ConsoleReader: "
                                       + e.getMessage(), e);
        }
    }

    @Override
    public void tearDown () throws Exception {
        super.tearDown();
    }

    public void testTokenizer () throws Exception {
        String[] result;
        String cmd;

        cmd = "foo";
        result = core.tokenize(cmd);
        assertTrue(Arrays.equals(result, new String[]{"foo"}));

        cmd = "foo bar";
        result = core.tokenize(cmd);
        assertTrue(Arrays.equals(result, new String[]{"foo", "bar"}));

        cmd = "foo \"bar\"";
        result = core.tokenize(cmd);
        assertTrue(Arrays.equals(result, new String[]{"foo", "bar"}));

        cmd = "\"foo\" bar";
        result = core.tokenize(cmd);
        assertTrue(Arrays.equals(result, new String[]{"foo", "bar"}));

        cmd = "\"foo bar\"";
        result = core.tokenize(cmd);
        assertTrue(Arrays.equals(result, new String[]{"foo bar"}));

        cmd = "\"foo bar\" baz";
        result = core.tokenize(cmd);
        assertTrue(Arrays.equals(result, new String[]{"foo bar", "baz"}));

        cmd = "\"foo    bar\"   baz   ";
        result = core.tokenize(cmd);
        assertTrue(Arrays.equals(result, new String[]{"foo    bar", "baz"}));

        cmd = "\"foo    bar\"   \"baz bug\"";
        result = core.tokenize(cmd);
        assertTrue(Arrays.equals(result, new String[]{"foo    bar", "baz bug"}));
    }

    public void testTokenizerErrors () throws Exception {
        String cmd;

        cmd = "\"foo";
        try {
            core.tokenize(cmd);
            fail("Tokenizing '" + cmd + "' should raise an exception");
        } catch (SyntaxErrorNotification e) {
            // success
        }

        cmd = "foo\"";
        try {
            core.tokenize(cmd);
            fail("Tokenizing '" + cmd + "' should raise an exception");
        } catch (SyntaxErrorNotification e) {
            // success
        }

        cmd = "f\"oo";
        try {
            core.tokenize(cmd);
            fail("Tokenizing '" + cmd + "' should raise an exception");
        } catch (SyntaxErrorNotification e) {
            // success
        }
    }

    public void testScript () throws Exception {
        Script sc;
        String[] stmts;
        Iterator<String> iter;

        // Check semi-colon delimiting
        sc = new Script("foo; bar");
        stmts = new String[] {"foo", "bar"};
        iter = sc.iterator();
        for (String stmt : stmts) {
            assertEquals(stmt, iter.next());
        }

        sc = new Script("  foo  ;    bar");
        stmts = new String[] {"foo", "bar"};
        iter = sc.iterator();
        for (String stmt : stmts) {
            assertEquals(stmt, iter.next());
        }

        // Check compact layout and trailing ;
        sc = new Script("foo;bar;");
        stmts = new String[] {"foo", "bar"};
        iter = sc.iterator();
        for (String stmt : stmts) {
            assertEquals(stmt, iter.next());
        }

        // Check newline delimiting
        sc = new Script("foo\nbar");
        stmts = new String[] {"foo", "bar"};
        iter = sc.iterator();
        for (String stmt : stmts) {
            assertEquals(stmt, iter.next());
        }

        // Three statements and a trailing \n
        sc = new Script("foo\nbar\nbaz\n");
        stmts = new String[] {"foo", "bar", "baz"};
        iter = sc.iterator();
        for (String stmt : stmts) {
            assertEquals(stmt, iter.next());
        }

        // Check escapes
        sc = new Script("foo  \\;    bar");
        stmts = new String[] {"foo  ;    bar"};
        iter = sc.iterator();
        for (String stmt : stmts) {
            assertEquals(stmt, iter.next());
        }

        sc = new Script("foo  \\n    bar");
        stmts = new String[] {"foo  \n    bar"};
        iter = sc.iterator();
        for (String stmt : stmts) {
            assertEquals(stmt, iter.next());
        }

        // Empty scripts 
        sc = new Script("\n");
        iter = sc.iterator();
        assertFalse(iter.hasNext());

        sc = new Script(";");
        iter = sc.iterator();
        //noinspection UnusedDeclaration
        for (String s : sc) {
            // Do nothing
        }
        assertFalse(iter.hasNext());
    }

    public void testScriptTokens () throws Exception {
        Script sc;

        sc = new Script(new String[] {"foo", "-w"});
        assertEquals("Should have exactly one statement",
                     1, sc.getStatements().size());
        assertEquals("Should have the statement 'foo -w'",
                     "foo -w", sc.getStatements().get(0));

        sc = new Script(new String[] {"foo", "-w"}, 1);
        assertEquals("Should have exactly one statement",
                     1, sc.getStatements().size());
        assertEquals("Should have the statement '-w'",
                     "-w", sc.getStatements().get(0));
    }

    public void testShellContextPushLine () throws Exception {
        ctx.pushLine("foo");
        ctx.pushLine("bar\n");
        ctx.pushLine("baz;quiz");

        String line = ctx.readLine();
        assertEquals(line, "baz;quiz");

        line = ctx.readLine();
        assertEquals(line, "bar");

        line = ctx.readLine();
        assertEquals(line, "foo");
    }

    public void testExecCommand () throws Exception {
        new Exec();
        Script script = new Script("foo -s; bar -f 'quiz//'; baz");

        script.pushToShellContext(ctx);

        String line = ctx.readLine();
        assertEquals(line, "foo -s");

        line = ctx.readLine();
        assertEquals(line, "bar -f 'quiz//'");

        line = ctx.readLine();
        assertEquals(line, "baz");
    }

    public void testNonInteractiveQuit () throws Exception {
        String script = "quit";
        Core core = new Core (ctx, true, true);
        int exitCode = core.run(new Script(script));
        
        assertEquals("Issuing the script '" + script
                     + "' should return with code 0", 0, exitCode);
    }

    public void testAliases() throws Exception {
        String script = "exit";
        Core core = new Core (ctx, true, true);
        int exitCode = core.run(new Script(script));

        assertEquals("Issuing the script '" + script
                     + "' should return with code 0", 0, exitCode);
    }

    public void testNonInteractiveHelpQuit () throws Exception {
        String script = "help;quit";
        Core core = new Core (ctx, true, true);
        int exitCode = core.run(new Script(script));

        assertEquals("Issuing the script '" + script
                     + "' should return with code 0", 0, exitCode);
    }

    public void testNonInteractiveBadCommand () throws Exception {
        String script = "baaazooo -w --pretty; help; quit";
        Core core = new Core (ctx, true, true);
        int exitCode = core.run(new Script(script));

        assertEquals("Issuing the script '" + script
                     + "' should return with code -1", -1, exitCode);
    }

    public void testNonInteractiveBadSwitch () throws Exception {
        String script = "help -boogawoo; help; quit";
        Core core = new Core (ctx, true, true);
        int exitCode = core.run(new Script(script));

        assertEquals("Issuing the script '" + script
                     + "' should return with code -4", -4, exitCode);
    }

    public static void main (String[] args) {
        ShellTest test = new ShellTest();
        try {
            test.setUp();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }

        Core core = new Core (test.ctx, true, true);
        int exitCode = core.run(null);

        System.exit(exitCode);
    }
}