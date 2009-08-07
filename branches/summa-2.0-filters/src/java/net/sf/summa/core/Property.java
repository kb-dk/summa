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
}
