package net.sf.summa.core;

import java.lang.reflect.Type;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;

/**
 * FIXME: Missing class docs for net.sf.summa.core.Property
 *
 * @author mke
 * @since Aug 7, 2009
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Property {

    public enum Access {
        READ,
        WRITE,
        READ_WRITE
    }

    String value() default "";

    String name();

    Class type() default String.class;

    Access access() default Access.READ;

    boolean mandatory() default false;

    boolean allowNull() default false;

    //FIXME: Bounds checking

    /**
     * A property has been declared in an invalid way. This is consiedered a
     * fatal bug in the code declaraing the property, hence this throwable
     * inherits from {@code Error} and is not simply a {@code RuntimeException}
     */
    public static class InvalidPropertyDeclaration extends Error {

        public InvalidPropertyDeclaration(String msg) {
            super(msg);
        }
    }

    /**
     * Thrown when a property is assigned an invalid value or if a value
     * is missing from a mandatory property
     */
    public static class InvalidPropertyAssignment extends RuntimeException {

        public InvalidPropertyAssignment(String msg) {
            super(msg);
        }
    }
}
