package dk.statsbiblioteket.summa.common.shell;

import dk.statsbiblioteket.util.Strings;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * An abstract representation of a shell script - a series of commands
 * running in a shell {@link Core}.
 */
public class Script implements Iterable<String>{

    private String script;
    private List<String> statements;

    public Script(String script) {
        if (script == null) {
            throw new NullPointerException("Can not instantiate Script from"
                                           + " null-string");
        }

        this.script = script;

        /* Split on newlines and semi-colon, but allow these to be
         * escaped with a backslash */
        String[] statements_a;
        statements_a = script.split("(?<=[^\\\\])[\\n;]");

        /* Trim the statements and unescape \\n and \; */
        statements = new ArrayList<String>(statements_a.length);
        for (String stmt : statements_a) {
            stmt = stmt.trim().replace("\\;", ";").replace("\\n", "\n");
            if (!stmt.equals("") && !stmt.equals(";") && !stmt.equals("\n")) {
                statements.add (stmt);
            }
        }
    }

    /**
     * Concatenate all strings in {@code tokens} delimited by spaces and
     * interpret it as a script.
     * <p/>
     * This is useful if you want to pass the arguments from your {@code main}
     * method to a {@code Script}.
     *
     * @param tokens collection of tokens that should comprise the statements
     *               in the script
     */
    public Script(String[] tokens) {
        this(Strings.join(tokens, " "));
    }

    /**
     * As {@link #Script(String[])} but only start the script from
     * {@code offset} into the {@code tokens} array
     * @param tokens array of tokens to build the script statements from
     * @param offset the offset into {@code tokens} to start from
     */
    public Script (String[] tokens, int offset) {
        this(Arrays.copyOfRange(tokens, offset, tokens.length,
                                           new String[0].getClass()));
    }

    public Iterator<String> iterator() {
        return statements.iterator();
    }

    /**
     * Get a list of all statements declared in the script.
     * @return A list of strings. The strings will not be newline-terminated
     */
    public List<String> getStatements () {
        return statements;
    }

    /**
     * Push the statements in this script to a {@link ShellContext}. The
     * statements will be pushed in reverse order, so that they will read
     * in the correct order when you call {@link ShellContext#readLine()}.
     *
     * If the {@code ShellContext} is owned by a shell {@link Core}
     * the core will execute the statements automatically.
     *
     * @param ctx the shell context to push the statements to
     */
    public void pushToShellContext (ShellContext ctx) {
        /* Iterate through the statements in reverse
        * to make sure we push the lines to the context
        * in the correct order*/
        for (int i = statements.size() - 1; i >= 0; i--) {
            ctx.pushLine(statements.get(i));
        }
    }
}



