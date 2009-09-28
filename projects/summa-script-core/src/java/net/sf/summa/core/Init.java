package net.sf.summa.core;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * FIXME: Missing class docs for net.sf.summa.core.Init
 *
 * @author mke
 * @since Sep 25, 2009
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Init {
    
    /**
     * Thrown if there is an exception raised during the invocation of
     * template init functions marked with the {@link net.sf.summa.core.Init} annotation.
     *
     * @author mke
     * @since Sep 25, 2009
     */
    public static class TemplateInitException extends RuntimeException {

        public TemplateInitException() {

        }

        public TemplateInitException(String msg) {
            super(msg);
        }

        public TemplateInitException(String msg, Throwable cause) {
            super(msg, cause);
        }

        public TemplateInitException(Throwable cause) {
            super(cause);
        }

    }
}
