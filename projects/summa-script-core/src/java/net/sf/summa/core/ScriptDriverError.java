package net.sf.summa.core;

/**
 * Thrown if there is an internal error in a {@link ScriptDriver}, this
 * is conisdered a fatal exception because the driver can not be expected to
 * work properly.
 *
 * @author mke
 * @since Oct 5, 2009
 */
public class ScriptDriverError extends Error {

    public ScriptDriverError() {

    }

    public ScriptDriverError(String msg) {
        super(msg);
    }

    public ScriptDriverError(String msg, Throwable cause) {
        super(msg, cause);
    }

    public ScriptDriverError(Throwable cause) {
        super(cause);
    }

}
