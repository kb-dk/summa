package net.sf.summa.core;

import java.util.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import static java.util.AbstractMap.SimpleImmutableEntry;

import static net.sf.summa.core.Init.*;
import static net.sf.summa.core.Property.*;

/**
 * FIXME: Missing class docs for net.sf.summa.core.Template
 *
 * @author mke
 * @since Aug 7, 2009
 */
public class Template<E> implements Map<String,Object> {

    private static class Prop {
        Object value;
        Object defaultValue;
        String fieldName;
        String name;
        Class type;
        Access access;
        boolean mandatory;
        boolean allowNull;
    }

    protected Class<E> templateClass;
    protected String nick;
    private Map<String, Prop> props;

    Template(Class<E> cls) {
        if (cls == null) {
            throw new NullPointerException("Can not create config template " +
                                           "for class: null");
        }

        props = new HashMap<String, Prop>();
        templateClass = cls;
        parseTemplate();
        nick = cls.getName();
    }

    Template(Class<E> cls, String nick) {
        this(cls);
        this.nick = nick;
    }

    public static <K> Template<K> forClass(Class<K> cls) {
        return new Template<K>(cls);
    }

    public static <K> Template<K> forClass(Class<K> cls, String nick) {
        return new Template<K>(cls, nick);        
    }

    public Class<E> getTemplateClass() {
        return templateClass;
    }

    public String getNick() {
        return nick;
    }

    public E create() {
        E inst = null;

        try {
            inst = templateClass.newInstance();
        } catch (IllegalAccessException e) {
            throw new TemplateInstantiationError("Non-accessible no-args " +
                                                 "constructor for "
                                                 + templateClass.getName(), e);
        } catch (InstantiationException e) {
            throw new TemplateInstantiationError("Non-instantiable class "
                                                 + templateClass.getName(), e);
        }

        for (Prop p : props.values()) {
            // Assert that the property is set if it is mandatory
            if (p.value == null && p.mandatory) {
                throw new InvalidPropertyAssignment("No value assigned for "
                                                    + "mandatory property "
                                                    + p.name);
            }

            // For private fields we do a quick twiddling of the
            // accessibility flag to grant our selves access
            Field f;
            try {
                f = templateClass.getDeclaredField(p.fieldName);
            } catch (NoSuchFieldException e) {
                throw new InvalidPropertyDeclaration(
                        "No field matching property " + p.name);
            }

            boolean fieldAccessible = f.isAccessible();

            if (!fieldAccessible) {
                f.setAccessible(true);
            }

            try {
                f.set(inst, p.value);
            } catch (IllegalAccessException e) {
                throw new InvalidPropertyDeclaration(
                        "Non-accessible field for property " + p.name, e);
            }


            if (!fieldAccessible) {
                f.setAccessible(false);
            }

        }

        // Call any @Init annotated methods
        for (Method init : templateClass.getDeclaredMethods()) {
            if (init.getAnnotation(Init.class) == null) continue;

            boolean initAccesible = init.isAccessible();

            if (!initAccesible) {
                init.setAccessible(true);
            }

            try {
                if (init.getParameterTypes().length != 0) {
                    throw new TemplateInstantiationError(
                            "@Init method " + templateClass.getName() + "#"
                            + init.getName() + " must be a no-args method");
                }
                init.invoke(inst);
            } catch (InvocationTargetException e) {
                // The 'cause' is the actual exception raised by the method
                throw new TemplateInitException(e.getCause());
            } catch (IllegalAccessException e) {
                throw new TemplateInstantiationError(
                        "Failed to invoke @Init function "
                        + templateClass.getName() + "#" + init.getName(), e);
            }

            if (!initAccesible) {
                init.setAccessible(false);
            }
        }

        return inst;        
    }

    public static <K> K create(Class<K> cls, Map<String,Object> templ) {
        Template<K> t  = Template.forClass(cls);
        t.putAll(templ);
        return t.create();
    }

    public static Object create(Map<String,Object> templ, String classKey)
                                                 throws ClassNotFoundException {
        Object _cls = templ.get(classKey);
        Class cls;
        if (_cls instanceof Class) {
            cls = (Class)_cls;
        } else {
            cls = Class.forName(_cls.toString());
        }

        // FIXME: We need to mask the classKey key out of templ, ,and then use
        // the masked map to create the Template instance
        throw new UnsupportedOperationException();
    }

    /**
     * Convenience method calling {@link Template#create(Map, String)} with the
     * string "__class__" as {@code classKey} argument.
     * @param templ the map to use for template values
     * @return a new object instance created from the properties defined in
     *         {@code templ}
     * @throws ClassNotFoundException if the class referenced by the
     *                                {@code __class__} is unknown
     */
    public static Object create(Map<String,Object> templ)
                                                 throws ClassNotFoundException {
        return Template.create(templ, "__class__");
    }

    private void parseTemplate() {
        props.clear();
        for (Field field : templateClass.getDeclaredFields()) {
            Property a = field.getAnnotation(Property.class);

            if (a == null) {
                continue;
            }

            Prop p = new Prop();
            p.defaultValue = a.value();
            p.fieldName = field.getName();
            p.name = a.name();
            p.type = a.type();
            p.access = a.access();
            p.mandatory = a.mandatory();

            // See if we must convert the default value to the correct type.
            // Note that assigned values are checked on assignment time, and
            // that mandatory properties are left unset on purpose
            if (p.defaultValue instanceof String &&
                p.type != String.class &&
                ! p.mandatory) {
                // We extract the static 'valueOf(String)' method on the class
                // of the p.defaultValue member, and use that for conversion
                try {
                    Method convert = p.type.getMethod("valueOf", String.class);
                    p.defaultValue = convert.invoke(null, p.defaultValue);
                } catch (NoSuchMethodException e) {
                    throw new InvalidPropertyDeclaration(
                                 a.name() + "=" + p.defaultValue
                                 + " has no conversion method "
                                 + p.type.getSimpleName() + ".valueOf(String)");
                } catch (InvocationTargetException e) {
                    throw new InvalidPropertyDeclaration(
                                                 a.name() + "=" + p.defaultValue
                                                 + " can not be converted to a "
                                                 + p.type.getSimpleName());
                } catch (IllegalAccessException e) {
                    throw new InvalidPropertyDeclaration(
                                 a.name() + "=" + p.defaultValue
                                 + " has non-accessible conversion method "
                                 + p.type.getSimpleName() + ".valueOf(String)");
                }

            }

            // We leave mandatory properties unset to be able
            // to detect this later
            p.value = p.mandatory ? null : p.defaultValue;
            
            props.put(p.name, p);
        }
    }

    public boolean admits(String key, Object value) {
        Prop p = props.get(key);
        return p != null && p.type.isInstance(value);

    }

    public int size() {
        return props.size();
    }

    public boolean isEmpty() {
        return props.isEmpty();
    }

    public boolean containsKey(Object key) {
        return props.containsKey(key);
    }

    public boolean containsValue(Object value) {
        for (Prop p : props.values()) {
            if (value == null) {
                if (p.value == null) {
                    return true;
                } else {
                    continue;
                }
            } else if (value.equals(p.value)){
                return true;
            }
        }
        return false;
    }

    public Object get(Object key) {
        Prop p = props.get(key);
        return p == null ? null : p.value;
    }

    public Object put(String key, Object value) {
        if (!admits(key,value)) {
            throw new IllegalArgumentException("Assignment '" + key + "' = "
                                               + value
                                               + ", not valid within template");
        }

        // Store the value and returns the previous value
        // as per the Map interface contract
        Prop p = props.get(key);
        Object prev = p.value;
        p.value = value;
        return prev;
    }

    public Object remove(Object key) {
        Prop p = props.get(key.toString());

        if (p == null) {
            return null;
        }

        // Reset the value and return the old one as per Map contract
        Object prev = p.value;
        p.value = p.defaultValue;
        return prev;
    }

    public void putAll(Map<? extends String, ?> m) {
        for (Entry<? extends String,?> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public void clear() {
        for (Prop p : props.values()) {
            p.value = p.defaultValue;
        }
    }

    public Set<String> keySet() {
        return props.keySet();
    }

    public Collection<Object> values() {
        ArrayList<Object> l = new ArrayList<Object>(props.size());
        for (Prop p : props.values()) {
            l.add(p.value);
        }
        return l;
    }

    public Set<Entry<String,Object>> entrySet() {
        Set<Entry<String,Object>> entries =
                              new HashSet<Entry<String,Object>>(props.size());

        for (Entry<String,Prop> e : props.entrySet()) {
            entries.add(
                  new SimpleImmutableEntry<String,Object>(e.getKey(),
                                                          e.getValue().value));
        }
        return entries;
    }


}
