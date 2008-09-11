package dk.statsbiblioteket.summa.common.shell.commands;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.Script;

/**
 * <p>This command launches a script which is a series of commands delimited
 * by the semi-colon character ";". Semi colons can be escaped with back slahes
 * </p>
 */
public class Exec extends Command {

    public Exec() {
        super ("exec", "Execute a series of commands delimied by ';' or "
                       + " newline");
        setUsage ("exec <command> [;command...]");
    }
    
    public void invoke(ShellContext ctx) throws Exception {
        Script script = new Script(getRawCommandLine());
        script.pushToShellContext(ctx);        
    }
}



