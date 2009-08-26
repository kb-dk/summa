package net.sf.summa.core;

/**
 * Thrown when a programming error or mismatching libraries result in
 * a non-instantiatable {@link Template}.
 * <p/>
 * This error is considered a fatal and the program can not be expected to
 * run properly. This is also why it is an {@code Error} and not an
 * {@code Exception}.
 */
public class TemplateInstantiationError extends Error {

    public TemplateInstantiationError (String msg) {
        super(msg);
    }

    public TemplateInstantiationError (String msg, Throwable cause) {
        super(msg, cause);
    }
}
